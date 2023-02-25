package `in`.avimarine.waypointracing.utils

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.ui.DialogUtils

class Utils {
    companion object {
        fun timeDiffInSeconds(first: Long, second: Long): Double {
            return ((second - first) / 1000).toDouble()
        }

        fun getInstalledVersion(c: Context): Long {
            try {
                val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    c.packageManager.getPackageInfo(c.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION") c.packageManager.getPackageInfo(c.packageName, 0)
                }
                return if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.getLongVersionCode() // avoid huge version numbers and you will be ok
                } else {
                    @Suppress("DEPRECATION") pInfo.versionCode.toLong()
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.d(TAG, "Error retrieving versioncode: ", e)
            }
            return -1
        }

        fun alertOnUnsupportedVersion(c: Context) {
            Log.d(TAG, "Version not supported, starting dialog")
            val d: Dialog = DialogUtils.createDialog(c,
                R.string.version_not_supported_dialog_title,
                R.string.version_not_supported_dialog_message,
                { dialog, which ->
                    val appPackageName: String = c.packageName
                    try {
                        startActivity(c,
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=$appPackageName")
                            ), null
                        )
                    } catch (e: ActivityNotFoundException) {
                        Log.e(
                            TAG,
                            "Error FirebaseDB OnConnect",
                            e
                        )
                        startActivity(c,
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                            ), null
                        )
                    }
                }, { dialog, which -> (c as Activity).finish() },
                { dialog -> (c as Activity).finish()}
            )
            d.show()
        }



    }
}