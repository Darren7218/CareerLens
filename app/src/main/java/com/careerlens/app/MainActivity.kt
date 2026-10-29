package com.careerlens.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.careerlens.app.data.local.JobApplicationEntity
import com.careerlens.app.ui.screens.AddOrEditJobDialog
import com.careerlens.app.ui.screens.AnalyticsAndPlaybookScreen
import com.careerlens.app.ui.screens.ApplicationDetailDialog
import com.careerlens.app.ui.screens.CurrencyExchangeEditorDialog
import com.careerlens.app.ui.screens.InterviewAgendaScreen
import com.careerlens.app.ui.screens.OcrScannerScreen
import com.careerlens.app.ui.screens.PipelineDashboardScreen
import com.careerlens.app.ui.screens.SalaryComparisonScreen
import com.careerlens.app.ui.theme.CareerLensTheme
import com.careerlens.app.ui.theme.ElectricIndigo
import com.careerlens.app.ui.theme.EmeraldOffer
import com.careerlens.app.ui.viewmodel.CareerLensViewModel

private enum class BottomNavTab(val label: String, val icon: ImageVector) {
    PIPELINE("Pipeline", Icons.Default.WorkOutline),
    OCR_SCANNER("OCR Scan", Icons.Default.DocumentScanner),
    SALARY_STUDIO("Salary & EPF", Icons.Default.Payments),
    PLAYBOOK("Playbook", Icons.Default.Insights),
    INTERVIEWS("Interviews", Icons.Default.CalendarMonth)
}

class MainActivity : ComponentActivity() {

    private val viewModel: CareerLensViewModel by viewModels()
    private var sharedScreenshotSignal by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingShareIntent(intent)

        setContent {
            CareerLensTheme {
                CareerLensAppRoot(
                    viewModel = viewModel,
                    openOcrTabInitially = sharedScreenshotSignal,
                    onConsumeShareSignal = { sharedScreenshotSignal = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val sharedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (sharedUri != null) {
                sharedScreenshotSignal = true
                viewModel.scanScreenshotUri(sharedUri)
            }
        }
    }
}

@Composable
private fun CareerLensAppRoot(
    viewModel: CareerLensViewModel,
    openOcrTabInitially: Boolean,
    onConsumeShareSignal: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenshotUri by viewModel.activeScreenshotUri.collectAsStateWithLifecycle()
    val activeOcrMode by viewModel.activeOcrMode.collectAsStateWithLifecycle()
    val parsedOcrResult by viewModel.parsedOcrResult.collectAsStateWithLifecycle()
    val isOcrProcessing by viewModel.isOcrProcessing.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(BottomNavTab.PIPELINE.ordinal) }
    var selectedJobForDetail by remember { mutableStateOf<JobApplicationEntity?>(null) }
    var showAddJobDialog by remember { mutableStateOf(false) }
    var showCurrencyEditorDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(openOcrTabInitially) {
        if (openOcrTabInitially) {
            selectedTab = BottomNavTab.OCR_SCANNER.ordinal
            onConsumeShareSignal()
        }
    }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBannerMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                BottomNavTab.entries.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    val badgeCount = when (tab) {
                        BottomNavTab.SALARY_STUDIO -> uiState.evaluatedOffers.size
                        BottomNavTab.INTERVIEWS -> uiState.interviews.count { !it.isCompleted }
                        else -> 0
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (badgeCount > 0) {
                                        Badge(containerColor = EmeraldOffer) {
                                            Text(badgeCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(imageVector = tab.icon, contentDescription = tab.label)
                            }
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricIndigo,
                            selectedTextColor = ElectricIndigo,
                            indicatorColor = ElectricIndigo.copy(alpha = 0.16f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (BottomNavTab.entries[selectedTab]) {
                BottomNavTab.PIPELINE -> {
                    PipelineDashboardScreen(
                        uiState = uiState,
                        onSearchChange = viewModel::setSearchQuery,
                        onLocationFilterChange = viewModel::setLocationFilter,
                        onStageFilterChange = viewModel::setStageFilter,
                        onToggleKanbanMode = viewModel::toggleKanbanMode,
                        onTogglePrivacyMask = viewModel::toggleSalaryPrivacyMask,
                        onOpenJobDetail = { selectedJobForDetail = it },
                        onQuickAdvanceStage = viewModel::advanceJobStage,
                        onOpenAddJobModal = { showAddJobDialog = true },
                        onOpenOcrScanner = { selectedTab = BottomNavTab.OCR_SCANNER.ordinal },
                        onOpenSalaryStudio = { selectedTab = BottomNavTab.SALARY_STUDIO.ordinal },
                        onOpenAnalyticsPlaybook = { selectedTab = BottomNavTab.PLAYBOOK.ordinal },
                        onOpenCurrencyDialog = { showCurrencyEditorDialog = true }
                    )
                }

                BottomNavTab.OCR_SCANNER -> {
                    OcrScannerScreen(
                        screenshotUri = screenshotUri,
                        activeOcrMode = activeOcrMode,
                        parsedResult = parsedOcrResult,
                        isProcessing = isOcrProcessing,
                        availableCurrencies = uiState.currencies.map { it.code },
                        existingApplications = uiState.applications,
                        onChangeOcrMode = viewModel::setOcrMode,
                        onPickScreenshotUri = viewModel::scanScreenshotUri,
                        onLoadSampleTemplate = viewModel::loadDemoMalaysiaJobScreenshotTemplate,
                        onUpdateParsedResult = viewModel::updateParsedOcrResult,
                        onSaveExtractedJob = { newJob, createOffer ->
                            viewModel.saveJobApplication(newJob, createOffer)
                            selectedTab = if (createOffer) {
                                BottomNavTab.SALARY_STUDIO.ordinal
                            } else {
                                BottomNavTab.PIPELINE.ordinal
                            }
                        },
                        onSaveExtractedOfferPackage = { app, base, allow, bonus, epf, leave ->
                            viewModel.saveOfferFromOcr(app, base, allow, bonus, epf, leave)
                            selectedTab = BottomNavTab.SALARY_STUDIO.ordinal
                        },
                        onSaveExtractedInterviewRound = { round, comp, role ->
                            viewModel.saveInterviewRound(round, comp, role)
                            selectedTab = BottomNavTab.INTERVIEWS.ordinal
                        }
                    )
                }

                BottomNavTab.SALARY_STUDIO -> {
                    SalaryComparisonScreen(
                        evaluatedOffers = uiState.evaluatedOffers,
                        allApplications = uiState.applications,
                        currencies = uiState.currencies,
                        displayCurrencyCode = uiState.displayCurrencyCode,
                        displayCurrencySymbol = uiState.displayCurrencySymbol,
                        currentBaseline = uiState.currentBaseline,
                        isSalaryMasked = uiState.isSalaryPrivacyMasked,
                        onSelectDisplayCurrency = viewModel::selectDisplayCurrency,
                        onSaveCurrencyRate = { code, sym, name, rate ->
                            viewModel.saveCurrencyRate(code, sym, name, rate)
                        },
                        onSaveBaseline = viewModel::saveCurrentBaseline,
                        onSaveOffer = viewModel::saveJobOffer,
                        onExportPdfToUri = viewModel::exportOfferComparisonPdfToUri
                    )
                }

                BottomNavTab.PLAYBOOK -> {
                    AnalyticsAndPlaybookScreen(
                        applications = uiState.applications,
                        starStories = uiState.starStories,
                        onSaveStarStory = viewModel::saveStarStory,
                        onShowToastMessage = viewModel::postBannerMessage
                    )
                }

                BottomNavTab.INTERVIEWS -> {
                    InterviewAgendaScreen(
                        interviews = uiState.interviews,
                        applications = uiState.applications,
                        onSaveInterview = viewModel::saveInterviewRound,
                        onToggleComplete = viewModel::toggleInterviewCompleted,
                        onDeleteInterview = viewModel::deleteInterviewRound,
                        onExportJson = viewModel::generateBackupJson,
                        onExportCsv = viewModel::generateExportCsv,
                        onSaveFileToUri = viewModel::saveTextFileToUri,
                        onExportPdfToUri = viewModel::exportOfferComparisonPdfToUri,
                        onRestoreFromUri = viewModel::restoreFromBackupUri,
                        onImportJson = viewModel::restoreFromBackupJson
                    )
                }
            }
        }
    }

    // Application Detail Modal
    val activeDetailJob = selectedJobForDetail?.let { current ->
        uiState.applications.firstOrNull { it.id == current.id } ?: current
    }
    if (activeDetailJob != null) {
        ApplicationDetailDialog(
            app = activeDetailJob,
            appInterviews = uiState.interviews.filter { it.applicationId == activeDetailJob.id },
            evaluatedOffer = uiState.evaluatedOffers.firstOrNull { it.application.id == activeDetailJob.id },
            availableCurrencies = uiState.currencies.map { it.code },
            onDismiss = { selectedJobForDetail = null },
            onUpdateStage = { newStage -> viewModel.updateJobStage(activeDetailJob, newStage) },
            onSaveUpdatedApp = { updated -> viewModel.saveJobApplication(updated) },
            onSaveOffer = viewModel::saveJobOffer,
            onSaveInterview = viewModel::saveInterviewRound,
            onDeleteApp = {
                viewModel.deleteJobApplication(activeDetailJob)
                selectedJobForDetail = null
            }
        )
    }

    // Add Job Modal
    if (showAddJobDialog) {
        AddOrEditJobDialog(
            initialJob = null,
            availableCurrencies = uiState.currencies.map { it.code },
            onDismiss = { showAddJobDialog = false },
            onSave = { job, createOffer ->
                viewModel.saveJobApplication(job, createOffer)
                showAddJobDialog = false
            }
        )
    }

    // Currency Preset & Offline Exchange Rate Modal
    if (showCurrencyEditorDialog) {
        CurrencyExchangeEditorDialog(
            currencies = uiState.currencies,
            onDismiss = { showCurrencyEditorDialog = false },
            onSaveCurrency = { code, sym, name, rate ->
                viewModel.saveCurrencyRate(code, sym, name, rate)
            }
        )
    }
}
