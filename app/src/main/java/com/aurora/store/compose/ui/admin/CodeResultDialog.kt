/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.aurora.extensions.copyToClipBoard
import com.aurora.extensions.shareText
import com.aurora.store.R
import com.aurora.store.util.RedeemCodeUtil

/**
 * Shows a freshly issued code so the admin can pass it on.
 * @param code The code, unformatted
 * @param displayName Name of the app the code unlocks
 * @param onDismiss Callback on dismiss
 */
@Composable
fun CodeResultDialog(code: String, displayName: String, onDismiss: () -> Unit = {}) {
    val context = LocalContext.current
    val formatted = RedeemCodeUtil.format(code)
    val message = stringResource(R.string.admin_code_message, displayName, formatted)

    AlertDialog(
        title = { Text(text = stringResource(R.string.admin_code_title, displayName)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.margin_medium)
                )
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = formatted,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.admin_code_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.margin_small),
                        Alignment.CenterHorizontally
                    )
                ) {
                    OutlinedButton(onClick = { context.copyToClipBoard(message) }) {
                        Text(text = stringResource(R.string.action_copy))
                    }
                    Button(onClick = { context.shareText(message) }) {
                        Text(text = stringResource(R.string.action_share))
                    }
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_done))
            }
        }
    )
}

@Preview
@Composable
private fun CodeResultDialogPreview() {
    CodeResultDialog(code = "K7QM2X4B", displayName = "Signal")
}
