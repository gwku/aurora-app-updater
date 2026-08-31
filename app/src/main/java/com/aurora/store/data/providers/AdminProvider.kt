/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.providers

import android.content.Context
import androidx.core.content.edit
import com.aurora.store.util.Preferences
import com.aurora.store.util.Preferences.PREFERENCE_REDEEMED_CODES
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the admin's GitHub token and remembers which codes this device has already spent.
 *
 * The token lives in its own preferences file rather than the app's shared ones, so it stays out
 * of anything the app imports or exports.
 */
@Singleton
class AdminProvider @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val PREF_FILE = "admin_preferences"
        private const val PREFERENCE_ADMIN_TOKEN = "PREFERENCE_ADMIN_TOKEN"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    private val _isUnlocked = MutableStateFlow(!token.isNullOrBlank())

    /**
     * Whether admin tools are available on this device.
     */
    val isUnlocked = _isUnlocked.asStateFlow()

    /**
     * GitHub personal access token used to publish grants, null when admin mode is locked.
     */
    var token: String?
        get() = prefs.getString(PREFERENCE_ADMIN_TOKEN, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit {
                if (value.isNullOrBlank()) {
                    remove(PREFERENCE_ADMIN_TOKEN)
                } else {
                    putString(PREFERENCE_ADMIN_TOKEN, value)
                }
            }
            _isUnlocked.value = !value.isNullOrBlank()
        }

    /**
     * Forgets the stored token and locks admin mode again.
     */
    fun lock() {
        token = null
    }

    /**
     * Whether a code has already been spent on this device.
     * @param codeHash Hash of the code, see [com.aurora.store.util.RedeemCodeUtil.hash]
     */
    fun isCodeSpent(codeHash: String): Boolean =
        codeHash in Preferences.getStringSet(context, PREFERENCE_REDEEMED_CODES)

    /**
     * Marks a code as spent, so it cannot be used for a second install.
     * @param codeHash Hash of the code, see [com.aurora.store.util.RedeemCodeUtil.hash]
     */
    fun markCodeSpent(codeHash: String) {
        val spent = Preferences.getStringSet(context, PREFERENCE_REDEEMED_CODES)
        Preferences.putStringSet(context, PREFERENCE_REDEEMED_CODES, spent + codeHash)
    }
}
