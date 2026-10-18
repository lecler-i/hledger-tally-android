package dev.ghjkl.tally.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"

/** Whether we hold com.termux.permission.RUN_COMMAND, plus a function to request it. */
class RunCommandPermission(val granted: Boolean, val request: () -> Unit)

@Composable
fun rememberRunCommandPermission(): RunCommandPermission {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, RUN_COMMAND_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted -> granted = isGranted }
    return RunCommandPermission(granted) { launcher.launch(RUN_COMMAND_PERMISSION) }
}
