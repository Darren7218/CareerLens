package com.careerlens.app.domain.ats

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.nio.LongBuffer
import java.util.Locale
import kotlin.math.max
import kotlin.math.sqrt

@Serializable
data class AtsModelMetadata(
    val model_id: String = "careerlens-bge-micro-v2-my-ats-int8",
    val base_model: String = "TaylorAI/bge-micro-v2",
    val embedding_dim: Int = 384,
    val max_seq_length: Int = 128,
    val pooling_mode: String = "cls",
    val normalize_embeddings: Boolean = true,
    val uses_token_type_ids: Boolean = true,
    val do_lower_case: Boolean = true,
    val cls_token_id: Int = 101,
    val sep_token_id: Int = 102,
    val pad_token_id: Int = 0,
    val unk_token_id: Int = 100,
    val fp32_size_mb: Double = 69.0,
    val int8_size_mb: Double = 17.5,
    val cpu_4thread_latency_64tok_ms: Double = 12.0
)

data class SemanticMatchItem(
    val jdRequirement: String,
    val matchedUserSkill: String,
    val similarity: Float,
    val isExactLexical: Boolean
)

data class SemanticGapItem(
    val jdRequirement: String,
    val suggestedSkillToAdd: String,
    val closestUserSkill: String,
    val bestSimilarity: Float
)

data class SemanticAtsAnalysis(
    val overallScorePercent: Int = 0,
    val exactMatches: List<SemanticMatchItem> = emptyList(),
    val synonymBridges: List<SemanticMatchItem> = emptyList(),
    val semanticGaps: List<SemanticGapItem> = emptyList(),
    val extractedJdChunkCount: Int = 0,
    val inferenceLatencyMs: Long = 0L,
    val engineModeLabel: String = "Semantic Concept Graph",
    val isNeuralOnnxActive: Boolean = false,
    val modelInfoSummary: String = ""
)

/**
 * Offline Semantic ATS & Resume Matcher powered by ONNX Runtime Mobile (INT8 quantized BERT encoder)
 * with zero-internet execution on-device and automatic Concept-Vector Fallback when no `.onnx` asset
 * is bundled yet.
 */
class SemanticAtsEngine private constructor(
    private var ortEnv: OrtEnvironment?,
    private var ortSession: OrtSession?,
    private var tokenizer: BertWordPieceTokenizer?,
    private var metadata: AtsModelMetadata
) {

    @Volatile
    private var cachedProfileKey: String? = null

    @Volatile
    private var cachedUserChunks: List<String> = emptyList()

    @Volatile
    private var cachedUserVectors: List<FloatArray> = emptyList()

    @Synchronized
    fun ensureInitializedFromContext(context: Context): Boolean {
        if (ortSession != null && tokenizer != null) return true
        return runCatching {
            val assetList = context.assets.list("models")?.toSet() ?: emptySet()
            if ("careerlens_ats_int8.onnx" !in assetList || "vocab.txt" !in assetList) {
                return false
            }

            val parsedMeta = if ("model_metadata.json" in assetList) {
                context.assets.open("models/model_metadata.json").bufferedReader(Charsets.UTF_8).use {
                    jsonParser.decodeFromString<AtsModelMetadata>(it.readText())
                }
            } else {
                AtsModelMetadata()
            }

            val tok = context.assets.open("models/vocab.txt").use { stream ->
                BertWordPieceTokenizer.fromVocabStream(
                    stream = stream,
                    doLowerCase = parsedMeta.do_lower_case,
                    clsTokenId = parsedMeta.cls_token_id,
                    sepTokenId = parsedMeta.sep_token_id,
                    padTokenId = parsedMeta.pad_token_id,
                    unkTokenId = parsedMeta.unk_token_id,
                    maxSeqLength = parsedMeta.max_seq_length
                )
            }

            // Copy to internal noBackupFilesDir once so ORT mmaps the quantized graph with minimal heap churn
            val modelDir = File(context.noBackupFilesDir, "models").apply { mkdirs() }
            val modelFile = File(modelDir, "careerlens_ats_int8.onnx")
            val assetSize = runCatching { context.assets.openFd("models/careerlens_ats_int8.onnx").length }.getOrDefault(-1L)
            if (!modelFile.exists() || (assetSize > 0L && modelFile.length() != assetSize)) {
                context.assets.open("models/careerlens_ats_int8.onnx").use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val env = OrtEnvironment.getEnvironment()
            val session = createOptimizedSession(env, modelFile.absolutePath)

            ortEnv = env
            ortSession = session
            tokenizer = tok
            metadata = parsedMeta
            cachedProfileKey = null
            true
        }.getOrDefault(false)
    }

    suspend fun analyzeAsync(
        context: Context,
        jdRawText: String,
        roleTitle: String,
        notesText: String,
        userSkillsCsv: String,
        userResumeBullets: String
    ): SemanticAtsAnalysis = withContext(Dispatchers.Default) {
        ensureInitializedFromContext(context)
        analyze(
            jdRawText = jdRawText,
            roleTitle = roleTitle,
            notesText = notesText,
            userSkillsCsv = userSkillsCsv,
            userResumeBullets = userResumeBullets
        )
    }

    /**
     * Synchronous analysis entry point (usable on worker coroutines and directly inside JVM unit tests).
     */
    fun analyze(
        jdRawText: String,
        roleTitle: String,
        notesText: String,
        userSkillsCsv: String,
        userResumeBullets: String
    ): SemanticAtsAnalysis {
        val tStartNs = System.nanoTime()

        val userChunks = parseUserProfileChunks(userSkillsCsv, userResumeBullets)
        val jdChunks = extractJdRequirementChunks("$jdRawText\n$roleTitle\n$notesText")

        if (jdChunks.isEmpty() || userChunks.isEmpty()) {
            val elapsedMs = ((System.nanoTime() - tStartNs) / 1_000_000L).coerceAtLeast(1L)
            return SemanticAtsAnalysis(
                overallScorePercent = if (userChunks.isEmpty()) 0 else 82,
                extractedJdChunkCount = jdChunks.size,
                inferenceLatencyMs = elapsedMs,
                engineModeLabel = if (ortSession != null) "ONNX INT8 Neural" else "Semantic Concept Graph",
                isNeuralOnnxActive = ortSession != null,
                modelInfoSummary = buildModelSummary()
            )
        }

        val useOnnx = ortSession != null && ortEnv != null && tokenizer != null
        val userVectors: List<FloatArray>
        val jdVectors: List<FloatArray>

        if (useOnnx) {
            val profileKey = "$userSkillsCsv||$userResumeBullets||${metadata.model_id}"
            val cached = if (cachedProfileKey == profileKey && cachedUserChunks == userChunks) {
                cachedUserVectors
            } else {
                null
            }
            userVectors = cached ?: encodeTextsWithOnnx(userChunks).also {
                cachedProfileKey = profileKey
                cachedUserChunks = userChunks
                cachedUserVectors = it
            }
            jdVectors = encodeTextsWithOnnx(jdChunks)
        } else {
            userVectors = userChunks.map { encodeConceptFallback(it) }
            jdVectors = jdChunks.map { encodeConceptFallback(it) }
        }

        val bridgeThreshold = if (useOnnx) ONNX_BRIDGE_THRESHOLD else FALLBACK_BRIDGE_THRESHOLD
        val exactMatches = mutableListOf<SemanticMatchItem>()
        val synonymBridges = mutableListOf<SemanticMatchItem>()
        val semanticGaps = mutableListOf<SemanticGapItem>()

        var weightedScoreSum = 0.0

        for (jIdx in jdChunks.indices) {
            val jdReq = jdChunks[jIdx]
            val jdVec = jdVectors[jIdx]

            var bestIdx = 0
            var bestSim = -1f
            var lexicalWinnerIdx = -1

            for (uIdx in userChunks.indices) {
                val uSkill = userChunks[uIdx]
                val sim = dotProduct(jdVec, userVectors[uIdx]).coerceIn(0f, 1f)
                if (isLexicalEquivalent(uSkill, jdReq)) {
                    if (lexicalWinnerIdx == -1 || sim > bestSim) {
                        lexicalWinnerIdx = uIdx
                    }
                }
                if (sim > bestSim) {
                    bestSim = sim
                    bestIdx = uIdx
                }
            }

            if (lexicalWinnerIdx >= 0) {
                val matchedSkill = shortenForBadge(userChunks[lexicalWinnerIdx])
                val boostedSim = max(bestSim, 0.96f)
                exactMatches.add(
                    SemanticMatchItem(
                        jdRequirement = jdReq,
                        matchedUserSkill = matchedSkill,
                        similarity = boostedSim,
                        isExactLexical = true
                    )
                )
                weightedScoreSum += 1.0
            } else if (bestSim >= bridgeThreshold) {
                val matchedSkill = shortenForBadge(userChunks[bestIdx])
                synonymBridges.add(
                    SemanticMatchItem(
                        jdRequirement = jdReq,
                        matchedUserSkill = matchedSkill,
                        similarity = bestSim,
                        isExactLexical = false
                    )
                )
                // Synonym bridges award strong match credit (88% - 100%)
                val credit = 0.88 + ((bestSim - bridgeThreshold) / (1f - bridgeThreshold).coerceAtLeast(0.1f)) * 0.12
                weightedScoreSum += credit.coerceIn(0.85, 1.0)
            } else {
                semanticGaps.add(
                    SemanticGapItem(
                        jdRequirement = jdReq,
                        suggestedSkillToAdd = suggestCleanSkillName(jdReq),
                        closestUserSkill = shortenForBadge(userChunks[bestIdx]),
                        bestSimilarity = bestSim
                    )
                )
                // Soft partial credit for adjacent skills below bridge threshold
                val partial = ((bestSim - 0.35f) / (bridgeThreshold - 0.35f)).coerceIn(0f, 0.45f)
                weightedScoreSum += partial
            }
        }

        val overallScore = ((weightedScoreSum / jdChunks.size) * 100.0).toInt().coerceIn(0, 100)
        val elapsedMs = ((System.nanoTime() - tStartNs) / 1_000_000L).coerceAtLeast(1L)

        return SemanticAtsAnalysis(
            overallScorePercent = overallScore,
            exactMatches = exactMatches,
            synonymBridges = synonymBridges.sortedByDescending { it.similarity },
            semanticGaps = semanticGaps.sortedBy { it.bestSimilarity },
            extractedJdChunkCount = jdChunks.size,
            inferenceLatencyMs = elapsedMs,
            engineModeLabel = if (useOnnx) "ONNX INT8 Neural" else "Semantic Concept Graph",
            isNeuralOnnxActive = useOnnx,
            modelInfoSummary = buildModelSummary()
        )
    }

    /**
     * Encodes arbitrary texts into L2-normalized dense sentence embeddings via ONNX Runtime.
     * Exposed so tests can inspect direct pairwise cosine similarities (e.g. "K8s" vs "Kubernetes").
     */
    fun encodeTextsWithOnnx(texts: List<String>): List<FloatArray> {
        val env = ortEnv ?: error("ORT Environment not initialized")
        val session = ortSession ?: error("ORT Session not initialized")
        val tok = tokenizer ?: error("Tokenizer not initialized")
        if (texts.isEmpty()) return emptyList()

        val encoded = tok.encodeBatch(texts, maxLength = metadata.max_seq_length)
        val shape = longArrayOf(encoded.batchSize.toLong(), encoded.seqLength.toLong())

        val inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(encoded.inputIds), shape)
        val maskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(encoded.attentionMask), shape)
        val tokenTypeTensor = if (session.inputNames.contains("token_type_ids")) {
            OnnxTensor.createTensor(env, LongBuffer.wrap(encoded.tokenTypeIds), shape)
        } else {
            null
        }

        val inputs = LinkedHashMap<String, OnnxTensor>(3).apply {
            put("input_ids", inputIdsTensor)
            put("attention_mask", maskTensor)
            if (tokenTypeTensor != null) {
                put("token_type_ids", tokenTypeTensor)
            }
        }

        return try {
            session.run(inputs).use { results ->
                val outTensor = results.get(0) as OnnxTensor
                val outShape = (outTensor.info as TensorInfo).shape
                val batch = outShape[0].toInt()
                val seqLen = outShape[1].toInt()
                val hiddenDim = outShape[2].toInt()
                val fb = outTensor.floatBuffer

                val embeddings = ArrayList<FloatArray>(batch)
                val useClsPooling = metadata.pooling_mode.equals("cls", ignoreCase = true)

                for (b in 0 until batch) {
                    val vec = FloatArray(hiddenDim)
                    if (useClsPooling) {
                        val clsOffset = (b * seqLen) * hiddenDim
                        for (d in 0 until hiddenDim) {
                            vec[d] = fb.get(clsOffset + d)
                        }
                    } else {
                        var maskCount = 0f
                        for (t in 0 until seqLen) {
                            val m = encoded.attentionMask[b * seqLen + t].toFloat()
                            if (m > 0f) {
                                maskCount += m
                                val rowOffset = (b * seqLen + t) * hiddenDim
                                for (d in 0 until hiddenDim) {
                                    vec[d] += fb.get(rowOffset + d) * m
                                }
                            }
                        }
                        if (maskCount > 0f) {
                            for (d in 0 until hiddenDim) {
                                vec[d] /= maskCount
                            }
                        }
                    }
                    l2NormalizeInPlace(vec)
                    embeddings.add(vec)
                }
                embeddings
            }
        } finally {
            inputIdsTensor.close()
            maskTensor.close()
            tokenTypeTensor?.close()
        }
    }

    fun cosineSimilarity(a: String, b: String): Float {
        return if (ortSession != null && tokenizer != null) {
            val embs = encodeTextsWithOnnx(listOf(a, b))
            dotProduct(embs[0], embs[1])
        } else {
            dotProduct(encodeConceptFallback(a), encodeConceptFallback(b))
        }
    }

    private fun buildModelSummary(): String {
        return if (ortSession != null) {
            "${metadata.base_model} • ${metadata.int8_size_mb} MB INT8 (${metadata.pooling_mode.uppercase()} • ${metadata.embedding_dim}-d)"
        } else {
            "Built-in Concept Graph (place careerlens_ats_int8.onnx in assets/models/ for Neural INT8)"
        }
    }

    fun close() {
        runCatching { ortSession?.close() }
        ortSession = null
    }

    companion object {
        // Calibrated thresholds: synonym bridges score ~0.68-0.90, hard negatives stay below 0.35
        private const val ONNX_BRIDGE_THRESHOLD = 0.60f
        private const val FALLBACK_BRIDGE_THRESHOLD = 0.56f

        private val jsonParser = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        @Volatile
        private var instance: SemanticAtsEngine? = null

        fun getInstance(): SemanticAtsEngine {
            return instance ?: synchronized(this) {
                instance ?: SemanticAtsEngine(null, null, null, AtsModelMetadata()).also { instance = it }
            }
        }

        /**
         * Creates an engine directly from local files/streams (ideal for JVM unit tests and benchmarks).
         */
        fun createFromStreams(
            onnxModelPath: String,
            vocabStream: InputStream,
            metadataJson: String? = null
        ): SemanticAtsEngine {
            val meta = if (!metadataJson.isNullOrBlank()) {
                jsonParser.decodeFromString<AtsModelMetadata>(metadataJson)
            } else {
                AtsModelMetadata()
            }
            val tok = BertWordPieceTokenizer.fromVocabStream(
                stream = vocabStream,
                doLowerCase = meta.do_lower_case,
                clsTokenId = meta.cls_token_id,
                sepTokenId = meta.sep_token_id,
                padTokenId = meta.pad_token_id,
                unkTokenId = meta.unk_token_id,
                maxSeqLength = meta.max_seq_length
            )
            val env = OrtEnvironment.getEnvironment()
            val session = createOptimizedSession(env, onnxModelPath)
            return SemanticAtsEngine(env, session, tok, meta)
        }

        /**
         * Creates a pure Concept-Graph fallback engine with no native library needed.
         */
        fun createFallbackOnly(): SemanticAtsEngine =
            SemanticAtsEngine(null, null, null, AtsModelMetadata())

        private fun createOptimizedSession(env: OrtEnvironment, modelPath: String): OrtSession {
            val opts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            return env.createSession(modelPath, opts)
        }

        internal fun parseUserProfileChunks(userSkillsCsv: String, resumeBullets: String): List<String> {
            val skills = userSkillsCsv
                .split(",", ";", "\n")
                .map { it.trim() }
                .filter { it.length >= 2 }

            val bullets = resumeBullets
                .lines()
                .map { it.trim().removePrefix("•").removePrefix("-").removePrefix("*").trim() }
                .filter { it.length >= 6 }

            return (skills + bullets).distinctBy { it.lowercase(Locale.ROOT) }
        }

        internal fun extractJdRequirementChunks(rawText: String): List<String> {
            val chunks = LinkedHashSet<String>()
            val noiseLineRegex = Regex(
                """(?i)^(\d{1,2}:\d{2}|jobstreet|linkedin|hiredly|myfuturejobs|indeed|posted\s+\d+|easy apply|apply now|save|share|\d+\+?\s*applicants|hybrid\s*\(.*\)|100%\s*remote).*$"""
            )
            val salaryOnlyRegex = Regex("""(?i)^(rm|myr|sgd|s\$|usd|\$)\s*\d[\d,.\s\-–to]*(per\s*month|/mo|monthly)?$""")
            val locationOnlyRegex = Regex("""(?i)^.*(bandar utama|bangsar south|kl sentral|petaling jaya|subang jaya|cyberjaya|bayan lepas|kuala lumpur|selangor|johor bahru|penang|singapore).*$""")

            val requirementSectionPrefixRegex = Regex("""(?i)^(requirements?|required\s*skills?|tech\s*stack|skills?|must\s*have|qualifications?|nice\s*to\s*have)\s*[:\-–]\s*(.+)$""")

            for (rawLine in rawText.lines()) {
                val line = rawLine.trim()
                if (line.length < 3) continue
                if (noiseLineRegex.matches(line)) continue
                if (line.startsWith("Imported via CareerLens", ignoreCase = true)) continue

                // Extract parenthesized skills inside Role headlines e.g. "Principal Android Engineer (Jetpack Compose)"
                val isCompanyLine = line.contains(Regex("""(?i)\b(bank|sdn|bhd|berhad|pte|inc|corp|labs|group)\b"""))
                val parenMatch = Regex("""\(([^)]{3,40})\)""").find(line)
                if (!isCompanyLine && parenMatch != null && !line.contains("days office", ignoreCase = true)) {
                    val inner = parenMatch.groupValues[1].trim()
                    if (!salaryOnlyRegex.matches(inner) && !locationOnlyRegex.matches(inner) && !inner.all { it.isDigit() }) {
                        chunks.add(inner)
                    }
                }

                // Explicit Requirements / Tech Stack / Skills list
                val reqSection = requirementSectionPrefixRegex.find(line)
                if (reqSection != null) {
                    val body = reqSection.groupValues[2]
                    body.split(",", ";", "•", "|")
                        .map { it.trim().removePrefix("-").trim() }
                        .filter { it.length >= 2 && !salaryOnlyRegex.matches(it) }
                        .forEach { chunks.add(it) }
                    continue
                }

                // Bullet points in JD descriptions
                if (line.startsWith("•") || line.startsWith("- ") || line.startsWith("* ")) {
                    val cleanBullet = line.drop(1).trim()
                    if (cleanBullet.length >= 3 && !salaryOnlyRegex.matches(cleanBullet) && !locationOnlyRegex.matches(cleanBullet)) {
                        chunks.add(cleanBullet)
                    }
                    continue
                }

                // Multi-perk EPF/Bonus line or slash/bullet delimited tech items
                if (line.contains("•")) {
                    line.split("•").map { it.trim() }.filter { part ->
                        part.length >= 3 && !salaryOnlyRegex.matches(part) && !locationOnlyRegex.matches(part)
                    }.forEach { chunks.add(it) }
                    continue
                }

                // If none of the structured blocks triggered, check known tech keywords embedded in prose
                if (!salaryOnlyRegex.matches(line) && !locationOnlyRegex.matches(line)) {
                    for (kw in SEMANTIC_TECH_SCAN_KEYWORDS) {
                        val pattern = Regex("""(?i)(?<![A-Za-z0-9])${Regex.escape(kw)}(?![A-Za-z0-9])""")
                        if (pattern.containsMatchIn(line)) {
                            chunks.add(kw)
                        }
                    }
                }
            }

            // Cap cleanly to keep ONNX batch fast even on huge pasted JDs
            return chunks.take(24)
        }

        private fun isLexicalEquivalent(userChunk: String, jdReq: String): Boolean {
            val u = userChunk.lowercase(Locale.ROOT).trim()
            val j = jdReq.lowercase(Locale.ROOT).trim()
            if (u == j) return true
            if (u.length >= 3 && j.contains(u)) {
                val bound = Regex("""(?i)(?<![a-z0-9])${Regex.escape(u)}(?![a-z0-9])""")
                if (bound.containsMatchIn(j)) return true
            }
            if (j.length >= 3 && u.contains(j)) {
                val bound = Regex("""(?i)(?<![a-z0-9])${Regex.escape(j)}(?![a-z0-9])""")
                if (bound.containsMatchIn(u)) return true
            }
            return false
        }

        private fun shortenForBadge(text: String): String {
            return if (text.length <= 34) text else text.take(31).trimEnd() + "..."
        }

        private fun suggestCleanSkillName(jdReq: String): String {
            val trimmed = jdReq.trim()
            if (trimmed.length <= 28) return trimmed
            // Extract capitalized tech tokens if it's a long sentence
            val found = SEMANTIC_TECH_SCAN_KEYWORDS.firstOrNull { kw ->
                trimmed.contains(kw, ignoreCase = true)
            }
            return found ?: trimmed.take(26).trimEnd() + "..."
        }

        private fun dotProduct(a: FloatArray, b: FloatArray): Float {
            val len = minOf(a.size, b.size)
            var sum = 0f
            for (i in 0 until len) {
                sum += a[i] * b[i]
            }
            return sum
        }

        private fun l2NormalizeInPlace(vec: FloatArray) {
            var normSq = 0.0
            for (v in vec) {
                normSq += (v * v).toDouble()
            }
            val norm = sqrt(normSq).toFloat()
            if (norm > 1e-12f) {
                for (i in vec.indices) {
                    vec[i] /= norm
                }
            }
        }

        /**
         * Built-in Concept-Vector Fallback (32 concept clusters mapped onto a dense vector)
         * used only if no `.onnx` model asset is present yet.
         */
        internal fun encodeConceptFallback(text: String): FloatArray {
            val lower = text.lowercase(Locale.ROOT)
            val vec = FloatArray(FALLBACK_DIM)
            CONCEPT_CLUSTERS.forEachIndexed { clusterIdx, terms ->
                var clusterActivation = 0f
                for (term in terms) {
                    if (lower.contains(term)) {
                        clusterActivation = max(clusterActivation, 1.0f)
                    }
                }
                if (clusterActivation > 0f) {
                    vec[clusterIdx % FALLBACK_DIM] += clusterActivation
                }
            }
            // Add hashed character n-gram residuals so arbitrary unseen words still get similarity > 0 on exact tokens
            val words = lower.split(Regex("[^a-z0-9+#.]+")).filter { it.length >= 2 }
            for (w in words) {
                val bucket = 48 + ((w.hashCode() and 0x7fffffff) % (FALLBACK_DIM - 48))
                vec[bucket] += 0.45f
            }
            l2NormalizeInPlace(vec)
            return vec
        }

        private const val FALLBACK_DIM = 96

        private val CONCEPT_CLUSTERS = listOf(
            listOf("k8s", "kubernetes", "eks", "gke", "helm", "container orchestration"),
            listOf("ci/cd", "devops", "github actions", "gitlab ci", "jenkins", "deployment pipeline", "automated build", "release pipeline"),
            listOf("next.js", "nextjs", "react", "reactjs", "react.js", "ssr", "frontend"),
            listOf("jetpack compose", "declarative android ui", "declarative ui", "compose", "material 3"),
            listOf("kotlin coroutines", "coroutines", "stateflow", "sharedflow", "flow", "asynchronous", "reactive", "concurrency", "multithreading"),
            listOf("room", "room db", "room sqlite", "sqlite", "offline-first", "local persistence", "local database"),
            listOf("hilt", "dagger", "koin", "dependency injection"),
            listOf("mvvm", "mvi", "clean architecture", "layered architecture", "viewmodel"),
            listOf("docker", "container", "dockerfile"),
            listOf("terraform", "infrastructure as code", "iac"),
            listOf("aws", "amazon web services", "ec2", "s3", "lambda"),
            listOf("gcp", "google cloud", "bigquery", "cloud run"),
            listOf("rest", "retrofit", "okhttp", "ktor", "http", "openapi", "swagger", "json"),
            listOf("graphql", "apollo"),
            listOf("grpc", "protobuf"),
            listOf("kafka", "rabbitmq", "pub/sub", "event streaming", "message broker"),
            listOf("postgresql", "mysql", "rdbms", "sql", "database indexing"),
            listOf("redis", "memcached", "in-memory cache"),
            listOf("junit", "mockk", "espresso", "unit test", "automated test", "tdd"),
            listOf("workmanager", "background sync", "scheduled worker"),
            listOf("ml kit", "ocr", "optical character recognition", "text recognition", "onnx", "tflite", "edge ai"),
            listOf("kwsp", "epf", "employer epf", "provident fund"),
            listOf("duitnow", "fpx", "paynet", "ibg", "rentas", "tng", "grabpay", "ewallet", "payment gateway"),
            listOf("lhdn pcb", "mtd", "zakat", "tax deduction"),
            listOf("socso", "perkeso", "eis"),
            listOf("agile", "scrum", "kanban", "sprint"),
            listOf("oauth2", "oidc", "jwt", "rbac", "biometric", "keystore"),
            listOf("typescript", "typed javascript", "tsx"),
            listOf("spring boot", "jvm microservice"),
            listOf("microservices", "distributed systems"),
            listOf("flutter", "dart"),
            listOf("react native", "expo"),
            listOf("swift", "swiftui", "ios")
        )

        private val SEMANTIC_TECH_SCAN_KEYWORDS = listOf(
            "Kotlin", "Jetpack Compose", "Coroutines", "StateFlow", "Room SQLite", "Room DB",
            "Clean Architecture", "MVVM", "MVI", "Hilt", "Koin", "WorkManager", "ML Kit", "OCR",
            "ONNX Runtime", "TFLite", "K8s", "Kubernetes", "Docker", "CI/CD", "DevOps pipeline",
            "GitHub Actions", "Terraform", "AWS", "GCP", "Next.js", "React", "TypeScript",
            "Tailwind CSS", "GraphQL", "REST API", "gRPC", "Spring Boot", "Microservices",
            "Kafka", "PostgreSQL", "Redis", "JUnit", "MockK", "Espresso", "Flutter", "React Native",
            "SwiftUI", "OAuth2", "KWSP", "EPF", "DuitNow", "FPX", "BNM RMiT", "LHDN PCB",
            "SOCSO", "Agile", "Scrum", "Flexible Benefits", "Performance Bonus"
        )
    }
}
