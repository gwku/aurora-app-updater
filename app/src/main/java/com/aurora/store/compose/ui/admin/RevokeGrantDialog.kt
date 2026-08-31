/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.admin

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.store.R

/**
 * Dialog for revoking a code that has been handed out
 * @param displayName Name of the app the code unlocks
 * @param onRevoke Callback on revoking the code
 * @param onDismiss Callback on dismiss
 */
@Composable
fun RevokeGrantDialog(displayName: String, onRevoke: () -> Unit = {}, onDismiss: () -> Unit = {}) {
    AlertDialog(
        title = { Text(text = stringResource(R.string.revoke_grant_title)) },
        text = {
            Text(text = stringResource(R.string.revoke_grant_summary, displayName))
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onRevoke) {
                Text(text = stringResource(R.string.action_revoke))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}

@Preview
@Composable
private fun RevokeGrantDialogPreview() {
    RevokeGrantDialog(displayName = "Signal")
}
