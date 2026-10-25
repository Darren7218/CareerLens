package com.careerlens.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.careerlens.app.data.backup.BackupExportManager
import com.careerlens.app.data.local.CareerLensDatabase
import com.careerlens.app.data.local.CurrencyRateEntity
import com.careerlens.app.data.local.InterviewRoundEntity
import com.careerlens.app.data.local.JobApplicationEntity
import com.careerlens.app.data.local.JobOfferEntity
import com.careerlens.app.domain.calculator.MalaysiaCompCalculator
import com.careerlens.app.domain.calculator.OfferComparisonResult
import com.careerlens.app.domain.model.JobStage
import com.careerlens.app.domain.notification.InterviewReminderWorker
import com.careerlens.app.domain.ocr.MalaysiaOcrParserEngine
import com.careerlens.app.domain.ocr.ParsedJobScreenshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CareerLensUiState(
    val applications: List<JobApplicationEntity> = emptyList(),
    val filteredApplications: List<JobApplicationEntity> = emptyList(),
    val interviews: List<InterviewRoundEntity> = emptyList(),
    val offers: List<JobOfferEntity> = emptyList(),
    val currencies: List<CurrencyRateEntity> = emptyList(),
    val displayCurrencyCode: String = "MYR",
    val displayCurrencySymbol: String = "RM",
    val evaluatedOffers: List<OfferComparisonResult> = emptyList(),
    val searchQuery: String = "",
    val selectedLocationFilter: String = "All",
    val selectedStageFilter: JobStage? = null,
    val isKanbanMode: Boolean = false
)

class CareerLensViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = CareerLensDatabase.getInstance(application).dao()
    private val ocrEngine = MalaysiaOcrParserEngine()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedLocationFilter = MutableStateFlow("All")
    private val _selectedStageFilter = MutableStateFlow<JobStage?>(null)
    private val _isKanbanMode = MutableStateFlow(false)

    // OCR Scanner State
    private val _activeScreenshotUri = MutableStateFlow<Uri?>(null)
    val activeScreenshotUri: StateFlow<Uri?> = _activeScreenshotUri.asStateFlow()

    private val _parsedOcrResult = MutableStateFlow(ParsedJobScreenshot())
    val parsedOcrResult: StateFlow<ParsedJobScreenshot> = _parsedOcrResult.asStateFlow()

    private val _isOcrProcessing = MutableStateFlow(false)
    val isOcrProcessing: StateFlow<Boolean> = _isOcrProcessing.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    val uiState: StateFlow<CareerLensUiState> = combine(
        dao.observeAllApplications(),
        dao.observeAllInterviews(),
        dao.observeAllOffers(),
        dao.observeCurrencyRates(),
        combine(_searchQuery, _selectedLocationFilter, _selectedStageFilter, _isKanbanMode) { q, loc, stage, kanban ->
            FilterTuple(q, loc, stage, kanban)
        }
    ) { apps, interviews, offers, currencies, filter ->
        val currencyMap = currencies.associateBy { it.code }
        val activeDisplayCurrency = currencies.firstOrNull { it.isDefaultDisplayCurrency }
            ?: currencies.firstOrNull { it.code == "MYR" }
            ?: CurrencyRateEntity("MYR", "RM", "Malaysian Ringgit", 1.0, "🇲🇾", true)

        val appsById = apps.associateBy { it.id }
        val evaluatedOffers = offers.mapNotNull { offer ->
            val parentApp = appsById[offer.applicationId] ?: return@mapNotNull null
            MalaysiaCompCalculator.evaluateOffer(
                application = parentApp,
                offer = offer,
                currencyMap = currencyMap,
                displayCurrencyCode = activeDisplayCurrency.code
            )
        }.sortedByDescending { it.annualTotalCompensationPackage }

        val filtered = apps.filter { app ->
            val matchesQuery = filter.query.isBlank() ||
                app.companyName.contains(filter.query, ignoreCase = true) ||
                app.roleTitle.contains(filter.query, ignoreCase = true) ||
                app.location.contains(filter.query, ignoreCase = true) ||
                app.notes.contains(filter.query, ignoreCase = true)

            val matchesLoc = filter.location == "All" ||
                app.location.contains(filter.location, ignoreCase = true)

            val matchesStage = filter.stage == null || app.stage == filter.stage
            matchesQuery && matchesLoc && matchesStage
        }

        CareerLensUiState(
            applications = apps,
            filteredApplications = filtered,
            interviews = interviews,
            offers = offers,
            currencies = currencies,
            displayCurrencyCode = activeDisplayCurrency.code,
            displayCurrencySymbol = activeDisplayCurrency.symbol,
            evaluatedOffers = evaluatedOffers,
            searchQuery = filter.query,
            selectedLocationFilter = filter.location,
            selectedStageFilter = filter.stage,
            isKanbanMode = filter.isKanban
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CareerLensUiState()
    )

    private data class FilterTuple(
        val query: String,
        val location: String,
        val stage: JobStage?,
        val isKanban: Boolean
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLocationFilter(location: String) {
        _selectedLocationFilter.value = location
    }

    fun setStageFilter(stage: JobStage?) {
        _selectedStageFilter.value = stage
    }

    fun toggleKanbanMode() {
        _isKanbanMode.value = !_isKanbanMode.value
    }

    fun clearBannerMessage() {
        _statusBannerMessage.value = null
    }

    // --- Application CRUD & Stage Progression ---
    fun saveJobApplication(app: JobApplicationEntity, createDefaultOfferIfOfferStage: Boolean = false) {
        viewModelScope.launch {
            val savedId = dao.upsertApplication(app.copy(updatedAtEpoch = System.currentTimeMillis()))
            val targetId = if (app.id == 0L) savedId else app.id
            if (createDefaultOfferIfOfferStage || app.stage == JobStage.OFFER) {
                val existingOffer = uiState.value.offers.firstOrNull { it.applicationId == targetId }
                if (existingOffer == null && app.salaryMaxMonthly > 0) {
                    dao.upsertOffer(
                        JobOfferEntity(
                            applicationId = targetId,
                            currencyCode = app.currencyCode,
                            monthlyBaseSalary = app.salaryMaxMonthly,
                            monthlyFixedAllowance = if (app.currencyCode == "MYR") 400.0 else 0.0,
                            targetPerformanceBonusMonths = 1.5,
                            employeeEpfPercent = if (app.currencyCode == "MYR") 11.0 else 0.0,
                            employerEpfPercent = if (app.currencyCode == "MYR") 12.0 else 0.0
                        )
                    )
                }
            }
            _statusBannerMessage.value = "Saved ${app.companyName} (${app.roleTitle})"
        }
    }

    fun advanceJobStage(app: JobApplicationEntity) {
        val nextStage = when (app.stage) {
            JobStage.WISHLIST -> JobStage.APPLIED
            JobStage.APPLIED -> JobStage.ASSESSMENT
            JobStage.ASSESSMENT -> JobStage.INTERVIEW
            JobStage.INTERVIEW -> JobStage.OFFER
            JobStage.OFFER -> JobStage.ACCEPTED
            else -> app.stage
        }
        if (nextStage != app.stage) {
            saveJobApplication(
                app = app.copy(stage = nextStage, updatedAtEpoch = System.currentTimeMillis()),
                createDefaultOfferIfOfferStage = (nextStage == JobStage.OFFER)
            )
        }
    }

    fun updateJobStage(app: JobApplicationEntity, newStage: JobStage) {
        saveJobApplication(
            app = app.copy(stage = newStage, updatedAtEpoch = System.currentTimeMillis()),
            createDefaultOfferIfOfferStage = (newStage == JobStage.OFFER)
        )
    }

    fun deleteJobApplication(app: JobApplicationEntity) {
        viewModelScope.launch {
            dao.deleteApplication(app)
            _statusBannerMessage.value = "Removed ${app.companyName}"
        }
    }

    // --- Interview CRUD & Offline Reminder Scheduling ---
    fun saveInterviewRound(round: InterviewRoundEntity, companyName: String, roleTitle: String) {
        viewModelScope.launch {
            val id = dao.upsertInterviewRound(round)
            val actualId = if (round.id == 0L) id else round.id
            if (!round.isCompleted && round.scheduledAtEpoch > System.currentTimeMillis()) {
                InterviewReminderWorker.scheduleReminder(
                    context = getApplication(),
                    interviewId = actualId,
                    companyName = companyName,
                    roleTitle = roleTitle,
                    roundLabel = round.roundType.label,
                    scheduledAtEpoch = round.scheduledAtEpoch
                )
            }
            _statusBannerMessage.value = "Saved ${round.roundType.label} for $companyName"
        }
    }

    fun toggleInterviewCompleted(round: InterviewRoundEntity) {
        viewModelScope.launch {
            dao.upsertInterviewRound(round.copy(isCompleted = !round.isCompleted))
        }
    }

    fun deleteInterviewRound(round: InterviewRoundEntity) {
        viewModelScope.launch {
            dao.deleteInterviewRound(round)
        }
    }

    // --- Offer & Salary Comparison CRUD ---
    fun saveJobOffer(offer: JobOfferEntity) {
        viewModelScope.launch {
            dao.upsertOffer(offer)
            val parent = dao.getApplicationById(offer.applicationId)
            if (parent != null && parent.stage != JobStage.OFFER && parent.stage != JobStage.ACCEPTED) {
                dao.upsertApplication(parent.copy(stage = JobStage.OFFER, updatedAtEpoch = System.currentTimeMillis()))
            }
            _statusBannerMessage.value = "Updated Offer Package & Compensation Breakdown"
        }
    }

    fun deleteJobOffer(offer: JobOfferEntity) {
        viewModelScope.launch {
            dao.deleteOffer(offer)
        }
    }

    // --- Currency Presets & Manual Offline Exchange Rate Editor ---
    fun selectDisplayCurrency(code: String) {
        viewModelScope.launch {
            dao.setDefaultDisplayCurrency(code)
        }
    }

    fun saveCurrencyRate(
        code: String,
        symbol: String,
        displayName: String,
        rateToMyr: Double,
        flagEmoji: String = "💱"
    ) {
        viewModelScope.launch {
            val currentDefault = uiState.value.displayCurrencyCode == code.uppercase()
            dao.upsertCurrencyRate(
                CurrencyRateEntity(
                    code = code.uppercase().trim(),
                    symbol = symbol.trim(),
                    displayName = displayName.trim(),
                    rateToMyr = rateToMyr.coerceAtLeast(0.0001),
                    flagEmoji = flagEmoji,
                    isDefaultDisplayCurrency = currentDefault
                )
            )
            _statusBannerMessage.value = "Updated offline rate: 1 ${code.uppercase()} = RM $rateToMyr"
        }
    }

    // --- On-Device Screenshot OCR Pipeline ---
    fun scanScreenshotUri(uri: Uri) {
        _activeScreenshotUri.value = uri
        _isOcrProcessing.value = true
        viewModelScope.launch {
            val result = ocrEngine.processScreenshotUri(getApplication(), uri)
            result.onSuccess { parsed ->
                _parsedOcrResult.value = parsed
            }.onFailure { err ->
                _statusBannerMessage.value = "OCR notice: ${err.localizedMessage ?: "Could not read image"}"
            }
            _isOcrProcessing.value = false
        }
    }

    fun loadDemoMalaysiaJobScreenshotTemplate(templateIndex: Int) {
        val sampleTexts = listOf(
            """
            JobStreet Malaysia • Posted 2 hours ago
            Principal Android Engineer (Jetpack Compose)
            GXBank (GrabFin Digital Bank)
            First Avenue, Bandar Utama, Petaling Jaya, Selangor
            Hybrid (3 days office, 2 days WFH)
            RM 13,500 - RM 17,000 per month
            16% Employer EPF • Flexible Benefits • Annual Performance Bonus
            Requirements: Kotlin, Coroutines, Offline-First Room DB, Clean Architecture
            """.trimIndent(),
            """
            LETTER OF EMPLOYMENT OFFER — PRIVATE & CONFIDENTIAL
             Wise Payments Malaysia Sdn Bhd
            Senior Mobile Engineer — Android
            Location: Bangsar South, Kuala Lumpur (Hybrid)
            Monthly Base Salary: RM 15,200
            RSU Equity Grant: RM 24,000 / year
            Annual Leave: 25 days + Wise Sabbatical
            """.trimIndent(),
            """
            LinkedIn Jobs • Singapore
            Staff Mobile Software Engineer
            Foodpanda (Delivery Hero APAC)
            Singapore (Cross-Border / Remote)
            SGD 8,200 - SGD 9,800 per month
            Hybrid work arrangement • Relocation support available
            """.trimIndent()
        )
        val chosen = sampleTexts[templateIndex % sampleTexts.size]
        _activeScreenshotUri.value = null
        _parsedOcrResult.value = ocrEngine.parseRawText(chosen)
    }

    fun updateParsedOcrResult(updated: ParsedJobScreenshot) {
        _parsedOcrResult.value = updated
    }

    // --- Offline Backup & Export ---
    fun generateBackupJson(): String {
        val s = uiState.value
        return BackupExportManager.exportToJsonString(
            applications = s.applications,
            interviews = s.interviews,
            offers = s.offers,
            currencies = s.currencies
        )
    }

    fun generateExportCsv(): String {
        val s = uiState.value
        return BackupExportManager.exportApplicationsAndOffersToCsv(
            applications = s.applications,
            offersByAppId = s.offers.associateBy { it.applicationId }
        )
    }

    fun restoreFromBackupJson(jsonString: String) {
        viewModelScope.launch {
            BackupExportManager.importFromJsonString(jsonString).onSuccess { bundle ->
                dao.upsertCurrencyRates(bundle.currencies)
                bundle.applications.forEach { dao.upsertApplication(it) }
                bundle.interviews.forEach { dao.upsertInterviewRound(it) }
                bundle.offers.forEach { dao.upsertOffer(it) }
                _statusBannerMessage.value = "Restored ${bundle.applications.size} applications & ${bundle.offers.size} offers!"
            }.onFailure {
                _statusBannerMessage.value = "Invalid backup JSON format."
            }
        }
    }
}
