package com.careerlens.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.careerlens.app.domain.model.InterviewRoundType
import com.careerlens.app.domain.model.JobStage
import com.careerlens.app.domain.model.WorkMode
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "job_applications")
data class JobApplicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyName: String,
    val roleTitle: String,
    val stage: JobStage = JobStage.APPLIED,
    val workMode: WorkMode = WorkMode.HYBRID,
    val location: String = "Kuala Lumpur (KLCC / Bukit Bintang)",
    val currencyCode: String = "MYR",
    val salaryMinMonthly: Double = 0.0,
    val salaryMaxMonthly: Double = 0.0,
    val jobPortalSource: String = "JobStreet MY",
    val sourceUrl: String = "",
    val resumeVersion: String = "Resume_MY_v1.pdf",
    val recruiterName: String = "",
    val recruiterContact: String = "",
    val notes: String = "",
    val priorityRating: Int = 4, // 1 to 5 stars
    val screenshotUri: String? = null,
    val rawOcrText: String = "",
    val appliedDateEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "interview_rounds",
    foreignKeys = [
        ForeignKey(
            entity = JobApplicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["applicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("applicationId")]
)
data class InterviewRoundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val applicationId: Long,
    val roundNumber: Int = 1,
    val roundType: InterviewRoundType = InterviewRoundType.HR_SCREEN,
    val scheduledAtEpoch: Long = System.currentTimeMillis() + 86_400_000L,
    val durationMins: Int = 45,
    val interviewerNames: String = "",
    val formatOrLink: String = "Google Meet",
    val prepChecklist: String = "",
    val questionsAsked: String = "",
    val confidenceScore: Int = 4, // 1 to 5
    val isCompleted: Boolean = false
)

/**
 * Comprehensive Malaysian & Multi-Currency Offer Package Entity.
 * Supports EPF (KWSP) custom employee/employer rates, contractual & performance bonuses,
 * fixed tax-exempt allowances, RSUs/ESOS, and Klang Valley / Penang / JB commute costs.
 */
@Serializable
@Entity(
    tableName = "job_offers",
    foreignKeys = [
        ForeignKey(
            entity = JobApplicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["applicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("applicationId", unique = true)]
)
data class JobOfferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val applicationId: Long,
    val currencyCode: String = "MYR",
    val monthlyBaseSalary: Double = 0.0,
    // Fixed monthly allowances (Parking, Transport, Meal, WFH, Phone)
    val monthlyFixedAllowance: Double = 0.0,
    // Bonuses in months (e.g., 1.0 = 13th month AWS, 2.0 = 2 months performance bonus)
    val contractualBonusMonths: Double = 0.0,
    val targetPerformanceBonusMonths: Double = 1.0,
    val signOnBonusOneOff: Double = 0.0,
    // Equity / RSU / ESOS annualized value in offer currency
    val annualizedEquityValue: Double = 0.0,
    // Malaysian Statutory EPF (KWSP) Settings (Set to 0% for SGD/Foreign non-EPF offers if needed)
    val employeeEpfPercent: Double = 11.0,
    val employerEpfPercent: Double = 12.0, // 13% for <= RM5,000 or 15-16% for MNC/Bank perks
    val includeSocsoEisAndPcbEstimate: Boolean = true,
    // Work & Commute Reality (Toll + Petrol + Parking or LRT/MRT)
    val onsiteDaysPerWeek: Int = 3,
    val estimatedMonthlyCommuteCostMyr: Double = 350.0,
    // Non-cash perks & personal weighted scoring
    val annualLeaveDays: Int = 16,
    val annualMedicalBenefitValue: Double = 2500.0,
    val workLifeBalanceScore: Int = 8, // 1 to 10
    val careerGrowthScore: Int = 8, // 1 to 10
    val offerDeadlineEpoch: Long = System.currentTimeMillis() + (7 * 86_400_000L),
    val offerNotes: String = ""
)

@Serializable
@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey
    val code: String, // e.g., "MYR", "SGD", "USD"
    val symbol: String,
    val displayName: String,
    val rateToMyr: Double, // Manual offline rate: 1 unit of [code] = X MYR
    val flagEmoji: String,
    val isDefaultDisplayCurrency: Boolean = false
)
