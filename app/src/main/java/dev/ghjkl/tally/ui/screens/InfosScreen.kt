package dev.ghjkl.tally.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ghjkl.tally.data.Infos
import dev.ghjkl.tally.data.MonthExpenses
import dev.ghjkl.tally.ui.rememberRunCommandPermission
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InfosScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permission = rememberRunCommandPermission()

    PullToRefreshBox(
        isRefreshing = Infos.isLoading,
        onRefresh = { permission.runOrRequest { scope.launch { Infos.refresh(context) } } },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section("Cash", Infos.cashBalance)
            for (month in listOf(Infos.thisMonth, Infos.lastMonth)) {
                Section(monthTitle(month), month.text) { scope.launch { month.cycleDepth(context) } }
            }
            Section("Transactions", Infos.transactions)
        }
    }
}

private fun monthTitle(month: MonthExpenses): String {
    val name = month.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "$name expenses (depth ${month.depth}, tap to cycle)"
}

@Composable
private fun Section(title: String, body: String, onClick: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = body,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        )
    }
}
