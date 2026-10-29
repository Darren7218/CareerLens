package com.careerlens.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.careerlens.app.data.local.InterviewRoundEntity
import com.careerlens.app.data.local.JobApplicationEntity
import com.careerlens.app.domain.model.InterviewRoundType
import com.careerlens.app.domain.model.JobStage
import com.careerlens.app.domain.model.MalaysiaPresets
import com.careerlens.app.domain.model.OcrScanMode
import com.careerlens.app.domain.model.WorkMode
import com.careerlens.app.domain.ocr.OcrBlockChip
import com.careerlens.app.domain.ocr.ParsedJobScreenshot
import com.careerlens.app.ui.theme.AmberAssessment
import com.careerlens.app.ui.theme.CyanInterview
import com.careerlens.app.ui.theme.ElectricIndigo
import com.careerlens.app.ui.theme.EmeraldOffer

private enum class AssignTargetField(val label: String, val color: Color) {
    ROLE("Role Title", ElectricIndigo),
    COMPANY("Company Name", EmeraldOffer),
    LOCATION("Location", CyanInterview),
    SALARY_MIN("Min / Base Salary", AmberAssessment),
    SALARY_MAX("Max Salary", AmberAssessment),
    NOTES("Add to Notes", Color(0xFFA855F7))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OcrScannerScreen(
    screenshotUri: Uri?,
    activeOcrMode: OcrScanMode,
    parsedResult: ParsedJobScreenshot,
    isProcessing: Boolean,
    availableCurrencies: List<String>,
    existingApplications: List<JobApplicationEntity>,
    onChangeOcrMode: (OcrScanMode) -> Unit,
    onPickScreenshotUri: (Uri) -> Unit,
    onLoadSampleTemplate: (Int) -> Unit,
    onUpdateParsedResult: (ParsedJobScreenshot) -> Unit,
    onSaveExtractedJob: (JobApplicationEntity, createOfferIfOfferStage: Boolean) -> Unit,
    onSaveExtractedOfferPackage: (
        app: JobApplicationEntity,
        monthlyBase: Double,
        monthlyAllowance: Double,
        bonusMonths: Double,
        employerEpfPct: Double,
        annualLeaveDays: Int
    ) -> Unit,
    onSaveExtractedInterviewRound: (InterviewRoundEntity, String, String) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onPickScreenshotUri(uri)
        }
    }

    var activeTargetField by remember { mutableStateOf(AssignTargetField.ROLE) }
    var demoIndex by remember { mutableIntStateOf(0) }
    var lastTappedChipId by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        if (parsedResult.fullRawText.isBlank() && screenshotUri == null) {
            onLoadSampleTemplate(0)
        }
    }

    var companyText by remember(parsedResult) { mutableStateOf(parsedResult.detectedCompany) }
    var roleText by remember(parsedResult) { mutableStateOf(parsedResult.detectedRole) }
    var currencyCode by remember(parsedResult) { mutableStateOf(parsedResult.detectedCurrency) }
    var salaryMinText by remember(parsedResult) {
        mutableStateOf(if (parsedResult.detectedSalaryMin > 0) parsedResult.detectedSalaryMin.toInt().toString() else "")
    }
    var salaryMaxText by remember(parsedResult) {
        mutableStateOf(if (parsedResult.detectedSalaryMax > 0) parsedResult.detectedSalaryMax.toInt().toString() else "")
    }
    var locationText by remember(parsedResult) { mutableStateOf(parsedResult.detectedLocation) }
    var selectedWorkMode by remember(parsedResult) { mutableStateOf(parsedResult.detectedWorkMode) }
    var selectedStage by remember(parsedResult) { mutableStateOf(parsedResult.detectedStage) }
    var portalText by remember(parsedResult) { mutableStateOf(parsedResult.detectedPortalSource) }
    var notesText by remember(parsedResult) { mutableStateOf(parsedResult.detectedNotes) }

    // v2.0 Offer Letter Mode state
    var allowanceText by remember(parsedResult) { mutableStateOf(parsedResult.detectedAllowanceMonthly.toInt().toString()) }
    var bonusMonthsText by remember(parsedResult) { mutableStateOf(parsedResult.detectedBonusMonths.toString()) }
    var employerEpfText by remember(parsedResult) { mutableStateOf(parsedResult.detectedEmployerEpfPercent.toInt().toString()) }
    var leaveDaysText by remember(parsedResult) { mutableStateOf(parsedResult.detectedAnnualLeaveDays.toString()) }

    // v2.0 Interview Invite Mode state
    var selectedRoundType by remember(parsedResult) { mutableStateOf(parsedResult.detectedInterviewRoundType) }
    var meetingLinkText by remember(parsedResult) { mutableStateOf(parsedResult.detectedMeetingLinkOrPlatform) }
    var selectedAppForInterview by remember(existingApplications) {
        mutableStateOf(existingApplications.firstOrNull())
    }

    fun applyChipToActiveField(chip: OcrBlockChip) {
        lastTappedChipId = chip.id
        when (activeTargetField) {
            AssignTargetField.ROLE -> {
                roleText = chip.text
                activeTargetField = AssignTargetField.COMPANY
            }
            AssignTargetField.COMPANY -> {
                companyText = chip.text
                activeTargetField = AssignTargetField.LOCATION
            }
            AssignTargetField.LOCATION -> {
                locationText = chip.text
            }
            AssignTargetField.SALARY_MIN -> {
                val num = Regex("\\d[\\d,.]*").find(chip.text)?.value?.replace(",", "") ?: ""
                if (num.isNotBlank()) salaryMinText = num
            }
            AssignTargetField.SALARY_MAX -> {
                val num = Regex("\\d[\\d,.]*").find(chip.text)?.value?.replace(",", "") ?: ""
                if (num.isNotBlank()) salaryMaxText = num
            }
            AssignTargetField.NOTES -> {
                notesText = if (notesText.isBlank()) chip.text else "$notesText\n• ${chip.text}"
            }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = ElectricIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "3-Mode Screenshot OCR",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Text(
                        text = "Interactive Bounding-Box Canvas + Persistent Offline Vault",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // v2.0 3-Mode OCR Switcher (Job Ad / Offer Letter / Interview Invite)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OcrScanMode.entries.forEach { mode ->
                    val isSelected = activeOcrMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onChangeOcrMode(mode) },
                        label = {
                            Text(
                                text = mode.label,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricIndigo.copy(alpha = 0.22f),
                            selectedLabelColor = ElectricIndigo
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Screenshot")
                }

                OutlinedButton(
                    onClick = {
                        demoIndex = (demoIndex + 1) % 3
                        onLoadSampleTemplate(demoIndex)
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EmeraldOffer.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = EmeraldOffer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val demoTypeLabel = when ((demoIndex + 1) % 3) {
                        0 -> "Job Ad"
                        1 -> "Offer"
                        else -> "Invite"
                    }
                    Text("Demo: $demoTypeLabel", color = EmeraldOffer)
                }
            }
        }

        // 2. v2.0 Interactive Bounding-Box Screenshot Canvas
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(255.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.55f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (screenshotUri != null) {
                        AsyncImage(
                            model = screenshotUri,
                            contentDescription = "Selected Job Screenshot",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    } else {
                        // Synthetic visual preview canvas when testing built-in Malaysia OCR Demo Templates
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF131C31), Color(0xFF0B111E))
                                    )
                                )
                        )
                    }

                    // Interactive Bounding Box Overlay — Tap any bounding box directly on the screenshot!
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(parsedResult.blockChips, activeTargetField) {
                                detectTapGestures { tapOffset ->
                                    val normX = tapOffset.x / size.width.coerceAtLeast(1)
                                    val normY = tapOffset.y / size.height.coerceAtLeast(1)
                                    val hitChip = parsedResult.blockChips.firstOrNull { c ->
                                        normX in (c.normalizedLeft - 0.03f)..(c.normalizedRight + 0.03f) &&
                                            normY in (c.normalizedTop - 0.02f)..(c.normalizedBottom + 0.02f)
                                    }
                                    if (hitChip != null) {
                                        applyChipToActiveField(hitChip)
                                    }
                                }
                            }
                    ) {
                        parsedResult.blockChips.forEach { chip ->
                            val boxColor = when {
                                chip.id == lastTappedChipId -> Color.White
                                chip.suggestedTag == "ROLE" -> ElectricIndigo
                                chip.suggestedTag == "COMPANY" -> EmeraldOffer
                                chip.suggestedTag == "SALARY" || chip.suggestedTag == "PERK" -> AmberAssessment
                                chip.suggestedTag == "LOCATION" -> CyanInterview
                                else -> Color(0xFF94A3B8)
                            }
                            val leftPx = chip.normalizedLeft * size.width
                            val topPx = chip.normalizedTop * size.height
                            val widthPx = ((chip.normalizedRight - chip.normalizedLeft) * size.width).coerceAtLeast(40f)
                            val heightPx = ((chip.normalizedBottom - chip.normalizedTop) * size.height).coerceAtLeast(22f)

                            drawRoundRect(
                                color = boxColor.copy(alpha = 0.22f),
                                topLeft = Offset(leftPx, topPx),
                                size = Size(widthPx, heightPx),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                            drawRoundRect(
                                color = boxColor.copy(alpha = 0.85f),
                                topLeft = Offset(leftPx, topPx),
                                size = Size(widthPx, heightPx),
                                cornerRadius = CornerRadius(8f, 8f),
                                style = Stroke(width = if (chip.id == lastTappedChipId) 4f else 2f)
                            )
                        }
                    }

                    // Overlay Text Labels when in Demo Preview Mode (no external photo bitmap)
                    if (screenshotUri == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            parsedResult.blockChips.take(7).forEach { chip ->
                                Text(
                                    text = chip.text,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    modifier = Modifier.clickable { applyChipToActiveField(chip) }
                                )
                            }
                        }
                    }

                    if (isProcessing) {
                        OcrLaserScanOverlay()
                    }
                }
            }
        }

        // 3. Interactive Tap-to-Fill OCR Chips
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = AmberAssessment,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tap Screenshot Boxes or Pills Below",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "Select target field → Tap any bounding box on the screenshot above or any text pill below:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AssignTargetField.entries) { field ->
                            val isSelected = activeTargetField == field
                            FilterChip(
                                selected = isSelected,
                                onClick = { activeTargetField = field },
                                label = {
                                    Text(
                                        text = "Assign to: ${field.label}",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = field.color.copy(alpha = 0.22f),
                                    selectedLabelColor = field.color
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        parsedResult.blockChips.forEach { chip ->
                            val chipTint = when (chip.suggestedTag) {
                                "ROLE" -> ElectricIndigo
                                "COMPANY" -> EmeraldOffer
                                "SALARY", "PERK" -> AmberAssessment
                                "LOCATION" -> CyanInterview
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = chipTint.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, chipTint.copy(alpha = 0.45f)),
                                modifier = Modifier.clickable { applyChipToActiveField(chip) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (chip.suggestedTag != null) {
                                        Text(
                                            text = "[${chip.suggestedTag}] ",
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                                            color = chipTint,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Text(
                                        text = chip.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Mode-Specific Verification Form (Job Ad vs Offer Letter vs Interview Invite)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = when (activeOcrMode) {
                            OcrScanMode.JOB_AD -> "Job Posting Details"
                            OcrScanMode.OFFER_LETTER -> "Offer Letter & EPF Package Extraction"
                            OcrScanMode.INTERVIEW_INVITE -> "Interview Invite Extraction"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (activeOcrMode == OcrScanMode.INTERVIEW_INVITE && existingApplications.isNotEmpty()) {
                        Text("Attach Interview to Application:", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(existingApplications) { app ->
                                FilterChip(
                                    selected = selectedAppForInterview?.id == app.id,
                                    onClick = {
                                        selectedAppForInterview = app
                                        companyText = app.companyName
                                        roleText = app.roleTitle
                                    },
                                    label = { Text(app.companyName) }
                                )
                            }
                        }

                        Text("Detected Interview Round Type:", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(InterviewRoundType.entries) { rt ->
                                FilterChip(
                                    selected = selectedRoundType == rt,
                                    onClick = { selectedRoundType = rt },
                                    label = { Text(rt.label, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = meetingLinkText,
                            onValueChange = { meetingLinkText = it },
                            label = { Text("Meeting Platform / Link (Meet, Teams, Zoom)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("Prep Checklist / Extracted Invite Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Button(
                            onClick = {
                                val targetApp = selectedAppForInterview ?: existingApplications.first()
                                onSaveExtractedInterviewRound(
                                    InterviewRoundEntity(
                                        applicationId = targetApp.id,
                                        roundNumber = 2,
                                        roundType = selectedRoundType,
                                        scheduledAtEpoch = System.currentTimeMillis() + 86_400_000L,
                                        durationMins = selectedRoundType.defaultDurationMins,
                                        formatOrLink = meetingLinkText,
                                        prepChecklist = notesText
                                    ),
                                    targetApp.companyName,
                                    targetApp.roleTitle
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanInterview)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Interview Round & Schedule Reminder", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Shared fields for JOB_AD & OFFER_LETTER
                        OutlinedTextField(
                            value = roleText,
                            onValueChange = { roleText = it },
                            label = { Text("Role Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = companyText,
                            onValueChange = { companyText = it },
                            label = { Text("Company Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text(
                            text = "Currency Preset & Monthly Salary",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val codes = if (availableCurrencies.isNotEmpty()) availableCurrencies else listOf("MYR", "SGD", "USD")
                            items(codes) { code ->
                                FilterChip(
                                    selected = currencyCode == code,
                                    onClick = { currencyCode = code },
                                    label = { Text(code) }
                                )
                            }
                        }

                        if (activeOcrMode == OcrScanMode.OFFER_LETTER) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = salaryMaxText.ifBlank { salaryMinText },
                                    onValueChange = {
                                        salaryMaxText = it
                                        salaryMinText = it
                                    },
                                    label = { Text("Monthly Base ($currencyCode)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = allowanceText,
                                    onValueChange = { allowanceText = it },
                                    label = { Text("Fixed Allowance/mo") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = employerEpfText,
                                    onValueChange = { employerEpfText = it },
                                    label = { Text("Employer EPF %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = bonusMonthsText,
                                    onValueChange = { bonusMonthsText = it },
                                    label = { Text("Bonus (Months)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = leaveDaysText,
                                    onValueChange = { leaveDaysText = it },
                                    label = { Text("Leave Days") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = salaryMinText,
                                    onValueChange = { salaryMinText = it },
                                    label = { Text("Min Monthly ($currencyCode)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = salaryMaxText,
                                    onValueChange = { salaryMaxText = it },
                                    label = { Text("Max Monthly ($currencyCode)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        OutlinedTextField(
                            value = locationText,
                            onValueChange = { locationText = it },
                            label = { Text("Location (Malaysia / Regional Hub)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(MalaysiaPresets.popularMalaysiaLocations) { presetLoc ->
                                FilterChip(
                                    selected = locationText == presetLoc,
                                    onClick = { locationText = presetLoc },
                                    label = { Text(presetLoc.substringBefore(","), fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("Notes / Perks Extracted") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Button(
                            onClick = {
                                val minSal = salaryMinText.toDoubleOrNull() ?: 0.0
                                val maxSal = (salaryMaxText.toDoubleOrNull() ?: minSal).coerceAtLeast(minSal)
                                val newJob = JobApplicationEntity(
                                    companyName = companyText.ifBlank { "Unnamed Company" },
                                    roleTitle = roleText.ifBlank { "Software Engineer" },
                                    stage = if (activeOcrMode == OcrScanMode.OFFER_LETTER) JobStage.OFFER else selectedStage,
                                    workMode = selectedWorkMode,
                                    location = locationText.ifBlank { "Kuala Lumpur" },
                                    currencyCode = currencyCode,
                                    salaryMinMonthly = minSal,
                                    salaryMaxMonthly = maxSal,
                                    jobPortalSource = portalText,
                                    notes = notesText,
                                    screenshotUri = parsedResult.vaultedScreenshotUri ?: screenshotUri?.toString(),
                                    rawOcrText = parsedResult.fullRawText
                                )
                                if (activeOcrMode == OcrScanMode.OFFER_LETTER) {
                                    onSaveExtractedOfferPackage(
                                        newJob,
                                        maxSal.coerceAtLeast(8000.0),
                                        allowanceText.toDoubleOrNull() ?: 400.0,
                                        bonusMonthsText.toDoubleOrNull() ?: 1.5,
                                        employerEpfText.toDoubleOrNull() ?: 12.0,
                                        leaveDaysText.toIntOrNull() ?: 18
                                    )
                                } else {
                                    onSaveExtractedJob(newJob, selectedStage == JobStage.OFFER)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldOffer)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (activeOcrMode == OcrScanMode.OFFER_LETTER)
                                    "Save Offer Letter & Compare in Studio"
                                else
                                    "Save Application to Pipeline",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OcrLaserScanOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "ocr_laser")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.TopCenter)
                .padding(top = (progress * 200).dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, ElectricIndigo, EmeraldOffer, Color.Transparent)
                    )
                )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = EmeraldOffer)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Running On-Device ML Kit OCR...",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
