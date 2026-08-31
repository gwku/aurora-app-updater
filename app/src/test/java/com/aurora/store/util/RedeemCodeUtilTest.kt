/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RedeemCodeUtilTest {

    @Test
    fun testGeneratedCodesAreValid() {
        repeat(100) {
            val code = RedeemCodeUtil.generate()
            assertThat(code).hasLength(RedeemCodeUtil.LENGTH)
            assertThat(RedeemCodeUtil.isValid(code)).isTrue()
            assertThat(RedeemCodeUtil.normalize(code)).isEqualTo(code)
        }
    }

    @Test
    fun testGeneratedCodesAreNotRepeated() {
        val codes = List(500) { RedeemCodeUtil.generate() }
        assertThat(codes.toSet()).hasSize(codes.size)
    }

    @Test
    fun testNormalizeIgnoresCasingAndSeparators() {
        assertThat(RedeemCodeUtil.normalize(" k7qm-2x4b ")).isEqualTo("K7QM2X4B")
        assertThat(RedeemCodeUtil.normalize("K7QM 2X4B")).isEqualTo("K7QM2X4B")
    }

    @Test
    fun testNormalizeFoldsConfusableCharacters() {
        // O reads as zero, I and L read as one, U reads as V
        assertThat(RedeemCodeUtil.normalize("OILU")).isEqualTo("011V")
        assertThat(RedeemCodeUtil.normalize("oilu")).isEqualTo("011V")
    }

    @Test
    fun testFormatGroupsCodeInFours() {
        assertThat(RedeemCodeUtil.format("K7QM2X4B")).isEqualTo("K7QM-2X4B")
        assertThat(RedeemCodeUtil.format("k7qm-2x4b")).isEqualTo("K7QM-2X4B")
    }

    @Test
    fun testValidityRequiresFullLength() {
        assertThat(RedeemCodeUtil.isValid("K7QM2X4")).isFalse()
        assertThat(RedeemCodeUtil.isValid("K7QM2X4BC")).isFalse()
        assertThat(RedeemCodeUtil.isValid("K7QM-2X4B")).isTrue()
    }

    @Test
    fun testHashIsStableAcrossHowACodeIsTyped() {
        val hash = RedeemCodeUtil.hash("K7QM2X4B")

        assertThat(RedeemCodeUtil.hash("k7qm-2x4b")).isEqualTo(hash)
        assertThat(RedeemCodeUtil.hash(" K7QM 2X4B ")).isEqualTo(hash)
        assertThat(hash).hasLength(64)
    }

    @Test
    fun testHashDiffersBetweenCodes() {
        assertThat(RedeemCodeUtil.hash("K7QM2X4B"))
            .isNotEqualTo(RedeemCodeUtil.hash("K7QM2X4C"))
    }
}
