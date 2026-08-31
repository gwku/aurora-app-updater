/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.TAG
import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.SearchHelper
import com.aurora.gplayapi.helpers.contracts.SearchContract
import com.aurora.gplayapi.helpers.web.WebSearchHelper
import com.aurora.store.data.model.Grant
import com.aurora.store.data.network.GrantStore
import com.aurora.store.data.providers.AdminProvider
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.util.RedeemCodeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A code that has just been issued, held so the admin can copy or share it.
 */
data class IssuedCode(val code: String, val app: App)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val authProvider: AuthProvider,
    private val adminProvider: AdminProvider,
    private val grantStore: GrantStore,
    private val appDetailsHelper: AppDetailsHelper,
    private val searchHelper: SearchHelper,
    private val webSearchHelper: WebSearchHelper
) : ViewModel() {

    companion object {
        /**
         * How long a code stays usable, unless the admin picks something else.
         */
        const val DEFAULT_VALIDITY_DAYS = 7

        private const val SEARCH_RESULT_LIMIT = 20
    }

    private val contract: SearchContract
        get() = if (authProvider.isAnonymous) webSearchHelper else searchHelper

    private val _grants = MutableStateFlow<List<Grant>?>(null)
    val grants = _grants.asStateFlow()

    private val _searchResults = MutableStateFlow<List<App>?>(null)
    val searchResults = _searchResults.asStateFlow()

    private val _issuedCode = MutableStateFlow<IssuedCode?>(null)
    val issuedCode = _issuedCode.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        fetchGrants()
    }

    fun fetchGrants() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _grants.value = grantStore.fetchGrants().sortedByDescending { it.issuedAt }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to fetch grants", exception)
                _grants.value = emptyList()
                _error.value = exception.message
            }
        }
    }

    /**
     * Finds apps to issue a code for. A query that looks like a package name is looked up
     * directly, so an app that search will not surface can still be granted.
     * @param query App name or package name
     */
    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = null
            return
        }

        _searchResults.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val direct = if (query.looksLikePackageName()) {
                runCatching { appDetailsHelper.getAppByPackageName(query.trim()) }.getOrNull()
            } else {
                null
            }

            val results = try {
                contract.searchResults(query).streamClusters.values
                    .flatMap { it.clusterAppList }
                    .distinctBy { it.packageName }
                    .take(SEARCH_RESULT_LIMIT)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to search for $query", exception)
                emptyList()
            }

            _searchResults.value = (listOfNotNull(direct) + results)
                .distinctBy { it.packageName }
        }
    }

    /**
     * Issues a code for an app and publishes the matching grant.
     * @param app App the recipient should be allowed to install
     * @param validityDays How long the code stays usable
     * @param note Free-text reminder of who the code went to
     */
    fun issueCode(app: App, validityDays: Int = DEFAULT_VALIDITY_DAYS, note: String = "") {
        val token = adminProvider.token ?: return
        val code = RedeemCodeUtil.generate()
        val now = System.currentTimeMillis() / 1000

        _busy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                grantStore.publishGrant(
                    token = token,
                    grant = Grant(
                        codeHash = RedeemCodeUtil.hash(code),
                        packageName = app.packageName,
                        displayName = app.displayName,
                        iconURL = app.iconArtwork.url,
                        issuedAt = now,
                        expiresAt = now + validityDays.days.inWholeSeconds,
                        note = note
                    )
                )
                _issuedCode.value = IssuedCode(code, app)
                fetchGrants()
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to issue a code for ${app.packageName}", exception)
                _error.value = exception.message
            } finally {
                _busy.value = false
            }
        }
    }

    /**
     * Withdraws a grant, so its code stops working for anyone who has not used it yet.
     */
    fun revokeGrant(grant: Grant) {
        val token = adminProvider.token ?: return

        _busy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                grantStore.revokeGrant(token, grant)
                fetchGrants()
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to revoke grant for ${grant.packageName}", exception)
                _error.value = exception.message
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearIssuedCode() {
        _issuedCode.value = null
    }

    fun clearSearch() {
        _searchResults.value = null
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Forgets the stored token, locking the admin tools on this device.
     */
    fun lock() {
        adminProvider.lock()
    }

    private fun String.looksLikePackageName(): Boolean =
        trim().let { it.contains('.') && !it.contains(' ') }
}
