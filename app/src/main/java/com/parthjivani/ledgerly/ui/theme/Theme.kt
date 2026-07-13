package com.parthjivani.ledgerly.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════════════
//  DARK SCHEME — Bloomberg / trading terminal aesthetic
//  Near-black surfaces, bright blue primary, green/red semantics
// ═══════════════════════════════════════════════════════════════════
private val DarkColorScheme = darkColorScheme(
    primary              = FinanceBlue,               // Exact same as light theme
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFF1240A8),         // Deep blue container
    onPrimaryContainer   = Color(0xFFB8CFFF),

    secondary            = FinanceTeal,               // Exact same as light theme
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFF004F57),
    onSecondaryContainer = FinanceTealContainer,

    tertiary             = FinanceGold,               // Exact same as light theme
    onTertiary           = Color(0xFF000000),
    tertiaryContainer    = Color(0xFF5C3700),
    onTertiaryContainer  = FinanceGoldContainer,

    error                = FinanceRed,                // Exact same as light theme
    onError              = Color(0xFF000000),
    errorContainer       = Color(0xFF7A0000),
    onErrorContainer     = Color(0xFFFFDAD6),

    background           = DarkBackground,            // #0D1117 — near black
    onBackground         = DarkOnBackground,          // #E6EDF3

    surface              = DarkSurface,               // #161B22
    onSurface            = DarkOnSurface,             // #E6EDF3
    surfaceVariant       = DarkSurfaceVariant,        // #21262D
    onSurfaceVariant     = DarkOnSurfaceVariant,      // #8B949E

    outline              = DarkOutline,               // #30363D
    outlineVariant       = Color(0xFF21262D),

    inverseSurface       = LightSurface,
    inverseOnSurface     = LightOnSurface,
    inversePrimary       = FinanceBlue,

    scrim                = Color(0xFF000000)
)

// ═══════════════════════════════════════════════════════════════════
//  LIGHT SCHEME — Clean banking app aesthetic
//  Off-white surfaces, deep blue primary, vibrant green/red semantics
// ═══════════════════════════════════════════════════════════════════
private val LightColorScheme = lightColorScheme(
    primary              = FinanceBlue,               // #1A56DB — deep trust blue
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = FinanceBlueContainer,      // #DCEAFF
    onPrimaryContainer   = FinanceBlueOnContainer,    // #0A2C75

    secondary            = FinanceTeal,               // #0097A7
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = FinanceTealContainer,      // #D0F5F8
    onSecondaryContainer = FinanceTealOnContainer,    // #002B30

    tertiary             = FinanceGold,               // #F5A623
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = FinanceGoldContainer,      // #FFF1D0
    onTertiaryContainer  = FinanceGoldOnContainer,    // #5C3700

    error                = FinanceRed,                // #DE2027
    onError              = Color(0xFFFFFFFF),
    errorContainer       = FinanceRedContainer,       // #FFE4E4
    onErrorContainer     = FinanceRedOnContainer,     // #6B0000

    background           = LightBackground,           // #F6F8FA — soft off-white
    onBackground         = LightOnBackground,         // #0D1117

    surface              = LightSurface,              // #FFFFFF
    onSurface            = LightOnSurface,            // #0D1117
    surfaceVariant       = LightSurfaceVariant,       // #EAF0F6
    onSurfaceVariant     = LightOnSurfaceVariant,     // #57606A

    outline              = LightOutline,              // #D0D7DE
    outlineVariant       = Color(0xFFE8EDF2),

    inverseSurface       = DarkSurface,
    inverseOnSurface     = DarkOnSurface,
    inversePrimary       = FinanceBlueLight,

    scrim                = Color(0xFF000000)
)

@Composable
fun MyApplicationTheme(
    appTheme: String = "system", // "light", "dark", "system"
    darkTheme: Boolean = isSystemInDarkTheme(), // Automatically follows device dark/light mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = when (appTheme.lowercase()) {
        "light" -> false
        "dark" -> true
        else -> darkTheme
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
