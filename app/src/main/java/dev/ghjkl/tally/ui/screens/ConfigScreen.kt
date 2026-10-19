package dev.ghjkl.tally.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import dev.ghjkl.tally.data.ThemeMode
import dev.ghjkl.tally.termux.refreshExpenseAccounts
import dev.ghjkl.tally.ui.rankByFuzzyScore
import dev.ghjkl.tally.ui.rememberRunCommandPermission
import kotlinx.coroutines.launch

// hledger's "accounts" list only contains leaves actually posted to (e.g.
// Expenses:Taxes:France) — derive every ancestor prefix (Expenses, Expenses:Taxes, …)
// too, so a whole subtree like Expenses:Taxes can be picked for exclusion in one go.
private fun withAncestors(accounts: List<String>): List<String> {
    val result = LinkedHashSet<String>()
    for (account in accounts) {
        val parts = account.split(":")
        for (depth in 1..parts.size) {
            result.add(parts.subList(0, depth).joinToString(":"))
        }
    }
    return result.toList()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permission = rememberRunCommandPermission()

    var themeMode by remember { mutableStateOf(AppSettings.themeMode) }
    var path by remember { mutableStateOf(AppSettings.manualJournalPath) }
    var currency by remember { mutableStateOf(AppSettings.defaultCurrency) }
    var showInCurrency by remember { mutableStateOf(AppSettings.showInCurrency) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshStatus by remember { mutableStateOf("") }
    var excludeQuery by remember { mutableStateOf("") }
    var excludeMenuExpanded by remember { mutableStateOf(false) }

    val excludeMatches = remember(excludeQuery, AppSettings.expenseAccounts, AppSettings.excludedExpenseAccounts) {
        rankByFuzzyScore(excludeQuery, withAncestors(AppSettings.expenseAccounts), limit = 8)
            .filter { it !in AppSettings.excludedExpenseAccounts }
    }

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
            Text("Theme", fontSize = 13.sp)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = {
                            themeMode = mode
                            AppSettings.updateThemeMode(mode)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        label = {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "Follow system"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                },
                            )
                        },
                    )
                }
            }

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

            Text("Hide from Expenses breakdown", fontSize = 13.sp)
            ExposedDropdownMenuBox(
                expanded = excludeMenuExpanded && excludeMatches.isNotEmpty(),
                onExpandedChange = { excludeMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = excludeQuery,
                    onValueChange = {
                        excludeQuery = it
                        excludeMenuExpanded = true
                    },
                    label = { Text("Search account to hide") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
                )
                ExposedDropdownMenu(
                    expanded = excludeMenuExpanded && excludeMatches.isNotEmpty(),
                    onDismissRequest = { excludeMenuExpanded = false },
                ) {
                    excludeMatches.forEach { match ->
                        DropdownMenuItem(
                            text = { Text(match) },
                            onClick = {
                                AppSettings.toggleExcludedExpenseAccount(match)
                                excludeQuery = ""
                                excludeMenuExpanded = false
                            },
                        )
                    }
                }
            }
            if (AppSettings.excludedExpenseAccounts.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppSettings.excludedExpenseAccounts.forEach { account ->
                        InputChip(
                            selected = false,
                            onClick = { AppSettings.toggleExcludedExpenseAccount(account) },
                            label = { Text(account.removePrefix("Expenses:")) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(InputChipDefaults.IconSize),
                                )
                            },
                        )
                    }
                }
            }
            Text(
                "Hidden accounts (e.g. Taxes) are excluded from the Infos page's " +
                    "Expenses totals, not from the ledger itself. Tap a chip to unhide it.",
                fontSize = 12.sp,
            )
        }
    }
}
