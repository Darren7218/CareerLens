package com.careerlens.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careerlens.app.data.local.CurrencyRateEntity
import com.careerlens.app.data.local.JobApplicationEntity
import com.careerlens.app.data.local.JobOfferEntity
import com.careerlens.app.domain.calculator.OfferComparisonResult
import com.careerlens.app.ui.components.CompanyMonogramAvatar
import com.careerlens.app.ui.components.formatCompactNumber
import com.careerlens.app.ui.components.formatCurrencyAmount
import com.careerlens.app.ui.theme.AmberAssessment
import com.careerlens.app.ui.theme.CyanInterview
import com.careerlens.app.ui.theme.ElectricIndigo
import com.careerlens.app.ui.theme.EmeraldOffer
import com.careerlens.app.ui.theme.RoseRejected
import com.careerlens.app.ui.theme.VioletWishlist
import kotlin.math.max

@Composable
fun SalaryComparisonScreen(
    evaluatedOffers: List<OfferComparisonResult>,
    allApplications: List<JobApplicationEntity>,
    currencies: List<CurrencyRateEntity>,
    displayCurrencyCode: String,
    displayCurrencySymbol: String,
    onSelectDisplayCurrency: (String) -> Unit,
    onSaveCurrencyRate: (code: String, symbol: String, name: String, rateToMyr: Double) -> Unit,
    onSaveOffer: (JobOfferEntity) -> Unit
) {
    var editingOffer by remember { mutableStateOf<JobOfferEntity?>(null) }
    var editingOfferCompany by remember { mutableStateOf("") }
    var showCurrencyRateModal by remember { mutableStateOf(false) }
    var showAddOfferPicker by remember { mutableStateOf(false) }

    val highestTcOffer = evaluatedOffers.maxByOrNull { it.annualTotalCompensationPackage }
    val highestDisposableOffer = evaluatedOffers.maxByOrNull { it.monthlyTrueDisposablePay }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header + Currency Preset Switcher & Rate Customizer
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Offer & Salary Studio",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "EPF (KWSP), SOCSO, PCB Tax, Commute & Multi-Currency Matrix",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { showCurrencyRateModal = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Default.CurrencyExchange,
                        contentDescription = null,
                        tint = ElectricIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Rates", color = ElectricIndigo)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Instant Currency Normalization Bar
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(currencies) { curr ->
                    val isSelected = curr.code == displayCurrencyCode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectDisplayCurrency(curr.code) },
                        label = {
                            Text(
                                text = "${curr.flagEmoji} ${curr.code} (${curr.symbol})" +
                                    if (curr.code != "MYR") " • RM ${String.format("%.2f", curr.rateToMyr)}" else " • Base",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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

        // 2. Stacked Annual Total Compensation Chart (Compose Canvas)
        if (evaluatedOffers.isNotEmpty()) {
            item {
                StackedCompensationChartCard(
                    evaluatedOffers = evaluatedOffers,
                    displaySymbol = displayCurrencySymbol
                )
            }

            // 3. Winner Insight Callout Banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (highestTcOffer != null) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = EmeraldOffer.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.dp, EmeraldOffer.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = EmeraldOffer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "HIGHEST TOTAL PACKAGE",
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                                        color = EmeraldOffer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = highestTcOffer.application.companyName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$displayCurrencySymbol ${formatCurrencyAmount(highestTcOffer.annualTotalCompensationPackage)} / yr",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldOffer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (highestDisposableOffer != null) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = CyanInterview.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.dp, CyanInterview.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = CyanInterview,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "BEST MONTHLY NET CASH",
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                                        color = CyanInterview
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = highestDisposableOffer.application.companyName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$displayCurrencySymbol ${formatCurrencyAmount(highestDisposableOffer.monthlyTrueDisposablePay)} / mo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyanInterview,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 4. Side-by-Side Horizontal Comparison Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Side-by-Side Offer Matrix (${evaluatedOffers.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = { showAddOfferPicker = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Offer to Compare")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    for (item in evaluatedOffers) {
                        DetailedOfferComparisonCard(
                            result = item,
                            onEditOffer = {
                                editingOfferCompany = item.application.companyName
                                editingOffer = item.offer
                            }
                        )
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No Active Offers Yet", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Add an offer package to any application to unlock the Malaysia EPF, PCB Tax, and Multi-Currency Comparison Matrix.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = { showAddOfferPicker = true }) {
                            Text("Create Offer Package")
                        }
                    }
                }
            }
        }
    }

    // Modal 1: Edit Offer Compensation Details (EPF %, Bonus, Allowances, Commute)
    if (editingOffer != null) {
        EditMalaysiaOfferDialog(
            companyName = editingOfferCompany,
            initialOffer = editingOffer!!,
            availableCurrencies = currencies.map { it.code },
            onDismiss = { editingOffer = null },
            onSave = { updated ->
                onSaveOffer(updated)
                editingOffer = null
            }
        )
    }

    // Modal 2: Pick an Application to attach a new Offer to
    if (showAddOfferPicker) {
        AlertDialog(
            onDismissRequest = { showAddOfferPicker = false },
            title = { Text("Select Application to Add/Edit Offer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allApplications.forEach { app ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAddOfferPicker = false
                                    editingOfferCompany = app.companyName
                                    editingOffer = evaluatedOffers
                                        .firstOrNull { it.application.id == app.id }
                                        ?.offer
                                        ?: JobOfferEntity(
                                            applicationId = app.id,
                                            currencyCode = app.currencyCode,
                                            monthlyBaseSalary = if (app.salaryMaxMonthly > 0) app.salaryMaxMonthly else 10000.0,
                                            monthlyFixedAllowance = 500.0,
                                            targetPerformanceBonusMonths = 2.0,
                                            employeeEpfPercent = if (app.currencyCode == "MYR") 11.0 else 0.0,
                                            employerEpfPercent = if (app.currencyCode == "MYR") 12.0 else 0.0
                                        )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompanyMonogramAvatar(companyName = app.companyName, size = 34.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(app.companyName, fontWeight = FontWeight.Bold)
                                    Text(app.roleTitle, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddOfferPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Modal 3: Offline Currency Exchange Rate Editor
    if (showCurrencyRateModal) {
        CurrencyExchangeEditorDialog(
            currencies = currencies,
            onDismiss = { showCurrencyRateModal = false },
            onSaveCurrency = onSaveCurrencyRate
        )
    }
}

@Composable
private fun StackedCompensationChartCard(
    evaluatedOffers: List<OfferComparisonResult>,
    displaySymbol: String
) {
    val maxTotalPackage = max(1.0, evaluatedOffers.maxOfOrNull { it.annualTotalCompensationPackage } ?: 1.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Annual Total Package Breakdown ($displaySymbol)",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Base + Fixed Allowances + Bonus + Employer EPF + RSU/Sign-On",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendDot("Base + Allow.", ElectricIndigo)
                LegendDot("Bonus", AmberAssessment)
                LegendDot("Employer EPF", EmeraldOffer)
                LegendDot("RSU / Equity", VioletWishlist)
            }

            Spacer(modifier = Modifier.height(14.dp))

            evaluatedOffers.forEach { res ->
                val baseAndAllow = res.annualBaseSalary + res.annualFixedAllowances
                val bonus = res.annualTotalBonus
                val employerEpf = res.annualEmployerEpfContribution
                val equity = res.annualEquityAndSignOn
                val total = res.annualTotalCompensationPackage

                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${res.application.companyName} (${res.offer.currencyCode})",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp)
                        )
                        Text(
                            text = "$displaySymbol ${formatCurrencyAmount(total)}/yr",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                            color = EmeraldOffer,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    ) {
                        val widthScale = (size.width / maxTotalPackage).toFloat()
                        var xCursor = 0f

                        fun drawSegment(amount: Double, color: Color) {
                            val segWidth = (amount * widthScale).toFloat()
                            if (segWidth > 0.5f) {
                                drawRect(
                                    color = color,
                                    topLeft = Offset(xCursor, 0f),
                                    size = Size(segWidth, size.height)
                                )
                                xCursor += segWidth
                            }
                        }

                        drawSegment(baseAndAllow, ElectricIndigo)
                        drawSegment(bonus, AmberAssessment)
                        drawSegment(employerEpf, EmeraldOffer)
                        drawSegment(equity, VioletWishlist)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailedOfferComparisonCard(
    result: OfferComparisonResult,
    onEditOffer: () -> Unit
) {
    val sym = result.displayCurrencySymbol
    val offer = result.offer
    val app = result.application

    Card(
        modifier = Modifier.width(305.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ),
        border = BorderStroke(1.dp, EmeraldOffer.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CompanyMonogramAvatar(companyName = app.companyName, size = 42.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = app.companyName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.roleTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Weighted Match Score Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElectricIndigo.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, ElectricIndigo)
                ) {
                    Text(
                        text = "${result.weightedDecisionScore}/100",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricIndigo,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Hero Metric: Monthly Net Take-Home & True Disposable After Commute
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = EmeraldOffer.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "EST. MONTHLY NET TAKE-HOME",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                        color = EmeraldOffer
                    )
                    Text(
                        text = "$sym ${formatCurrencyAmount(result.monthlyNetTakeHomePay)} / mo",
                        style = MaterialTheme.typography.headlineMedium,
                        color = EmeraldOffer
                    )
                    Text(
                        text = "After Commute (${offer.onsiteDaysPerWeek}d onsite): $sym ${formatCurrencyAmount(result.monthlyTrueDisposablePay)}/mo",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Detailed Breakdown Rows
            MetricLine("Gross Monthly Base + Allow.", "$sym ${formatCurrencyAmount(result.monthlyGrossBaseAndAllowance)}")
            MetricLine(
                "Employee EPF (${offer.employeeEpfPercent.toInt()}%)",
                "- $sym ${formatCurrencyAmount(result.monthlyEmployeeEpfDeduction)}",
                valueColor = RoseRejected
            )
            MetricLine(
                "Est. PCB Tax + SOCSO/EIS",
                "- $sym ${formatCurrencyAmount(result.monthlyEstimatedPcbTax + result.monthlySocsoEisDeduction)}",
                valueColor = RoseRejected
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            MetricLine(
                "Annual Base + Allowances",
                "$sym ${formatCurrencyAmount(result.annualBaseSalary + result.annualFixedAllowances)}"
            )
            MetricLine(
                "Bonuses (${offer.contractualBonusMonths + offer.targetPerformanceBonusMonths} mos)",
                "+ $sym ${formatCurrencyAmount(result.annualTotalBonus)}",
                valueColor = AmberAssessment
            )
            MetricLine(
                "Employer EPF (${offer.employerEpfPercent.toInt()}%)",
                "+ $sym ${formatCurrencyAmount(result.annualEmployerEpfContribution)}",
                valueColor = EmeraldOffer
            )
            if (result.annualEquityAndSignOn > 0) {
                MetricLine(
                    "RSU Equity + Sign-On / yr",
                    "+ $sym ${formatCurrencyAmount(result.annualEquityAndSignOn)}",
                    valueColor = VioletWishlist
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            MetricLine(
                "TOTAL ANNUAL PACKAGE",
                "$sym ${formatCurrencyAmount(result.annualTotalCompensationPackage)}",
                valueColor = EmeraldOffer,
                isBold = true
            )
            MetricLine(
                "Effective Net Hourly",
                "$sym ${formatCompactNumber(result.effectiveHourlyNetRate)} / hr"
            )
            MetricLine(
                "Annual Leave & WLB",
                "${offer.annualLeaveDays} days AL • WLB ${offer.workLifeBalanceScore}/10"
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onEditOffer,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Customize EPF, Bonus & Commute")
            }
        }
    }
}

@Composable
private fun MetricLine(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = valueColor,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

@Composable
fun EditMalaysiaOfferDialog(
    companyName: String,
    initialOffer: JobOfferEntity,
    availableCurrencies: List<String>,
    onDismiss: () -> Unit,
    onSave: (JobOfferEntity) -> Unit
) {
    var currencyCode by remember { mutableStateOf(initialOffer.currencyCode) }
    var baseSalary by remember { mutableStateOf(initialOffer.monthlyBaseSalary.toInt().toString()) }
    var allowance by remember { mutableStateOf(initialOffer.monthlyFixedAllowance.toInt().toString()) }
    var contractualBonus by remember { mutableStateOf(initialOffer.contractualBonusMonths.toString()) }
    var perfBonus by remember { mutableStateOf(initialOffer.targetPerformanceBonusMonths.toString()) }
    var equityAnnual by remember { mutableStateOf(initialOffer.annualizedEquityValue.toInt().toString()) }
    var signOn by remember { mutableStateOf(initialOffer.signOnBonusOneOff.toInt().toString()) }
    var employeeEpf by remember { mutableDoubleStateOf(initialOffer.employeeEpfPercent) }
    var employerEpf by remember { mutableDoubleStateOf(initialOffer.employerEpfPercent) }
    var includeTax by remember { mutableStateOf(initialOffer.includeSocsoEisAndPcbEstimate) }
    var onsiteDays by remember { mutableIntStateOf(initialOffer.onsiteDaysPerWeek) }
    var commuteCostMyr by remember { mutableStateOf(initialOffer.estimatedMonthlyCommuteCostMyr.toInt().toString()) }
    var leaveDays by remember { mutableStateOf(initialOffer.annualLeaveDays.toString()) }
    var wlbScore by remember { mutableIntStateOf(initialOffer.workLifeBalanceScore) }
    var growthScore by remember { mutableIntStateOf(initialOffer.careerGrowthScore) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offer Package: $companyName") },
        text = {
            LazyColumn(
                modifier = Modifier.height(440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Offer Currency Preset", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableCurrencies) { code ->
                            FilterChip(
                                selected = currencyCode == code,
                                onClick = {
                                    currencyCode = code
                                    if (code != "MYR") {
                                        employeeEpf = 0.0
                                        employerEpf = 0.0
                                    } else if (employeeEpf == 0.0 && employerEpf == 0.0) {
                                        employeeEpf = 11.0
                                        employerEpf = 12.0
                                    }
                                },
                                label = { Text(code) }
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = baseSalary,
                            onValueChange = { baseSalary = it },
                            label = { Text("Monthly Base ($currencyCode)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = allowance,
                            onValueChange = { allowance = it },
                            label = { Text("Fixed Allow./mo") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = contractualBonus,
                            onValueChange = { contractualBonus = it },
                            label = { Text("AWS / Fixed Bonus (mos)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = perfBonus,
                            onValueChange = { perfBonus = it },
                            label = { Text("Perf. Bonus (mos)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = equityAnnual,
                            onValueChange = { equityAnnual = it },
                            label = { Text("RSU/Equity per Yr") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = signOn,
                            onValueChange = { signOn = it },
                            label = { Text("Sign-On Bonus") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Text(
                        text = "EPF (KWSP) Rates — Employee: ${employeeEpf.toInt()}% | Employer: ${employerEpf.toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                        color = EmeraldOffer
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.0 to 0.0, 11.0 to 12.0, 11.0 to 13.0, 11.0 to 16.0).forEach { (emp, empr) ->
                            FilterChip(
                                selected = employeeEpf == emp && employerEpf == empr,
                                onClick = {
                                    employeeEpf = emp
                                    employerEpf = empr
                                },
                                label = { Text("${emp.toInt()}% / ${empr.toInt()}%", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Estimate PCB Tax + SOCSO/EIS", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = includeTax, onCheckedChange = { includeTax = it })
                    }
                }

                item {
                    Text(
                        text = "Onsite Days/Week: $onsiteDays days",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = onsiteDays.toFloat(),
                        onValueChange = { onsiteDays = it.toInt() },
                        valueRange = 0f..5f,
                        steps = 4
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = commuteCostMyr,
                            onValueChange = { commuteCostMyr = it },
                            label = { Text("Monthly Toll/Parking (RM)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = leaveDays,
                            onValueChange = { leaveDays = it },
                            label = { Text("Annual Leave Days") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Text("Work-Life Balance ($wlbScore/10) & Career Growth ($growthScore/10)", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = wlbScore.toFloat(),
                        onValueChange = { wlbScore = it.toInt() },
                        valueRange = 1f..10f
                    )
                    Slider(
                        value = growthScore.toFloat(),
                        onValueChange = { growthScore = it.toInt() },
                        valueRange = 1f..10f
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initialOffer.copy(
                            currencyCode = currencyCode,
                            monthlyBaseSalary = baseSalary.toDoubleOrNull() ?: 0.0,
                            monthlyFixedAllowance = allowance.toDoubleOrNull() ?: 0.0,
                            contractualBonusMonths = contractualBonus.toDoubleOrNull() ?: 0.0,
                            targetPerformanceBonusMonths = perfBonus.toDoubleOrNull() ?: 0.0,
                            annualizedEquityValue = equityAnnual.toDoubleOrNull() ?: 0.0,
                            signOnBonusOneOff = signOn.toDoubleOrNull() ?: 0.0,
                            employeeEpfPercent = employeeEpf,
                            employerEpfPercent = employerEpf,
                            includeSocsoEisAndPcbEstimate = includeTax,
                            onsiteDaysPerWeek = onsiteDays,
                            estimatedMonthlyCommuteCostMyr = commuteCostMyr.toDoubleOrNull() ?: 0.0,
                            annualLeaveDays = leaveDays.toIntOrNull() ?: 16,
                            workLifeBalanceScore = wlbScore,
                            careerGrowthScore = growthScore
                        )
                    )
                }
            ) {
                Text("Save Offer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CurrencyExchangeEditorDialog(
    currencies: List<CurrencyRateEntity>,
    onDismiss: () -> Unit,
    onSaveCurrency: (code: String, symbol: String, name: String, rateToMyr: Double) -> Unit
) {
    var selectedCode by remember { mutableStateOf("SGD") }
    var symbolText by remember { mutableStateOf("S$") }
    var nameText by remember { mutableStateOf("Singapore Dollar") }
    var rateToMyrText by remember { mutableStateOf("3.32") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offline Currency Presets & Rates") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "All comparisons convert offline using your custom anchor rates (1 Unit = X MYR). Tap any currency below to edit or type a new code.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(currencies) { curr ->
                        FilterChip(
                            selected = selectedCode == curr.code,
                            onClick = {
                                selectedCode = curr.code
                                symbolText = curr.symbol
                                nameText = curr.displayName
                                rateToMyrText = curr.rateToMyr.toString()
                            },
                            label = { Text("${curr.flagEmoji} ${curr.code} = RM ${curr.rateToMyr}") }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = selectedCode,
                        onValueChange = { selectedCode = it.uppercase() },
                        label = { Text("Code (e.g. SGD)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = symbolText,
                        onValueChange = { symbolText = it },
                        label = { Text("Symbol (S$)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = rateToMyrText,
                    onValueChange = { rateToMyrText = it },
                    label = { Text("1 $selectedCode equals how many MYR (RM)?") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = rateToMyrText.toDoubleOrNull() ?: 1.0
                    onSaveCurrency(selectedCode, symbolText, nameText, rate)
                    onDismiss()
                }
            ) {
                Text("Save Offline Rate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
