package com.careerlens.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.careerlens.app.domain.model.InterviewRoundType
import com.careerlens.app.domain.model.JobStage
import com.careerlens.app.domain.model.MalaysiaPresets
import com.careerlens.app.domain.model.WorkMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CareerLensConverters {
    @TypeConverter
    fun fromJobStage(stage: JobStage): String = stage.name

    @TypeConverter
    fun toJobStage(value: String): JobStage =
        runCatching { JobStage.valueOf(value) }.getOrDefault(JobStage.APPLIED)

    @TypeConverter
    fun fromWorkMode(mode: WorkMode): String = mode.name

    @TypeConverter
    fun toWorkMode(value: String): WorkMode =
        runCatching { WorkMode.valueOf(value) }.getOrDefault(WorkMode.HYBRID)

    @TypeConverter
    fun fromRoundType(type: InterviewRoundType): String = type.name

    @TypeConverter
    fun toRoundType(value: String): InterviewRoundType =
        runCatching { InterviewRoundType.valueOf(value) }.getOrDefault(InterviewRoundType.HR_SCREEN)
}

@Dao
interface CareerLensDao {
    // --- Job Applications ---
    @Query("SELECT * FROM job_applications ORDER BY updatedAtEpoch DESC")
    fun observeAllApplications(): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications WHERE id = :id LIMIT 1")
    suspend fun getApplicationById(id: Long): JobApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApplication(application: JobApplicationEntity): Long

    @Update
    suspend fun updateApplication(application: JobApplicationEntity)

    @Delete
    suspend fun deleteApplication(application: JobApplicationEntity)

    // --- Interview Rounds ---
    @Query("SELECT * FROM interview_rounds ORDER BY scheduledAtEpoch ASC")
    fun observeAllInterviews(): Flow<List<InterviewRoundEntity>>

    @Query("SELECT * FROM interview_rounds WHERE applicationId = :applicationId ORDER BY roundNumber ASC, scheduledAtEpoch ASC")
    fun observeInterviewsForApplication(applicationId: Long): Flow<List<InterviewRoundEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInterviewRound(round: InterviewRoundEntity): Long

    @Delete
    suspend fun deleteInterviewRound(round: InterviewRoundEntity)

    // --- Job Offers ---
    @Query("SELECT * FROM job_offers")
    fun observeAllOffers(): Flow<List<JobOfferEntity>>

    @Query("SELECT * FROM job_offers WHERE applicationId = :applicationId LIMIT 1")
    fun observeOfferForApplication(applicationId: Long): Flow<JobOfferEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOffer(offer: JobOfferEntity): Long

    @Delete
    suspend fun deleteOffer(offer: JobOfferEntity)

    // --- Currency Presets & Rates ---
    @Query("SELECT * FROM currency_rates ORDER BY CASE WHEN code = 'MYR' THEN 0 WHEN code = 'SGD' THEN 1 WHEN code = 'USD' THEN 2 ELSE 3 END, code ASC")
    fun observeCurrencyRates(): Flow<List<CurrencyRateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrencyRate(rate: CurrencyRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrencyRates(rates: List<CurrencyRateEntity>)

    @Query("UPDATE currency_rates SET isDefaultDisplayCurrency = (code = :selectedCode)")
    suspend fun setDefaultDisplayCurrency(selectedCode: String)
}

@Database(
    entities = [
        JobApplicationEntity::class,
        InterviewRoundEntity::class,
        JobOfferEntity::class,
        CurrencyRateEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(CareerLensConverters::class)
abstract class CareerLensDatabase : RoomDatabase() {
    abstract fun dao(): CareerLensDao

    companion object {
        @Volatile
        private var INSTANCE: CareerLensDatabase? = null

        fun getInstance(context: Context): CareerLensDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CareerLensDatabase::class.java,
                    "careerlens_offline.db"
                ).build()
                INSTANCE = instance
                CoroutineScope(Dispatchers.IO).launch {
                    seedInitialDataIfEmpty(instance.dao())
                }
                instance
            }
        }

        /**
         * Seeds currency presets and realistic Malaysian tech/finance job applications,
         * interviews, and offers on first launch so the user can immediately explore
         * the Kanban pipeline, Interview Agenda, and Malaysia EPF/Salary Comparison charts.
         */
        private suspend fun seedInitialDataIfEmpty(dao: CareerLensDao) {
            val existingCurrencies = dao.observeCurrencyRates().first()
            if (existingCurrencies.isEmpty()) {
                val currencyEntities = MalaysiaPresets.defaultCurrencies.map { preset ->
                    CurrencyRateEntity(
                        code = preset.code,
                        symbol = preset.symbol,
                        displayName = preset.displayName,
                        rateToMyr = preset.rateToMyr,
                        flagEmoji = preset.flagEmoji,
                        isDefaultDisplayCurrency = preset.code == "MYR"
                    )
                }
                dao.upsertCurrencyRates(currencyEntities)
            }

            val existingApps = dao.observeAllApplications().first()
            if (existingApps.isNotEmpty()) return

            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L

            // 1. Grab Malaysia (Offer Received - Petaling Jaya / First Avenue)
            val grabId = dao.upsertApplication(
                JobApplicationEntity(
                    companyName = "Grab Malaysia",
                    roleTitle = "Senior Android Engineer",
                    stage = JobStage.OFFER,
                    workMode = WorkMode.HYBRID,
                    location = "Damansara / TTDI / Bandar Utama, PJ",
                    currencyCode = "MYR",
                    salaryMinMonthly = 11500.0,
                    salaryMaxMonthly = 14000.0,
                    jobPortalSource = "LinkedIn",
                    recruiterName = "Aisyah Rahman",
                    recruiterContact = "aisyah.r@grab.com",
                    notes = "Great regional mobility fintech team at First Avenue PJ. Hybrid 3 days onsite, strong RSU package and GrabFlex benefits.",
                    priorityRating = 5,
                    appliedDateEpoch = now - (18 * dayMs),
                    updatedAtEpoch = now - (1 * dayMs)
                )
            )

            // 2. Maybank (Malayan Banking Berhad) (Offer Received - High Employer EPF 16%)
            val maybankId = dao.upsertApplication(
                JobApplicationEntity(
                    companyName = "Maybank (MAE Digital)",
                    roleTitle = "Lead Mobile Developer (Android)",
                    stage = JobStage.OFFER,
                    workMode = WorkMode.HYBRID,
                    location = "KL Sentral / Brickfields, KL",
                    currencyCode = "MYR",
                    salaryMinMonthly = 11000.0,
                    salaryMaxMonthly = 13000.0,
                    jobPortalSource = "JobStreet MY",
                    recruiterName = "Daniel Tan",
                    recruiterContact = "+6012-345 6789",
                    notes = "Employer EPF contribution is 16%! Guaranteed 2.5+ months average performance bonus & medical coverage.",
                    priorityRating = 5,
                    appliedDateEpoch = now - (22 * dayMs),
                    updatedAtEpoch = now - (2 * dayMs)
                )
            )

            // 3. Shopee Singapore (Offer Received in SGD - Cross-Border / Relocation Comparison)
            val shopeeSgId = dao.upsertApplication(
                JobApplicationEntity(
                    companyName = "Shopee (Sea Group)",
                    roleTitle = "Software Engineer - Mobile Core",
                    stage = JobStage.OFFER,
                    workMode = WorkMode.ONSITE,
                    location = "Singapore (Cross-Border / Remote)",
                    currencyCode = "SGD",
                    salaryMinMonthly = 6200.0,
                    salaryMaxMonthly = 7200.0,
                    jobPortalSource = "LinkedIn",
                    recruiterName = "Chloe Lim",
                    notes = "Offer in SGD (S$6,500/mo). Compare purchasing power and Singapore tax vs staying in KL with EPF.",
                    priorityRating = 4,
                    appliedDateEpoch = now - (25 * dayMs),
                    updatedAtEpoch = now - (3 * dayMs)
                )
            )

            // 4. PETRONAS Digital (Interviewing - KLCC)
            val petronasId = dao.upsertApplication(
                JobApplicationEntity(
                    companyName = "PETRONAS Digital",
                    roleTitle = "Mobile Solutions Architect",
                    stage = JobStage.INTERVIEW,
                    workMode = WorkMode.HYBRID,
                    location = "Kuala Lumpur (KLCC / Bukit Bintang)",
                    currencyCode = "MYR",
                    salaryMinMonthly = 12000.0,
                    salaryMaxMonthly = 15500.0,
                    jobPortalSource = "MyFutureJobs",
                    recruiterName = "Farah Nadia",
                    notes = "Passed Technical Architecture round. Final Hiring Manager interview coming up this week.",
                    priorityRating = 5,
                    appliedDateEpoch = now - (12 * dayMs),
                    updatedAtEpoch = now - (1 * dayMs)
                )
            )

            // 5. Carsome (Online Assessment - Bangsar South)
            val carsomeId = dao.upsertApplication(
                JobApplicationEntity(
                    companyName = "Carsome",
                    roleTitle = "Android Kotlin Developer",
                    stage = JobStage.ASSESSMENT,
                    workMode = WorkMode.HYBRID,
                    location = "Bangsar South / Mid Valley, KL",
                    currencyCode = "MYR",
                    salaryMinMonthly = 8500.0,
                    salaryMaxMonthly = 11000.0,
                    jobPortalSource = "Hiredly",
                    notes = "Received HackerRank & take-home Jetpack Compose challenge.",
                    priorityRating = 4,
                    appliedDateEpoch = now - (6 * dayMs),
                    updatedAtEpoch = now - (2 * dayMs)
                )
            )

            // 6. TNG Digital (Touch 'n Go eWallet) (Applied - Bangsar South)
            dao.upsertApplication(
                JobApplicationEntity(
                    companyName = "TNG Digital",
                    roleTitle = "Senior Android Software Engineer",
                    stage = JobStage.APPLIED,
                    workMode = WorkMode.HYBRID,
                    location = "Bangsar South / Mid Valley, KL",
                    currencyCode = "MYR",
                    salaryMinMonthly = 10000.0,
                    salaryMaxMonthly = 13500.0,
                    jobPortalSource = "JobStreet MY",
                    notes = "Applied via JobStreet screenshot OCR.",
                    priorityRating = 4,
                    appliedDateEpoch = now - (3 * dayMs),
                    updatedAtEpoch = now - (3 * dayMs)
                )
            )

            // Seed Offers for Grab, Maybank, and Shopee SG
            dao.upsertOffer(
                JobOfferEntity(
                    applicationId = grabId,
                    currencyCode = "MYR",
                    monthlyBaseSalary = 12800.0,
                    monthlyFixedAllowance = 600.0, // GrabFlex + Mobile/Internet allowance
                    contractualBonusMonths = 0.0,
                    targetPerformanceBonusMonths = 2.0,
                    signOnBonusOneOff = 5000.0,
                    annualizedEquityValue = 18000.0, // RSU grant per year in MYR
                    employeeEpfPercent = 11.0,
                    employerEpfPercent = 12.0,
                    includeSocsoEisAndPcbEstimate = true,
                    onsiteDaysPerWeek = 3,
                    estimatedMonthlyCommuteCostMyr = 320.0, // LDP toll + PJ parking
                    annualLeaveDays = 18,
                    annualMedicalBenefitValue = 4000.0,
                    workLifeBalanceScore = 8,
                    careerGrowthScore = 9,
                    offerDeadlineEpoch = now + (5 * dayMs),
                    offerNotes = "Strong regional exposure + RM 18k/yr RSU vesting over 4 years."
                )
            )

            dao.upsertOffer(
                JobOfferEntity(
                    applicationId = maybankId,
                    currencyCode = "MYR",
                    monthlyBaseSalary = 12000.0,
                    monthlyFixedAllowance = 500.0,
                    contractualBonusMonths = 1.0,
                    targetPerformanceBonusMonths = 2.5,
                    signOnBonusOneOff = 0.0,
                    annualizedEquityValue = 0.0,
                    employeeEpfPercent = 11.0,
                    employerEpfPercent = 16.0, // Banking union/executive perk: 16% Employer EPF!
                    includeSocsoEisAndPcbEstimate = true,
                    onsiteDaysPerWeek = 3,
                    estimatedMonthlyCommuteCostMyr = 150.0, // LRT/MRT My50 + occasional grab to KL Sentral
                    annualLeaveDays = 21,
                    annualMedicalBenefitValue = 5000.0,
                    workLifeBalanceScore = 9,
                    careerGrowthScore = 8,
                    offerDeadlineEpoch = now + (7 * dayMs),
                    offerNotes = "16% Employer EPF + 21 days AL + low commute cost via KL Sentral LRT."
                )
            )

            dao.upsertOffer(
                JobOfferEntity(
                    applicationId = shopeeSgId,
                    currencyCode = "SGD",
                    monthlyBaseSalary = 6500.0, // S$ 6,500 (~RM 21,580 gross)
                    monthlyFixedAllowance = 200.0,
                    contractualBonusMonths = 0.0,
                    targetPerformanceBonusMonths = 2.0,
                    signOnBonusOneOff = 2000.0,
                    annualizedEquityValue = 4000.0, // in SGD
                    employeeEpfPercent = 0.0, // Work Pass (EP) in SG has 0% CPF
                    employerEpfPercent = 0.0,
                    includeSocsoEisAndPcbEstimate = true,
                    onsiteDaysPerWeek = 5,
                    estimatedMonthlyCommuteCostMyr = 450.0, // MRT + SG transport converted to MYR
                    annualLeaveDays = 15,
                    annualMedicalBenefitValue = 3000.0,
                    workLifeBalanceScore = 6,
                    careerGrowthScore = 9,
                    offerDeadlineEpoch = now + (10 * dayMs),
                    offerNotes = "High currency multiplier (SGD -> MYR), no EPF for EP holders, higher SG rental if relocating."
                )
            )

            // Seed Interview Rounds
            dao.upsertInterviewRound(
                InterviewRoundEntity(
                    applicationId = petronasId,
                    roundNumber = 1,
                    roundType = InterviewRoundType.HR_SCREEN,
                    scheduledAtEpoch = now - (9 * dayMs),
                    durationMins = 30,
                    interviewerNames = "Farah Nadia (Talent Acquisition)",
                    formatOrLink = "Microsoft Teams",
                    prepChecklist = "Expected salary range RM 12k-15k, notice period 1 month",
                    questionsAsked = "Walk through current Android architecture experience, team size, notice period.",
                    confidenceScore = 5,
                    isCompleted = true
                )
            )
            dao.upsertInterviewRound(
                InterviewRoundEntity(
                    applicationId = petronasId,
                    roundNumber = 2,
                    roundType = InterviewRoundType.SYSTEM_DESIGN,
                    scheduledAtEpoch = now + (1 * dayMs) + 14_400_000L, // Tomorrow
                    durationMins = 60,
                    interviewerNames = "Irwan Syah (Principal Architect) & Mei Ling",
                    formatOrLink = "Microsoft Teams • KLCC Tower 1",
                    prepChecklist = "Review Offline-First Room sync, Modularization, ML Kit OCR pipeline & CI/CD",
                    questionsAsked = "",
                    confidenceScore = 4,
                    isCompleted = false
                )
            )
            dao.upsertInterviewRound(
                InterviewRoundEntity(
                    applicationId = carsomeId,
                    roundNumber = 1,
                    roundType = InterviewRoundType.ONLINE_ASSESSMENT,
                    scheduledAtEpoch = now + (3 * dayMs),
                    durationMins = 90,
                    interviewerNames = "Engineering Panel",
                    formatOrLink = "HackerRank + GitHub Repo Submission",
                    prepChecklist = "Complete Coroutines Flow unit tests & Compose UI state handling",
                    questionsAsked = "",
                    confidenceScore = 4,
                    isCompleted = false
                )
            )
        }
    }
}
