package com.careerlens.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careerlens.app.data.local.JobApplicationEntity
import com.careerlens.app.domain.model.JobStage
import com.careerlens.app.ui.components.BentoMetricCard
import com.careerlens.app.ui.components.JobApplicationCard
import com.careerlens.app.ui.components.StagePillBadge
import com.careerlens.app.ui.components.formatCompactNumber
import com.careerlens.app.ui.theme.AmberAssessment
import com.careerlens.app.ui.theme.CyanInterview
import com.careerlens.app.ui.theme.ElectricIndigo
import com.careerlens.app.ui.theme.EmeraldOffer
import com.careerlens.app.ui.theme.accentColor
import com.careerlens.app.ui.viewmodel.CareerLensUiState

@Composable
fun PipelineDashboardScreen(
    uiState: CareerLensUiState,
    onSearchChange: (String) -> Unit,
    onLocationFilterChange: (String) -> Unit,
    onStageFilterChange: (JobStage?) -> Unit,
    onToggleKanbanMode: () -> Unit,
    onOpenJobDetail: (JobApplicationEntity) -> Unit,
    onQuickAdvanceStage: (JobApplicationEntity) -> Unit,
    onOpenAddJobModal: () -> Unit,
    onOpenOcrScanner: () -> Unit,
    onOpenSalaryStudio: () -> Unit,
    onOpenCurrencyDialog: () -> Unit
) {
    val activeCount = uiState.applications.count { it.stage.isActivePipeline }
    val upcomingInterviewsCount = uiState.interviews.count { !it.isCompleted }
    val offersCount = uiState.evaluatedOffers.size
    val topOfferMonthlyNet = uiState.evaluatedOffers.maxOfOrNull { it.monthlyNetTakeHomePay } ?: 0.0

    val currencyMap = uiState.currencies.associateBy { it.code }
    val locationQuickFilters = listOf("All", "Kuala Lumpur", "Bangsar South", "PJ", "Cyberjaya", "Penang", "JB", "Singapore", "Remote")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Flagship Executive Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CareerLens",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = EmeraldOffer.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, EmeraldOffer.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "100% OFFLINE",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                            color = EmeraldOffer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = "Malaysia Career & Compensation Intelligence",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Active Display Currency Switcher Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.clickable { onOpenCurrencyDialog() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = "Currency",
                            tint = ElectricIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${uiState.displayCurrencyCode} (${uiState.displayCurrencySymbol})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Toggle Kanban / List View
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (uiState.isKanbanMode) ElectricIndigo.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (uiState.isKanbanMode) ElectricIndigo else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.clickable { onToggleKanbanMode() }
                ) {
                    Icon(
                        imageVector = if (uiState.isKanbanMode) Icons.Default.ViewColumn else Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = "Toggle Kanban/List",
                        tint = if (uiState.isKanbanMode) ElectricIndigo else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp).size(18.dp)
                    )
                }
            }
        }

        // --- Main Content Scroll ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. Executive Bento Metrics Grid
            item {
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoMetricCard(
                            title = "ACTIVE PIPELINE",
                            value = "$activeCount Roles",
                            subtitle = "${uiState.applications.size} total tracked",
                            accentColor = ElectricIndigo,
                            icon = Icons.Default.WorkOutline,
                            modifier = Modifier.weight(1f)
                        )
                        BentoMetricCard(
                            title = "INTERVIEWS",
                            value = "$upcomingInterviewsCount Upcoming",
                            subtitle = "Local reminders active",
                            accentColor = CyanInterview,
                            icon = Icons.Default.CalendarMonth,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoMetricCard(
                            title = "TOP NET TAKE-HOME",
                            value = if (topOfferMonthlyNet > 0) "${uiState.displayCurrencySymbol} ${formatCompactNumber(topOfferMonthlyNet)}/mo" else "—",
                            subtitle = "After EPF, SOCSO & PCB",
                            accentColor = EmeraldOffer,
                            icon = Icons.Default.Payments,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenSalaryStudio
                        )
                        BentoMetricCard(
                            title = "SCREENSHOT OCR",
                            value = "Instant Scan",
                            subtitle = "JobStreet / LinkedIn / Offer",
                            accentColor = AmberAssessment,
                            icon = Icons.Default.DocumentScanner,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenOcrScanner
                        )
                    }
                }
            }

            // 2. Quick Action Banner: Scan Screenshot or Add Manually
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clickable { onOpenOcrScanner() },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        ElectricIndigo.copy(alpha = 0.22f),
                                        EmeraldOffer.copy(alpha = 0.14f)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = ElectricIndigo,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Read Job / Offer Screenshot via Offline OCR",
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Auto-detects RM/SGD salary, KL/PJ/Penang hub, role & company",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ElectricIndigo
                            ) {
                                Text(
                                    text = "Scan",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Search Bar & Malaysia Location / Stage Chips
            item {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    placeholder = {
                        Text(
                            "Search company, role, Bangsar South, RM...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricIndigo,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Stage Filter Row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedStageFilter == null,
                            onClick = { onStageFilterChange(null) },
                            label = { Text("All Stages (${uiState.applications.size})") }
                        )
                    }
                    items(JobStage.entries) { stage ->
                        val count = uiState.applications.count { it.stage == stage }
                        if (count > 0 || uiState.selectedStageFilter == stage) {
                            FilterChip(
                                selected = uiState.selectedStageFilter == stage,
                                onClick = {
                                    onStageFilterChange(if (uiState.selectedStageFilter == stage) null else stage)
                                },
                                label = { Text("${stage.shortLabel} ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = stage.accentColor().copy(alpha = 0.22f),
                                    selectedLabelColor = stage.accentColor()
                                )
                            )
                        }
                    }
                }

                // Malaysia Location Filter Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(locationQuickFilters) { loc ->
                        val isSelected = uiState.selectedLocationFilter == loc
                        FilterChip(
                            selected = isSelected,
                            onClick = { onLocationFilterChange(loc) },
                            label = {
                                Text(
                                    text = if (loc == "All") "🇲🇾 All Hubs" else loc,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }
            }

            // 4. Either Horizontal Kanban Board OR Vertical Pipeline List
            if (uiState.isKanbanMode) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    KanbanBoardView(
                        applications = uiState.filteredApplications,
                        currencyMap = currencyMap.mapValues { it.value.symbol },
                        onOpenJobDetail = onOpenJobDetail,
                        onQuickAdvanceStage = onQuickAdvanceStage
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Applications (${uiState.filteredApplications.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ElectricIndigo.copy(alpha = 0.16f),
                            modifier = Modifier.clickable { onOpenAddJobModal() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = ElectricIndigo,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add Job",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricIndigo
                                )
                            }
                        }
                    }
                }

                items(uiState.filteredApplications, key = { it.id }) { app ->
                    val symbol = currencyMap[app.currencyCode]?.symbol ?: "RM"
                    JobApplicationCard(
                        app = app,
                        currencySymbol = symbol,
                        onClick = { onOpenJobDetail(app) },
                        onQuickAdvanceStage = { onQuickAdvanceStage(app) },
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KanbanBoardView(
    applications: List<JobApplicationEntity>,
    currencyMap: Map<String, String>,
    onOpenJobDetail: (JobApplicationEntity) -> Unit,
    onQuickAdvanceStage: (JobApplicationEntity) -> Unit
) {
    val stagesToShow = listOf(
        JobStage.WISHLIST,
        JobStage.APPLIED,
        JobStage.ASSESSMENT,
        JobStage.INTERVIEW,
        JobStage.OFFER,
        JobStage.ACCEPTED,
        JobStage.REJECTED
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (stage in stagesToShow) {
            val columnApps = applications.filter { it.stage == stage }
            val stageColor = stage.accentColor()

            Card(
                modifier = Modifier.width(285.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, stageColor.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StagePillBadge(stage = stage)
                        Text(
                            text = "${columnApps.size}",
                            style = MaterialTheme.typography.titleMedium,
                            color = stageColor
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (columnApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No roles in ${stage.shortLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (app in columnApps) {
                                JobApplicationCard(
                                    app = app,
                                    currencySymbol = currencyMap[app.currencyCode] ?: "RM",
                                    onClick = { onOpenJobDetail(app) },
                                    onQuickAdvanceStage = { onQuickAdvanceStage(app) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
