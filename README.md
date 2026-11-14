# CareerLens 🇲🇾 (v4.0) — Offline Android Job Tracker, Semantic Neural ATS (`INT8 ONNX`), 3-Mode OCR & 4-Year Wealth/EPF Studio

**CareerLens** is a **100% offline, privacy-first native Android application** built with **Kotlin, Jetpack Compose (Material 3), Room SQLite, bundled Google ML Kit OCR, and on-device ONNX Runtime Mobile (`INT8`)**. It is tailored for the **Malaysian & regional job market** (with full multi-currency support) to track job applications, multi-round interviews, neural semantic resume-to-JD matching, notice period buyouts, and 4-year cumulative wealth & KWSP (EPF) projections.

---

## 🧠 New in v4.0: Offline Semantic Neural ATS & Resume Matcher (`.onnx`)

Unlike brittle exact-keyword matchers that fail on paraphrases and tech acronyms, CareerLens v4.0 bundles a **fine-tuned, per-channel INT8 quantized `TaylorAI/bge-micro-v2` neural sentence encoder (`16.65 MB`, `384-d` vectors)** trained on Google Colab (`Tesla T4`, `CoSENTLoss` + `CosineSimilarityLoss`) across tech synonym clusters, full resume-bullet pairs, Malaysian statutory taxonomy (`KWSP/EPF`, `LHDN PCB`, `SOCSO`, `DuitNow/FPX`, `Klang Valley`), and hard negatives (`Java ≠ JavaScript`, `LHDN PCB Payroll ≠ Hardware PCB`), executing **100% offline via `onnxruntime-android` + Pure-Kotlin BERT WordPiece Tokenizer**:

| Evaluation Metric / Benchmark Pair | Pre-Fine-Tune (`bge-micro-v2`) | Fine-Tuned INT8 ONNX (`16.65 MB`) | Gain / Separation |
| :--- | :---: | :---: | :---: |
| **`K8s` ↔ `Kubernetes`** | `0.6426` | **`0.8956`** | `+0.2530` |
| **`Next.js` ↔ `React`** | `0.5660` | **`0.8888`** | `+0.3228` |
| **`CI/CD` ↔ `DevOps pipeline`** | `0.4813` | **`0.8675`** | `+0.3862` |
| **`Jetpack Compose` ↔ `Declarative Android UI`** | `0.4212` | **`0.7927`** | `+0.3715` |
| **`KWSP` ↔ `EPF`** | `0.6226` | **`0.7903`** | `+0.1677` |
| **`Java` ↔ `JavaScript`** *(Hard Negative)* | `0.7410` *(False Positive!)* | **`0.0062`** | `-0.7348` |
| **Unseen Eval Margin** *(Positive Mean − Hard Negative Mean)* | `0.0276` | **`0.6846`** | **24.8× Margin Gain** |

- **Quantized Model Footprint:** Compressed `65.83 MB` (FP32) → **`16.65 MB` (`17.45 MB` on disk)** (`3.96×` smaller) with **`0.9976` FP32→INT8 cosine fidelity**.
- **Reproducible Colab Notebook:** Retrain, swap to `all-MiniLM-L6-v2`, or export `.tflite` anytime using [`colab/CareerLens_Semantic_ATS_FineTune_Quantize.ipynb`](colab/CareerLens_Semantic_ATS_FineTune_Quantize.ipynb).
- **Semantic Synonym Bridges + Experience Bullets Profile:** Stores both your comma-separated **Technical Skills** and full **Resume Experience Bullets**, highlighting **🔗 Semantic Synonym Bridges** (`"CI/CD" ↔ "DevOps automated release pipeline"`) and **⚠️ True Semantic Gaps** with 1-tap `+ Add` into your profile.

---

## ✨ Core Features (v1.0 – v4.0)

### 1. 📸 3-Mode Bounding-Box OCR + Neural Semantic ATS Analyzer
- **Interactive Bounding-Box Image Canvas:** Tap glowing bounding boxes directly on any shared screenshot from **JobStreet MY, LinkedIn, Hiredly, MyFutureJobs, or WhatsApp** to assign fields in 1 tap.
- **On-Device Semantic ATS Engine (`SemanticAtsEngine.kt` + `BertWordPieceTokenizer.kt`):** Encodes your skills + resume bullets and scanned JD requirements completely offline with live latency readout (`ms`).
- **3 Specialized OCR Modes:** *Job Posting*, *Offer Letter* (extracts Base, Allowance, Bonus, Employer EPF `%`, Leave), and *Interview Invite* (extracts Round Type & Meet/Teams/Zoom links).

### 2. 🏦 4-Year Cumulative Wealth & KWSP (EPF) 3-Account Dividend Projector
- **4-Year RSU / Equity Vesting Schedules:** Choose `Equal (25/25/25/25)`, `Back-Loaded (10/20/30/40)`, or `Amazon-Style (5/15/40/40)` to compare Year-1 packages against 4-Year cumulative wealth.
- **KWSP (EPF) 3-Account Splitter & Compounding (`5.8% p.a.`):** Automatically splits Employee (`11%`) + Employer (`12%–16%`) EPF contributions into **Akaun Persaraan (75%)**, **Akaun Sejahtera (15%)**, and **Akaun Fleksi (10%)**, compounding your 4-year EPF balance.
- **Malaysia vs. Singapore / Regional Cost-of-Living (COL) Engine:** Deducts real **Rent & Living presets** (*KL Staying w/ Family*, *KL Room Rental*, *KL Studio*, *JB → SG Daily Commute*, *Singapore Common Room / Studio*) with manual offline FX rates.

### 3. 🗓️ Notice Period Buyout Planner & 1-Tap Phone Calendar Integration
- **Malaysian Notice Period, Leave Offset & Buyout Calculator:** Calculates your **Official Last Day**, **Last Physical Working Day** (after offsetting unused Annual Leave), **Short-Notice Buyout Cost (`RM`)**, and net cash surplus after your new offer's **Sign-On Bonus**.
- **1-Tap Phone Calendar Sync (`CalendarContract`):** Push any interview round directly into Google Calendar or Samsung Calendar with zero extra permissions.

### 4. 📄 Offline PDF Offer Report, Native File Save & Privacy Salary Mask
- **1-Page Printable PDF Offer Comparison Report:** Generates an A4 Landscape PDF comparing all offers side-by-side.
- **Native Android `Downloads` File Save/Restore:** Save `.pdf`, `.csv`, and `.json` backup files directly via Android's Storage Access Framework (`CreateDocument` / `OpenDocument`).
- **1-Tap Privacy Salary Mask (`RM •••••`):** Mask all salary figures when viewing your pipeline in public.

---

## 🚀 Building & Testing

```bash
# Run JVM ONNX Runtime unit tests verifying the bundled INT8 neural model
./gradlew testDebugUnitTest

# Build the 100% offline Android APK (zero INTERNET permission)
./gradlew assembleDebug
```
The compiled debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
