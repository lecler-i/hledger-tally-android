package dev.ghjkl.tally.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"

fun Context.hasRunCommandPermission() =
    checkSelfPermission(RUN_COMMAND_PERMISSION) == PackageManager.PERMISSION_GRANTED

class RunCommandPermission(private val granted: Boolean, private val request: () -> Unit) {
    /** Runs [action] if Termux's RUN_COMMAND permission is held, otherwise asks for it. */
    fun runOrRequest(action: () -> Unit) = if (granted) action() else request()
}

@Composable
fun rememberRunCommandPermission(): RunCommandPermission {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasRunCommandPermission()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    return RunCommandPermission(granted) { launcher.launch(RUN_COMMAND_PERMISSION) }
}
