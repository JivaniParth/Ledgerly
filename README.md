# Ledgerly

<div align="center">

![Ledgerly Banner](https://img.shields.io/badge/Ledgerly-Personal_Finance-059669?style=for-the-badge&logo=cashapp&logoColor=white)

**Smart, Offline-First Personal Finance Management for Android & Web**

[![Download Android APK](https://img.shields.io/badge/Download-Android_APK-059669?style=for-the-badge&logo=android&logoColor=white)](https://github.com/JivaniParth/Ledgerly/releases/latest)
[![Latest Release](https://img.shields.io/github/v/release/JivaniParth/Ledgerly?style=for-the-badge&color=blue)](https://github.com/JivaniParth/Ledgerly/releases/latest)
[![Android Min SDK](https://img.shields.io/badge/Min_SDK-24+-blue?style=for-the-badge&logo=android)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target_SDK-36-green?style=for-the-badge&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![React](https://img.shields.io/badge/Web-React_19_+_Vite-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev)

</div>

---

## 🌟 Overview

**Ledgerly** is a personal finance management application designed for speed, privacy, and reliability. Engineered with an **offline-first** philosophy, Ledgerly lets you log expenses instantly on your Android home screen or mobile app without waiting for network connectivity, while seamlessly syncing your financial data to the cloud via Supabase whenever you're online.

Ledgerly also includes a modern companion **Web App**, automated in-app updates (OTA), and hardware-backed credential encryption.

---

## 🚀 Key Features

### 💰 Core Financial Tracking & Smart Entry
- **Instant Logging & Custom Categories:** Quickly categorize expenses and incomes with rich emoji icons.
- **Embedded Calculator Keyboard:** Solve arithmetic expressions directly in the input field (`+`, `-`, `*`, `/`) with division-by-zero protection.
- **Real-Time Overdraft Protection:** Receive alerts immediately if an expense exceeds the selected wallet's remaining balance.
- **Sub-Penny Precision Safeguards:** Enforces strict 2-decimal rounding to prevent floating-point drift across all balance and ledger computations.
- **Dynamic Recurring Transactions:** Set up recurring schedules (daily, weekly, monthly, yearly) for automated utility bills, subscriptions, and recurring salaries.

### 📱 Android Home Screen Widget & Quick-Add
- **Live Glanceable Balances:** View current Expense Balance and Savings Balance right from your Android launcher.
- **1-Tap Privacy Toggle:** Mask your financial balances with a single tap (`₹ ••••••`) to prevent shoulder-surfing.
- **Instant Quick-Add Overlay:** Add transactions in under 3 seconds directly from the widget without launching the full application.
- **Guaranteed Synchronization:** Uses Android `WorkManager` with Room database persistence—transactions added while offline are queued and synced automatically without losing a single record.

### 🔄 In-App Auto-Updater (OTA Updates)
- **Zero-Friction Updates:** Checks GitHub Releases for new updates automatically on startup or manually via Settings.
- **Background Downloader:** Downloads APK releases with Android system progress notifications and checks hash integrity.
- **One-Tap In-App Installer:** Directly prompts the Android package installer from within the app—no manual browser downloads required.

### 👥 Split Expenses & Family Hub
- **Group Expense Splitting:** Split shared dinners, trips, and household bills across contacts or family members with equal or custom percentage splits.
- **Borrow & Lend Ledger:** Keep track of debts owed to you or borrowed from others, with partial and full settlement logs.
- **Family Hub:** Organize family finances and record internal transfers seamlessly.

### 📊 Intelligent Bank Statement & Excel Import
- **Automated Statement Parsing:** Import `.xlsx` and `.xls` files directly into Ledgerly.
- **Accounting & Debit Preservation:** Intelligently identifies debit amounts, bracketed negative amounts (e.g., `(500.00)`), and dual-column Debit/Credit structures.
- **Multi-Format Date Parsing:** Built-in parser supporting 20+ date formats (ISO-8601, US `MM/dd/yyyy`, Indian `dd-MM-yyyy`, European formats, and sheet year inference).

### 🎯 Wallets, Credit Cards & Savings Goals
- **Multi-Wallet Support:** Manage Expense Wallets, Savings Wallets, and customizable temporary wallets.
- **Credit Card Tracking:** Keep track of credit limits, billing cycles, available balances, and repayments.
- **Visual Savings Goals:** Set target goals with visual progress bars, deposit funds, or withdraw as goals are reached.

### 🔒 Enterprise-Grade Privacy & Security
- **Hardware-Backed Token Encryption:** Android KeyStore AES-GCM encryption secures Supabase access and refresh tokens at rest in local SQLite storage.
- **Strict Row-Level Security (RLS):** Backed by Supabase PostgreSQL RLS policies; users can only read and write their own financial records.
- **Incremental Cloud Delta Sync:** Syncs only new or modified transactions since the last timestamp, minimizing data transfer and battery consumption.
- **Forgot Password Recovery:** Direct self-service password recovery via email on both Android and Web.
- **Complete Local Purge on Logout:** Securely wipes local transactions, categories, and split entities upon signing out to prevent cross-account leaks on shared devices.

---

## 🛠️ Architecture & Tech Stack

### 📱 Android Application
- **Language:** 100% [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 styling
- **Architecture:** MVVM (Model-View-ViewModel) + Unidirectional Data Flow (UDF)
- **Local Database:** [AndroidX Room](https://developer.android.com/training/data-storage/room) (SQLite) with reactive Flows
- **Background Work:** [AndroidX WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for reliable offline sync
- **Network & API:** [Retrofit 2](https://square.github.io/retrofit/) & [Moshi](https://github.com/square/moshi)
- **Security:** Android KeyStore Provider with AES-GCM 256-bit cryptography
- **Excel Engine:** [Apache POI](https://poi.apache.org/)

### 💻 Web Companion App
- **Framework:** [React 19](https://react.dev/) + [TypeScript](https://www.typescriptlang.org/)
- **Build Tool:** [Vite](https://vite.dev/)
- **Styling:** [Tailwind CSS](https://tailwindcss.com/)
- **Charts & Visualizations:** [Chart.js](https://www.chartjs.org/) & [react-chartjs-2](https://react-chartjs-2.js.org/)
- **Icons:** [Lucide React](https://lucide.dev/)
- **Spreadsheet Processing:** [SheetJS (xlsx)](https://sheetjs.com/)

### ☁️ Cloud & Backend
- **Platform:** [Supabase](https://supabase.com/) (Managed PostgreSQL)
- **Security:** PostgreSQL Row Level Security (RLS) policies
- **Authentication:** Supabase Auth (JWT with automated token refresh cycle)

---

## 📁 Repository Structure

```text
Ledgerly/
├── app/                                 # Native Android Application
│   ├── src/main/java/com/parthjivani/ledgerly/
│   │   ├── data/                        # Room Database, DAOs, Supabase Services, Repository
│   │   ├── ui/                          # Jetpack Compose Screens, ViewModels, Theme
│   │   │   ├── components/              # CalculatorKeyboard, UI widgets
│   │   │   └── screens/                 # Auth, Dashboard, Transactions, Analytics, etc.
│   │   ├── utils/                       # SecurityUtils (KeyStore AES-GCM), Date & Math helpers
│   │   └── widget/                      # Android AppWidgetProvider & QuickAddTransactionActivity
│   └── build.gradle.kts
├── web/                                 # React + Vite Web Application
│   ├── src/
│   │   ├── components/                  # Dashboard, Entry Modals, Settings, Transactions
│   │   ├── context/                     # AuthContext, ThemeContext
│   │   ├── lib/                         # Calculations, amountUtils, types, Supabase client
│   │   └── pages/                       # AuthPage, Web Views
│   └── package.json
├── keystore/                            # Shared keystores for consistent signing
├── .github/workflows/                   # GitHub Actions CI/CD workflows (APK Release automation)
└── README.md
```

---

## ⚡ Getting Started

### 📲 Download the Android App
Grab the latest release APK directly from GitHub:
👉 **[Download Latest Ledgerly APK](https://github.com/JivaniParth/Ledgerly/releases/latest)**

---

### 💻 Building from Source

#### Prerequisites
- **Android Studio Ladybug | 2024.2+** or newer
- **JDK 17**
- **Node.js 18+** and **npm** (for the Web application)
- A **Supabase** project (free tier works great)

#### 1. Clone the Repository
```bash
git clone https://github.com/JivaniParth/Ledgerly.git
cd Ledgerly
```

#### 2. Configure Environment Variables
Create a `.env` file in the project root:
```env
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your-supabase-anon-key
```

#### 3. Build & Run Android Application
```bash
# Build the Debug APK
./gradlew assembleDebug

# Install onto connected device or emulator
./gradlew installDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/ledgerly-debug.apk`

#### 4. Run the Web Companion Application
```bash
cd web
npm install

# Create web environment config
echo "VITE_SUPABASE_URL=https://your-project-id.supabase.co" > .env.local
echo "VITE_SUPABASE_ANON_KEY=your-supabase-anon-key" >> .env.local

# Start development server
npm run dev

# Or build for production
npm run build
```

---

## 🚢 CI/CD & Automated Releases

Ledgerly uses **GitHub Actions** for continuous integration and automated builds:
- Pushing a version tag (e.g. `git tag v2.1.0 && git push origin v2.1.0`) triggers `.github/workflows/build-apk.yml`.
- The pipeline sets up JDK 17, compiles the Android APK, packages it, and creates a tagged GitHub Release with downloadable APK assets and release notes.

---

## 📄 License

This project is open-source and available under the terms of the **MIT License**.

---

<div align="center">
Made with ❤️ by <a href="https://github.com/JivaniParth">Parth Jivani</a>
</div>
