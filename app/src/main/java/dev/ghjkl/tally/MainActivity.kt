package dev.ghjkl.tally

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.ghjkl.tally.data.AppSettings
import dev.ghjkl.tally.data.Infos
import dev.ghjkl.tally.termux.refreshExpenseAccounts
import dev.ghjkl.tally.ui.hasRunCommandPermission
import dev.ghjkl.tally.ui.screens.AddEntryScreen
import dev.ghjkl.tally.ui.screens.ConfigScreen
import dev.ghjkl.tally.ui.screens.InfosScreen
import dev.ghjkl.tally.ui.theme.TallyTheme
import dev.ghjkl.tally.ui.theme.isAppInDarkTheme

private enum class Screen(val label: String, val icon: ImageVector) {
    Infos("Infos", Icons.Default.Info),
    AddEntry("Add Entry", Icons.Default.Add),
    Config("Config", Icons.Default.Settings),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        setContent {
            val dark = isAppInDarkTheme()
            // Re-applied on theme changes so the system bar icons follow the in-app setting.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(style, style)
                onDispose {}
            }
            TallyTheme(dark) { TallyApp() }
        }
    }
}

@Composable
private fun TallyApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.AddEntry) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Prefetch in the background, without asking for the permission on launch.
    LaunchedEffect(Unit) {
        if (context.hasRunCommandPermission()) {
            Infos.refresh(context)
            refreshExpenseAccounts(context)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach {
                    NavigationBarItem(
                        selected = screen == it,
                        onClick = { screen = it },
                        icon = { ScreenIcon(it) },
                        label = { Text(it.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.Infos -> InfosScreen()
                Screen.AddEntry -> AddEntryScreen(snackbarHostState)
                Screen.Config -> ConfigScreen()
            }
        }
    }
}

// Add Entry is the main action, so its icon sits in a filled circle.
@Composable
private fun ScreenIcon(screen: Screen) {
    if (screen != Screen.AddEntry) return Icon(screen.icon, contentDescription = null)
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
        Icon(
            screen.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(6.dp),
        )
    }
}
