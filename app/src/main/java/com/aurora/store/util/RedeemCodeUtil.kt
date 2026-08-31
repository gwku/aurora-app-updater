/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.util

import com.aurora.store.data.model.Algorithm
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Helpers for the short codes an admin hands out to allow a single app install.
 *
 * Codes use Crockford's Base32 alphabet, which drops the characters people confuse with each
 * other, so a code stays readable over the phone or in a text message.
 */
object RedeemCodeUtil {

    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val GROUP_SIZE = 4

    const val LENGTH = 8

    private val random by lazy { SecureRandom() }

    /**
     * Generates a new random code, e.g. `K7QM2X4B`.
     */
    fun generate(): String = (1..LENGTH)
        .map { ALPHABET[random.nextInt(ALPHABET.length)] }
        .joinToString(separator = "")

    /**
     * Strips separators and folds the characters people typically mistype onto the ones the
     * alphabet actually uses, so `o-slz` and `0512` end up identical.
     */
    fun normalize(input: String): String = input.uppercase()
        .mapNotNull { char ->
            when (char) {
                'O' -> '0'
                'I', 'L' -> '1'
                'U' -> 'V'
                in ALPHABET -> char
                else -> null
            }
        }
        .joinToString(separator = "")

    /**
     * Formats a code for display, e.g. `K7QM-2X4B`.
     */
    fun format(code: String): String = normalize(code)
        .chunked(GROUP_SIZE)
        .joinToString(separator = "-")

    /**
     * Whether the input could be a code at all, before we go looking for it.
     */
    fun isValid(input: String): Boolean = normalize(input).length == LENGTH

    /**
     * Hash published alongside a grant. Codes themselves never leave the admin's device, so a
     * public grants file cannot be mined for working codes.
     */
    fun hash(code: String): String {
        val digest = MessageDigest.getInstance(Algorithm.SHA256.value)
            .digest(normalize(code).toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
