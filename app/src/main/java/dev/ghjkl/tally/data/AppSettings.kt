package dev.ghjkl.tally.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

enum class ThemeMode(val label: String) {
    SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark"),
}

/** Settings saved in SharedPreferences and exposed as Compose state. */
object AppSettings {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    var themeMode by pref("theme_mode", ThemeMode.SYSTEM, { s -> ThemeMode.entries.find { it.name == s } }, { it.name })
    var manualJournalPath by stringPref("manual_journal_path", "/storage/emulated/0/finance/journals/phone-manual.journal")
    var defaultCurrency by stringPref("default_currency", "INR")
    var showInCurrency by stringPref("show_in_currency", "EUR")
    var lastUsedAccount by stringPref("last_used_account", "")
    var expenseAccounts by listPref("expense_accounts")
    var excludedExpenseAccounts by listPref("excluded_expense_accounts")

    private fun stringPref(key: String, default: String) = pref(key, default, { it }, { it })

    private fun listPref(key: String) =
        pref(key, emptyList(), { s -> s.lines().filter { it.isNotBlank() } }, { it.joinToString("\n") })

    private fun <T> pref(key: String, default: T, decode: (String) -> T?, encode: (T) -> String) =
        object : ReadWriteProperty<Any?, T> {
            private val state by lazy { mutableStateOf(prefs.getString(key, null)?.let(decode) ?: default) }

            override fun getValue(thisRef: Any?, property: KProperty<*>) = state.value

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                state.value = value
                prefs.edit().putString(key, encode(value)).apply()
            }
        }
}
