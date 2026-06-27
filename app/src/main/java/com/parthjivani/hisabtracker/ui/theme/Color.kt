package com.parthjivani.hisabtracker.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════
//  HISAB — Professional Finance Color System
//  Inspired by: Bloomberg, Zerodha, HDFC, Groww, Monzo
//
//  Role map:
//    Blue   → Primary brand / trust / CTAs
//    Green  → Income, profit, positive states
//    Red    → Expense, loss, alerts
//    Gold   → Premium accent, highlights
//    Teal   → Credit card / secondary actions
//    Dark   → Dark mode backgrounds / surfaces
//    Light  → Light mode backgrounds / surfaces
// ═══════════════════════════════════════════════════════════════════

// ── Core Blue (Primary Brand) ─────────────────────────────────────
val FinanceBlue         = Color(0xFF1A56DB)   // Deep trust blue — primary CTA
val FinanceBlueDark     = Color(0xFF1240A8)   // Pressed / darker shade
val FinanceBlueLight    = Color(0xFF3D7FF5)   // Hover / lighter accent
val FinanceBlueContainer = Color(0xFFDCEAFF)  // Light container
val FinanceBlueOnContainer = Color(0xFF0A2C75)

// ── Finance Green (Income / Growth / Positive) ────────────────────
val FinanceGreen        = Color(0xFF0D9E6A)   // Profit green — income tiles
val FinanceGreenDark    = Color(0xFF087A50)   // Pressed
val FinanceGreenLight   = Color(0xFF1BBF82)   // Lighter accent
val FinanceGreenContainer = Color(0xFFD2F5E9) // Light container
val FinanceGreenOnContainer = Color(0xFF003D27)

// ── Finance Red (Expense / Loss / Alert) ──────────────────────────
val FinanceRed          = Color(0xFFDE2027)   // Alert red — expense tiles
val FinanceRedDark      = Color(0xFFB01018)   // Pressed
val FinanceRedContainer = Color(0xFFFFE4E4)   // Light container
val FinanceRedOnContainer = Color(0xFF6B0000)

// ── Gold Accent (Premium / Highlight / Credit) ────────────────────
val FinanceGold         = Color(0xFFF5A623)   // Gold — credit card, premium
val FinanceGoldDark     = Color(0xFFD48A0A)
val FinanceGoldContainer = Color(0xFFFFF1D0)
val FinanceGoldOnContainer = Color(0xFF5C3700)

// ── Teal (Credit Card / Secondary Actions) ────────────────────────
val FinanceTeal         = Color(0xFF0097A7)   // Teal — credit / secondary
val FinanceTealDark     = Color(0xFF006978)
val FinanceTealContainer = Color(0xFFD0F5F8)
val FinanceTealOnContainer = Color(0xFF002B30)

// ── Dark Theme Surfaces ───────────────────────────────────────────
val DarkBackground      = Color(0xFF0D1117)   // Near-black — Bloomberg dark
val DarkSurface         = Color(0xFF161B22)   // Card surface
val DarkSurfaceVariant  = Color(0xFF21262D)   // Elevated card
val DarkOutline         = Color(0xFF30363D)   // Borders
val DarkOnBackground    = Color(0xFFE6EDF3)   // Primary text
val DarkOnSurface       = Color(0xFFE6EDF3)
val DarkOnSurfaceVariant = Color(0xFF8B949E)  // Muted / secondary text

// ── Light Theme Surfaces ──────────────────────────────────────────
val LightBackground     = Color(0xFFF6F8FA)   // Off-white — readable
val LightSurface        = Color(0xFFFFFFFF)   // Cards
val LightSurfaceVariant = Color(0xFFEAEEF3)   // Elevated
val LightOutline        = Color(0xFFD0D7DE)   // Borders
val LightOnBackground   = Color(0xFF0D1117)   // Primary text
val LightOnSurface      = Color(0xFF0D1117)
val LightOnSurfaceVariant = Color(0xFF57606A) // Muted text

// ── Wallet Card Gradients ─────────────────────────────────────────
// Expense Wallet — uses deep blue gradient
val ExpenseGradientStart = Color(0xFF1A56DB)
val ExpenseGradientEnd   = Color(0xFF1240A8)

// Savings Wallet — uses rich green gradient
val SavingsGradientStart = Color(0xFF0D9E6A)
val SavingsGradientEnd   = Color(0xFF087A50)

// UI selector colors (for wallet/direction picker chips)
val ExpenseColor         = FinanceRed
val ExpenseBgColor       = FinanceRedContainer
val ExpenseTextColor     = FinanceRedOnContainer

val SavingsColor         = FinanceGreen
val SavingsBgColor       = FinanceGreenContainer
val SavingsTextColor     = FinanceGreenOnContainer

val TealGreenPrimary     = FinanceTeal
val TealGreenDark        = FinanceTealDark
val TealGreenLight       = FinanceTealContainer

val GoldenAccent         = FinanceGold

// Legacy tokens kept to avoid breaking references
val SophisticatedDarkBackground    = DarkBackground
val SophisticatedDarkSurface       = DarkSurface
val SophisticatedDarkPrimary       = FinanceBlueLight
val SophisticatedDarkOnPrimary     = DarkBackground
val SophisticatedDarkSecondary     = FinanceTeal
val SophisticatedDarkOnSecondary   = DarkBackground
val SophisticatedDarkOnBackground  = DarkOnBackground
val SophisticatedDarkOnSurface     = DarkOnSurface
val SophisticatedDarkOnSurfaceVariant = DarkOnSurfaceVariant
val SophisticatedDarkOutline       = DarkOutline
val SophisticatedDarkNavBarBg      = DarkBackground

// Default template tokens (unused but kept for safety)
val Purple80    = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80      = Color(0xFFEFB8C8)
val Purple40    = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40      = Color(0xFF7D5260)
