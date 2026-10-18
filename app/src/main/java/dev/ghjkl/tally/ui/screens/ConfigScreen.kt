package dev.ghjkl.tally.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ghjkl.tally.data.AppSettings
import dev.ghjkl.tally.termux.refreshExpenseAccounts
import dev.ghjkl.tally.ui.rememberRunCommandPermission
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permission = rememberRunCommandPermission()

    var path by remember { mutableStateOf(AppSettings.manualJournalPath) }
    var currency by remember { mutableStateOf(AppSettings.defaultCurrency) }
    var showInCurrency by remember { mutableStateOf(AppSettings.showInCurrency) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshStatus by remember { mutableStateOf("") }

    fun refresh() {
        if (!permission.granted) {
            permission.request()
            return
        }
        isRefreshing = true
        refreshStatus = ""
        scope.launch {
            val result = refreshExpenseAccounts(context)
            isRefreshing = false
            refreshStatus = if (result.exitCode == 0) {
                "Refreshed."
            } else {
                "Failed (exit ${result.exitCode}): " +
                    result.errmsg.ifBlank { result.stderr.ifBlank { "unknown error" } }
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = ::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Manual entry journal path", fontSize = 13.sp)
            OutlinedTextField(
                value = path,
                onValueChange = {
                    path = it
                    AppSettings.updateManualJournalPath(it)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "This is a shell path Termux's sh will expand directly — " +
                    "\"~\" resolves to Termux's own home, same as typing it in a terminal.",
                fontSize = 12.sp,
            )

            Text("Default currency", fontSize = 13.sp)
            OutlinedTextField(
                value = currency,
                onValueChange = {
                    currency = it
                    AppSettings.updateDefaultCurrency(it)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Prefills the Currency field on Add Entry; you can still override it per entry.",
                fontSize = 12.sp,
            )

            Text("Show in currency", fontSize = 13.sp)
            OutlinedTextField(
                value = showInCurrency,
                onValueChange = {
                    showInCurrency = it
                    AppSettings.updateShowInCurrency(it)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Reporting currency (-X) for the converted balances on the Infos page.",
                fontSize = 12.sp,
            )

            Text(
                "Cached Expenses accounts (${AppSettings.expenseAccounts.size}) — " +
                    "pull down here to refresh.",
                fontSize = 12.sp,
            )
            if (refreshStatus.isNotBlank()) {
                Text(refreshStatus, fontSize = 12.sp)
            }
        }
    }
}
