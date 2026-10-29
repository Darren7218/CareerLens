package com.careerlens.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careerlens.app.data.local.JobApplicationEntity
import com.careerlens.app.domain.model.JobStage
import com.careerlens.app.domain.model.MalaysiaPresets
import com.careerlens.app.domain.model.StarStoryItem
import com.careerlens.app.ui.components.CompanyMonogramAvatar
import com.careerlens.app.ui.theme.AmberAssessment
import com.careerlens.app.ui.theme.CyanInterview
import com.careerlens.app.ui.theme.ElectricIndigo
import com.careerlens.app.ui.theme.EmeraldOffer
import com.careerlens.app.ui.theme.RoseRejected
import java.util.Locale
import kotlin.math.max

@Composable
fun AnalyticsAndPlaybookScreen(
    applications: List<JobApplicationEntity>,
    starStories: List<StarStoryItem>,
    onSaveStarStory: (StarStoryItem) -> Unit,
    onShowToastMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Funnel & Radar, 1 = Negotiation Scripts, 2 = STAR Story Bank
    var showAddStarDialog by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val staleApps = remember(applications) {
        applications.filter { app ->
            val daysSinceUpdate = (now - app.updatedAtEpoch) / 86_400_000L
            app.stage.isActivePipeline && daysSinceUpdate >= 10L
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Sub-Tab Switcher
        item {
            Text(
                text = "Analytics & Negotiation Playbook",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Pipeline Conversion Funnel, Portal ROI, Recruiter Scripts & STAR Stories",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    "Funnel & Radar",
                    "MY Scripts",
                    "STAR Bank (${starStories.size})"
                )
                tabs.forEachIndexed { idx, title ->
                    val isSelected = selectedSubTab == idx
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSubTab = idx },
                        label = {
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricIndigo.copy(alpha = 0.22f),
                            selectedLabelColor = ElectricIndigo
                        )
                    )
                }
            }
        }

        when (selectedSubTab) {
            0 -> {
                // Sub-Tab 0: Pipeline Conversion Funnel + Portal Callback ROI + Stale Radar
                item {
                    ConversionFunnelCard(applications = applications)
                }
                item {
                    PortalRoiCard(applications = applications)
                }
                item {
                    StaleFollowUpRadarCard(
                        staleApps = staleApps,
                        onCopyFollowUpScript = { app ->
                            val script = "Hi ${app.recruiterName.ifBlank { "Hiring Team" }}, hope you're having a great week! I wanted to follow up on my application for the ${app.roleTitle} role at ${app.companyName}. I remain very keen on contributing to the team—please let me know if there are any updates on the next steps. Thank you!"
                            copyText(context, "Follow-Up Script", script)
                            onShowToastMessage("Copied follow-up message for ${app.companyName}!")
                        }
                    )
                }
            }

            1 -> {
                // Sub-Tab 1: Malaysia Recruiter Negotiation & Counter-Offer Scripts
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = EmeraldOffer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Malaysia Salary & Counter-Offer Scripts",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        text = "Tailored for Malaysian EPF differentials (16% vs 12%), bonus buyouts, and deadline extensions. Tap Copy to paste into WhatsApp or Email.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(MalaysiaPresets.negotiationScripts) { script ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(1.dp, EmeraldOffer.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(15.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ElectricIndigo.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = script.category.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                                    color = ElectricIndigo,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = script.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "When to use: ${script.scenario}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                color = AmberAssessment
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = script.templateText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    copyText(context, script.title, script.templateText)
                                    onShowToastMessage("Copied '${script.title}' to clipboard!")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy WhatsApp / Email Script")
                            }
                        }
                    }
                }
            }

            2 -> {
                // Sub-Tab 2: STAR Behavioral & Architecture Story Bank
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = CyanInterview,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "STAR Behavioral Story Bank",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Button(
                            onClick = { showAddStarDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Story")
                        }
                    }
                }

                items(starStories, key = { it.id }) { story ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(1.dp, CyanInterview.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(15.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = story.competency.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanInterview
                            )
                            Text(
                                text = "Q: ${story.questionPrompt}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "• Situation/Task: ${story.situationTask}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                            )
                            Text(
                                text = "• Action: ${story.actionTaken}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                            )
                            Text(
                                text = "• Result: ${story.resultImpact}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = EmeraldOffer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddStarDialog) {
        var competency by remember { mutableStateOf("") }
        var prompt by remember { mutableStateOf("") }
        var sitTask by remember { mutableStateOf("") }
        var action by remember { mutableStateOf("") }
        var result by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddStarDialog = false },
            title = { Text("Add STAR Interview Story") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = competency,
                        onValueChange = { competency = it },
                        label = { Text("Competency (e.g. System Design / Leadership)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("Interview Question") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sitTask,
                        onValueChange = { sitTask = it },
                        label = { Text("Situation & Task") }
                    )
                    OutlinedTextField(
                        value = action,
                        onValueChange = { action = it },
                        label = { Text("Action Taken") }
                    )
                    OutlinedTextField(
                        value = result,
                        onValueChange = { result = it },
                        label = { Text("Quantifiable Result (e.g. -50% latency)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveStarStory(
                            StarStoryItem(
                                id = (starStories.maxOfOrNull { it.id } ?: 0) + 1,
                                competency = competency.ifBlank { "Technical Excellence" },
                                questionPrompt = prompt.ifBlank { "Key achievement" },
                                situationTask = sitTask,
                                actionTaken = action,
                                resultImpact = result
                            )
                        )
                        showAddStarDialog = false
                    }
                ) {
                    Text("Save Story")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStarDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ConversionFunnelCard(applications: List<JobApplicationEntity>) {
    val totalApplied = max(1, applications.count { it.stage != JobStage.WISHLIST })
    val reachedOaOrHigher = applications.count {
        it.stage == JobStage.ASSESSMENT || it.stage == JobStage.INTERVIEW || it.stage == JobStage.OFFER || it.stage == JobStage.ACCEPTED
    }
    val reachedInterviewOrHigher = applications.count {
        it.stage == JobStage.INTERVIEW || it.stage == JobStage.OFFER || it.stage == JobStage.ACCEPTED
    }
    val reachedOffer = applications.count {
        it.stage == JobStage.OFFER || it.stage == JobStage.ACCEPTED
    }

    val steps = listOf(
        Triple("1. Applied", totalApplied, ElectricIndigo),
        Triple("2. OA / Assessment", reachedOaOrHigher, AmberAssessment),
        Triple("3. Interviewing", reachedInterviewOrHigher, CyanInterview),
        Triple("4. Offer Received", reachedOffer, EmeraldOffer)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Insights, contentDescription = null, tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pipeline Conversion Funnel", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "Stage progression across your active Malaysian & regional job hunt",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            steps.forEach { (label, count, color) ->
                val pct = (count.toFloat() / totalApplied.toFloat()).coerceIn(0f, 1f)
                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp))
                        Text(
                            "$count roles (${(pct * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    ) {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset.Zero,
                            size = Size(size.width * pct.coerceAtLeast(0.04f), size.height),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortalRoiCard(applications: List<JobApplicationEntity>) {
    val byPortal = applications.groupBy { it.jobPortalSource.ifBlank { "JobStreet MY" } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Job Portal Callback ROI", style = MaterialTheme.typography.titleLarge)
            Text(
                "Which Malaysian job board yields the highest interview & offer callback rate",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            byPortal.entries.forEach { (portal, apps) ->
                val callbacks = apps.count {
                    it.stage == JobStage.ASSESSMENT || it.stage == JobStage.INTERVIEW || it.stage == JobStage.OFFER || it.stage == JobStage.ACCEPTED
                }
                val rate = if (apps.isNotEmpty()) (callbacks * 100) / apps.size else 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(portal, style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp))
                    Text(
                        "$callbacks / ${apps.size} callbacks ($rate%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (rate >= 50) EmeraldOffer else CyanInterview
                    )
                }
            }
        }
    }
}

@Composable
private fun StaleFollowUpRadarCard(
    staleApps: List<JobApplicationEntity>,
    onCopyFollowUpScript: (JobApplicationEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, AmberAssessment.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Radar, contentDescription = null, tint = AmberAssessment, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Follow-Up Radar (${staleApps.size} Stale >10d)", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "Active applications with no stage update in 10+ days — tap to copy a personalized recruiter follow-up:",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (staleApps.isEmpty()) {
                Text(
                    "All active applications were updated within the last 10 days!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmeraldOffer
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    staleApps.forEach { app ->
                        val days = (System.currentTimeMillis() - app.updatedAtEpoch) / 86_400_000L
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCopyFollowUpScript(app) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    CompanyMonogramAvatar(companyName = app.companyName, size = 34.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("${app.companyName} (${days}d ago)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(app.roleTitle, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp))
                                    }
                                }
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Follow-Up", tint = AmberAssessment, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copyText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
