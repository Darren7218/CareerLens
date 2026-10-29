package com.careerlens.app.data.backup

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.careerlens.app.data.local.CurrencyRateEntity
import com.careerlens.app.data.local.InterviewRoundEntity
import com.careerlens.app.data.local.JobApplicationEntity
import com.careerlens.app.data.local.JobOfferEntity
import com.careerlens.app.domain.calculator.OfferComparisonResult
import com.careerlens.app.domain.model.CurrentJobBaselineProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class CareerLensBackupBundle(
    val version: Int = 2,
    val exportedAtEpoch: Long = System.currentTimeMillis(),
    val applications: List<JobApplicationEntity>,
    val interviews: List<InterviewRoundEntity>,
    val offers: List<JobOfferEntity>,
    val currencies: List<CurrencyRateEntity>,
    val currentBaseline: CurrentJobBaselineProfile = CurrentJobBaselineProfile()
)

object BackupExportManager {

    private val prettyJson = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun exportToJsonString(
        applications: List<JobApplicationEntity>,
        interviews: List<InterviewRoundEntity>,
        offers: List<JobOfferEntity>,
        currencies: List<CurrencyRateEntity>,
        currentBaseline: CurrentJobBaselineProfile = CurrentJobBaselineProfile()
    ): String {
        val bundle = CareerLensBackupBundle(
            applications = applications,
            interviews = interviews,
            offers = offers,
            currencies = currencies,
            currentBaseline = currentBaseline
        )
        return prettyJson.encodeToString(bundle)
    }

    fun importFromJsonString(jsonString: String): Result<CareerLensBackupBundle> {
        return runCatching {
            prettyJson.decodeFromString<CareerLensBackupBundle>(jsonString)
        }
    }

    fun exportApplicationsAndOffersToCsv(
        applications: List<JobApplicationEntity>,
        offersByAppId: Map<Long, JobOfferEntity>
    ): String {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sb = StringBuilder()
        sb.appendLine(
            "Company,Role,Stage,Work Mode,Location,Currency,Posted Min/Mo,Posted Max/Mo,Offer Base/Mo,Offer Allowance/Mo,Bonus Months,Employer EPF %,Portal,Applied Date"
        )
        for (app in applications) {
            val offer = offersByAppId[app.id]
            val dateStr = df.format(Date(app.appliedDateEpoch))
            val row = listOf(
                escapeCsv(app.companyName),
                escapeCsv(app.roleTitle),
                escapeCsv(app.stage.label),
                escapeCsv(app.workMode.label),
                escapeCsv(app.location),
                escapeCsv(offer?.currencyCode ?: app.currencyCode),
                app.salaryMinMonthly.toInt().toString(),
                app.salaryMaxMonthly.toInt().toString(),
                (offer?.monthlyBaseSalary?.toInt() ?: 0).toString(),
                (offer?.monthlyFixedAllowance?.toInt() ?: 0).toString(),
                ((offer?.contractualBonusMonths ?: 0.0) + (offer?.targetPerformanceBonusMonths ?: 0.0)).toString(),
                (offer?.employerEpfPercent ?: 0.0).toString(),
                escapeCsv(app.jobPortalSource),
                dateStr
            )
            sb.appendLine(row.joinToString(","))
        }
        return sb.toString()
    }

    /**
     * Generates a clean 1-page printable A4 Landscape PDF Offer Comparison Report
     * directly to the user's selected Android Document URI (100% offline).
     */
    fun writeOfferComparisonPdfToUri(
        context: Context,
        targetUri: Uri,
        evaluatedOffers: List<OfferComparisonResult>,
        baseline: CurrentJobBaselineProfile,
        displaySymbol: String
    ): Result<Unit> = runCatching {
        val pdfDocument = PdfDocument()
        // A4 Landscape: 842 x 595 points
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 11f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(79, 70, 229)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 11f
            isAntiAlias = true
        }
        val boldGreenPaint = Paint().apply {
            color = Color.rgb(5, 150, 105)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
        }

        val nf = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
        fun fmt(v: Double) = "$displaySymbol ${nf.format(v)}"

        // Header
        canvas.drawText("CareerLens — Malaysia Offer & Total Compensation Matrix", 40f, 45f, titlePaint)
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText(
            "Generated 100% Offline on $dateStr | Current Baseline: ${baseline.currentRoleAndCompany} (RM ${nf.format(baseline.monthlyBaseSalary)}/mo)",
            40f,
            65f,
            subtitlePaint
        )
        canvas.drawLine(40f, 78f, 802f, 78f, linePaint)

        // Columns: Metric Label + up to 4 Offers
        val offersToPrint = evaluatedOffers.take(4)
        val startX = 230f
        val colWidth = 145f

        canvas.drawText("COMPENSATION METRIC", 40f, 105f, headerPaint)
        offersToPrint.forEachIndexed { idx, item ->
            val x = startX + idx * colWidth
            canvas.drawText(item.application.companyName.take(18), x, 102f, headerPaint)
            canvas.drawText(item.application.roleTitle.take(22), x, 117f, subtitlePaint)
        }
        canvas.drawLine(40f, 128f, 802f, 128f, linePaint)

        val rows = listOf<Pair<String, (OfferComparisonResult) -> String>>(
            "Offer Currency" to { it.offer.currencyCode },
            "Gross Monthly Base + Allow." to { fmt(it.monthlyGrossBaseAndAllowance) },
            "Employee EPF Deduction" to { "- ${fmt(it.monthlyEmployeeEpfDeduction)} (${it.offer.employeeEpfPercent.toInt()}%)" },
            "Est. Monthly PCB + SOCSO" to { "- ${fmt(it.monthlyEstimatedPcbTax + it.monthlySocsoEisDeduction)}" },
            "Monthly Net Take-Home Pay" to { fmt(it.monthlyNetTakeHomePay) },
            "Monthly Commute Deduction" to { "- ${fmt(it.monthlyCommuteCost)} (${it.offer.onsiteDaysPerWeek}d/wk)" },
            "True Monthly Disposable" to { fmt(it.monthlyTrueDisposablePay) },
            "Net Jump vs Current Job" to {
                val sign = if (it.netMonthlyDeltaVsCurrent >= 0) "+" else ""
                "$sign${fmt(it.netMonthlyDeltaVsCurrent)} (${String.format(Locale.US, "%+.1f%%", it.netMonthlyJumpPercent)})"
            },
            "Annual Base + Allowances" to { fmt(it.annualBaseSalary + it.annualFixedAllowances) },
            "Annual Bonus Total" to { "+ ${fmt(it.annualTotalBonus)} (${it.offer.contractualBonusMonths + it.offer.targetPerformanceBonusMonths}m)" },
            "Annual Employer EPF" to { "+ ${fmt(it.annualEmployerEpfContribution)} (${it.offer.employerEpfPercent.toInt()}%)" },
            "Annual RSU / Equity + Sign-On" to { "+ ${fmt(it.annualEquityAndSignOn)}" },
            "TOTAL ANNUAL PACKAGE" to { fmt(it.annualTotalCompensationPackage) },
            "Annual Leave & WLB Score" to { "${it.offer.annualLeaveDays}d AL • WLB ${it.offer.workLifeBalanceScore}/10" },
            "Weighted Decision Score" to { "${it.weightedDecisionScore} / 100" }
        )

        var y = 152f
        rows.forEachIndexed { rIdx, (label, extractor) ->
            val isHighlight = rIdx == 4 || rIdx == 6 || rIdx == 7 || rIdx == 12
            val p = if (isHighlight) boldGreenPaint else bodyPaint
            canvas.drawText(label, 40f, y, p)
            offersToPrint.forEachIndexed { cIdx, item ->
                val x = startX + cIdx * colWidth
                canvas.drawText(extractor(item), x, y, p)
            }
            canvas.drawLine(40f, y + 8f, 802f, y + 8f, linePaint)
            y += 27f
        }

        pdfDocument.finishPage(page)
        context.contentResolver.openOutputStream(targetUri)?.use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }
}
