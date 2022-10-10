package `in`.avimarine.waypointracing.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class LocationPermissions {
    companion object {
        const val PERMISSIONS_REQUEST_LOCATION_UI = 4

        fun arePermissionsGranted(context: Context): Boolean {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
            return true
        }

        fun askForLocationPermission(context: Activity, permissionRequestCode: Int) {
            val requiredPermissions: MutableSet<String> = mutableSetOf(Manifest.permission.ACCESS_FINE_LOCATION)
            if (arePermissionsGranted(context)) {
                return
            }
//            if (ActivityCompat.shouldShowRequestPermissionRationale(
//                    context,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                )
//            ) {
                val alertBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
                alertBuilder.setCancelable(true)
                alertBuilder.setTitle("Fine location permission necessary")
                alertBuilder.setMessage("Waypoint Racing collects location data to enable boat tracking, and race course navigation even when the app is closed or not in use.")
                alertBuilder.setPositiveButton(android.R.string.yes
                ) { _, _ ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        context.requestPermissions(
                            requiredPermissions.toTypedArray(),
                            permissionRequestCode
                        )
                    }
                }
                val alert: AlertDialog = alertBuilder.create()
                alert.show()
//            } else {
//                // No explanation needed, we can request the permission.
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    context.requestPermissions(
//                        requiredPermissions.toTypedArray(),
//                        permissionRequestCode
//                    )
//                }
//            }
        }


    }
}