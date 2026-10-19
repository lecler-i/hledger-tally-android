package dev.ghjkl.tally.ui.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.ghjkl.tally.data.AppSettings
import dev.ghjkl.tally.termux.SH_PATH
import dev.ghjkl.tally.termux.TermuxRunner
import dev.ghjkl.tally.ui.rankByFuzzyScore
import dev.ghjkl.tally.ui.rememberRunCommandPermission
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

// DatePicker operates in UTC internally to avoid timezone date-shifting — convert
// through ZoneOffset.UTC specifically, not the system default zone, both ways.
private fun LocalDate.toUtcEpochMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toLocalDateUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permission = rememberRunCommandPermission()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val amountFocusRequester = remember { FocusRequester() }
    val dateFieldInteractionSource = remember { MutableInteractionSource() }

    var date by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(AppSettings.defaultCurrency) }
    var payee by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var payeeError by remember { mutableStateOf(false) }
    var currencyError by remember { mutableStateOf(false) }

    val accountMatches = remember(account, AppSettings.expenseAccounts) {
        rankByFuzzyScore(account, AppSettings.expenseAccounts, limit = 8)
    }

    LaunchedEffect(Unit) { amountFocusRequester.requestFocus() }

    LaunchedEffect(dateFieldInteractionSource) {
        dateFieldInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) showDatePicker = true
        }
    }

    fun save() {
        focusManager.clearFocus()
        keyboardController?.hide()
        if (!permission.granted) {
            permission.request()
            return
        }
        val amountValue = amount.trim().toDoubleOrNull()
        val resolvedAccount = account.trim().ifBlank { AppSettings.lastUsedAccount }

        amountError = amountValue == null
        payeeError = payee.isBlank()
        currencyError = currency.isBlank()

        if (amountError || payeeError || currencyError || resolvedAccount.isBlank()) {
            status = if (resolvedAccount.isBlank()) {
                "Fill in the required fields (and an account, unless you have a " +
                    "previously-used one to fall back on)."
            } else {
                ""
            }
            scope.launch { snackbarHostState.showSnackbar("Please fill in the required fields") }
            return
        }
        isSaving = true
        status = ""
        scope.launch {
            val amountText = String.format(Locale.US, "%10.2f %s", amountValue, currency)
            val negativeAmountText = String.format(Locale.US, "%10.2f %s", -amountValue!!, currency)
            // Pad accounts to a fixed column so amounts line up regardless of account name
            // length, with a guaranteed minimum 2-space gap hledger requires.
            val assetField = "Assets:Cash".padEnd(28)
            val accountField = (resolvedAccount + "  ").padEnd(28)
            val entry = buildString {
                append("$date $payee\n")
                append("    $assetField$negativeAmountText\n")
                append("    $accountField$amountText\n")
                // No extra trailing blank line here — Termux's RUN_COMMAND_STDIN already
                // adds one, so adding our own doubled up the gap between entries.
            }
            val result = TermuxRunner.run(
                context,
                SH_PATH,
                // Deliberately unquoted: lets sh expand a leading `~` in the configured path.
                listOf("-c", "cat >> ${AppSettings.manualJournalPath}"),
                stdin = entry,
            )
            isSaving = false
            if (result.exitCode == 0) {
                AppSettings.updateLastUsedAccount(resolvedAccount)
                date = LocalDate.now()
                amount = ""
                payee = ""
                account = ""
                status = ""
                amountError = false
                payeeError = false
                currencyError = false
                snackbarHostState.showSnackbar("Entry added")
                amountFocusRequester.requestFocus()
            } else {
                status = "Failed (exit ${result.exitCode}): " +
                    result.errmsg.ifBlank { result.stderr.ifBlank { "unknown error" } }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date.toUtcEpochMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = it.toLocalDateUtc() }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = date.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Date") },
            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Pick date") },
            singleLine = true,
            // Reports its own press events here instead of accepting typed input — this is
            // the documented way to make a whole read-only field act as a tap target.
            interactionSource = dateFieldInteractionSource,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it; amountError = false },
            label = { Text("Amount") },
            isError = amountError,
            supportingText = if (amountError) { { Text("Required") } } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(amountFocusRequester),
        )
        OutlinedTextField(
            value = payee,
            onValueChange = { payee = it; payeeError = false },
            label = { Text("Payee") },
            isError = payeeError,
            supportingText = if (payeeError) { { Text("Required") } } else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ExposedDropdownMenuBox(
            expanded = accountMenuExpanded && accountMatches.isNotEmpty(),
            onExpandedChange = { accountMenuExpanded = it },
        ) {
            OutlinedTextField(
                value = account,
                onValueChange = {
                    account = it
                    accountMenuExpanded = true
                },
                // Once there's a real last-used account, drop the label entirely so the
                // placeholder (always visible while empty, shown in a muted colour by
                // Material3) can show that real value instead of a floating "label".
                label = if (AppSettings.lastUsedAccount.isBlank()) {
                    { Text("Expense account") }
                } else {
                    null
                },
                placeholder = if (AppSettings.lastUsedAccount.isNotBlank()) {
                    { Text(AppSettings.lastUsedAccount) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    accountMenuExpanded = false
                    focusManager.moveFocus(FocusDirection.Down)
                }),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
            )
            ExposedDropdownMenu(
                expanded = accountMenuExpanded && accountMatches.isNotEmpty(),
                onDismissRequest = { accountMenuExpanded = false },
            ) {
                accountMatches.forEach { match ->
                    DropdownMenuItem(
                        text = { Text(match) },
                        onClick = {
                            account = match
                            accountMenuExpanded = false
                            focusManager.moveFocus(FocusDirection.Down)
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = currency,
            onValueChange = { currency = it; currencyError = false },
            label = { Text("Currency") },
            isError = currencyError,
            supportingText = if (currencyError) { { Text("Required") } } else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("From: Assets:Cash")
        Button(
            onClick = ::save,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isSaving) "Saving…" else "Add entry")
        }
        if (status.isNotBlank()) {
            Text(status)
        }
    }
}
