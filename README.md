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
