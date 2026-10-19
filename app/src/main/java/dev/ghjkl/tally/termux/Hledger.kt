package dev.ghjkl.tally.termux

import android.content.Context
import dev.ghjkl.tally.data.AppSettings
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

private const val BIN = "/data/data/com.termux/files/usr/bin"
const val CASH_ACCOUNT = "Assets:Cash"

private suspend fun hledger(context: Context, args: List<String>) =
    TermuxRunner.run(context, "$BIN/hledger", listOf("--pager=no") + args)

// Goes through sh so a leading "~" in the (unquoted) journal path gets expanded.
private suspend fun shell(context: Context, command: String, stdin: String? = null) =
    TermuxRunner.run(context, "$BIN/sh", listOf("-c", command), stdin)

private inline fun TermuxResult.text(transform: (String) -> String) =
    if (ok) transform(stdout) else errorText()

private fun regexEscape(text: String) = text.replace(Regex("""[\\^$.|?*+()\[\]{}]""")) { "\\" + it.value }

suspend fun fetchCashBalance(context: Context): String =
    hledger(context, listOf("balance", CASH_ACCOUNT, "--no-total")).text { it.ifBlank { "(empty output)" } }

// --flat makes -S rank all rows globally instead of within each parent.
suspend fun fetchMonthExpenses(context: Context, month: YearMonth, depth: Int): String {
    val excluded = AppSettings.excludedExpenseAccounts.map { "not:acct:^${regexEscape(it)}(:|$)" }
    val args = listOf(
        "balance", "Expenses", "-p", month.toString(), "--depth", depth.toString(), "--flat", "-S",
        "-X", AppSettings.showInCurrency, "--infer-market-prices",
    ) + excluded
    return hledger(context, args).text { it.replace("Expenses:", "").ifBlank { "(no expenses)" } }
}

// `print` has no reverse option, so flip the entries to show the newest first.
suspend fun fetchRecentTransactions(context: Context): String =
    shell(context, "$BIN/hledger --pager=no -f ${AppSettings.manualJournalPath} print").text { out ->
        out.split(Regex("\n\\s*\n"))
            .filter { it.isNotBlank() }
            .reversed()
            .joinToString("\n\n")
            .ifBlank { "(no transactions yet)" }
    }

suspend fun refreshExpenseAccounts(context: Context): TermuxResult {
    val result = hledger(context, listOf("accounts", "^Expenses"))
    if (result.ok) {
        AppSettings.expenseAccounts = result.stdout.lines().map { it.trim() }.filter { it.isNotBlank() }
    }
    return result
}

// No trailing blank line: Termux's stdin already adds one, which separates entries.
fun formatEntry(date: LocalDate, payee: String, account: String, amount: BigDecimal, currency: String) =
    "$date $payee\n" +
        "    $CASH_ACCOUNT  ${amount.negate().toPlainString()} $currency\n" +
        "    $account  ${amount.toPlainString()} $currency\n"

suspend fun appendEntry(context: Context, entry: String) =
    shell(context, "cat >> ${AppSettings.manualJournalPath}", stdin = entry)
