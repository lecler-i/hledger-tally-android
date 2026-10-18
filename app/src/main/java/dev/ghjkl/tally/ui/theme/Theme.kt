package dev.ghjkl.tally.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.ghjkl.tally.data.AppSettings
import dev.ghjkl.tally.data.ThemeMode

@Composable
fun TallyTheme(content: @Composable () -> Unit) {
    val dark = when (AppSettings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }

    // enableEdgeToEdge() only reads the system dark-mode setting once, at onCreate — this
    // keeps the status/nav bar icon contrast in sync when the in-app Light/Dark override
    // changes at runtime, without needing to recreate the Activity.
    val view = LocalView.current
    LaunchedEffect(dark) {
        val window = (view.context as? android.app.Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
