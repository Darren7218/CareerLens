# CareerLens 🇲🇾 (v3.0) — Offline Android Job Tracker, 3-Mode Bounding-Box OCR, ATS Keyword Matcher & 4-Year Wealth/EPF Studio

**CareerLens** is a **100% offline, privacy-first native Android application** built with **Kotlin, Jetpack Compose (Material 3), Room SQLite, and bundled on-device Google ML Kit OCR**. It is tailored for the **Malaysian & regional job market** (with full multi-currency support) to track job applications, multi-round interviews, ATS resume keyword matching, notice period buyouts, and 4-year cumulative wealth & KWSP (EPF) projections.

---

## ✨ Key Features (v3.0 Upgrade)

### 1. 🏦 4-Year Cumulative Wealth & KWSP (EPF) 3-Account Dividend Projector
- **4-Year RSU / Equity Vesting Schedules:** Choose `Equal (25/25/25/25)`, `Back-Loaded (10/20/30/40)`, or `Amazon-Style (5/15/40/40)` to compare Year-1 packages against 4-Year cumulative wealth.
- **KWSP (EPF) 3-Account Splitter & Compounding (`5.8% p.a.`):** Automatically splits Employee (`11%`) + Employer (`12%–16%`) EPF contributions into **Akaun Persaraan (75%)**, **Akaun Sejahtera (15%)**, and **Akaun Fleksi (10% — withdrawable anytime)**, and compounds your 4-year EPF balance.
- **Malaysia vs. Singapore / Regional Cost-of-Living (COL) Rent & Food Engine:** Deducts real **Rent & Living presets** (*KL Staying w/ Family*, *KL Room Rental*, *KL Studio*, *JB → SG Daily Commute*, *Singapore Common Room / Studio*) to reveal **True Monthly Net Savings After Rent**.

### 2. 📸 3-Mode Bounding-Box OCR + Offline ATS Keyword Gap Analyzer
- **Interactive Bounding-Box Image Canvas:** Tap glowing bounding boxes directly on any shared screenshot from **JobStreet MY, LinkedIn, Hiredly, MyFutureJobs, or WhatsApp** to assign fields in 1 tap.
- **Offline Resume-to-JD ATS Keyword Matcher:** Compares scanned job posting screenshots against your customizable **Skills Profile** to compute an instant **ATS Match `%`**, highlighting **✓ Matched Keywords** and **+ Missing Keywords** to add before applying.
- **3 Specialized OCR Modes:** *Job Posting*, *Offer Letter* (extracts Base, Allowance, Bonus, Employer EPF `%`, Leave), and *Interview Invite* (extracts Round Type & Meet/Teams/Zoom links).

### 3. 🗓️ Notice Period Buyout Planner & 1-Tap Phone Calendar Integration
- **Malaysian Notice Period, Leave Offset & Buyout Calculator:** Calculates your **Official Last Day**, **Last Physical Working Day** (after offsetting unused Annual Leave), **Short-Notice Buyout Cost (`RM`)**, and net cash surplus after your new offer's **Sign-On Bonus**.
- **1-Tap Phone Calendar Sync (`CalendarContract`):** Push any interview round directly into Google Calendar or Samsung Calendar with zero permissions required.

### 4. 📄 Offline PDF Offer Report, Native File Save & Privacy Salary Mask
- **1-Page Printable PDF Offer Comparison Report:** Generates an A4 Landscape PDF comparing all offers side-by-side.
- **Native Android `Downloads` File Save/Restore:** Save `.pdf`, `.csv`, and `.json` backup files directly via the system file picker.
- **1-Tap Privacy Salary Mask (`RM •••••`):** Blur/mask all salary numbers when showing your phone in public.

---

## 🚀 Building the APK
```bash
./gradlew assembleDebug
```
The compiled debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
