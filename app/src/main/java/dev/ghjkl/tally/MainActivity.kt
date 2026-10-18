package dev.ghjkl.tally

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.ghjkl.tally.data.AppSettings
import dev.ghjkl.tally.termux.refreshExpenseAccounts
import dev.ghjkl.tally.termux.refreshInfos
import dev.ghjkl.tally.ui.RUN_COMMAND_PERMISSION
import dev.ghjkl.tally.ui.screens.AddEntryScreen
import dev.ghjkl.tally.ui.screens.ConfigScreen
import dev.ghjkl.tally.ui.screens.InfosScreen
import dev.ghjkl.tally.ui.theme.TallyTheme
import kotlinx.coroutines.launch

private enum class Screen(val label: String) {
    Infos("Infos"),
    AddEntry("Add Entry"),
    Config("Config"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        enableEdgeToEdge()
        setContent {
            TallyTheme {
                HledgerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HledgerApp() {
    var screen by remember { mutableStateOf(Screen.AddEntry) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Kick off the Infos (cash balance + transactions) and Expenses account cache fetch
    // once in the background at app start, so it's already ready by the time you switch
    // tabs — but only if we already hold the permission, so a cold launch doesn't
    // surprise you with a prompt.
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, RUN_COMMAND_PERMISSION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            scope.launch {
                refreshInfos(context)
                refreshExpenseAccounts(context)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = screen == Screen.Infos,
                    onClick = { screen = Screen.Infos },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text(Screen.Infos.label) },
                )
                NavigationBarItem(
                    selected = screen == Screen.AddEntry,
                    onClick = { screen = Screen.AddEntry },
                    // Center, most-used action — a filled circular badge makes it stand
                    // out from the two plain icons either side of it.
                    icon = {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(6.dp),
                            )
                        }
                    },
                    label = { Text(Screen.AddEntry.label) },
                )
                NavigationBarItem(
                    selected = screen == Screen.Config,
                    onClick = { screen = Screen.Config },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(Screen.Config.label) },
                )
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
