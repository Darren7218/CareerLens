package com.careerlens.app.domain.calculator

import com.careerlens.app.data.local.CurrencyRateEntity
import com.careerlens.app.data.local.JobApplicationEntity
import com.careerlens.app.data.local.JobOfferEntity
import com.careerlens.app.domain.model.CurrentJobBaselineProfile
import kotlin.math.max
import kotlin.math.min

/**
 * Detailed Compensation Breakdown normalized into both the Offer's Original Currency
 * and the user's selected Display Currency (default MYR), plus v2.0 Increment % vs Current Job.
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
    val weightedDecisionScore: Int, // 0 to 100 composite score
    // v2.0 Comparison against User's Current Job Baseline
    val netMonthlyDeltaVsCurrent: Double = 0.0,
    val netMonthlyJumpPercent: Double = 0.0,
    val annualPackageDeltaVsCurrent: Double = 0.0,
    val annualPackageJumpPercent: Double = 0.0
)

object MalaysiaCompCalculator {

    /**
     * Evaluates the user's Current Job Baseline into Display Currency so every Offer can be
     * compared directly against what the user currently earns.
     */
    fun evaluateBaseline(
        baseline: CurrentJobBaselineProfile,
        currencyMap: Map<String, CurrencyRateEntity>,
        displayCurrencyCode: String = "MYR"
    ): Pair<Double, Double> {
        val syntheticApp = JobApplicationEntity(
            id = -1L,
            companyName = baseline.currentRoleAndCompany,
            roleTitle = "Current Baseline"
        )
        val syntheticOffer = JobOfferEntity(
            applicationId = -1L,
            currencyCode = baseline.currencyCode,
            monthlyBaseSalary = baseline.monthlyBaseSalary,
            monthlyFixedAllowance = baseline.monthlyFixedAllowance,
            targetPerformanceBonusMonths = baseline.annualBonusMonths,
            employeeEpfPercent = baseline.employeeEpfPercent,
            employerEpfPercent = baseline.employerEpfPercent,
            estimatedMonthlyCommuteCostMyr = baseline.monthlyCommuteCostMyr
        )
        val res = evaluateOfferInternal(
            application = syntheticApp,
            offer = syntheticOffer,
            currencyMap = currencyMap,
            displayCurrencyCode = displayCurrencyCode,
            additionalTaxReliefsMyr = baseline.additionalTaxReliefsMyr,
            annualZakatRebateMyr = baseline.annualZakatRebateMyr
        )
        return res.monthlyTrueDisposablePay to res.annualTotalCompensationPackage
    }

    /**
     * Computes Malaysian statutory deductions (EPF/KWSP, SOCSO/PERKESO, EIS, and progressive PCB Tax estimate)
     * and converts all metrics into the chosen display currency using the user's offline exchange rate table.
     */
    fun evaluateOffer(
        application: JobApplicationEntity,
        offer: JobOfferEntity,
        currencyMap: Map<String, CurrencyRateEntity>,
        displayCurrencyCode: String = "MYR",
        baseline: CurrentJobBaselineProfile = CurrentJobBaselineProfile()
    ): OfferComparisonResult {
        val (baselineMonthlyDisposable, baselineAnnualPackage) = evaluateBaseline(
            baseline = baseline,
            currencyMap = currencyMap,
            displayCurrencyCode = displayCurrencyCode
        )

        val raw = evaluateOfferInternal(
            application = application,
            offer = offer,
            currencyMap = currencyMap,
            displayCurrencyCode = displayCurrencyCode,
            additionalTaxReliefsMyr = baseline.additionalTaxReliefsMyr,
            annualZakatRebateMyr = baseline.annualZakatRebateMyr
        )

        val netDelta = raw.monthlyTrueDisposablePay - baselineMonthlyDisposable
        val netJumpPct = if (baselineMonthlyDisposable > 0) (netDelta / baselineMonthlyDisposable) * 100.0 else 0.0

        val pkgDelta = raw.annualTotalCompensationPackage - baselineAnnualPackage
        val pkgJumpPct = if (baselineAnnualPackage > 0) (pkgDelta / baselineAnnualPackage) * 100.0 else 0.0

        return raw.copy(
            netMonthlyDeltaVsCurrent = netDelta,
            netMonthlyJumpPercent = netJumpPct,
            annualPackageDeltaVsCurrent = pkgDelta,
            annualPackageJumpPercent = pkgJumpPct
        )
    }

    private fun evaluateOfferInternal(
        application: JobApplicationEntity,
        offer: JobOfferEntity,
        currencyMap: Map<String, CurrencyRateEntity>,
        displayCurrencyCode: String,
        additionalTaxReliefsMyr: Double,
        annualZakatRebateMyr: Double
    ): OfferComparisonResult {
        val offerRateToMyr = currencyMap[offer.currencyCode]?.rateToMyr ?: 1.0
        val displayCurrency = currencyMap[displayCurrencyCode]
        val displayRateToMyr = displayCurrency?.rateToMyr ?: 1.0
        val displaySymbol = displayCurrency?.symbol ?: "RM"

        fun toMyr(amountInOfferCurrency: Double): Double = amountInOfferCurrency * offerRateToMyr
        fun fromMyrToDisplay(amountInMyr: Double): Double =
            if (displayRateToMyr > 0.0) amountInMyr / displayRateToMyr else amountInMyr

        val monthlyBaseMyr = toMyr(offer.monthlyBaseSalary)
        val monthlyAllowanceMyr = toMyr(offer.monthlyFixedAllowance)
        val totalBonusMonths = offer.contractualBonusMonths + offer.targetPerformanceBonusMonths
        val annualBonusMyr = monthlyBaseMyr * totalBonusMonths
        val monthlyAmortizedBonusMyr = annualBonusMyr / 12.0

        // 1. Employee & Employer EPF (KWSP)
        val epfSubjectMonthlyMyr = monthlyBaseMyr + monthlyAmortizedBonusMyr
        val monthlyEmployeeEpfMyr = epfSubjectMonthlyMyr * (offer.employeeEpfPercent / 100.0)
        val annualEmployerEpfMyr = (epfSubjectMonthlyMyr * 12.0) * (offer.employerEpfPercent / 100.0)

        // 2. SOCSO (PERKESO) + EIS (SIP) — Capped at RM 6,000 monthly wage ceiling in Malaysia
        val monthlySocsoEisMyr = if (offer.includeSocsoEisAndPcbEstimate && offer.currencyCode == "MYR") {
            val insurableSalary = min(monthlyBaseMyr, 6000.0)
            min(insurableSalary * 0.007, 41.65)
        } else {
            0.0
        }

        // 3. Malaysia Progressive PCB (Monthly Tax Deduction / LHDN) Estimate with Custom Reliefs & Zakat
        val annualGrossTaxableMyr = (monthlyBaseMyr * 12.0) + annualBonusMyr + (monthlyAllowanceMyr * 12.0 * 0.5)
        val annualTaxMyr = if (offer.includeSocsoEisAndPcbEstimate) {
            if (offer.currencyCode == "MYR") {
                estimateMalaysiaAnnualIncomeTaxMyr(
                    annualGrossMyr = annualGrossTaxableMyr,
                    annualEmployeeEpfMyr = monthlyEmployeeEpfMyr * 12.0,
                    additionalReliefsMyr = additionalTaxReliefsMyr,
                    zakatRebateMyr = annualZakatRebateMyr
                )
            } else if (offer.currencyCode == "SGD") {
                estimateSingaporeAnnualTaxInMyr(annualGrossTaxableMyr, offerRateToMyr)
            } else {
                annualGrossTaxableMyr * 0.12
            }
        } else {
            0.0
        }
        val monthlyPcbTaxMyr = annualTaxMyr / 12.0

        // 4. Monthly Net Take-Home & Disposable After Commute
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
        val workingWeeksPerYear = max(40.0, 52.0 - ((offer.annualLeaveDays + 11) / 5.0))
        val weeklyCommuteHours = offer.onsiteDaysPerWeek * 1.5
        val totalHoursPerYear = workingWeeksPerYear * (40.0 + weeklyCommuteHours)
        val effectiveHourlyMyr = if (totalHoursPerYear > 0) annualNetAfterTaxAndCommuteMyr / totalHoursPerYear else 0.0

        // 7. Composite Decision Score
        val compScore = min(50.0, (annualTotalPackageMyr / 240_000.0) * 45.0)
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
     * Includes standard RM 9,000 Individual Relief + up to RM 4,000 EPF Relief + RM 2,500 Lifestyle Relief
     * + User's Custom Reliefs (Medical, SSPN, Child, Insurance) and 1:1 Zakat Tax Rebate.
     */
    private fun estimateMalaysiaAnnualIncomeTaxMyr(
        annualGrossMyr: Double,
        annualEmployeeEpfMyr: Double,
        additionalReliefsMyr: Double,
        zakatRebateMyr: Double
    ): Double {
        val totalRelief = 9000.0 + min(4000.0, annualEmployeeEpfMyr) + 2500.0 + max(0.0, additionalReliefsMyr)
        val chargeableIncome = max(0.0, annualGrossMyr - totalRelief)

        val grossTax = if (chargeableIncome <= 35_000.0) {
            val rawTax = when {
                chargeableIncome <= 5_000.0 -> 0.0
                chargeableIncome <= 20_000.0 -> (chargeableIncome - 5_000.0) * 0.01
                else -> 150.0 + (chargeableIncome - 20_000.0) * 0.03
            }
            max(0.0, rawTax - 400.0)
        } else {
            when {
                chargeableIncome <= 50_000.0 -> 600.0 + (chargeableIncome - 35_000.0) * 0.06
                chargeableIncome <= 70_000.0 -> 1_500.0 + (chargeableIncome - 50_000.0) * 0.11
                chargeableIncome <= 100_000.0 -> 3_700.0 + (chargeableIncome - 70_000.0) * 0.19
                chargeableIncome <= 400_000.0 -> 9_400.0 + (chargeableIncome - 100_000.0) * 0.25
                chargeableIncome <= 600_000.0 -> 84_400.0 + (chargeableIncome - 400_000.0) * 0.26
                else -> 136_400.0 + (chargeableIncome - 600_000.0) * 0.28
            }
        }

        return max(0.0, grossTax - max(0.0, zakatRebateMyr))
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
