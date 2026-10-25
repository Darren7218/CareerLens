package com.careerlens.app.domain.calculator

import com.careerlens.app.data.local.CurrencyRateEntity
import com.careerlens.app.data.local.JobApplicationEntity
import com.careerlens.app.data.local.JobOfferEntity
import kotlin.math.max
import kotlin.math.min

/**
 * Detailed Compensation Breakdown normalized into both the Offer's Original Currency
 * and the user's selected Display Currency (default MYR).
 */
data class OfferComparisonResult(
    val application: JobApplicationEntity,
    val offer: JobOfferEntity,
    val displayCurrencyCode: String,
    val displayCurrencySymbol: String,
    val exchangeRateToMyr: Double,
    // Monthly Figures (in Display Currency)
    val monthlyGrossBaseAndAllowance: Double,
    val monthlyEmployeeEpfDeduction: Double,
    val monthlySocsoEisDeduction: Double,
    val monthlyEstimatedPcbTax: Double,
    val monthlyNetTakeHomePay: Double,
    val monthlyCommuteCost: Double,
    val monthlyTrueDisposablePay: Double, // Net Take-Home minus Commute Cost
    // Annual Figures (in Display Currency)
    val annualBaseSalary: Double,
    val annualFixedAllowances: Double,
    val annualTotalBonus: Double,
    val annualEmployerEpfContribution: Double,
    val annualEquityAndSignOn: Double,
    val annualTotalCompensationPackage: Double, // Base + Allowances + Bonus + Employer EPF + Equity + SignOn
    val annualNetAfterTaxAndCommute: Double,
    // Effective Hourly & Personal Score
    val effectiveHourlyNetRate: Double,
    val weightedDecisionScore: Int // 0 to 100 composite score
)

object MalaysiaCompCalculator {

    /**
     * Computes Malaysian statutory deductions (EPF/KWSP, SOCSO/PERKESO, EIS, and progressive PCB Tax estimate)
     * and converts all metrics into the chosen display currency using the user's offline exchange rate table.
     */
    fun evaluateOffer(
        application: JobApplicationEntity,
        offer: JobOfferEntity,
        currencyMap: Map<String, CurrencyRateEntity>,
        displayCurrencyCode: String = "MYR"
    ): OfferComparisonResult {
        val offerRateToMyr = currencyMap[offer.currencyCode]?.rateToMyr ?: 1.0
        val displayCurrency = currencyMap[displayCurrencyCode]
        val displayRateToMyr = displayCurrency?.rateToMyr ?: 1.0
        val displaySymbol = displayCurrency?.symbol ?: "RM"

        // Helper to convert from Offer Currency -> MYR -> Display Currency
        fun toMyr(amountInOfferCurrency: Double): Double = amountInOfferCurrency * offerRateToMyr
        fun fromMyrToDisplay(amountInMyr: Double): Double =
            if (displayRateToMyr > 0.0) amountInMyr / displayRateToMyr else amountInMyr

        val monthlyBaseMyr = toMyr(offer.monthlyBaseSalary)
        val monthlyAllowanceMyr = toMyr(offer.monthlyFixedAllowance)
        val totalBonusMonths = offer.contractualBonusMonths + offer.targetPerformanceBonusMonths
        val annualBonusMyr = monthlyBaseMyr * totalBonusMonths
        val monthlyAmortizedBonusMyr = annualBonusMyr / 12.0

        // 1. Employee & Employer EPF (KWSP)
        // In Malaysia, EPF is levied on Base Salary + Bonuses (Fixed allowances vary, we apply to base + bonus)
        val epfSubjectMonthlyMyr = monthlyBaseMyr + monthlyAmortizedBonusMyr
        val monthlyEmployeeEpfMyr = epfSubjectMonthlyMyr * (offer.employeeEpfPercent / 100.0)
        val annualEmployerEpfMyr = (epfSubjectMonthlyMyr * 12.0) * (offer.employerEpfPercent / 100.0)

        // 2. SOCSO (PERKESO) + EIS (SIP) — Capped at RM 6,000 monthly wage ceiling in Malaysia (2024/2026 rules)
        val monthlySocsoEisMyr = if (offer.includeSocsoEisAndPcbEstimate && offer.currencyCode == "MYR") {
            val insurableSalary = min(monthlyBaseMyr, 6000.0)
            // Approx 0.5% SOCSO + 0.2% EIS = ~0.7% up to RM 41.65 max/month
            min(insurableSalary * 0.007, 41.65)
        } else {
            0.0
        }

        // 3. Malaysia Progressive PCB (Monthly Tax Deduction / LHDN) Estimate
        val annualGrossTaxableMyr = (monthlyBaseMyr * 12.0) + annualBonusMyr + (monthlyAllowanceMyr * 12.0 * 0.5)
        val annualTaxMyr = if (offer.includeSocsoEisAndPcbEstimate) {
            if (offer.currencyCode == "MYR") {
                estimateMalaysiaAnnualIncomeTaxMyr(
                    annualGrossMyr = annualGrossTaxableMyr,
                    annualEmployeeEpfMyr = monthlyEmployeeEpfMyr * 12.0
                )
            } else if (offer.currencyCode == "SGD") {
                // Lightweight Singapore resident progressive tax approximation for cross-border comparison
                estimateSingaporeAnnualTaxInMyr(annualGrossTaxableMyr, offerRateToMyr)
            } else {
                annualGrossTaxableMyr * 0.12 // Generic 12% effective tax estimate
            }
        } else {
            0.0
        }
        val monthlyPcbTaxMyr = annualTaxMyr / 12.0

        // 4. Monthly Net Take-Home & Disposable After Commute
        // Note: Employee EPF on monthly payslip is strictly on monthly base
        val monthlyPayslipEmployeeEpfMyr = monthlyBaseMyr * (offer.employeeEpfPercent / 100.0)
        val monthlyGrossMyr = monthlyBaseMyr + monthlyAllowanceMyr
        val monthlyNetTakeHomeMyr = max(
            0.0,
            monthlyGrossMyr - monthlyPayslipEmployeeEpfMyr - monthlySocsoEisMyr - monthlyPcbTaxMyr
        )
        val monthlyCommuteMyr = if (offer.onsiteDaysPerWeek == 0) 0.0 else offer.estimatedMonthlyCommuteCostMyr
        val monthlyTrueDisposableMyr = max(0.0, monthlyNetTakeHomeMyr - monthlyCommuteMyr)

        // 5. Annual Total Compensation Package
        val annualBaseMyr = monthlyBaseMyr * 12.0
        val annualAllowanceMyr = monthlyAllowanceMyr * 12.0
        val annualEquityAndSignOnMyr = toMyr(offer.annualizedEquityValue + offer.signOnBonusOneOff)
        val annualTotalPackageMyr =
            annualBaseMyr + annualAllowanceMyr + annualBonusMyr + annualEmployerEpfMyr + annualEquityAndSignOnMyr

        val netBonusAfterEpfMyr = annualBonusMyr * (1.0 - (offer.employeeEpfPercent / 100.0))
        val annualNetAfterTaxAndCommuteMyr =
            (monthlyTrueDisposableMyr * 12.0) + netBonusAfterEpfMyr + annualEquityAndSignOnMyr

        // 6. Effective Hourly Rate (factoring in Annual Leave & Commute hours)
        val workingWeeksPerYear = max(40.0, 52.0 - ((offer.annualLeaveDays + 11) / 5.0)) // +11 MY public holidays
        val weeklyCommuteHours = offer.onsiteDaysPerWeek * 1.5 // ~1.5h round-trip per onsite day
        val totalHoursPerYear = workingWeeksPerYear * (40.0 + weeklyCommuteHours)
        val effectiveHourlyMyr = if (totalHoursPerYear > 0) annualNetAfterTaxAndCommuteMyr / totalHoursPerYear else 0.0

        // 7. Composite Decision Score (Financial + WLB + Career Growth + Leave + Work Mode flexibility)
        val compScore = min(50.0, (annualTotalPackageMyr / 240_000.0) * 45.0) // Normalized around RM 20k/mo TC
        val wlbPoints = (offer.workLifeBalanceScore / 10.0) * 20.0
        val growthPoints = (offer.careerGrowthScore / 10.0) * 20.0
        val flexibilityPoints = ((5 - offer.onsiteDaysPerWeek) / 5.0) * 10.0
        val compositeScore = (compScore + wlbPoints + growthPoints + flexibilityPoints).toInt().coerceIn(10, 99)

        return OfferComparisonResult(
            application = application,
            offer = offer,
            displayCurrencyCode = displayCurrencyCode,
            displayCurrencySymbol = displaySymbol,
            exchangeRateToMyr = offerRateToMyr,
            monthlyGrossBaseAndAllowance = fromMyrToDisplay(monthlyGrossMyr),
            monthlyEmployeeEpfDeduction = fromMyrToDisplay(monthlyPayslipEmployeeEpfMyr),
            monthlySocsoEisDeduction = fromMyrToDisplay(monthlySocsoEisMyr),
            monthlyEstimatedPcbTax = fromMyrToDisplay(monthlyPcbTaxMyr),
            monthlyNetTakeHomePay = fromMyrToDisplay(monthlyNetTakeHomeMyr),
            monthlyCommuteCost = fromMyrToDisplay(monthlyCommuteMyr),
            monthlyTrueDisposablePay = fromMyrToDisplay(monthlyTrueDisposableMyr),
            annualBaseSalary = fromMyrToDisplay(annualBaseMyr),
            annualFixedAllowances = fromMyrToDisplay(annualAllowanceMyr),
            annualTotalBonus = fromMyrToDisplay(annualBonusMyr),
            annualEmployerEpfContribution = fromMyrToDisplay(annualEmployerEpfMyr),
            annualEquityAndSignOn = fromMyrToDisplay(annualEquityAndSignOnMyr),
            annualTotalCompensationPackage = fromMyrToDisplay(annualTotalPackageMyr),
            annualNetAfterTaxAndCommute = fromMyrToDisplay(annualNetAfterTaxAndCommuteMyr),
            effectiveHourlyNetRate = fromMyrToDisplay(effectiveHourlyMyr),
            weightedDecisionScore = compositeScore
        )
    }

    /**
     * LHDN Malaysia Resident Progressive Income Tax Brackets (YA 2024/2025/2026)
     * Includes standard RM 9,000 Individual Relief + up to RM 4,000 EPF Relief + RM 2,500 Lifestyle Relief.
     */
    private fun estimateMalaysiaAnnualIncomeTaxMyr(
        annualGrossMyr: Double,
        annualEmployeeEpfMyr: Double
    ): Double {
        val standardRelief = 9000.0 + min(4000.0, annualEmployeeEpfMyr) + 2500.0
        val chargeableIncome = max(0.0, annualGrossMyr - standardRelief)

        if (chargeableIncome <= 35_000.0) {
            // Eligible for RM 400 tax rebate when chargeable income <= RM 35,000
            val rawTax = when {
                chargeableIncome <= 5_000.0 -> 0.0
                chargeableIncome <= 20_000.0 -> (chargeableIncome - 5_000.0) * 0.01
                else -> 150.0 + (chargeableIncome - 20_000.0) * 0.03
            }
            return max(0.0, rawTax - 400.0)
        }

        return when {
            chargeableIncome <= 50_000.0 -> 600.0 + (chargeableIncome - 35_000.0) * 0.06
            chargeableIncome <= 70_000.0 -> 1_500.0 + (chargeableIncome - 50_000.0) * 0.11
            chargeableIncome <= 100_000.0 -> 3_700.0 + (chargeableIncome - 70_000.0) * 0.19
            chargeableIncome <= 400_000.0 -> 9_400.0 + (chargeableIncome - 100_000.0) * 0.25
            chargeableIncome <= 600_000.0 -> 84_400.0 + (chargeableIncome - 400_000.0) * 0.26
            else -> 136_400.0 + (chargeableIncome - 600_000.0) * 0.28
        }
    }

    private fun estimateSingaporeAnnualTaxInMyr(annualGrossMyr: Double, sgdRateToMyr: Double): Double {
        val annualSgd = if (sgdRateToMyr > 0) annualGrossMyr / sgdRateToMyr else annualGrossMyr / 3.32
        val taxSgd = when {
            annualSgd <= 20_000 -> 0.0
            annualSgd <= 30_000 -> (annualSgd - 20_000) * 0.02
            annualSgd <= 40_000 -> 200.0 + (annualSgd - 30_000) * 0.035
            annualSgd <= 80_000 -> 550.0 + (annualSgd - 40_000) * 0.07
            annualSgd <= 120_000 -> 3_350.0 + (annualSgd - 80_000) * 0.115
            else -> 7_950.0 + (annualSgd - 120_000) * 0.15
        }
        return taxSgd * sgdRateToMyr
    }
}
