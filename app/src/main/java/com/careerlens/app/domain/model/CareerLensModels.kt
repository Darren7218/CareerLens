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
}
