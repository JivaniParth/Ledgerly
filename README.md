# Hisab & Expense Tracker

A feature-rich native Android application for personal finance management. 

## Key Features

### 💰 Personal Finance Management
- **Dashboard:** At-a-glance overview of your income, expenses, and current balances.
- **Transactions:** Log daily expenses and incomes with categories. View historical data with robust filtering.
- **Savings Goals:** Set target amounts for specific goals. Add to or withdraw from your savings, and track your progress visually.
- **Credit Cards:** Manage credit cards securely, tracking spending and limits.

### 🔒 Security & Multi-User Architecture
- **Authentication:** Secure login and sign-up flows.
- **Row-Level Security (RLS):** Built on Supabase, ensuring that users can strictly only sync, view, and modify their own personal data. No user can access another user's financial records.
- **Token Management:** Seamless background token refresh cycle. If your session expires, the app will securely request a new token and retry synchronization automatically.

### 📱 Technical Stack & Architecture
- **Language:** 100% Kotlin
- **UI Toolkit:** Jetpack Compose (Modern declarative UI)
- **Architecture:** MVVM (Model-View-ViewModel) with Unidirectional Data Flow.
- **Local Storage:** AndroidX Room Database (SQLite) for offline-first capabilities.
- **Cloud Database:** Supabase (PostgreSQL) for remote backup and multi-device synchronization.
- **Networking:** Retrofit & Moshi for API communication.
- **Concurrency:** Kotlin Coroutines & Flows for asynchronous data handling.

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)

1. Open Android Studio
2. Select **Open** and choose the directory containing this project.
3. Allow Android Studio to sync the Gradle project.
4. Create a file named `.env` in the root project directory and set your Supabase credentials (see `.env.example` for an example):
   ```env
   SUPABASE_URL=your_supabase_url
   SUPABASE_ANON_KEY=your_supabase_anon_key
   ```
5. Run the app on an emulator or physical device!

## Sync Mechanism
The app employs an **Offline-First** strategy:
1. All changes (adding transactions, modifying goals) are instantly saved to the local Room database, ensuring a snappy UI.
2. The app then attempts to push changes to Supabase in the background.
3. Upon startup, the app pulls any remote changes down to keep the local database up to date across devices.

## Building for Release
To generate a signed APK for release:
1. In Android Studio, go to `Build > Generate Signed Bundle / APK...`
2. Select `APK` and use your generated Keystore.
3. Your output file will automatically be named `hisab-tracker-release.apk` thanks to the custom Gradle configuration.
