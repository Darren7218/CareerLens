package com.careerlens.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careerlens.app.data.local.JobApplicationEntity
import com.careerlens.app.domain.model.JobStage
import com.careerlens.app.domain.model.WorkMode
import com.careerlens.app.ui.theme.AmberAssessment
import com.careerlens.app.ui.theme.CyanInterview
import com.careerlens.app.ui.theme.ElectricIndigo
import com.careerlens.app.ui.theme.EmeraldOffer
import com.careerlens.app.ui.theme.RoseRejected
import com.careerlens.app.ui.theme.VioletWishlist
import com.careerlens.app.ui.theme.accentColor
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Generates a vibrant gradient monogram avatar for any company name without requiring internet logos.
 */
@Composable
fun CompanyMonogramAvatar(
    companyName: String,
    size: Dp = 44.dp
) {
    val gradients = listOf(
        listOf(Color(0xFF10B981), Color(0xFF047857)), // Grab / Emerald
        listOf(Color(0xFFF59E0B), Color(0xFFB45309)), // Maybank / Gold
        listOf(Color(0xFFF97316), Color(0xFFEA580C)), // Shopee / Orange
        listOf(Color(0xFF06B6D4), Color(0xFF0E7490)), // Petronas / Teal
        listOf(Color(0xFF6366F1), Color(0xFF4338CA)), // Indigo
        listOf(Color(0xFFEC4899), Color(0xFFBE185D))  // Rose
    )
    val index = abs(companyName.hashCode()) % gradients.size
    val words = companyName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val initials = when {
        words.size >= 2 -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
        words.size == 1 && words[0].length >= 2 -> words[0].take(2).uppercase()
        words.size == 1 -> words[0].take(1).uppercase()
        else -> "JB"
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.30f))
            .background(Brush.linearGradient(gradients[index])),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.36f).sp
        )
    }
}

@Composable
fun StagePillBadge(
    stage: JobStage,
    onClick: (() -> Unit)? = null
) {
    val color = stage.accentColor()
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stage.shortLabel,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun WorkModeBadge(workMode: WorkMode) {
    val tint = when (workMode) {
        WorkMode.REMOTE -> EmeraldOffer
        WorkMode.HYBRID -> CyanInterview
        WorkMode.ONSITE -> AmberAssessment
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = tint.copy(alpha = 0.12f)
    ) {
        Text(
            text = workMode.badgeText,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun BentoMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun JobApplicationCard(
    app: JobApplicationEntity,
    currencySymbol: String,
    onClick: () -> Unit,
    onQuickAdvanceStage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stageColor = app.stage.accentColor()
    val daysAgo = ((System.currentTimeMillis() - app.updatedAtEpoch) / 86_400_000L).coerceAtLeast(0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CompanyMonogramAvatar(companyName = app.companyName, size = 44.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.roleTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.companyName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StagePillBadge(stage = app.stage, onClick = onQuickAdvanceStage)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location + WorkMode + Salary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = app.location.substringBefore(","),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    WorkModeBadge(workMode = app.workMode)
                }

                if (app.salaryMaxMonthly > 0) {
                    val salaryLabel = if (app.salaryMinMonthly > 0 && app.salaryMinMonthly != app.salaryMaxMonthly) {
                        "$currencySymbol ${formatCompactNumber(app.salaryMinMonthly)}–${formatCompactNumber(app.salaryMaxMonthly)}/mo"
                    } else {
                        "$currencySymbol ${formatCurrencyAmount(app.salaryMaxMonthly)}/mo"
                    }
                    Text(
                        text = salaryLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                        color = EmeraldOffer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Source Portal, Priority Stars, and Quick Advance Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${app.jobPortalSource} • ${if (daysAgo == 0L) "Today" else "${daysAgo}d ago"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    repeat(app.priorityRating.coerceIn(1, 5)) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AmberAssessment,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                if (app.stage.stepOrder < JobStage.OFFER.stepOrder) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onQuickAdvanceStage() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Next Stage",
                            style = MaterialTheme.typography.labelMedium,
                            color = stageColor
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Advance stage",
                            tint = stageColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatCurrencyAmount(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    return formatter.format(amount)
}

fun formatCompactNumber(amount: Double): String {
    return if (amount >= 1000.0) {
        val k = amount / 1000.0
        if (k % 1.0 == 0.0) "${k.toInt()}k" else String.format(Locale.US, "%.1fk", k)
    } else {
        amount.toInt().toString()
    }
}
