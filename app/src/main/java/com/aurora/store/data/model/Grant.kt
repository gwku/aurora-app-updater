/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A permission, published by an admin, allowing a single app to be installed on a device that is
 * otherwise restricted to updates-only.
 *
 * Grants are published to a public file, so the code itself is never stored — only its hash.
 * @see com.aurora.store.util.RedeemCodeUtil
 */
@Serializable
data class Grant(
    @SerialName("code_hash") val codeHash: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("icon_url") val iconURL: String = "",
    @SerialName("issued_at") val issuedAt: Long = 0L,
    @SerialName("expires_at") val expiresAt: Long = 0L,
    @SerialName("note") val note: String = ""
) {

    /**
     * Whether this grant is past its expiry. Grants without an expiry never go stale.
     * @param now Current time in epoch seconds
     */
    fun isExpired(now: Long = System.currentTimeMillis() / 1000): Boolean =
        expiresAt > 0L && expiresAt < now
}

/**
 * Contents of the published grants file.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GrantFile(
    /*
     * Written out even though it matches the default, otherwise the marker vanishes the first
     * time the app rewrites the file and there is nothing left to migrate against later.
     */
    @EncodeDefault @SerialName("version") val version: Int = VERSION,
    @SerialName("grants") val grants: List<Grant> = emptyList()
) {

    companion object {
        const val VERSION = 1
    }
}
