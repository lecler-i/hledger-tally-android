package dev.ghjkl.tally.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App-wide settings + small caches, backed by SharedPreferences. `manualJournalPath`
 * is a shell string Termux's `sh` interprets directly (so `~` expansion works) — kept
 * unquoted wherever a command is built with it, deliberately.
 */
object AppSettings {
    private const val PREFS_NAME = "hledgerbs_settings"
    private const val KEY_MANUAL_JOURNAL_PATH = "manual_journal_path"
    private const val KEY_DEFAULT_CURRENCY = "default_currency"
    private const val KEY_SHOW_IN_CURRENCY = "show_in_currency"
    private const val KEY_LAST_USED_ACCOUNT = "last_used_account"
    private const val KEY_EXPENSE_ACCOUNTS = "expense_accounts"

    const val DEFAULT_MANUAL_JOURNAL_PATH = "~/storage/finance/journals/phone-manual.journal"
    const val DEFAULT_CURRENCY = "INR"
    const val DEFAULT_SHOW_IN_CURRENCY = "EUR"

    var manualJournalPath by mutableStateOf(DEFAULT_MANUAL_JOURNAL_PATH)
        private set
    var defaultCurrency by mutableStateOf(DEFAULT_CURRENCY)
        private set
    // Reporting/display currency for the Infos page's converted balances (-X), distinct
    // from defaultCurrency (which is the per-entry transaction currency on Add Entry).
    var showInCurrency by mutableStateOf(DEFAULT_SHOW_IN_CURRENCY)
        private set
    var lastUsedAccount by mutableStateOf("")
        private set
    var expenseAccounts by mutableStateOf<List<String>>(emptyList())
        private set

    // Not persisted — always re-fetched fresh (kicked off once at app start, and again on
    // pull-to-refresh), so there's no point caching stale balances across app restarts.
    var cashBalanceOutput by mutableStateOf("Pull down to load…")
    var currentMonthExpensesOutput by mutableStateOf("Pull down to load…")
    var lastMonthExpensesOutput by mutableStateOf("Pull down to load…")
    var transactionsOutput by mutableStateOf("Pull down to load…")
    var isLoadingInfos by mutableStateOf(false)

    // Tap-to-cycle depth (2 -> 3 -> 4 -> 2) for the two month-expense breakdowns,
    // independent of each other. Resets to 2 on app restart — not persisted.
    var currentMonthDepth by mutableStateOf(2)
    var lastMonthDepth by mutableStateOf(2)

    fun cycleCurrentMonthDepth() {
        currentMonthDepth = if (currentMonthDepth >= 4) 2 else currentMonthDepth + 1
    }

    fun cycleLastMonthDepth() {
        lastMonthDepth = if (lastMonthDepth >= 4) 2 else lastMonthDepth + 1
    }

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        manualJournalPath = p.getString(KEY_MANUAL_JOURNAL_PATH, DEFAULT_MANUAL_JOURNAL_PATH)
            ?: DEFAULT_MANUAL_JOURNAL_PATH
        defaultCurrency = p.getString(KEY_DEFAULT_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY
        showInCurrency = p.getString(KEY_SHOW_IN_CURRENCY, DEFAULT_SHOW_IN_CURRENCY)
            ?: DEFAULT_SHOW_IN_CURRENCY
        lastUsedAccount = p.getString(KEY_LAST_USED_ACCOUNT, "") ?: ""
        expenseAccounts = p.getString(KEY_EXPENSE_ACCOUNTS, "")
            ?.lines()
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    fun updateManualJournalPath(path: String) {
        manualJournalPath = path
        prefs?.edit()?.putString(KEY_MANUAL_JOURNAL_PATH, path)?.apply()
    }

    fun updateDefaultCurrency(currency: String) {
        defaultCurrency = currency
        prefs?.edit()?.putString(KEY_DEFAULT_CURRENCY, currency)?.apply()
    }

    fun updateShowInCurrency(currency: String) {
        showInCurrency = currency
        prefs?.edit()?.putString(KEY_SHOW_IN_CURRENCY, currency)?.apply()
    }

    fun updateLastUsedAccount(account: String) {
        lastUsedAccount = account
        prefs?.edit()?.putString(KEY_LAST_USED_ACCOUNT, account)?.apply()
    }

    fun updateExpenseAccounts(accounts: List<String>) {
        expenseAccounts = accounts
        prefs?.edit()?.putString(KEY_EXPENSE_ACCOUNTS, accounts.joinToString("\n"))?.apply()
    }
}
