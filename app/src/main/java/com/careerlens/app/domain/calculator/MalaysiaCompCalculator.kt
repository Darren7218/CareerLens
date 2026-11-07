package com.careerlens.app.domain.calculator

import com.careerlens.app.data.local.CurrencyRateEntity
import com.careerlens.app.data.local.JobApplicationEntity
import com.careerlens.app.data.local.JobOfferEntity
import com.careerlens.app.domain.model.CurrentJobBaselineProfile
import kotlin.math.max
import kotlin.math.min

/**
 * Detailed Compensation Breakdown normalized into both the Offer's Original Currency
 * and the user's selected Display Currency (default MYR), plus:
 * - v2.0 Increment % vs Current Job
 * - v3.0 4-Year Cumulative Wealth Trajectory (Cash + Vested Equity + Compounded KWSP)
 * - v3.0 KWSP 3-Account Breakdown (75% Persaraan / 15% Sejahtera / 10% Fleksi)
 * - v3.0 Cross-Border Cost-of-Living (Rent + Food) True Monthly Net Savings
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
    // v3.0 Cost-of-Living (COL) Rent & Living Adjusted Monthly Savings
    val monthlyRentAndLivingCost: Double,
    val monthlyTrueNetSavingsAfterLiving: Double, // Disposable minus Rent & Living Cost
    // Annual Figures (in Display Currency)
    val annualBaseSalary: Double,
    val annualFixedAllowances: Double,
    val annualTotalBonus: Double,
    val annualEmployerEpfContribution: Double,
    val annualEquityAndSignOn: Double,
    val annualTotalCompensationPackage: Double,
    val annualNetAfterTaxAndCommute: Double,
    // v3.0 KWSP (EPF) 3-Account Annual Contribution + 4-Year Compounded Balance
    val annualTotalEpfContribution: Double, // Employee 11% + Employer 12-16%
    val annualEpfAccount1Persaraan: Double, // 75% Retirement
    val annualEpfAccount2Sejahtera: Double, // 15% Housing/Medical/Education
    val annualEpfAccount3Fleksi: Double,    // 10% Anytime Withdrawal
    val fourYearCompoundedEpfBalance: Double,
    // v3.0 4-Year Cumulative Wealth Trajectory (Year 1, Year 2, Year 3, Year 4 cumulative totals)
    val yearlyTotalCompByYear: List<Double>, // [Y1, Y2, Y3, Y4]
    val fourYearCumulativeWealth: Double,    // Total 4-year Net Cash + Vested Equity + Compounded EPF
    // Effective Hourly & Personal Score
    val effectiveHourlyNetRate: Double,
    val weightedDecisionScore: Int,
    // v2.0 Comparison against User's Current Job Baseline
    val netMonthlyDeltaVsCurrent: Double = 0.0,
    val netMonthlyJumpPercent: Double = 0.0,
    val annualPackageDeltaVsCurrent: Double = 0.0,
    val annualPackageJumpPercent: Double = 0.0
)

object MalaysiaCompCalculator {

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
            estimatedMonthlyCommuteCostMyr = baseline.monthlyCommuteCostMyr,
            estimatedMonthlyRentAndLivingMyr = baseline.monthlyRentAndLivingMyr
        )
        val res = evaluateOfferInternal(
            application = syntheticApp,
            offer = syntheticOffer,
            currencyMap = currencyMap,
            displayCurrencyCode = displayCurrencyCode,
            additionalTaxReliefsMyr = baseline.additionalTaxReliefsMyr,
            annualZakatRebateMyr = baseline.annualZakatRebateMyr,
            epfDividendPercent = baseline.expectedEpfDividendPercent
        )
        return res.monthlyTrueDisposablePay to res.annualTotalCompensationPackage
    }

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
            annualZakatRebateMyr = baseline.annualZakatRebateMyr,
            epfDividendPercent = baseline.expectedEpfDividendPercent
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
        annualZakatRebateMyr: Double,
        epfDividendPercent: Double
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
        val annualEmployeeEpfMyr = monthlyEmployeeEpfMyr * 12.0
        val annualEmployerEpfMyr = (epfSubjectMonthlyMyr * 12.0) * (offer.employerEpfPercent / 100.0)
        val annualTotalEpfMyr = annualEmployeeEpfMyr + annualEmployerEpfMyr

        // v3.0 KWSP 2024/2026 3-Account Allocation (75% Persaraan / 15% Sejahtera / 10% Fleksi)
        val epfAcc1Myr = annualTotalEpfMyr * 0.75
        val epfAcc2Myr = annualTotalEpfMyr * 0.15
        val epfAcc3FleksiMyr = annualTotalEpfMyr * 0.10

        // v3.0 4-Year Compounded KWSP Balance at expected dividend rate (default 5.8% p.a.)
        val divRate = (epfDividendPercent / 100.0).coerceIn(0.0, 0.15)
        var compoundedEpfMyr = 0.0
        for (yr in 1..4) {
            // Contributions made throughout the year earn ~half-year dividend in their deposit year, full dividend thereafter
            compoundedEpfMyr = (compoundedEpfMyr * (1.0 + divRate)) + (annualTotalEpfMyr * (1.0 + divRate * 0.5))
        }

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
                    annualEmployeeEpfMyr = annualEmployeeEpfMyr,
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

        // 4. Monthly Net Take-Home, Disposable After Commute & True Net Savings After Rent/Living
        val monthlyPayslipEmployeeEpfMyr = monthlyBaseMyr * (offer.employeeEpfPercent / 100.0)
        val monthlyGrossMyr = monthlyBaseMyr + monthlyAllowanceMyr
        val monthlyNetTakeHomeMyr = max(
            0.0,
            monthlyGrossMyr - monthlyPayslipEmployeeEpfMyr - monthlySocsoEisMyr - monthlyPcbTaxMyr
        )
        val monthlyCommuteMyr = if (offer.onsiteDaysPerWeek == 0) 0.0 else offer.estimatedMonthlyCommuteCostMyr
        val monthlyTrueDisposableMyr = max(0.0, monthlyNetTakeHomeMyr - monthlyCommuteMyr)
        val monthlyRentLivingMyr = max(0.0, offer.estimatedMonthlyRentAndLivingMyr)
        val monthlyTrueNetSavingsMyr = monthlyTrueDisposableMyr - monthlyRentLivingMyr

        // 5. Annual Total Compensation & v3.0 4-Year Vesting Trajectory
        val annualBaseMyr = monthlyBaseMyr * 12.0
        val annualAllowanceMyr = monthlyAllowanceMyr * 12.0
        val signOnMyr = toMyr(offer.signOnBonusOneOff)
        val annualEquityTargetMyr = toMyr(offer.annualizedEquityValue)
        val fourYearTotalGrantMyr = annualEquityTargetMyr * 4.0

        val vestWeights = parseVestingWeights(offer.vestingSchedule)
        val yearlyTotalCompDisplay = (0..3).map { yrIdx ->
            val vestedEquityThisYearMyr = fourYearTotalGrantMyr * (vestWeights[yrIdx] / 100.0)
            val signOnThisYearMyr = if (yrIdx == 0) signOnMyr else 0.0
            val totalYearMyr = annualBaseMyr + annualAllowanceMyr + annualBonusMyr + annualEmployerEpfMyr + vestedEquityThisYearMyr + signOnThisYearMyr
            fromMyrToDisplay(totalYearMyr)
        }

        val year1EquityMyr = fourYearTotalGrantMyr * (vestWeights[0] / 100.0)
        val annualEquityAndSignOnMyr = year1EquityMyr + signOnMyr
        val annualTotalPackageMyr =
            annualBaseMyr + annualAllowanceMyr + annualBonusMyr + annualEmployerEpfMyr + annualEquityAndSignOnMyr

        val netBonusAfterEpfMyr = annualBonusMyr * (1.0 - (offer.employeeEpfPercent / 100.0))
        val annualNetAfterTaxAndCommuteMyr =
            (monthlyTrueDisposableMyr * 12.0) + netBonusAfterEpfMyr + annualEquityAndSignOnMyr

        // 4-Year Cumulative Wealth = 4 yrs of Net Disposable Cash + 4 yrs Net Bonus + 4 yrs Vested Equity + SignOn + 4-Yr Compounded EPF
        val fourYearNetCashAndBonusMyr = ((monthlyTrueDisposableMyr * 12.0) + netBonusAfterEpfMyr) * 4.0
        val fourYearCumulativeWealthMyr =
            fourYearNetCashAndBonusMyr + fourYearTotalGrantMyr + signOnMyr + compoundedEpfMyr

        // 6. Effective Hourly Rate
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
            monthlyRentAndLivingCost = fromMyrToDisplay(monthlyRentLivingMyr),
            monthlyTrueNetSavingsAfterLiving = fromMyrToDisplay(monthlyTrueNetSavingsMyr),
            annualBaseSalary = fromMyrToDisplay(annualBaseMyr),
            annualFixedAllowances = fromMyrToDisplay(annualAllowanceMyr),
            annualTotalBonus = fromMyrToDisplay(annualBonusMyr),
            annualEmployerEpfContribution = fromMyrToDisplay(annualEmployerEpfMyr),
            annualEquityAndSignOn = fromMyrToDisplay(annualEquityAndSignOnMyr),
            annualTotalCompensationPackage = fromMyrToDisplay(annualTotalPackageMyr),
            annualNetAfterTaxAndCommute = fromMyrToDisplay(annualNetAfterTaxAndCommuteMyr),
            annualTotalEpfContribution = fromMyrToDisplay(annualTotalEpfMyr),
            annualEpfAccount1Persaraan = fromMyrToDisplay(epfAcc1Myr),
            annualEpfAccount2Sejahtera = fromMyrToDisplay(epfAcc2Myr),
            annualEpfAccount3Fleksi = fromMyrToDisplay(epfAcc3FleksiMyr),
            fourYearCompoundedEpfBalance = fromMyrToDisplay(compoundedEpfMyr),
            yearlyTotalCompByYear = yearlyTotalCompDisplay,
            fourYearCumulativeWealth = fromMyrToDisplay(fourYearCumulativeWealthMyr),
            effectiveHourlyNetRate = fromMyrToDisplay(effectiveHourlyMyr),
            weightedDecisionScore = compositeScore
        )
    }

    private fun parseVestingWeights(schedule: String): List<Double> {
        val parts = schedule.split(",").mapNotNull { it.trim().toDoubleOrNull() }
        if (parts.size == 4 && parts.sum() > 0.0) {
            return parts
        }
        return listOf(25.0, 25.0, 25.0, 25.0)
    }

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
