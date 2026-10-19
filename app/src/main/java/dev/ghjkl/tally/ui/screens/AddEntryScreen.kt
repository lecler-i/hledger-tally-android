package dev.ghjkl.tally.ui.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.ghjkl.tally.data.AppSettings
import dev.ghjkl.tally.termux.CASH_ACCOUNT
import dev.ghjkl.tally.termux.appendEntry
import dev.ghjkl.tally.termux.formatEntry
import dev.ghjkl.tally.ui.AccountSearchField
import dev.ghjkl.tally.ui.rememberRunCommandPermission
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun AddEntryScreen(snackbarHostState: SnackbarHostState, onEntryAdded: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permission = rememberRunCommandPermission()
    val focusManager = LocalFocusManager.current
    val amountFocus = remember { FocusRequester() }

    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var payee by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf(AppSettings.defaultCurrency) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val parsedAmount = amount.trim().replace(',', '.').toBigDecimalOrNull()
    val resolvedAccount = account.trim().ifBlank { AppSettings.lastUsedAccount }
    val valid = parsedAmount != null && payee.isNotBlank() && resolvedAccount.isNotBlank() && currency.isNotBlank()

    LaunchedEffect(Unit) { amountFocus.requestFocus() }

    fun save() = permission.runOrRequest {
        focusManager.clearFocus()
        submitted = true
        if (!valid) return@runOrRequest
        val entry = formatEntry(date, payee.trim(), resolvedAccount, parsedAmount, currency.trim())
        saving = true
        scope.launch {
            val result = appendEntry(context, entry)
            saving = false
            if (!result.ok) {
                error = result.errorText()
                return@launch
            }
            AppSettings.lastUsedAccount = resolvedAccount
            date = LocalDate.now()
            amount = ""
            payee = ""
            account = ""
            submitted = false
            error = ""
            onEntryAdded()
            snackbarHostState.showSnackbar("Entry added")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DateField(date, onDateChange = { date = it })
        RequiredField(
            value = amount,
            onValueChange = { amount = it },
            label = "Amount",
            showError = submitted && parsedAmount == null,
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.focusRequester(amountFocus),
        )
        RequiredField(payee, { payee = it }, "Payee", showError = submitted && payee.isBlank())
        val lastUsed = AppSettings.lastUsedAccount
        // Without a label, the last-used account stays visible as the placeholder.
        AccountSearchField(
            query = account,
            onQueryChange = { account = it },
            accounts = AppSettings.expenseAccounts,
            onPick = {
                account = it
                focusManager.moveFocus(FocusDirection.Next)
            },
            label = "Expense account".takeIf { lastUsed.isBlank() },
            placeholder = lastUsed.ifBlank { null },
            supportingText = "Required".takeIf { submitted && resolvedAccount.isBlank() },
            isError = submitted && resolvedAccount.isBlank(),
        )
        RequiredField(
            value = currency,
            onValueChange = { currency = it },
            label = "Currency",
            showError = submitted && currency.isBlank(),
            imeAction = ImeAction.Done,
        )
        Text("From: $CASH_ACCOUNT")
        Button(onClick = ::save, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
            Text(if (saving) "Saving…" else "Add entry")
        }
        if (error.isNotBlank()) Text(error)
    }
}

@Composable
private fun RequiredField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showError: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = showError,
        supportingText = if (showError) { { Text("Required") } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

// DatePicker works in UTC millis; converting through UTC keeps the day from shifting.
private fun LocalDate.toUtcMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toUtcDate() = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun DateField(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    LaunchedEffect(interactions) {
        interactions.interactions.collect { if (it is PressInteraction.Release) showPicker = true }
    }

    OutlinedTextField(
        value = date.toString(),
        onValueChange = {},
        readOnly = true,
        label = { Text("Date") },
        trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Pick date") },
        singleLine = true,
        interactionSource = interactions,
        modifier = Modifier.fillMaxWidth(),
    )

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDateChange(it.toUtcDate()) }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}
