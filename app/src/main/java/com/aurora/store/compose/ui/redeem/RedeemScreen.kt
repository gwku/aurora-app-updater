/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.redeem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.TopAppBar
import com.aurora.store.compose.composable.app.LargeAppListItem
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.PreviewTemplate
import com.aurora.store.compose.ui.admin.AdminUnlockDialog
import com.aurora.store.data.model.RedeemError
import com.aurora.store.data.model.RedeemState
import com.aurora.store.util.RedeemCodeUtil
import com.aurora.store.viewmodel.redeem.RedeemViewModel

/**
 * Number of taps on the header icon that reveals the admin unlock prompt.
 */
private const val ADMIN_TAPS = 7

@Composable
fun RedeemScreen(
    onNavigateUp: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    viewModel: RedeemViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

    var shouldShowUnlockDialog by rememberSaveable { mutableStateOf(false) }
    if (shouldShowUnlockDialog) {
        AdminUnlockDialog(
            onUnlock = { token, onResult -> viewModel.unlockAdmin(token, onResult) },
            onUnlocked = {
                shouldShowUnlockDialog = false
                onNavigateToAdmin()
            },
            onDismiss = { shouldShowUnlockDialog = false }
        )
    }

    ScreenContent(
        state = state,
        isAdmin = isAdmin,
        onNavigateUp = onNavigateUp,
        onNavigateToDownloads = onNavigateToDownloads,
        onNavigateToAdmin = onNavigateToAdmin,
        onRedeem = viewModel::redeem,
        onInstall = viewModel::install,
        onReset = viewModel::reset,
        onRequestAdmin = { if (isAdmin) onNavigateToAdmin() else shouldShowUnlockDialog = true }
    )
}

@Composable
private fun ScreenContent(
    state: RedeemState = RedeemState.Idle,
    isAdmin: Boolean = false,
    onNavigateUp: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    onRedeem: (code: String) -> Unit = {},
    onInstall: () -> Unit = {},
    onReset: () -> Unit = {},
    onRequestAdmin: () -> Unit = {}
) {
    var code by rememberSaveable { mutableStateOf("") }
    var taps by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        // The app draws edge to edge, so the keyboard would otherwise sit over the code field
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_redeem),
                onNavigateUp = onNavigateUp,
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = onNavigateToAdmin) {
                            Icon(
                                painter = painterResource(R.drawable.ic_shield),
                                contentDescription = stringResource(R.string.title_admin)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.padding_large)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.margin_large),
                Alignment.CenterVertically
            )
        ) {
            when (state) {
                is RedeemState.Checking -> ContainedLoadingIndicator()

                is RedeemState.Granted -> GrantedContent(
                    app = state.app,
                    onInstall = onInstall,
                    onReset = onReset
                )

                is RedeemState.Installing -> InstallingContent(
                    app = state.app,
                    onNavigateToDownloads = onNavigateToDownloads,
                    onReset = onReset
                )

                is RedeemState.DownloadFailed -> DownloadFailedContent(
                    app = state.app,
                    onRetry = onInstall,
                    onReset = onReset
                )

                else -> InputContent(
                    code = code,
                    error = (state as? RedeemState.Failed)?.error,
                    onCodeChange = { code = it },
                    onRedeem = onRedeem,
                    onIconTap = {
                        taps++
                        if (taps >= ADMIN_TAPS) {
                            taps = 0
                            onRequestAdmin()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InputContent(
    code: String = "",
    error: RedeemError? = null,
    onCodeChange: (code: String) -> Unit = {},
    onRedeem: (code: String) -> Unit = {},
    onIconTap: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

    fun redeem() {
        focusManager.clearFocus()
        onRedeem(code)
    }

    Icon(
        painter = painterResource(R.drawable.ic_sale),
        contentDescription = null,
        modifier = Modifier
            .requiredSize(dimensionResource(R.dimen.icon_size))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onIconTap
            )
    )
    Text(
        text = stringResource(R.string.redeem_summary),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = code,
        onValueChange = {
            onCodeChange(RedeemCodeUtil.normalize(it).take(RedeemCodeUtil.LENGTH))
        },
        placeholder = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.redeem_hint),
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace
            )
        },
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 8.sp,
            textAlign = TextAlign.Center
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
        singleLine = true,
        isError = error != null,
        supportingText = if (error != null) {
            {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(error.messageRes),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { redeem() })
    )
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = ::redeem,
        enabled = RedeemCodeUtil.isValid(code)
    ) {
        Text(text = stringResource(R.string.action_redeem))
    }
}

@Composable
private fun GrantedContent(app: App, onInstall: () -> Unit = {}, onReset: () -> Unit = {}) {
    Text(
        text = stringResource(R.string.redeem_granted),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center
    )
    LargeAppListItem(app = app)
    Button(modifier = Modifier.fillMaxWidth(), onClick = onInstall) {
        Text(text = stringResource(R.string.action_install))
    }
    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onReset) {
        Text(text = stringResource(R.string.action_redeem_another))
    }
}

@Composable
private fun InstallingContent(
    app: App,
    onNavigateToDownloads: () -> Unit = {},
    onReset: () -> Unit = {}
) {
    Icon(
        painter = painterResource(R.drawable.ic_check),
        contentDescription = null,
        modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size))
    )
    Text(
        text = stringResource(R.string.redeem_installing, app.displayName),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center
    )
    Button(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToDownloads) {
        Text(text = stringResource(R.string.title_download_manager))
    }
    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onReset) {
        Text(text = stringResource(R.string.action_redeem_another))
    }
}

@Composable
private fun DownloadFailedContent(app: App, onRetry: () -> Unit = {}, onReset: () -> Unit = {}) {
    Icon(
        painter = painterResource(R.drawable.ic_problem),
        contentDescription = null,
        modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size))
    )
    Text(
        text = stringResource(R.string.redeem_download_failed, app.displayName),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center
    )
    Text(
        text = stringResource(R.string.redeem_download_failed_summary),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
    Button(modifier = Modifier.fillMaxWidth(), onClick = onRetry) {
        Text(text = stringResource(R.string.action_retry))
    }
    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onReset) {
        Text(text = stringResource(R.string.action_redeem_another))
    }
}

private val RedeemError.messageRes: Int
    get() = when (this) {
        RedeemError.MALFORMED -> R.string.redeem_error_malformed
        RedeemError.UNKNOWN_CODE -> R.string.redeem_error_unknown
        RedeemError.EXPIRED -> R.string.redeem_error_expired
        RedeemError.ALREADY_USED -> R.string.redeem_error_used
        RedeemError.ALREADY_INSTALLED -> R.string.redeem_error_installed
        RedeemError.NETWORK -> R.string.redeem_error_network
        RedeemError.APP_UNAVAILABLE -> R.string.redeem_error_unavailable
    }

@Preview
@Composable
private fun RedeemScreenPreview() {
    PreviewTemplate {
        ScreenContent()
    }
}

@Preview
@Composable
private fun RedeemScreenErrorPreview() {
    PreviewTemplate {
        ScreenContent(state = RedeemState.Failed(RedeemError.UNKNOWN_CODE))
    }
}

@Preview
@Composable
private fun RedeemScreenGrantedPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    PreviewTemplate {
        ScreenContent(state = RedeemState.Granted(app))
    }
}

@Preview
@Composable
private fun RedeemScreenDownloadFailedPreview(
    @PreviewParameter(AppPreviewProvider::class) app: App
) {
    PreviewTemplate {
        ScreenContent(state = RedeemState.DownloadFailed(app))
    }
}
