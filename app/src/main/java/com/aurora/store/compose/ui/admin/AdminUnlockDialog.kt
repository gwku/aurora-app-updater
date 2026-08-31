/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.aurora.Constants
import com.aurora.extensions.browse
import com.aurora.store.R

/**
 * Prompt for the GitHub token that unlocks the admin tools.
 * @param onUnlock Callback to verify a token, reporting whether it was accepted
 * @param onUnlocked Callback once a token has been accepted
 * @param onDismiss Callback on dismiss
 */
@Composable
fun AdminUnlockDialog(
    onUnlock: (token: String, onResult: (Boolean) -> Unit) -> Unit = { _, _ -> },
    onUnlocked: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current

    var token by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    AlertDialog(
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_shield),
                contentDescription = null
            )
        },
        title = { Text(text = stringResource(R.string.title_admin)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin_medium))
            ) {
                Text(text = stringResource(R.string.admin_unlock_summary))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = token,
                    onValueChange = {
                        token = it.trim()
                        failed = false
                    },
                    placeholder = { Text(text = stringResource(R.string.admin_token_hint)) },
                    shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
                    singleLine = true,
                    enabled = !checking,
                    isError = failed,
                    supportingText = if (failed) {
                        { Text(text = stringResource(R.string.admin_token_rejected)) }
                    } else {
                        null
                    },
                    visualTransformation = PasswordVisualTransformation()
                )
                TextButton(onClick = { context.browse(Constants.GRANTS_TOKEN_HELP_URL) }) {
                    Text(text = stringResource(R.string.admin_token_help))
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = token.isNotBlank() && !checking,
                onClick = {
                    checking = true
                    onUnlock(token) { accepted ->
                        checking = false
                        failed = !accepted
                        if (accepted) onUnlocked()
                    }
                }
            ) {
                Text(text = stringResource(R.string.action_unlock))
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
private fun AdminUnlockDialogPreview() {
    AdminUnlockDialog()
}
