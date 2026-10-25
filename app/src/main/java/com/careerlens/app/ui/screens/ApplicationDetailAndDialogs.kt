package com.careerlens.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careerlens.app.data.local.InterviewRoundEntity
import com.careerlens.app.data.local.JobApplicationEntity
import com.careerlens.app.data.local.JobOfferEntity
import com.careerlens.app.domain.calculator.OfferComparisonResult
import com.careerlens.app.domain.model.JobStage
import com.careerlens.app.domain.model.MalaysiaPresets
import com.careerlens.app.domain.model.WorkMode
import com.careerlens.app.ui.components.CompanyMonogramAvatar
import com.careerlens.app.ui.components.StagePillBadge
import com.careerlens.app.ui.components.WorkModeBadge
import com.careerlens.app.ui.components.formatCurrencyAmount
import com.careerlens.app.ui.theme.CyanInterview
import com.careerlens.app.ui.theme.ElectricIndigo
import com.careerlens.app.ui.theme.EmeraldOffer
import com.careerlens.app.ui.theme.RoseRejected
import com.careerlens.app.ui.theme.accentColor

@Composable
fun ApplicationDetailDialog(
    app: JobApplicationEntity,
    appInterviews: List<InterviewRoundEntity>,
    evaluatedOffer: OfferComparisonResult?,
    availableCurrencies: List<String>,
    onDismiss: () -> Unit,
    onUpdateStage: (JobStage) -> Unit,
    onSaveUpdatedApp: (JobApplicationEntity) -> Unit,
    onSaveOffer: (JobOfferEntity) -> Unit,
    onSaveInterview: (InterviewRoundEntity, String, String) -> Unit,
    onDeleteApp: () -> Unit
) {
    var showEditAppForm by remember { mutableStateOf(false) }
    var showEditOfferForm by remember { mutableStateOf(false) }
    var showAddInterviewForm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CompanyMonogramAvatar(companyName = app.companyName, size = 42.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(app.roleTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            app.companyName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { showEditAppForm = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Job")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(430.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Stage Switcher Chips
                item {
                    Text(
                        text = "Pipeline Stage (Tap to update)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(JobStage.entries) { stage ->
                            FilterChip(
                                selected = app.stage == stage,
                                onClick = { onUpdateStage(stage) },
                                label = { Text(stage.shortLabel, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = stage.accentColor().copy(alpha = 0.22f),
                                    selectedLabelColor = stage.accentColor()
                                )
                            )
                        }
                    }
                }

                // 2. Overview Card
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Location:", style = MaterialTheme.typography.labelMedium)
                                Text(app.location, style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Work Mode:", style = MaterialTheme.typography.labelMedium)
                                WorkModeBadge(app.workMode)
                            }
                            if (app.salaryMaxMonthly > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Posted Salary:", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        "${app.currencyCode} ${formatCurrencyAmount(app.salaryMinMonthly)} – ${formatCurrencyAmount(app.salaryMaxMonthly)} / mo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = EmeraldOffer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (app.recruiterName.isNotBlank() || app.recruiterContact.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Recruiter:", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        "${app.recruiterName} ${app.recruiterContact}".trim(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            if (app.notes.isNotBlank()) {
                                Text("Notes:", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    app.notes,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 3. Offer & Compensation Summary Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Offer & Comp Package", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showEditOfferForm = true }) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (evaluatedOffer != null) "Edit Offer" else "Add Offer")
                        }
                    }

                    if (evaluatedOffer != null) {
                        val sym = evaluatedOffer.displayCurrencySymbol
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldOffer.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, EmeraldOffer.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Monthly Net Take-Home: $sym ${formatCurrencyAmount(evaluatedOffer.monthlyNetTakeHomePay)} / mo",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldOffer
                                )
                                Text(
                                    "Total Annual Package: $sym ${formatCurrencyAmount(evaluatedOffer.annualTotalCompensationPackage)} / yr",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "EPF: ${evaluatedOffer.offer.employeeEpfPercent.toInt()}% Employee / ${evaluatedOffer.offer.employerEpfPercent.toInt()}% Employer",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 4. Interviews for this Application
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Interview Rounds (${appInterviews.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = { showAddInterviewForm = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Round")
                        }
                    }

                    if (appInterviews.isEmpty()) {
                        Text(
                            "No interview rounds logged yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            appInterviews.forEach { round ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "Round ${round.roundNumber}: ${round.roundType.label}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (round.prepChecklist.isNotBlank()) {
                                            Text(
                                                "Prep: ${round.prepChecklist}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDeleteApp) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = RoseRejected,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete", color = RoseRejected)
            }
        }
    )

    if (showEditAppForm) {
        AddOrEditJobDialog(
            initialJob = app,
            availableCurrencies = availableCurrencies,
            onDismiss = { showEditAppForm = false },
            onSave = { updatedJob, createOffer ->
                onSaveUpdatedApp(updatedJob)
                showEditAppForm = false
            }
        )
    }

    if (showEditOfferForm) {
        val currentOffer = evaluatedOffer?.offer ?: JobOfferEntity(
            applicationId = app.id,
            currencyCode = app.currencyCode,
            monthlyBaseSalary = if (app.salaryMaxMonthly > 0) app.salaryMaxMonthly else 10000.0,
            monthlyFixedAllowance = 500.0,
            targetPerformanceBonusMonths = 2.0,
            employeeEpfPercent = if (app.currencyCode == "MYR") 11.0 else 0.0,
            employerEpfPercent = if (app.currencyCode == "MYR") 12.0 else 0.0
        )
        EditMalaysiaOfferDialog(
            companyName = app.companyName,
            initialOffer = currentOffer,
            availableCurrencies = availableCurrencies,
            onDismiss = { showEditOfferForm = false },
            onSave = { savedOffer ->
                onSaveOffer(savedOffer)
                showEditOfferForm = false
            }
        )
    }

    if (showAddInterviewForm) {
        AddInterviewRoundDialog(
            applications = listOf(app),
            preselectedAppId = app.id,
            onDismiss = { showAddInterviewForm = false },
            onSave = { round, comp, role ->
                onSaveInterview(round, comp, role)
                showAddInterviewForm = false
            }
        )
    }
}

@Composable
fun AddOrEditJobDialog(
    initialJob: JobApplicationEntity? = null,
    availableCurrencies: List<String>,
    onDismiss: () -> Unit,
    onSave: (JobApplicationEntity, Boolean) -> Unit
) {
    var companyName by remember { mutableStateOf(initialJob?.companyName ?: "") }
    var roleTitle by remember { mutableStateOf(initialJob?.roleTitle ?: "") }
    var stage by remember { mutableStateOf(initialJob?.stage ?: JobStage.APPLIED) }
    var workMode by remember { mutableStateOf(initialJob?.workMode ?: WorkMode.HYBRID) }
    var location by remember { mutableStateOf(initialJob?.location ?: MalaysiaPresets.popularMalaysiaLocations.first()) }
    var currencyCode by remember { mutableStateOf(initialJob?.currencyCode ?: "MYR") }
    var minSal by remember {
        mutableStateOf(if ((initialJob?.salaryMinMonthly ?: 0.0) > 0) initialJob!!.salaryMinMonthly.toInt().toString() else "")
    }
    var maxSal by remember {
        mutableStateOf(if ((initialJob?.salaryMaxMonthly ?: 0.0) > 0) initialJob!!.salaryMaxMonthly.toInt().toString() else "")
    }
    var portalSource by remember { mutableStateOf(initialJob?.jobPortalSource ?: "JobStreet MY") }
    var recruiterName by remember { mutableStateOf(initialJob?.recruiterName ?: "") }
    var notes by remember { mutableStateOf(initialJob?.notes ?: "") }
    var priority by remember { mutableIntStateOf(initialJob?.priorityRating ?: 4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialJob == null) "Add Job Application" else "Edit Job Application") },
        text = {
            LazyColumn(
                modifier = Modifier.height(430.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = roleTitle,
                        onValueChange = { roleTitle = it },
                        label = { Text("Role Title *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Text("Stage", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(JobStage.entries) { st ->
                            FilterChip(
                                selected = stage == st,
                                onClick = { stage = st },
                                label = { Text(st.shortLabel, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                item {
                    Text("Currency & Monthly Salary Range", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val codes = if (availableCurrencies.isNotEmpty()) availableCurrencies else listOf("MYR", "SGD", "USD")
                        items(codes) { code ->
                            FilterChip(
                                selected = currencyCode == code,
                                onClick = { currencyCode = code },
                                label = { Text(code) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minSal,
                            onValueChange = { minSal = it },
                            label = { Text("Min/Mo ($currencyCode)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = maxSal,
                            onValueChange = { maxSal = it },
                            label = { Text("Max/Mo ($currencyCode)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location (Malaysia / Hub)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MalaysiaPresets.popularMalaysiaLocations) { loc ->
                            FilterChip(
                                selected = location == loc,
                                onClick = { location = loc },
                                label = { Text(loc.substringBefore(","), fontSize = 11.sp) }
                            )
                        }
                    }
                }
                item {
                    Text("Work Mode & Portal", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WorkMode.entries.forEach { mode ->
                            FilterChip(
                                selected = workMode == mode,
                                onClick = { workMode = mode },
                                label = { Text(mode.badgeText) }
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MalaysiaPresets.popularJobPortals) { portal ->
                            FilterChip(
                                selected = portalSource == portal,
                                onClick = { portalSource = portal },
                                label = { Text(portal, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = recruiterName,
                        onValueChange = { recruiterName = it },
                        label = { Text("Recruiter / Contact") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Tech Stack / Perks") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sMin = minSal.toDoubleOrNull() ?: 0.0
                    val sMax = (maxSal.toDoubleOrNull() ?: sMin).coerceAtLeast(sMin)
                    val baseJob = initialJob ?: JobApplicationEntity(
                        companyName = "",
                        roleTitle = ""
                    )
                    onSave(
                        baseJob.copy(
                            companyName = companyName.ifBlank { "New Company" },
                            roleTitle = roleTitle.ifBlank { "Software Engineer" },
                            stage = stage,
                            workMode = workMode,
                            location = location,
                            currencyCode = currencyCode,
                            salaryMinMonthly = sMin,
                            salaryMaxMonthly = sMax,
                            jobPortalSource = portalSource,
                            recruiterName = recruiterName,
                            notes = notes,
                            priorityRating = priority,
                            updatedAtEpoch = System.currentTimeMillis()
                        ),
                        stage == JobStage.OFFER
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
