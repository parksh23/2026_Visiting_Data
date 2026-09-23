package kr.co.busanquest.util

import android.content.Context
import com.gaa.sdk.base.StoreEnvironment

/**
 * ALC purchase licensing does not apply to this free app. StoreEnvironment rejects a copied APK
 * before Play Integrity binds the request to the known ONE Store signing certificate and device.
 */
object DistributionGuard {
    const val channel = "ONESTORE"
    private const val STORE_TYPE_ONESTORE = 1

    fun requireTrustedInstall(context: Context) {
        check(StoreEnvironment.getStoreType(context) == STORE_TYPE_ONESTORE) {
            "ONE Store installation could not be verified. Please reinstall the app from ONE Store."
        }
    }
}
