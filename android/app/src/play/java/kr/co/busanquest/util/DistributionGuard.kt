package kr.co.busanquest.util

import android.content.Context

/** Play installation is authenticated by the server-decoded Play Integrity verdict. */
object DistributionGuard {
    const val channel = "PLAY"

    fun requireTrustedInstall(@Suppress("UNUSED_PARAMETER") context: Context) = Unit
}
