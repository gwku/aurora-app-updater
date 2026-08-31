/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.data.model

import com.aurora.gplayapi.data.models.App

/**
 * Why a redeem code could not be used.
 */
enum class RedeemError {
    MALFORMED,
    UNKNOWN_CODE,
    EXPIRED,
    ALREADY_USED,
    ALREADY_INSTALLED,
    NETWORK,
    APP_UNAVAILABLE
}

/**
 * State of the redeem code screen.
 */
sealed interface RedeemState {

    data object Idle : RedeemState

    data object Checking : RedeemState

    /**
     * The code is valid and the app behind it is ready to be installed.
     */
    data class Granted(val app: App) : RedeemState

    /**
     * The app has been handed to the download queue.
     */
    data class Installing(val app: App) : RedeemState

    /**
     * The code was good but the app never made it onto the device.
     */
    data class DownloadFailed(val app: App) : RedeemState

    data class Failed(val error: RedeemError) : RedeemState
}
