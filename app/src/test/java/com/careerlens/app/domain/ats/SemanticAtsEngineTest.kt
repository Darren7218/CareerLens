package com.careerlens.app.domain.ats

import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Verifies the Colab-trained & INT8 quantized ONNX model bundled in `app/src/main/assets/models/`
 * end-to-end using pure-Kotlin BertWordPieceTokenizer + ONNX Runtime.
 */
class SemanticAtsEngineTest {

    private lateinit var engine: SemanticAtsEngine

    private fun resolveAssetFile(filename: String): File {
        val candidates = listOf(
            File("src/main/assets/models/$filename"),
            File("app/src/main/assets/models/$filename")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Missing bundled model asset '$filename'. Checked: ${candidates.map { it.absolutePath }}")
    }

    @Before
    fun setup() {
        val onnxFile = resolveAssetFile("careerlens_ats_int8.onnx")
        val vocabFile = resolveAssetFile("vocab.txt")
        val metaFile = resolveAssetFile("model_metadata.json")
        engine = SemanticAtsEngine.createFromStreams(
            onnxModelPath = onnxFile.absolutePath,
            vocabStream = vocabFile.inputStream(),
            metadataJson = metaFile.readText(Charsets.UTF_8)
        )
    }

    @After
    fun teardown() {
        engine.close()
    }

    @Test
    fun `int8 onnx neural model bridges technical and Malaysian taxonomy synonyms accurately`() {
        val k8sSim = engine.cosineSimilarity("K8s", "Kubernetes")
        val nextReactSim = engine.cosineSimilarity("Next.js", "React")
        val cicdDevopsSim = engine.cosineSimilarity("CI/CD", "DevOps automated release pipeline")
        val kwspEpfSim = engine.cosineSimilarity("KWSP", "Malaysia EPF employer contribution")
        val composeUiSim = engine.cosineSimilarity("Jetpack Compose", "Declarative Android UI")
        val javaJsSim = engine.cosineSimilarity("Java", "JavaScript")
        val pcbDisambiguationSim = engine.cosineSimilarity(
            "LHDN PCB payroll monthly income tax deduction",
            "Hardware printed circuit board PCB soldering"
        )

        println("=== CareerLens INT8 ONNX Semantic Similarity Verification ===")
        println("K8s <-> Kubernetes                          : $k8sSim")
        println("Next.js <-> React                           : $nextReactSim")
        println("CI/CD <-> DevOps automated release pipeline : $cicdDevopsSim")
        println("KWSP <-> Malaysia EPF employer contribution : $kwspEpfSim")
        println("Jetpack Compose <-> Declarative Android UI  : $composeUiSim")
        println("Java <-> JavaScript (hard negative)         : $javaJsSim")
        println("PCB Payroll <-> PCB Hardware (hard neg)     : $pcbDisambiguationSim")

        assertTrue("K8s <-> Kubernetes ($k8sSim) must exceed 0.65", k8sSim > 0.65f)
        assertTrue("Next.js <-> React ($nextReactSim) must exceed 0.65", nextReactSim > 0.65f)
        assertTrue("CI/CD <-> DevOps ($cicdDevopsSim) must exceed 0.65", cicdDevopsSim > 0.65f)
        assertTrue("KWSP <-> EPF ($kwspEpfSim) must exceed 0.60", kwspEpfSim > 0.60f)
        assertTrue("Compose <-> Declarative UI ($composeUiSim) must exceed 0.60", composeUiSim > 0.60f)
        assertTrue("Java <-> JavaScript ($javaJsSim) must stay separated (< 0.40)", javaJsSim < 0.40f)
        assertTrue("PCB tax vs hardware ($pcbDisambiguationSim) must stay separated (< 0.35)", pcbDisambiguationSim < 0.35f)
    }

    @Test
    fun `end to end JD ATS analyzer finds semantic synonym bridges and missing gap`() {
        val sampleJd = """
            JobStreet Malaysia • Posted 2 hours ago
            Principal Android Engineer (Declarative Android UI)
            GXBank (GrabFin Digital Bank)
            First Avenue, Bandar Utama, Petaling Jaya, Selangor
            RM 13,500 - RM 17,000 per month
            Requirements: Kotlin, Coroutines, Offline-First Room DB, Clean Architecture, DevOps automated release pipeline, Kubernetes container orchestration
        """.trimIndent()

        val userSkills = "Kotlin, Jetpack Compose, Coroutines, Room SQLite, Clean Architecture, CI/CD"
        val userBullets = "• Built Jetpack Compose Material 3 apps with automated CI/CD GitHub Actions pipelines"

        // Warmup run then measured analysis run
        engine.analyze(sampleJd, "Principal Android Engineer", "", userSkills, userBullets)
        val result = engine.analyze(sampleJd, "Principal Android Engineer", "", userSkills, userBullets)

        println("=== CareerLens Full End-to-End ATS Analysis ===")
        println("Score=${result.overallScorePercent}% | Mode=${result.engineModeLabel} | Latency=${result.inferenceLatencyMs}ms")
        println("Exact Matches  : ${result.exactMatches.map { "${it.jdRequirement} <- ${it.matchedUserSkill}" }}")
        println("Synonym Bridges: ${result.synonymBridges.map { "${it.matchedUserSkill} <-> ${it.jdRequirement} (${it.similarity})" }}")
        println("Semantic Gaps  : ${result.semanticGaps.map { "${it.suggestedSkillToAdd} (closest=${it.closestUserSkill}:${it.bestSimilarity})" }}")

        assertTrue("Neural ONNX engine should be active", result.isNeuralOnnxActive)
        assertTrue("Should detect exact matches for Kotlin/Coroutines/Clean Architecture", result.exactMatches.isNotEmpty())
        assertTrue("Should bridge CI/CD <-> DevOps or Room SQLite <-> Room DB or Compose <-> Declarative UI", result.synonymBridges.isNotEmpty())
        assertTrue(
            "Should flag Kubernetes container orchestration as a semantic gap",
            result.semanticGaps.any { it.jdRequirement.contains("Kubernetes", ignoreCase = true) }
        )
    }
}
