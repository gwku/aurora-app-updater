/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.admin

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.store.R
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.Error
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.data.model.Grant
import com.aurora.store.viewmodel.admin.AdminViewModel

@Composable
fun AdminScreen(
    onNavigateUp: () -> Unit,
    onLocked: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val grants by viewModel.grants.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val issuedCode by viewModel.issuedCode.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var shouldShowIssueDialog by rememberSaveable { mutableStateOf(false) }

    // Hold the dialog open while the grant is being published, so the wait is visible
    LaunchedEffect(issuedCode) {
        if (issuedCode != null) {
            shouldShowIssueDialog = false
            viewModel.clearSearch()
        }
    }

    if (shouldShowIssueDialog) {
        IssueCodeDialog(
            searchResults = searchResults,
            busy = busy,
            onSearch = viewModel::search,
            onIssue = viewModel::issueCode,
            onDismiss = {
                shouldShowIssueDialog = false
                viewModel.clearSearch()
            }
        )
    }

    /*
     * Revoking deletes the grant outright, so make it deliberate. The code hash keeps this
     * saveable across a rotation, where the grant itself is not.
     */
    var revokeCodeHash: String? by rememberSaveable { mutableStateOf(null) }
    grants?.find { it.codeHash == revokeCodeHash }?.let { grant ->
        RevokeGrantDialog(
            displayName = grant.displayName.ifBlank { grant.packageName },
            onRevoke = {
                viewModel.revokeGrant(grant)
                revokeCodeHash = null
            },
            onDismiss = { revokeCodeHash = null }
        )
    }

    issuedCode?.let { issued ->
        CodeResultDialog(
            code = issued.code,
            displayName = issued.app.displayName,
            onDismiss = viewModel::clearIssuedCode
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val genericFailure = stringResource(R.string.admin_failed)
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it.ifBlank { genericFailure })
            viewModel.clearError()
        }
    }

    ScreenContent(
        grants = grants,
        snackbarHostState = snackbarHostState,
        onNavigateUp = onNavigateUp,
        onIssueCode = { shouldShowIssueDialog = true },
        onRevokeGrant = { grant -> revokeCodeHash = grant.codeHash },
        onLock = {
            viewModel.lock()
            onLocked()
        }
    )
}

@Composable
private fun ScreenContent(
    grants: List<Grant>? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateUp: () -> Unit = {},
    onIssueCode: () -> Unit = {},
    onRevokeGrant: (grant: Grant) -> Unit = {},
    onLock: () -> Unit = {}
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_admin),
                onNavigateUp = onNavigateUp,
                actions = {
                    IconButton(onClick = onLock) {
                        Icon(
                            painter = painterResource(R.drawable.ic_logout),
                            contentDescription = stringResource(R.string.action_lock)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!grants.isNullOrEmpty()) {
                FloatingActionButton(onClick = onIssueCode) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.admin_issue_title)
                    )
                }
            }
        }
    ) { paddingValues ->
        when {
            grants == null -> ContainedLoadingIndicator(
                modifier = Modifier.padding(paddingValues)
            )

            grants.isEmpty() -> Error(
                modifier = Modifier.padding(paddingValues),
                painter = painterResource(R.drawable.ic_sale),
                message = stringResource(R.string.admin_no_grants),
                actionMessage = stringResource(R.string.admin_issue_title),
                onAction = onIssueCode
            )

            else -> LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(vertical = dimensionResource(R.dimen.padding_medium))
            ) {
                items(items = grants, key = { it.codeHash }) { grant ->
                    GrantListItem(grant = grant, onRevoke = { onRevokeGrant(grant) })
                }
            }
        }
    }
}

@Composable
private fun GrantListItem(grant: Grant, onRevoke: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.padding_small)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = grant.displayName.ifBlank { grant.packageName },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = grant.subtitle(),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = onRevoke) {
            Text(text = stringResource(R.string.action_revoke))
        }
    }
}

@Composable
private fun Grant.subtitle(): String {
    val validity = when {
        expiresAt <= 0L -> stringResource(R.string.admin_grant_no_expiry)
        isExpired() -> stringResource(R.string.admin_grant_expired)
        else -> stringResource(
            R.string.admin_grant_expires,
            DateUtils.getRelativeTimeSpanString(
                expiresAt * 1000,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        )
    }

    return listOf(validity, note).filter { it.isNotBlank() }.joinToString(separator = "  •  ")
}

@Preview
@Composable
private fun AdminScreenPreview() {
    val now = System.currentTimeMillis() / 1000
    val grants = List(5) {
        Grant(
            codeHash = "hash$it",
            packageName = "org.thoughtcrime.securesms",
            displayName = "Signal",
            issuedAt = now,
            expiresAt = now + 604800,
            note = "for mum"
        )
    }

    PreviewTemplate {
        ScreenContent(grants = grants)
    }
}

@Preview
@Composable
private fun AdminScreenEmptyPreview() {
    PreviewTemplate {
        ScreenContent(grants = emptyList())
    }
}
