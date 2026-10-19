package dev.ghjkl.tally.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction

/** Text field with a fuzzy-matched dropdown of [accounts]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    accounts: List<String>,
    onPick: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val matches = remember(query, accounts) { rankByFuzzyScore(query, accounts, limit = 8) }

    ExposedDropdownMenuBox(
        expanded = expanded && matches.isNotEmpty(),
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                expanded = true
            },
            label = if (label != null) { { Text(label) } } else null,
            placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
            supportingText = if (supportingText != null) { { Text(supportingText) } } else null,
            isError = isError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                expanded = false
                defaultKeyboardAction(ImeAction.Next)
            }),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded && matches.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            matches.forEach { match ->
                DropdownMenuItem(
                    text = { Text(match) },
                    onClick = {
                        expanded = false
                        onPick(match)
                    },
                )
            }
        }
    }
}
