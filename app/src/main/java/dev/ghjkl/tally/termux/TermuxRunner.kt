package dev.ghjkl.tally.termux

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred

private const val TAG = "HledgerBS"

data class TermuxResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val errmsg: String,
)

/**
 * Sends a RUN_COMMAND intent to Termux's RunCommandService and awaits the result,
 * which Termux delivers back via a PendingIntent pointing at [TermuxResultService].
 *
 * Requires: Termux installed, `allow-external-apps=true` in
 * ~/.termux/termux.properties, and this app holding the
 * com.termux.permission.RUN_COMMAND permission.
 */
object TermuxRunner {

    private var pending: CompletableDeferred<TermuxResult>? = null

    suspend fun run(
        context: Context,
        executable: String,
        args: List<String>,
        stdin: String? = null,
    ): TermuxResult {
        val app = context.applicationContext
        val deferred = CompletableDeferred<TermuxResult>()
        pending = deferred

        val resultIntent = Intent(app, TermuxResultService::class.java)
        val pendingIntent = PendingIntent.getService(
            app,
            1001,
            resultIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val runIntent = Intent().apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            action = "com.termux.RUN_COMMAND"
            putExtra("com.termux.RUN_COMMAND_PATH", executable)
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args.toTypedArray())
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pendingIntent)
            if (stdin != null) {
                putExtra("com.termux.RUN_COMMAND_STDIN", stdin)
            }
        }
        Log.d(TAG, "sending RUN_COMMAND: $executable ${args.joinToString(" ")}")
        // Termux's RunCommandService promotes itself to a foreground service right
        // after onStartCommand(); startService() doesn't get the background-start
        // exemption that requires, so Android blocks it with
        // BackgroundServiceStartNotAllowedException once Termux itself isn't foreground.
        ContextCompat.startForegroundService(app, runIntent)

        val result = deferred.await()
        Log.d(
            TAG,
            "result exitCode=${result.exitCode} errmsg=${result.errmsg} " +
                "stdout(${result.stdout.length}ch)=${result.stdout.take(500)} " +
                "stderr(${result.stderr.length}ch)=${result.stderr.take(500)}",
        )
        return result
    }

    internal fun deliver(result: TermuxResult) {
        pending?.complete(result)
        pending = null
    }
}

/** Tiny no-UI service: Termux starts it via our PendingIntent to hand back stdout/stderr/exit code. */
class TermuxResultService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bundle = intent?.getBundleExtra("result")
        val result = TermuxResult(
            stdout = bundle?.getString("stdout").orEmpty(),
            stderr = bundle?.getString("stderr").orEmpty(),
            exitCode = bundle?.getInt("exitCode") ?: -1,
            errmsg = bundle?.getString("errmsg").orEmpty(),
        )
        TermuxRunner.deliver(result)
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
