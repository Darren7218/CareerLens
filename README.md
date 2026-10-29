# CareerLens 🇲🇾 (v2.0) — Offline Android Job Tracker, 3-Mode Bounding-Box OCR & Malaysia Salary/EPF Studio

**CareerLens** is a **100% offline, privacy-first native Android application** built with **Kotlin, Jetpack Compose (Material 3), Room SQLite, and bundled on-device Google ML Kit OCR**. It is tailored for the **Malaysian & regional job market** (with full multi-currency support) to track job applications, multi-round interviews, negotiation playbooks, and side-by-side total compensation comparisons.

---

## ✨ Key Features (v2.0 Upgrade)

### 1. 📸 3-Mode Interactive Bounding-Box OCR & Offline Vault
- **System Share Target (`Intent.ACTION_SEND`):** Share any screenshot from **JobStreet MY, LinkedIn, Hiredly, MyFutureJobs, Indeed, or WhatsApp** directly to CareerLens.
- **Interactive Bounding-Box Image Canvas:** Tap glowing bounding boxes directly on the screenshot image (or tap the extracted pills below) to assign fields in 1 second.
- **3 Specialized OCR Modes:**
  1. **Job Posting Mode:** Auto-detects Role, Company, `RM`/`SGD`/`USD` salary ranges, and Malaysian hubs (`Kuala Lumpur`, `Bangsar South`, `KL Sentral`, `PJ`, `Cyberjaya`, `Penang`, `JB`).
  2. **Offer Letter Mode:** Extracts Base Salary, Fixed Allowances, Bonus Months, Employer EPF `%`, and Annual Leave directly into the Offer Studio.
  3. **Interview Invite Mode:** Extracts interview round type and Google Meet / MS Teams / Zoom links with local reminder scheduling.
- **Persistent Internal Screenshot Vault:** Copies shared screenshots into internal app storage (`/files/screenshots/`) so gallery deletions never break attachments.

### 2. 💰 Malaysia Salary, EPF (KWSP), Baseline Jump `%` & Counter-Offer Studio
- **"My Current Job" Baseline & Increment `%` Calculator:** Set your current base salary, EPF `%`, and commute cost once to see exact **`+RM/mo` and `+%` Net Take-Home & Annual Package jumps** on every offer card.
- **Live What-If Counter-Offer Simulator:** Drag a live slider (`+RM 250` to `+RM 3,500/mo`) during recruiter calls to immediately see how a counter-offer affects Net Take-Home, Employer EPF, and LHDN PCB tax brackets.
- **LHDN Malaysia Tax Relief & Zakat Customizer:** Customize extra tax reliefs (Medical, SSPN, Child, Insurance) and 1:1 **Zakat / Fitrah rebates** for accurate monthly PCB estimates.
- **Currency Presets + Manual Rate Editor:** Built-in presets for **`MYR (RM)`**, **`SGD (S$)`**, **`USD ($)`**, **`EUR (€)`**, **`GBP (£)`**, and **`AUD (A$)`** with an offline exchange-rate editor.

### 3. 📊 Career Analytics Funnel & Malaysia Negotiation Playbook
- **Pipeline Conversion Funnel & Portal ROI:** Visualizes conversion across `Applied` → `Assessment` → `Interview` → `Offer` and tracks which job portal (*JobStreet MY* vs. *LinkedIn* vs. *Hiredly*) yields the highest callback rate.
- **Stale Application Follow-Up Radar:** Flags active applications with no update in `> 10 days` and copies personalized follow-up messages in 1 tap.
- **Malaysia Recruiter Negotiation Scripts & STAR Story Bank:** Copy-ready WhatsApp/Email templates for Employer EPF differentials (`16%` vs `12%`), unvested bonus buyouts, and deadline extensions, plus an editable **STAR Behavioral Story Bank**.

### 4. 📄 Offline PDF Offer Report, Native File Save & Privacy Salary Mask
- **1-Page Printable PDF Offer Comparison Report:** Generates an A4 Landscape PDF comparing all offers side-by-side using Android's native `PdfDocument` API.
- **Native Android `Downloads` File Save/Restore:** Save `.pdf`, `.csv`, and `.json` backup files directly via the system file picker.
- **1-Tap Privacy Salary Mask (`RM •••••`):** Toggle the eye icon in the top bar to blur/mask all salary figures when showing your app in public.

---

## 🚀 Building the APK
```bash
./gradlew assembleDebug
```
The compiled debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
