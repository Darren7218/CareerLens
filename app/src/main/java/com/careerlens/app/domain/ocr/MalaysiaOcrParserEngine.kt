package com.careerlens.app.domain.ocr

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import com.careerlens.app.domain.model.JobStage
import com.careerlens.app.domain.model.MalaysiaPresets
import com.careerlens.app.domain.model.WorkMode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a single recognized visual text block on a screenshot with its spatial coordinates
 * so the user can tap any pill on the screenshot to assign it to Company, Role, Salary, etc.
 */
data class OcrBlockChip(
    val id: Int,
    val text: String,
    val boundingBox: Rect?,
    val normalizedTop: Float, // 0f (top) to 1f (bottom)
    val suggestedTag: String? = null // e.g., "ROLE", "COMPANY", "SALARY", "LOCATION"
)

data class ParsedJobScreenshot(
    val detectedCompany: String = "",
    val detectedRole: String = "",
    val detectedCurrency: String = "MYR",
    val detectedSalaryMin: Double = 0.0,
    val detectedSalaryMax: Double = 0.0,
    val detectedLocation: String = "Kuala Lumpur (KLCC / Bukit Bintang)",
    val detectedWorkMode: WorkMode = WorkMode.HYBRID,
    val detectedPortalSource: String = "JobStreet MY",
    val detectedStage: JobStage = JobStage.APPLIED,
    val detectedNotes: String = "",
    val fullRawText: String = "",
    val blockChips: List<OcrBlockChip> = emptyList()
)

class MalaysiaOcrParserEngine {

    private val recognizer by lazy {
        // Uses bundled on-device Latin model (100% offline, zero internet needed)
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun processScreenshotUri(context: Context, imageUri: Uri): Result<ParsedJobScreenshot> {
        return runCatching {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val imageHeight = max(1, inputImage.height)
            val visionText = recognizer.process(inputImage).await()
            parseVisionText(visionText, imageHeight)
        }
    }

    fun parseRawText(rawText: String): ParsedJobScreenshot {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val syntheticChips = lines.mapIndexed { index, line ->
            OcrBlockChip(
                id = index,
                text = line,
                boundingBox = null,
                normalizedTop = index.toFloat() / max(1, lines.size)
            )
        }
        return runHeuristics(rawText, syntheticChips)
    }

    private fun parseVisionText(visionText: Text, imageHeight: Int): ParsedJobScreenshot {
        val rawBlocks = mutableListOf<OcrBlockChip>()
        var counter = 0
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val clean = line.text.trim()
                if (clean.length >= 2) {
                    val box = line.boundingBox
                    val normTop = if (box != null) (box.top.toFloat() / imageHeight).coerceIn(0f, 1f) else 0.5f
                    rawBlocks.add(
                        OcrBlockChip(
                            id = counter++,
                            text = clean,
                            boundingBox = box,
                            normalizedTop = normTop
                        )
                    )
                }
            }
        }
        val sortedChips = rawBlocks.sortedBy { it.normalizedTop }
        return runHeuristics(visionText.text, sortedChips)
    }

    private fun runHeuristics(fullText: String, chips: List<OcrBlockChip>): ParsedJobScreenshot {
        val lowerFull = fullText.lowercase()

        // 1. Detect Portal Source
        val portal = when {
            lowerFull.contains("jobstreet") -> "JobStreet MY"
            lowerFull.contains("hiredly") -> "Hiredly"
            lowerFull.contains("myfuturejobs") -> "MyFutureJobs"
            lowerFull.contains("linkedin") -> "LinkedIn"
            lowerFull.contains("indeed") -> "Indeed MY"
            else -> "JobStreet MY"
        }

        // 2. Detect Work Mode
        val workMode = when {
            lowerFull.contains("100% remote") || lowerFull.contains("fully remote") || lowerFull.contains("work from home") -> WorkMode.REMOTE
            lowerFull.contains("hybrid") -> WorkMode.HYBRID
            lowerFull.contains("on-site") || lowerFull.contains("onsite") || lowerFull.contains("wfo") -> WorkMode.ONSITE
            else -> WorkMode.HYBRID
        }

        // 3. Detect Stage (if screenshot is an Offer Letter or Interview Invite)
        val stage = when {
            lowerFull.contains("letter of offer") || lowerFull.contains("employment offer") || lowerFull.contains("pleased to offer") -> JobStage.OFFER
            lowerFull.contains("interview invitation") || lowerFull.contains("schedule an interview") || lowerFull.contains("google meet") || lowerFull.contains("microsoft teams") -> JobStage.INTERVIEW
            lowerFull.contains("hackerrank") || lowerFull.contains("codility") || lowerFull.contains("online assessment") || lowerFull.contains("take-home") -> JobStage.ASSESSMENT
            lowerFull.contains("application submitted") || lowerFull.contains("applied") -> JobStage.APPLIED
            else -> JobStage.APPLIED
        }

        // 4. Detect Currency & Salary Range (Supports "RM 8,000 - RM 12,000", "MYR 9.5k - 13k", "SGD 6,500", "$5,000")
        val salaryMatch = extractCurrencyAndSalaryRange(fullText)

        // 5. Detect Malaysian Hub / Location
        val detectedLocation = detectMalaysiaLocation(fullText)

        // 6. Detect Role Title & Company Name from top chips
        val roleKeywords = listOf(
            "engineer", "developer", "architect", "manager", "analyst", "designer",
            "lead", "specialist", "consultant", "scientist", "intern", "executive",
            "director", "officer", "android", "kotlin", "mobile", "frontend", "backend", "fullstack"
        )
        val companySuffixes = listOf(
            "sdn bhd", "berhad", "bhd", "pte ltd", "inc", "corp", "technologies",
            "digital", "group", "malaysia", "bank", "labs", "solutions", "global"
        )
        val ignoreNoise = listOf(
            "9:41", "100%", "5g", "4g", "lte", "jobstreet", "linkedin", "easy apply",
            "apply now", "save", "share", "posted", "ago", "applicants"
        )

        var roleCandidate = ""
        var companyCandidate = ""

        val filteredChips = chips.filter { chip ->
            val l = chip.text.lowercase()
            chip.text.length in 3..65 && ignoreNoise.none { l == it || l.startsWith("posted ") }
        }

        for (chip in filteredChips) {
            val l = chip.text.lowercase()
            if (roleCandidate.isBlank() && roleKeywords.any { l.contains(it) }) {
                roleCandidate = chip.text
                continue
            }
            if (companyCandidate.isBlank() && companySuffixes.any { l.contains(it) } && chip.text != roleCandidate) {
                companyCandidate = chip.text
            }
        }

        // Fallback to top spatial lines if keywords didn't match
        if (roleCandidate.isBlank() && filteredChips.isNotEmpty()) {
            roleCandidate = filteredChips.first().text
        }
        if (companyCandidate.isBlank() && filteredChips.size >= 2) {
            companyCandidate = filteredChips.firstOrNull { it.text != roleCandidate }?.text ?: ""
        }

        // Tag chips so the UI can highlight them with semantic colors
        val taggedChips = chips.map { chip ->
            val tag = when {
                chip.text == roleCandidate && roleCandidate.isNotBlank() -> "ROLE"
                chip.text == companyCandidate && companyCandidate.isNotBlank() -> "COMPANY"
                chip.text.contains(Regex("(?i)(RM|MYR|SGD|S\\$|USD|\\$)\\s*\\d")) -> "SALARY"
                chip.text.contains(
                    Regex("(?i)(Kuala Lumpur|Selangor|Petaling Jaya|Bangsar|Cyberjaya|Penang|Johor|Singapore|Hybrid|Remote)")
                ) -> "LOCATION"
                else -> null
            }
            chip.copy(suggestedTag = tag)
        }

        return ParsedJobScreenshot(
            detectedCompany = companyCandidate.ifBlank { "New Company" },
            detectedRole = roleCandidate.ifBlank { "Software Engineer" },
            detectedCurrency = salaryMatch.first,
            detectedSalaryMin = salaryMatch.second,
            detectedSalaryMax = salaryMatch.third,
            detectedLocation = detectedLocation,
            detectedWorkMode = workMode,
            detectedPortalSource = portal,
            detectedStage = stage,
            detectedNotes = "Imported via CareerLens Offline OCR (${portal})",
            fullRawText = fullText,
            blockChips = taggedChips
        )
    }

    /**
     * Parses Malaysian & international salary formats:
     * - "RM 8,500 – RM 12,000 per month"
     * - "MYR 10k - 14k"
     * - "SGD 6,000 - 7,500"
     * - "RM 144,000 p.a." (auto-normalized to monthly if > 45,000 and marked annual/p.a.)
     */
    private fun extractCurrencyAndSalaryRange(text: String): Triple<String, Double, Double> {
        val regex = Regex(
            pattern = """(?i)\b(RM|MYR|SGD|S\$|USD|US\$|\$)\s*(\d{1,3}(?:[.,]\d{3})*(?:\.\d+)?|\d+(?:\.\d+)?\s*k)(?:\s*[-–—to]+\s*(?:RM|MYR|SGD|S\$|USD|US\$|\$)?\s*(\d{1,3}(?:[.,]\d{3})*(?:\.\d+)?|\d+(?:\.\d+)?\s*k))?"""
        )
        val match = regex.find(text) ?: return Triple("MYR", 0.0, 0.0)

        val rawCurrency = match.groupValues[1].uppercase()
        val currencyCode = when {
            rawCurrency.contains("RM") || rawCurrency.contains("MYR") -> "MYR"
            rawCurrency.contains("SGD") || rawCurrency.contains("S$") -> "SGD"
            rawCurrency.contains("USD") || rawCurrency.contains("$") -> "USD"
            else -> "MYR"
        }

        fun parseAmount(raw: String): Double {
            if (raw.isBlank()) return 0.0
            val clean = raw.lowercase().replace(",", "").trim()
            return if (clean.endsWith("k")) {
                (clean.removeSuffix("k").trim().toDoubleOrNull() ?: 0.0) * 1000.0
            } else {
                clean.toDoubleOrNull() ?: 0.0
            }
        }

        var firstVal = parseAmount(match.groupValues[2])
        var secondVal = parseAmount(match.groupValues[3])
        if (secondVal == 0.0) secondVal = firstVal

        // Normalize annual figure to monthly if it looks like an annual package (e.g. RM 120,000 p.a.)
        val isAnnual = text.lowercase().let {
            it.contains("p.a") || it.contains("per annum") || it.contains("/yr") || it.contains("annual")
        } || firstVal >= 48_000.0

        if (isAnnual) {
            firstVal = (firstVal / 12.0)
            secondVal = (secondVal / 12.0)
        }

        return Triple(currencyCode, min(firstVal, secondVal), max(firstVal, secondVal))
    }

    private fun detectMalaysiaLocation(text: String): String {
        val l = text.lowercase()
        return when {
            l.contains("bangsar south") || l.contains("mid valley") || l.contains("kerinchi") ->
                "Bangsar South / Mid Valley, KL"
            l.contains("kl sentral") || l.contains("brickfields") ->
                "KL Sentral / Brickfields, KL"
            l.contains("damansara") || l.contains("bandar utama") || l.contains("ttdi") || l.contains("first avenue") ->
                "Damansara / TTDI / Bandar Utama, PJ"
            l.contains("petaling jaya") || l.contains("subang") || l.contains("sunway") ->
                "Petaling Jaya / Subang Jaya, Selangor"
            l.contains("cyberjaya") || l.contains("putrajaya") ->
                "Cyberjaya / Putrajaya"
            l.contains("shah alam") || l.contains("klang") ->
                "Shah Alam / Klang, Selangor"
            l.contains("penang") || l.contains("bayan lepas") || l.contains("george town") || l.contains("pulau pinang") ->
                "Bayan Lepas / George Town, Penang"
            l.contains("johor") || l.contains("iskandar") || l.contains("jb") ->
                "Johor Bahru / Iskandar Puteri, Johor"
            l.contains("singapore") ->
                "Singapore (Cross-Border / Remote)"
            l.contains("remote") ->
                "Remote (Malaysia)"
            else -> MalaysiaPresets.popularMalaysiaLocations.first()
        }
    }
}
