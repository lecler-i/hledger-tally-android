package dev.ghjkl.tally.termux

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

private const val EXTRA_ID = "dev.ghjkl.tally.REQUEST_ID"
private val TIMEOUT = 30.seconds

data class TermuxResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val errmsg: String,
) {
    val ok get() = exitCode == 0 && errmsg.isBlank()

    fun errorText(): String {
        val details = listOf(errmsg, stderr.ifBlank { stdout }).filter { it.isNotBlank() }
            .ifEmpty { listOf("No output: check allow-external-apps and the RUN_COMMAND permission.") }
        return (listOf("Exit code $exitCode") + details).joinToString("\n\n")
    }

    companion object {
        fun failure(message: String) = TermuxResult("", "", -1, message)
    }
}

/**
 * Runs commands through Termux's RUN_COMMAND intent. Termux sends each result back
 * to [TermuxResultService] through a PendingIntent tagged with the request id.
 */
object TermuxRunner {
    // Only touched from the main thread.
    private val pending = mutableMapOf<Int, CompletableDeferred<TermuxResult>>()
    private var nextId = 0

    suspend fun run(
        context: Context,
        executable: String,
        args: List<String>,
        stdin: String? = null,
    ): TermuxResult {
        val app = context.applicationContext
        val id = ++nextId
        val deferred = CompletableDeferred<TermuxResult>()
        pending[id] = deferred
        try {
            // Termux's RunCommandService goes foreground immediately; plain startService()
            // is blocked while Termux is in the background.
            val started = runCatching { app.startForegroundService(runIntent(app, id, executable, args, stdin)) }
            if (started.getOrNull() == null) {
                val reason = started.exceptionOrNull()?.message ?: "is it installed?"
                return TermuxResult.failure("Could not start Termux: $reason")
            }
            return withTimeoutOrNull(TIMEOUT) { deferred.await() }
                ?: TermuxResult.failure("Timed out waiting for Termux")
        } finally {
            pending.remove(id)
        }
    }

    internal fun deliver(id: Int, result: TermuxResult) {
        pending[id]?.complete(result)
    }

    private fun runIntent(app: Context, id: Int, executable: String, args: List<String>, stdin: String?): Intent {
        val resultIntent = Intent(app, TermuxResultService::class.java).putExtra(EXTRA_ID, id)
        // Mutable so Termux can add its result bundle.
        val pendingIntent = PendingIntent.getService(app, id, resultIntent, PendingIntent.FLAG_MUTABLE)
        return Intent("com.termux.RUN_COMMAND")
            .setClassName("com.termux", "com.termux.app.RunCommandService")
            .putExtra("com.termux.RUN_COMMAND_PATH", executable)
            .putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args.toTypedArray())
            .putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            .putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pendingIntent)
            .apply { if (stdin != null) putExtra("com.termux.RUN_COMMAND_STDIN", stdin) }
    }
}

class TermuxResultService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bundle = intent?.getBundleExtra("result")
        val result = TermuxResult(
            stdout = bundle?.getString("stdout").orEmpty(),
            stderr = bundle?.getString("stderr").orEmpty(),
            // Termux leaves exitCode out when the command never ran.
            exitCode = bundle?.getInt("exitCode", -1) ?: -1,
            errmsg = bundle?.getString("errmsg").orEmpty(),
        )
        intent?.let { TermuxRunner.deliver(it.getIntExtra(EXTRA_ID, 0), result) }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
