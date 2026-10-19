package dev.ghjkl.tally.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.ghjkl.tally.data.AppSettings
import dev.ghjkl.tally.data.ThemeMode
import dev.ghjkl.tally.termux.refreshExpenseAccounts
import dev.ghjkl.tally.ui.AccountSearchField
import dev.ghjkl.tally.ui.rememberRunCommandPermission
import kotlinx.coroutines.launch

@Composable
fun ConfigScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Title("Theme")
        ThemePicker()

        Title("Journal")
        SettingField(
            label = "Manual entry journal path",
            hint = "Expanded by Termux's sh, so \"~\" is Termux's home.",
            value = AppSettings.manualJournalPath,
            onValueChange = { AppSettings.manualJournalPath = it },
        )
        SettingField(
            label = "Default currency",
            hint = "Prefills the Currency field on Add Entry.",
            value = AppSettings.defaultCurrency,
            onValueChange = { AppSettings.defaultCurrency = it },
        )
        SettingField(
            label = "Show in currency",
            hint = "Reporting currency (-X) for the Infos page.",
            value = AppSettings.showInCurrency,
            onValueChange = { AppSettings.showInCurrency = it },
        )

        Title("Expenses accounts")
        RefreshAccountsButton()
        HiddenAccounts()
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun SettingField(label: String, hint: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = { Text(hint) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ThemePicker() {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = AppSettings.themeMode == mode,
                onClick = { AppSettings.themeMode = mode },
                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                label = { Text(mode.label) },
            )
        }
    }
}

@Composable
private fun RefreshAccountsButton() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permission = rememberRunCommandPermission()
    var refreshing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    OutlinedButton(
        onClick = {
            permission.runOrRequest {
                refreshing = true
                scope.launch {
                    val result = refreshExpenseAccounts(context)
                    refreshing = false
                    status = if (result.ok) "Refreshed." else result.errorText()
                }
            }
        },
        enabled = !refreshing,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (refreshing) "Refreshing…" else "Refresh accounts (${AppSettings.expenseAccounts.size} cached)")
    }
    if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
}

// hledger only lists accounts that have postings; add their parents (below Expenses itself)
// so a whole subtree like Expenses:Taxes can be hidden at once.
private fun withParents(accounts: List<String>): List<String> =
    accounts.flatMap { account ->
        val parts = account.split(":")
        (2..parts.size).map { parts.take(it).joinToString(":") }
    }.distinct()

@Composable
private fun HiddenAccounts() {
    var query by remember { mutableStateOf("") }
    val hidden = AppSettings.excludedExpenseAccounts
    val candidates = remember(AppSettings.expenseAccounts, hidden) {
        withParents(AppSettings.expenseAccounts) - hidden.toSet()
    }

    AccountSearchField(
        query = query,
        onQueryChange = { query = it },
        accounts = candidates,
        onPick = {
            AppSettings.excludedExpenseAccounts += it
            query = ""
        },
        label = "Hide from Expenses breakdown",
        supportingText = "Hidden on the Infos page only. Tap a chip to unhide.",
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        hidden.forEach { account ->
            InputChip(
                selected = false,
                onClick = { AppSettings.excludedExpenseAccounts -= account },
                label = { Text(account.removePrefix("Expenses:")) },
                trailingIcon = {
                    Icon(Icons.Default.Close, contentDescription = "Unhide", modifier = Modifier.size(InputChipDefaults.IconSize))
                },
            )
        }
    }
}
