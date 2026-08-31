/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class GrantFileTest {

    /**
     * Mirrors the app-wide instance from CommonModule, which does not encode defaults.
     */
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val grant = Grant(
        codeHash = "f80072a9e40ee3c10def4bfeecdf857450f7815a4644eeb6b793368aedabf561",
        packageName = "com.chess",
        displayName = "Chess",
        issuedAt = 1788164978,
        expiresAt = 1788251378
    )

    @Test
    fun testSchemaVersionSurvivesARewrite() {
        val encoded = json.encodeToString(GrantFile(grants = listOf(grant)))

        assertThat(encoded).contains("\"version\": 1")
        assertThat(json.decodeFromString<GrantFile>(encoded).version).isEqualTo(GrantFile.VERSION)
    }

    @Test
    fun testGrantsFileWithoutVersionStillReads() {
        val legacy = """{ "grants": [] }"""

        assertThat(json.decodeFromString<GrantFile>(legacy).version)
            .isEqualTo(GrantFile.VERSION)
    }

    @Test
    fun testCodeIsNeverPublished() {
        val encoded = json.encodeToString(GrantFile(grants = listOf(grant)))

        assertThat(encoded).contains(grant.codeHash)
        assertThat(encoded).doesNotContain("code\":")
    }

    @Test
    fun testExpiryIsHonoured() {
        assertThat(grant.isExpired(now = grant.expiresAt - 1)).isFalse()
        assertThat(grant.isExpired(now = grant.expiresAt + 1)).isTrue()
        // A grant without an expiry never goes stale
        assertThat(grant.copy(expiresAt = 0L).isExpired()).isFalse()
    }
}
