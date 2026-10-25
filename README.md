# CareerLens 🇲🇾 — Offline Android Job Tracker, Screenshot OCR & Salary/EPF Studio

**CareerLens** is a **100% offline, privacy-first native Android application** built with **Kotlin, Jetpack Compose (Material 3), Room SQLite, and bundled on-device Google ML Kit OCR**. It is tailored for the **Malaysian & regional job market** (with full multi-currency support) to track job applications, multi-round interviews, and side-by-side total compensation comparisons.

---

## ✨ Key Features

### 1. 📸 100% Offline Screenshot OCR & "Share-to-CareerLens"
- **System Share Target (`Intent.ACTION_SEND`):** Share any screenshot from **JobStreet MY, LinkedIn, Hiredly, MyFutureJobs, Indeed, or WhatsApp** directly to CareerLens.
- **Bundled On-Device ML Kit Text Recognition:** Extracts text and bounding boxes in `< 300ms` with **zero internet permissions**.
- **Interactive Tap-to-Fill Chips:** Tap any highlighted pill from the screenshot to assign it to `[Role]`, `[Company]`, `[Min/Max Salary]`, `[Location]`, or `[Notes]`.
- **Malaysia-Aware Regex Heuristics:** Automatically detects `RM` / `MYR` / `SGD` / `USD` monthly & annual salary ranges and Malaysian tech hubs (`Kuala Lumpur`, `Bangsar South`, `KL Sentral`, `Petaling Jaya`, `Cyberjaya`, `Penang`, `Johor Bahru`, `Remote`).

### 2. 💰 Malaysia Salary, EPF (KWSP) & Multi-Currency Comparison Studio
- **Currency Presets + Offline Exchange Rate Editor:** Built-in presets for **`MYR (RM)`**, **`SGD (S$)`**, **`USD ($)`**, **`EUR (€)`**, **`GBP (£)`**, and **`AUD (A$)`** with a manual exchange-rate editor.
- **Malaysian Statutory & Perk Breakdown:**
  - **Employee EPF (`11%`)** & **Employer EPF (`12%` / `13%` / `16%` MNC & Bank executive tiers)**
  - **SOCSO (PERKESO) + EIS** & **Progressive LHDN PCB Monthly Tax Estimate**
  - **Fixed Allowances**, **AWS (13th Month) & Performance Bonuses**, and **RSU / Equity Grants**
  - **Klang Valley / Regional Commute Deductor:** Factor in monthly tolls, petrol, season parking, or RapidKL passes based on `Onsite Days / Week` to compare **Monthly Net Take-Home Pay**, **True Disposable Cash After Commute**, and **Total Annual Package**.
- **Custom Compose Canvas Stacked Bar Charts:** Visualizes how Base, Allowances, Bonuses, Employer EPF, and Equity stack up across offers.

### 3. 📋 Executive Pipeline (Kanban + List) & Interview Hub
- **Dual Pipeline Views:** Switch seamlessly between a swipeable **Kanban Board** and a **Filterable List View** with Malaysia location quick-chips.
- **Interview Agenda & Local Reminders:** Track multi-round interviews (`HR Screen`, `Online Assessment`, `Technical`, `System Design`, `Hiring Manager`) with `WorkManager` local notifications.
- **100% Offline Data Portability:** Export and restore your full database via **JSON** or export applications and offers to **CSV**.

---

## 🛠️ Tech Stack
- **UI:** 100% Kotlin, Jetpack Compose, Material 3 (Executive Obsidian Dark & Clean Daylight themes)
- **Architecture:** Clean MVVM with Kotlin `Coroutines` & `StateFlow`
- **Database:** Room SQLite (`2.6.1`) with KSP
- **On-Device OCR:** Google ML Kit Text Recognition (`com.google.mlkit:text-recognition:16.0.1`)
- **Background Reminders:** AndroidX `WorkManager`

---

## 🚀 Building the APK
```bash
./gradlew assembleDebug
```
The compiled debug APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
