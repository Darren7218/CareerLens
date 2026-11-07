package com.careerlens.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Pipeline stages inspired by Huntr & Teal, adapted for rapid mobile workflow.
 */
@Serializable
enum class JobStage(val label: String, val shortLabel: String, val stepOrder: Int) {
    WISHLIST("Wishlist", "Saved", 0),
    APPLIED("Applied", "Applied", 1),
    ASSESSMENT("Online Assessment", "OA / Test", 2),
    INTERVIEW("Interviewing", "Interview", 3),
    OFFER("Offer Received", "Offer", 4),
    ACCEPTED("Offer Accepted", "Accepted", 5),
    REJECTED("Rejected / Closed", "Rejected", 6),
    GHOSTED("Ghosted / Stale", "Ghosted", 7);

    val isActivePipeline: Boolean
        get() = this == APPLIED || this == ASSESSMENT || this == INTERVIEW || this == OFFER
}

@Serializable
enum class WorkMode(val label: String, val badgeText: String, val defaultOnsiteDays: Int) {
    HYBRID("Hybrid", "Hybrid", 3),
    REMOTE("100% Remote", "Remote", 0),
    ONSITE("Full On-Site", "On-Site", 5)
}

@Serializable
enum class InterviewRoundType(val label: String, val defaultDurationMins: Int) {
    HR_SCREEN("HR / Recruiter Screen", 30),
    ONLINE_ASSESSMENT("Online Coding / Take-Home", 90),
    TECHNICAL("Technical / Coding Round", 60),
    SYSTEM_DESIGN("System Design / Architecture", 60),
    HIRING_MANAGER("Hiring Manager Round", 45),
    FINAL_CULTURE("Final / Executive / Culture", 45)
}

/**
 * v2.0 Multi-Mode Offline OCR Scanner modes.
 */
@Serializable
enum class OcrScanMode(val label: String, val subtitle: String) {
    JOB_AD("Job Posting", "JobStreet, LinkedIn, Hiredly"),
    OFFER_LETTER("Offer Letter", "Base, Bonus, EPF %, Leave"),
    INTERVIEW_INVITE("Interview Invite", "Date, Time, Meet/Teams Link")
}

/**
 * v2.0 + v3.0 User's Current Job Baseline, LHDN Malaysia Tax Reliefs, KWSP Dividend Rate & ATS Skills Profile.
 */
@Serializable
data class CurrentJobBaselineProfile(
    val currentRoleAndCompany: String = "Current Role (KL Hybrid)",
    val currencyCode: String = "MYR",
    val monthlyBaseSalary: Double = 9800.0,
    val monthlyFixedAllowance: Double = 400.0,
    val annualBonusMonths: Double = 1.5,
    val employeeEpfPercent: Double = 11.0,
    val employerEpfPercent: Double = 12.0,
    val monthlyCommuteCostMyr: Double = 280.0,
    val monthlyRentAndLivingMyr: Double = 2200.0,
    // LHDN Malaysia Custom Tax Reliefs (Default RM 15,500 = 9k Individual + 4k EPF + 2.5k Lifestyle)
    val additionalTaxReliefsMyr: Double = 2000.0, // e.g., Medical/Parents, SSPN, Insurance, Child relief
    val annualZakatRebateMyr: Double = 0.0, // Direct 1:1 tax rebate for Muslim taxpayers in Malaysia
    // v3.0 KWSP (EPF) Dividend Rate & Notice Period Settings
    val expectedEpfDividendPercent: Double = 5.8, // Historical KWSP conventional/shariah range ~5.5% - 6.3%
    val currentNoticePeriodMonths: Int = 2,
    val remainingAnnualLeaveDays: Int = 8,
    val userSkillsCsv: String = "Kotlin, Jetpack Compose, Android SDK, Coroutines, StateFlow, Room SQLite, MVVM, Clean Architecture, Google ML Kit, OCR, REST API, Git, CI/CD, Unit Testing, Agile"
)

/**
 * v3.0 Regional Cost-of-Living (COL) Rent + Food Presets in MYR equivalent per month.
 */
data class LivingCostPreset(
    val label: String,
    val monthlyCostMyr: Double,
    val description: String
)

/**
 * Built-in Malaysia Recruiter Negotiation & Follow-Up Script Playbook.
 */
data class NegotiationScriptTemplate(
    val title: String,
    val category: String,
    val scenario: String,
    val templateText: String
)

/**
 * Built-in STAR Behavioral & Technical Story Entry.
 */
@Serializable
data class StarStoryItem(
    val id: Int,
    val competency: String,
    val questionPrompt: String,
    val situationTask: String,
    val actionTaken: String,
    val resultImpact: String
)

/**
 * Built-in Currency Presets with Malaysia Ringgit (MYR) as the default base anchor.
 */
@Serializable
data class CurrencyPreset(
    val code: String,
    val symbol: String,
    val displayName: String,
    val rateToMyr: Double,
    val flagEmoji: String
)

object MalaysiaPresets {
    val defaultCurrencies = listOf(
        CurrencyPreset("MYR", "RM", "Malaysian Ringgit", 1.00, "🇲🇾"),
        CurrencyPreset("SGD", "S$", "Singapore Dollar", 3.32, "🇸🇬"),
        CurrencyPreset("USD", "US$", "US Dollar", 4.38, "🇺🇸"),
        CurrencyPreset("EUR", "€", "Euro", 4.75, "🇪🇺"),
        CurrencyPreset("GBP", "£", "British Pound", 5.65, "🇬🇧"),
        CurrencyPreset("AUD", "A$", "Australian Dollar", 2.90, "🇦🇺")
    )

    val livingCostPresets = listOf(
        LivingCostPreset("KL / PJ (Staying w/ Family)", 1100.0, "RM 0 Rent + RM 1,100 Food/Bills"),
        LivingCostPreset("KL / PJ (Room Rental)", 1950.0, "RM 850 Room + RM 1,100 Food/Bills"),
        LivingCostPreset("KL / PJ (Studio / Condo)", 3100.0, "RM 1,900 Studio + RM 1,200 Food/Bills"),
        LivingCostPreset("Penang / JB Local", 2200.0, "RM 1,100 Rent + RM 1,100 Food/Bills"),
        LivingCostPreset("JB → SG Daily Commute", 2600.0, "Live in JB (RM), Work in SG"),
        LivingCostPreset("Singapore (Common Room)", 7600.0, "~S$1,400 Room + S$900 Food/Transport"),
        LivingCostPreset("Singapore (Master/Studio)", 11200.0, "~S$2,400 Rent + S$1,000 Food/Transport")
    )

    val vestingPresets = listOf(
        "25,25,25,25" to "Equal (25% / yr)",
        "10,20,30,40" to "Back-Loaded (10/20/30/40)",
        "5,15,40,40" to "Amazon-Style (5/15/40/40)",
        "40,30,20,10" to "Front-Loaded (40/30/20/10)"
    )

    val commonTechAndDomainKeywords = listOf(
        "Kotlin", "Java", "Jetpack Compose", "Android", "MVVM", "MVI", "Clean Architecture",
        "Coroutines", "StateFlow", "Flow", "Room", "SQLite", "Hilt", "Dagger", "Koin",
        "Retrofit", "OkHttp", "GraphQL", "REST API", "gRPC", "Firebase", "ML Kit", "OCR",
        "WorkManager", "CameraX", "JUnit", "MockK", "Espresso", "CI/CD", "GitHub Actions",
        "Git", "Modularization", "Performance", "Security", "Encryption", "Biometric",
        "Fintech", "Banking", "Payments", "eWallet", "React Native", "Flutter", "Swift",
        "Spring Boot", "Microservices", "AWS", "GCP", "Docker", "Kubernetes", "Agile", "Scrum"
    )

    val popularMalaysiaLocations = listOf(
        "Kuala Lumpur (KLCC / Bukit Bintang)",
        "Bangsar South / Mid Valley, KL",
        "KL Sentral / Brickfields, KL",
        "Damansara / TTDI / Bandar Utama, PJ",
        "Petaling Jaya / Subang Jaya, Selangor",
        "Cyberjaya / Putrajaya",
        "Shah Alam / Klang, Selangor",
        "Bayan Lepas / George Town, Penang",
        "Johor Bahru / Iskandar Puteri, Johor",
        "Remote (Malaysia)",
        "Singapore (Cross-Border / Remote)"
    )

    val popularJobPortals = listOf(
        "JobStreet MY",
        "LinkedIn",
        "Hiredly",
        "MyFutureJobs",
        "Indeed MY",
        "Direct / Company Career Page",
        "Recruiter / Headhunter"
    )

    val negotiationScripts = listOf(
        NegotiationScriptTemplate(
            title = "Counter-Offer: Matching Higher Employer EPF (e.g. 16% -> 12%)",
            category = "Salary Negotiation",
            scenario = "Moving from a bank/GLC/MNC with 15–16% Employer EPF to a standard 12% EPF company.",
            templateText = "Hi [Recruiter Name], thank you so much for the offer to join [Company] as [Role]! I am very excited about the team. Looking at the total annual package, my current employer contributes 16% to Employer EPF plus fixed allowances. To make the transition neutral on retirement and net take-home pay, would [Company] be open to adjusting the monthly base salary to RM [Target Base] or adding a sign-on bonus of RM [Amount]? Looking forward to your thoughts!"
        ),
        NegotiationScriptTemplate(
            title = "Counter-Offer: Notice Period Buyout / Unvested Bonus",
            category = "Buyout & Bonus",
            scenario = "Your current company requires a 2–3 month notice period or you are forfeiting an upcoming bonus.",
            templateText = "Hi [Recruiter Name], I'm thrilled about the offer from [Company]. My current notice period is [2/3] months (or requires a RM [Amount] short-notice buyout to join earlier on [Target Date]). Could we explore a one-off sign-on / buyout bonus of RM [Amount] so I can onboard smoothly on your preferred start date? If we can align on this, I am ready to sign right away."
        ),
        NegotiationScriptTemplate(
            title = "Polite Follow-Up (10+ Days After Interview / Application)",
            category = "Follow-Up Radar",
            scenario = "No update from HR/Talent Acquisition after >10 days.",
            templateText = "Hi [Recruiter Name], hope you're having a great week! I wanted to follow up on my application/interview for the [Role] position at [Company]. I really enjoyed speaking with the team about [Topic] and remain very keen on the role. Please let me know if there are any updates on the timeline or if you need any additional details from my side. Thank you!"
        ),
        NegotiationScriptTemplate(
            title = "Offer Deadline Extension (Waiting for Competing KL/SG Offer)",
            category = "Timeline Management",
            scenario = "You have an offer expiring in 3 days but another final interview wrapping up.",
            templateText = "Hi [Recruiter Name], thank you again for the generous offer for the [Role] position. I am currently reviewing the full package details carefully with my family. Would it be possible to extend the offer acceptance deadline to [Date, e.g., next Wednesday] so I can make a well-considered commitment? Appreciate your understanding!"
        )
    )

    val defaultStarStories = listOf(
        StarStoryItem(
            id = 1,
            competency = "Offline-First Architecture & Reliability",
            questionPrompt = "Tell me about a time you solved a complex mobile performance or offline reliability issue.",
            situationTask = "Users in low-connectivity MRT/LRT tunnels experienced failed form submissions and slow startup.",
            actionTaken = "Architected a Room SQLite offline-first cache with Kotlin Coroutines StateFlow and WorkManager background sync.",
            resultImpact = "Reduced screen load time by 68% and achieved 99.9% crash-free offline sessions."
        ),
        StarStoryItem(
            id = 2,
            competency = "Production Incident & Root Cause Leadership",
            questionPrompt = "Describe a high-pressure production bug and how you handled it.",
            situationTask = "A third-party SDK update caused ANRs on Android 14 devices during peak payment hours.",
            actionTaken = "Profiled thread traces, isolated main-thread I/O blocking, shipped a hotfix moving initialization to Dispatchers.IO, and added automated benchmarks.",
            resultImpact = "Eliminated the ANR spike within 3 hours and improved Play Store rating from 4.2 to 4.7."
        ),
        StarStoryItem(
            id = 3,
            competency = "Cross-Functional Alignment (Product & Backend)",
            questionPrompt = "How do you handle disagreements with backend engineers or product managers?",
            situationTask = "Backend proposed 5 separate sequential API calls for a single dashboard screen, risking high mobile latency.",
            actionTaken = "Created a payload latency comparison prototype and collaborated on a unified aggregated endpoint with pagination.",
            resultImpact = "Cut mobile network overhead by 60% and shipped the feature 4 days ahead of schedule."
        )
    )
}
