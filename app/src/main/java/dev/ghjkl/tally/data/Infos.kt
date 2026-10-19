package dev.ghjkl.tally.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.ghjkl.tally.termux.fetchCashBalance
import dev.ghjkl.tally.termux.fetchMonthExpenses
import dev.ghjkl.tally.termux.fetchRecentTransactions
import java.time.YearMonth

private const val NOT_LOADED = "Pull down to load…"

/** One month's Expenses breakdown, at a depth that cycles 2 → 3 → 4. */
class MonthExpenses(private val monthsAgo: Long) {
    val month: YearMonth get() = YearMonth.now().minusMonths(monthsAgo)
    var depth by mutableIntStateOf(2)
        private set
    var text by mutableStateOf(NOT_LOADED)
        private set

    suspend fun refresh(context: Context) {
        text = fetchMonthExpenses(context, month, depth)
    }

    suspend fun cycleDepth(context: Context) {
        depth = if (depth >= 4) 2 else depth + 1
        refresh(context)
    }
}

/** What the Infos screen shows, kept here so it survives tab switches. */
object Infos {
    val thisMonth = MonthExpenses(monthsAgo = 0)
    val lastMonth = MonthExpenses(monthsAgo = 1)
    var cashBalance by mutableStateOf(NOT_LOADED)
        private set
    var transactions by mutableStateOf(NOT_LOADED)
        private set
    var isLoading by mutableStateOf(false)
        private set

    suspend fun refresh(context: Context) {
        isLoading = true
        try {
            cashBalance = fetchCashBalance(context)
            thisMonth.refresh(context)
            lastMonth.refresh(context)
            transactions = fetchRecentTransactions(context)
        } finally {
            isLoading = false
        }
    }
}
