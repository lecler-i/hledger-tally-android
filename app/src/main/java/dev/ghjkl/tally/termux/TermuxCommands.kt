package dev.ghjkl.tally.termux

import android.content.Context
import dev.ghjkl.tally.data.AppSettings
import java.time.LocalDate

// --- Edit these for your setup ---
const val HLEDGER_PATH = "/data/data/com.termux/files/usr/bin/hledger"
const val SH_PATH = "/data/data/com.termux/files/usr/bin/sh"
val HLEDGER_CASH_BALANCE_ARGS = listOf("--pager=no", "balance", "Assets:Cash", "--no-total")
val HLEDGER_ACCOUNTS_ARGS = listOf("--pager=no", "accounts", "^Expenses")

private fun startOfThisMonth(): LocalDate = LocalDate.now().withDayOfMonth(1)

private fun monthExpensesArgs(begin: LocalDate, end: LocalDate, depth: Int): List<String> = buildList {
    add("--pager=no")
    add("bal")
    add("-b")
    add(begin.toString())
    add("-e")
    add(end.toString())
    add("Expenses")
    add("--depth")
    add(depth.toString())
    // --flat: makes -S's sort a true global ranking across all rows, not just within
    // each sibling group under a common parent (tree mode's default sort scope).
    add("--flat")
    add("-S")
    add("-X")
    add(AppSettings.showInCurrency)
    add("--infer-market-prices")
}

private fun formatFailure(result: TermuxResult): String = buildString {
    append("hledger exited ${result.exitCode}")
    if (result.errmsg.isNotBlank()) append("\n\nTermux error: ${result.errmsg}")
    val body = result.stderr.ifBlank { result.stdout }
    if (body.isNotBlank()) append("\n\n$body")
    if (result.errmsg.isBlank() && body.isBlank()) {
        append(
            "\n\n(no stdout/stderr/errmsg at all — the RUN_COMMAND intent " +
                "may never have reached Termux; check allow-external-apps and the " +
                "RUN_COMMAND permission grant)",
        )
    }
}

/**
 * Re-fetches this month's Expenses at [AppSettings.currentMonthDepth]. Bounded at next
 * month's start — an open-ended query would also sweep in any future-dated transactions
 * already in the ledger (e.g. a pre-recorded December tax entry), corrupting the total.
 */
suspend fun refreshCurrentMonthExpenses(context: Context) {
    val start = startOfThisMonth()
    val args = monthExpensesArgs(begin = start, end = start.plusMonths(1), depth = AppSettings.currentMonthDepth)
    val result = TermuxRunner.run(context, HLEDGER_PATH, args)
    AppSettings.currentMonthExpensesOutput = if (result.exitCode == 0) {
        result.stdout.replace("Expenses:", "").ifBlank { "(no expenses yet this month)" }
    } else {
        formatFailure(result)
    }
}

/** Re-fetches last month's Expenses at [AppSettings.lastMonthDepth], bounded to that month only. */
suspend fun refreshLastMonthExpenses(context: Context) {
    val start = startOfThisMonth()
    val args = monthExpensesArgs(begin = start.minusMonths(1), end = start, depth = AppSettings.lastMonthDepth)
    val result = TermuxRunner.run(context, HLEDGER_PATH, args)
    AppSettings.lastMonthExpensesOutput = if (result.exitCode == 0) {
        result.stdout.replace("Expenses:", "").ifBlank { "(no expenses last month)" }
    } else {
        formatFailure(result)
    }
}

/**
 * Re-fetches everything shown on the Infos screen: the Assets:Cash balance (from the
 * main journal, via ~/.hledger.conf's default -f — "how much should I have on hand"),
 * this/last month's Expenses, and recent transactions from the manual-entry journal,
 * newest first. `print` has no native reverse-order flag, so the transaction blocks are
 * reversed here instead.
 */
suspend fun refreshInfos(context: Context) {
    AppSettings.isLoadingInfos = true

    val balanceResult = TermuxRunner.run(context, HLEDGER_PATH, HLEDGER_CASH_BALANCE_ARGS)
    AppSettings.cashBalanceOutput = if (balanceResult.exitCode == 0) {
        balanceResult.stdout.ifBlank { "(empty output)" }
    } else {
        formatFailure(balanceResult)
    }

    refreshCurrentMonthExpenses(context)
    refreshLastMonthExpenses(context)

    val path = AppSettings.manualJournalPath
    val txResult = TermuxRunner.run(
        context,
        SH_PATH,
        // Deliberately unquoted: lets sh expand a leading `~` in the configured path.
        listOf("-c", "$HLEDGER_PATH --pager=no -f $path print"),
    )
    AppSettings.transactionsOutput = if (txResult.exitCode == 0) {
        txResult.stdout
            .split(Regex("\n\\s*\n"))
            .filter { it.isNotBlank() }
            .reversed()
            .joinToString("\n\n")
            .ifBlank { "(no transactions yet)" }
    } else {
        formatFailure(txResult)
    }

    AppSettings.isLoadingInfos = false
}

/** Re-fetches the Expenses account list via Termux and updates the shared cache. */
suspend fun refreshExpenseAccounts(context: Context): TermuxResult {
    val result = TermuxRunner.run(context, HLEDGER_PATH, HLEDGER_ACCOUNTS_ARGS)
    if (result.exitCode == 0) {
        AppSettings.updateExpenseAccounts(
            result.stdout.lines().map { it.trim() }.filter { it.isNotBlank() },
        )
    }
    return result
}
