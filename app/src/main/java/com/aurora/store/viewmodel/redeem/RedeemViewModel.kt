/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.redeem

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.TAG
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.store.data.helper.DownloadHelper
import com.aurora.store.data.model.Grant
import com.aurora.store.data.model.RedeemError
import com.aurora.store.data.model.RedeemState
import com.aurora.store.data.network.GrantStore
import com.aurora.store.data.providers.AdminProvider
import com.aurora.store.util.PackageUtil
import com.aurora.store.util.RedeemCodeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RedeemViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adminProvider: AdminProvider,
    private val grantStore: GrantStore,
    private val appDetailsHelper: AppDetailsHelper,
    private val downloadHelper: DownloadHelper
) : ViewModel() {

    private val _state = MutableStateFlow<RedeemState>(RedeemState.Idle)
    val state = _state.asStateFlow()

    val isAdmin = adminProvider.isUnlocked

    private var grant: Grant? = null

    /**
     * Looks up the code and, if it is good for an app, loads that app ready to install.
     * @param input Code as typed by the user, separators and casing do not matter
     */
    fun redeem(input: String) {
        if (!RedeemCodeUtil.isValid(input)) {
            _state.value = RedeemState.Failed(RedeemError.MALFORMED)
            return
        }

        val codeHash = RedeemCodeUtil.hash(input)
        _state.value = RedeemState.Checking

        viewModelScope.launch(Dispatchers.IO) {
            val grants = try {
                grantStore.fetchGrants()
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch grants", exception)
                _state.value = RedeemState.Failed(RedeemError.NETWORK)
                return@launch
            }

            val match = grants.find { it.codeHash == codeHash }
            when {
                match == null -> {
                    _state.value = RedeemState.Failed(RedeemError.UNKNOWN_CODE)
                    return@launch
                }

                match.isExpired() -> {
                    _state.value = RedeemState.Failed(RedeemError.EXPIRED)
                    return@launch
                }

                /*
                 * A code that has been used and whose app is present has done its job. If the app
                 * is gone the download most likely failed, so we let the code through again
                 * rather than strand someone — reinstalling is the only thing it can do anyway,
                 * since a grant is bound to a single package. Expiry and revocation are what
                 * actually put a code out of use.
                 */
                adminProvider.isCodeSpent(codeHash) &&
                    PackageUtil.isInstalled(context, match.packageName) -> {
                    _state.value = RedeemState.Failed(RedeemError.ALREADY_USED)
                    return@launch
                }

                PackageUtil.isInstalled(context, match.packageName) -> {
                    _state.value = RedeemState.Failed(RedeemError.ALREADY_INSTALLED)
                    return@launch
                }
            }

            try {
                val app = appDetailsHelper.getAppByPackageName(match.packageName)
                grant = match
                _state.value = RedeemState.Granted(app)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch details for ${match.packageName}", exception)
                _state.value = RedeemState.Failed(RedeemError.APP_UNAVAILABLE)
            }
        }
    }

    /**
     * Hands the granted app to the download queue and spends the code.
     */
    fun install() {
        val app = (state.value as? RedeemState.Granted)?.app ?: return
        val codeHash = grant?.codeHash ?: return

        viewModelScope.launch(Dispatchers.IO) {
            downloadHelper.enqueueGrantedApp(app)
            adminProvider.markCodeSpent(codeHash)
            _state.value = RedeemState.Installing(app)
        }
    }

    fun reset() {
        grant = null
        _state.value = RedeemState.Idle
    }

    /**
     * Unlocks the admin tools if the token can reach the repository holding the grants.
     * @param token GitHub personal access token
     * @param onResult Callback with whether the token was accepted
     */
    fun unlockAdmin(token: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val valid = grantStore.verifyToken(token)
            if (valid) adminProvider.token = token
            onResult(valid)
        }
    }
}
