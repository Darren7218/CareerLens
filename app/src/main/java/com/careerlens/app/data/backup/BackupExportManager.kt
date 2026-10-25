package com.careerlens.app.data.backup

import com.careerlens.app.data.local.CurrencyRateEntity
import com.careerlens.app.data.local.InterviewRoundEntity
import com.careerlens.app.data.local.JobApplicationEntity
import com.careerlens.app.data.local.JobOfferEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class CareerLensBackupBundle(
    val version: Int = 1,
    val exportedAtEpoch: Long = System.currentTimeMillis(),
    val applications: List<JobApplicationEntity>,
    val interviews: List<InterviewRoundEntity>,
    val offers: List<JobOfferEntity>,
    val currencies: List<CurrencyRateEntity>
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
        currencies: List<CurrencyRateEntity>
    ): String {
        val bundle = CareerLensBackupBundle(
            applications = applications,
            interviews = interviews,
            offers = offers,
            currencies = currencies
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

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }
}
