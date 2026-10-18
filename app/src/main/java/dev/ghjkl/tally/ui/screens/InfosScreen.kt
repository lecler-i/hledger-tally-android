package dev.ghjkl.tally.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ghjkl.tally.data.AppSettings
import dev.ghjkl.tally.termux.refreshCurrentMonthExpenses
import dev.ghjkl.tally.termux.refreshInfos
import dev.ghjkl.tally.termux.refreshLastMonthExpenses
import dev.ghjkl.tally.ui.rememberRunCommandPermission
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfosScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permission = rememberRunCommandPermission()

    val now = LocalDate.now()
    val thisMonthName = now.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val lastMonthName = now.minusMonths(1).month.getDisplayName(TextStyle.FULL, Locale.getDefault())

    fun refresh() {
        if (!permission.granted) {
            permission.request()
            return
        }
        scope.launch { refreshInfos(context) }
    }

    PullToRefreshBox(
        isRefreshing = AppSettings.isLoadingInfos,
        onRefresh = ::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(text = AppSettings.cashBalanceOutput, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "$thisMonthName expenses (depth ${AppSettings.currentMonthDepth}, tap to cycle)", fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = AppSettings.currentMonthExpensesOutput,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    AppSettings.cycleCurrentMonthDepth()
                    scope.launch { refreshCurrentMonthExpenses(context) }
                },
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "$lastMonthName expenses (depth ${AppSettings.lastMonthDepth}, tap to cycle)", fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = AppSettings.lastMonthExpensesOutput,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    AppSettings.cycleLastMonthDepth()
                    scope.launch { refreshLastMonthExpenses(context) }
                },
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Transactions", fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = AppSettings.transactionsOutput, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
    }
}
