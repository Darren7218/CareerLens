package com.careerlens.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careerlens.app.ui.theme.CyanInterview
import com.careerlens.app.ui.theme.ElectricIndigo
import com.careerlens.app.ui.theme.EmeraldOffer

/**
 * Enables tabular numerals (`tnum`) so aligned salaries, counters, and percentages
 * do not jitter horizontally during state transitions.
 */
fun TextStyle.withTabularNumbers(): TextStyle = copy(fontFeatureSettings = "tnum")

/**
 * Subtle dual-glow atmospheric backdrop that adds depth behind CareerLens screens
 * in both Obsidian Dark and Clean Daylight themes without cluttering foreground cards.
 */
@Composable
fun CareerLensAmbientBackdrop(
    modifier: Modifier = Modifier,
    primaryGlow: Color = ElectricIndigo,
    secondaryGlow: Color = CyanInterview,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryGlow.copy(alpha = 0.11f), Color.Transparent),
                    center = Offset(size.width * 0.14f, size.height * 0.04f),
                    radius = size.width * 0.72f
                ),
                center = Offset(size.width * 0.14f, size.height * 0.04f),
                radius = size.width * 0.72f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryGlow.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.14f),
                    radius = size.width * 0.58f
                ),
                center = Offset(size.width * 0.92f, size.height * 0.14f),
                radius = size.width * 0.58f
            )
        }
        content()
    }
}

/**
 * Animated circular score ring gauge used in the v4.0 Semantic ATS Match Card
 * and analytical score readouts.
 */
@Composable
fun AnimatedScoreRing(
    scorePercent: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 66.dp,
    strokeWidth: Dp = 6.5.dp,
    caption: String = "MATCH"
) {
    val clamped = scorePercent.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing),
        label = "scoreRingProgress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = accentColor.copy(alpha = 0.16f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.72f),
                        accentColor,
                        accentColor
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$clamped%",
                style = MaterialTheme.typography.titleMedium.withTabularNumbers().copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = accentColor,
                maxLines = 1
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 8.sp,
                    letterSpacing = 0.7.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * Animated horizontal progress bar for cosine similarity and conversion telemetry.
 */
@Composable
fun AnimatedSimilarityMeter(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 5.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedWidth by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "similarityMeterProgress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
    ) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(
            color = color.copy(alpha = 0.16f),
            cornerRadius = radius,
            size = size
        )
        if (animatedWidth > 0.005f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(color.copy(alpha = 0.75f), color)
                ),
                cornerRadius = radius,
                size = Size(width = size.width * animatedWidth, height = size.height)
            )
        }
    }
}

data class SegmentedPillOption<T>(
    val value: T,
    val label: String,
    val accentColor: Color = ElectricIndigo,
    val icon: ImageVector? = null
)

/**
 * Floating segmented pill control that animates selection state smoothly.
 * Set [scrollable] to true when rendering 4+ tabs on narrow viewports.
 */
@Composable
fun <T> SegmentedPillControl(
    options: List<SegmentedPillOption<T>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    ) {
        val rowModifier = if (scrollable) {
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(4.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(4.dp)
        }

        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = option.value == selectedValue
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) option.accentColor.copy(alpha = 0.22f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "pillBg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) option.accentColor.copy(alpha = 0.52f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "pillBorder"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) option.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 200),
                    label = "pillLabel"
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = bgColor,
                    border = if (isSelected) BorderStroke(1.dp, borderColor) else null,
                    modifier = (if (scrollable) Modifier else Modifier.weight(1f).fillMaxHeight())
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(option.value) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (option.icon != null) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = labelColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            ),
                            color = labelColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom styled floating pill SnackbarHost matching CareerLens Dark & Daylight surfaces.
 */
@Composable
fun CareerLensSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data: SnackbarData ->
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, EmeraldOffer.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(EmeraldOffer.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldOffer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = data.visuals.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                data.visuals.actionLabel?.let { actionText ->
                    TextButton(onClick = { data.performAction() }) {
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricIndigo
                        )
                    }
                }
            }
        }
    }
}
