/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.app.LargeAppListItem
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.viewmodel.admin.AdminViewModel

/**
 * Validity periods an admin can pick from, in days.
 */
private val VALIDITY_OPTIONS = listOf(1, AdminViewModel.DEFAULT_VALIDITY_DAYS, 30)

/**
 * Dialog for issuing a code: find the app, then say how long the code should last.
 * @param searchResults Apps matching the current query, null while a search is running
 * @param busy Whether a code is currently being published
 * @param onSearch Callback to search for an app
 * @param onIssue Callback to issue a code for the picked app
 * @param onDismiss Callback on dismiss
 */
@Composable
fun IssueCodeDialog(
    searchResults: List<App>? = null,
    busy: Boolean = false,
    onSearch: (query: String) -> Unit = {},
    onIssue: (app: App, validityDays: Int, note: String) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit = {}
) {
    var picked: App? by remember { mutableStateOf(null) }

    AlertDialog(
        title = { Text(text = stringResource(R.string.admin_issue_title)) },
        text = {
            when (val app = picked) {
                null -> SearchStep(searchResults = searchResults, onSearch = onSearch) {
                    picked = it
                }

                else -> DetailsStep(app = app, busy = busy, onIssue = onIssue)
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { if (picked != null) picked = null else onDismiss() }) {
                Text(
                    text = when {
                        picked != null -> stringResource(R.string.action_back)
                        else -> stringResource(android.R.string.cancel)
                    }
                )
            }
        }
    )
}

@Composable
private fun SearchStep(
    searchResults: List<App>?,
    onSearch: (query: String) -> Unit,
    onPick: (app: App) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var searched by remember { mutableStateOf(false) }

    fun search() {
        focusManager.clearFocus()
        searched = true
        onSearch(query)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
    ) {
        Text(text = stringResource(R.string.admin_issue_search_summary))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(text = stringResource(R.string.admin_issue_search_hint)) },
            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search() })
        )

        when {
            !searched -> Unit
            searchResults == null -> CircularProgressIndicator()
            searchResults.isEmpty() -> Text(text = stringResource(R.string.no_apps_available))
            else -> LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(items = searchResults, key = { it.packageName }) { app ->
                    LargeAppListItem(app = app, onClick = { onPick(app) })
                }
            }
        }
    }
}

@Composable
private fun DetailsStep(
    app: App,
    busy: Boolean,
    onIssue: (app: App, validityDays: Int, note: String) -> Unit
) {
    var validityDays by remember { mutableIntStateOf(AdminViewModel.DEFAULT_VALIDITY_DAYS) }
    var note by remember { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
    ) {
        LargeAppListItem(app = app)
        Text(text = stringResource(R.string.admin_issue_validity))
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_small))
        ) {
            VALIDITY_OPTIONS.forEach { days ->
                FilterChip(
                    selected = days == validityDays,
                    onClick = { validityDays = days },
                    label = {
                        Text(text = pluralStringResource(R.plurals.admin_issue_days, days, days))
                    }
                )
            }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = note,
            onValueChange = { note = it },
            placeholder = { Text(text = stringResource(R.string.admin_issue_note_hint)) },
            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
            singleLine = true
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
            onClick = { onIssue(app, validityDays, note) }
        ) {
            Text(
                text = when {
                    busy -> stringResource(R.string.loading)
                    else -> stringResource(R.string.admin_issue_action)
                }
            )
        }
    }
}

@Preview
@Composable
private fun IssueCodeDialogPreview() {
    PreviewTemplate {
        IssueCodeDialog()
    }
}
