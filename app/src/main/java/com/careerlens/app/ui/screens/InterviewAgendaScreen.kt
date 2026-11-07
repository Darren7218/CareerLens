package com.careerlens.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careerlens.app.data.local.InterviewRoundEntity
import com.careerlens.app.data.local.JobApplicationEntity
import com.careerlens.app.domain.model.InterviewRoundType
import com.careerlens.app.ui.theme.AmberAssessment
import com.careerlens.app.ui.theme.CyanInterview
import com.careerlens.app.ui.theme.ElectricIndigo
import com.careerlens.app.ui.theme.EmeraldOffer
import com.careerlens.app.ui.theme.RoseRejected
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InterviewAgendaScreen(
    interviews: List<InterviewRoundEntity>,
    applications: List<JobApplicationEntity>,
    onSaveInterview: (InterviewRoundEntity, String, String) -> Unit,
    onToggleComplete: (InterviewRoundEntity) -> Unit,
    onDeleteInterview: (InterviewRoundEntity) -> Unit,
    onExportJson: () -> String,
    onExportCsv: () -> String,
    onSaveFileToUri: (Uri, String, String) -> Unit,
    onExportPdfToUri: (Uri) -> Unit,
    onRestoreFromUri: (Uri) -> Unit,
    onImportJson: (String) -> Unit
) {
    val context = LocalContext.current
    val appsById = remember(applications) { applications.associateBy { it.id } }
    val upcoming = interviews.filter { !it.isCompleted }.sortedBy { it.scheduledAtEpoch }
    val completed = interviews.filter { it.isCompleted }.sortedByDescending { it.scheduledAtEpoch }

    var showAddRoundDialog by remember { mutableStateOf(false) }
    var showBackupModal by remember { mutableStateOf(false) }
    var exportedTextPreview by remember { mutableStateOf("") }

    val saveJsonFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            onSaveFileToUri(uri, onExportJson(), "JSON Backup")
        }
    }

    val saveCsvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            onSaveFileToUri(uri, onExportCsv(), "CSV Spreadsheet")
        }
    }

    val savePdfFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportPdfToUri(uri)
        }
    }

    val openJsonFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onRestoreFromUri(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Interviews & Offline Backup",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "1-Tap Phone Calendar sync, prep checklists & native Downloads file export",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showAddRoundDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Round")
                }
            }
        }

        // 2. Upcoming Interviews Section
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = CyanInterview,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Upcoming Interview Rounds (${upcoming.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        if (upcoming.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "No upcoming interviews scheduled. Tap 'Log Round' to schedule a screen or technical round.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(upcoming, key = { it.id }) { round ->
                val parentApp = appsById[round.applicationId]
                val company = parentApp?.companyName ?: "Company"
                val role = parentApp?.roleTitle ?: "Role"
                InterviewRoundCard(
                    round = round,
                    companyName = company,
                    roleTitle = role,
                    onToggleComplete = { onToggleComplete(round) },
                    onAddToSystemCalendar = {
                        launchSystemCalendarIntent(context, round, company, role)
                    },
                    onDelete = { onDeleteInterview(round) }
                )
            }
        }

        // 3. Completed Rounds & Question Bank
        if (completed.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Completed Rounds & Post-Interview Notes (${completed.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(completed, key = { it.id }) { round ->
                val parentApp = appsById[round.applicationId]
                val company = parentApp?.companyName ?: "Company"
                val role = parentApp?.roleTitle ?: "Role"
                InterviewRoundCard(
                    round = round,
                    companyName = company,
                    roleTitle = role,
                    onToggleComplete = { onToggleComplete(round) },
                    onAddToSystemCalendar = {
                        launchSystemCalendarIntent(context, round, company, role)
                    },
                    onDelete = { onDeleteInterview(round) }
                )
            }
        }

        // 4. 100% Offline Data Portability (Save to Downloads: PDF, JSON, CSV + Restore)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ),
                border = BorderStroke(1.dp, EmeraldOffer.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "100% Offline File Backup & PDF Report",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Save directly to your Android Downloads folder or Google Drive with zero cloud lock-in.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { savePdfFileLauncher.launch("CareerLens_Malaysia_Offer_Report.pdf") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldOffer)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save PDF Report")
                        }
                        OutlinedButton(
                            onClick = { saveCsvFileLauncher.launch("CareerLens_Applications_Offers.csv") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save CSV File")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { saveJsonFileLauncher.launch("CareerLens_Full_Backup.json") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save JSON File")
                        }
                        OutlinedButton(
                            onClick = { openJsonFileLauncher.launch(arrayOf("application/json", "text/*")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore File")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = {
                            exportedTextPreview = onExportJson()
                            copyToClipboard(context, "CareerLens JSON Backup", exportedTextPreview)
                            showBackupModal = true
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Or Copy / Paste Raw JSON via Clipboard")
                    }
                }
            }
        }
    }

    if (showAddRoundDialog && applications.isNotEmpty()) {
        AddInterviewRoundDialog(
            applications = applications,
            onDismiss = { showAddRoundDialog = false },
            onSave = { round, comp, role ->
                onSaveInterview(round, comp, role)
                showAddRoundDialog = false
            }
        )
    }

    if (showBackupModal) {
        var importInput by remember { mutableStateOf(exportedTextPreview) }
        AlertDialog(
            onDismissRequest = { showBackupModal = false },
            title = { Text("Offline Backup / CSV Export (Copied to Clipboard)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Copied to your clipboard! You can also paste a previously saved CareerLens JSON backup below and tap Restore.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )
                    OutlinedTextField(
                        value = importInput,
                        onValueChange = { importInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportJson(importInput)
                        showBackupModal = false
                    }
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun InterviewRoundCard(
    round: InterviewRoundEntity,
    companyName: String,
    roleTitle: String,
    onToggleComplete: () -> Unit,
    onAddToSystemCalendar: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(round.scheduledAtEpoch))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        border = BorderStroke(
            1.dp,
            if (round.isCompleted) EmeraldOffer.copy(alpha = 0.35f) else CyanInterview.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onToggleComplete) {
                        Icon(
                            imageVector = if (round.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Complete",
                            tint = if (round.isCompleted) EmeraldOffer else CyanInterview
                        )
                    }
                    Column {
                        Text(
                            text = "Round ${round.roundNumber}: ${round.roundType.label}",
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (round.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Text(
                            text = "$companyName • $roleTitle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // v3.0 1-Tap Add to Phone Calendar Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElectricIndigo.copy(alpha = 0.16f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAddToSystemCalendar() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = "Add to Phone Calendar",
                                tint = ElectricIndigo,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Calendar",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                                color = ElectricIndigo
                            )
                        }
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = RoseRejected.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = AmberAssessment,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$formattedDate (${round.durationMins}m)",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberAssessment
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VideoCall,
                        contentDescription = null,
                        tint = CyanInterview,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = round.formatOrLink,
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanInterview
                    )
                }
            }

            if (round.prepChecklist.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "PREP CHECKLIST",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                            color = ElectricIndigo
                        )
                        Text(
                            text = round.prepChecklist,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                        )
                    }
                }
            }

            if (round.questionsAsked.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldOffer.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "QUESTIONS ASKED / REFLECTION",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                            color = EmeraldOffer
                        )
                        Text(
                            text = round.questionsAsked,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddInterviewRoundDialog(
    applications: List<JobApplicationEntity>,
    preselectedAppId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (InterviewRoundEntity, String, String) -> Unit
) {
    var selectedApp by remember {
        mutableStateOf(
            applications.firstOrNull { it.id == preselectedAppId } ?: applications.first()
        )
    }
    var roundNumberText by remember { mutableStateOf("1") }
    var selectedType by remember { mutableStateOf(InterviewRoundType.TECHNICAL) }
    var daysFromNow by remember { mutableIntStateOf(1) }
    var interviewerText by remember { mutableStateOf("") }
    var formatLinkText by remember { mutableStateOf("Google Meet / Microsoft Teams") }
    var prepText by remember { mutableStateOf("Review system architecture, Kotlin Coroutines & STAR behavioral stories") }
    var questionsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule / Log Interview Round") },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (preselectedAppId == null) {
                    item {
                        Text("Company / Role", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(applications) { app ->
                                FilterChip(
                                    selected = selectedApp.id == app.id,
                                    onClick = { selectedApp = app },
                                    label = { Text(app.companyName) }
                                )
                            }
                        }
                    }
                }

                item {
                    Text("Interview Stage Type", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(InterviewRoundType.entries) { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = roundNumberText,
                            onValueChange = { roundNumberText = it },
                            label = { Text("Round #") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = formatLinkText,
                            onValueChange = { formatLinkText = it },
                            label = { Text("Platform / Office") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Text("When: In $daysFromNow day(s) (schedules local reminder)", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = daysFromNow.toFloat(),
                        onValueChange = { daysFromNow = it.toInt() },
                        valueRange = 0f..14f
                    )
                }

                item {
                    OutlinedTextField(
                        value = interviewerText,
                        onValueChange = { interviewerText = it },
                        label = { Text("Interviewer Name(s) & Role") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = prepText,
                        onValueChange = { prepText = it },
                        label = { Text("Prep Checklist") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                item {
                    OutlinedTextField(
                        value = questionsText,
                        onValueChange = { questionsText = it },
                        label = { Text("Questions Asked (Post-Interview Log)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val scheduledEpoch = System.currentTimeMillis() + (daysFromNow * 86_400_000L)
                    onSave(
                        InterviewRoundEntity(
                            applicationId = selectedApp.id,
                            roundNumber = roundNumberText.toIntOrNull() ?: 1,
                            roundType = selectedType,
                            scheduledAtEpoch = scheduledEpoch,
                            durationMins = selectedType.defaultDurationMins,
                            interviewerNames = interviewerText,
                            formatOrLink = formatLinkText,
                            prepChecklist = prepText,
                            questionsAsked = questionsText
                        ),
                        selectedApp.companyName,
                        selectedApp.roleTitle
                    )
                }
            ) {
                Text("Save & Set Reminder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * v3.0 Launches Android's native System Calendar app (Google Calendar / Samsung Calendar)
 * pre-populated with the interview title, duration, video call link, and prep checklist—100% offline.
 */
private fun launchSystemCalendarIntent(
    context: Context,
    round: InterviewRoundEntity,
    companyName: String,
    roleTitle: String
) {
    runCatching {
        val endTime = round.scheduledAtEpoch + (round.durationMins * 60_000L)
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, round.scheduledAtEpoch)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
            putExtra(
                CalendarContract.Events.TITLE,
                "Interview: $companyName — ${round.roundType.label} ($roleTitle)"
            )
            putExtra(CalendarContract.Events.EVENT_LOCATION, round.formatOrLink)
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Interviewer(s): ${round.interviewerNames}\nPrep Checklist: ${round.prepChecklist}"
            )
        }
        context.startActivity(intent)
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
