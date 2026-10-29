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
 * v2.0 User's Current Job Baseline & LHDN Malaysia Tax Relief Profile.
 * Used to calculate exact +RM and +% increments for every job offer.
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
    // LHDN Malaysia Custom Tax Reliefs (Default RM 15,500 = 9k Individual + 4k EPF + 2.5k Lifestyle)
    val additionalTaxReliefsMyr: Double = 2000.0, // e.g., Medical/Parents, SSPN, Insurance, Child relief
    val annualZakatRebateMyr: Double = 0.0 // Direct 1:1 tax rebate for Muslim taxpayers in Malaysia
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
 * Exchange rates are 100% offline and manually editable by the user at any time.
 */
@Serializable
data class CurrencyPreset(
    val code: String,
    val symbol: String,
    val displayName: String,
    val rateToMyr: Double, // e.g., 1 SGD = 3.32 MYR, 1 MYR = 1.0 MYR
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
            title = "Counter-Offer: Unvested Bonus / Increment Buyout",
            category = "Salary Negotiation",
            scenario = "Leaving right before your annual performance bonus payout in Q1/Q2.",
            templateText = "Hi [Recruiter Name], I'm thrilled about the offer from [Company]. Because joining in [Month] means forfeiting my upcoming [X]-month performance bonus at my current company, could we explore a one-off sign-on bonus of RM [Amount] or bumping the base salary to RM [Target Base] to bridge that gap? If we can align on this, I am ready to sign right away."
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
