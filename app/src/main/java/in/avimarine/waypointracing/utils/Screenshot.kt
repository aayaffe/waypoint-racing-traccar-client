package `in`.avimarine.waypointracing.utils

import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import eu.bolt.screenshotty.ScreenshotActionOrder
import eu.bolt.screenshotty.ScreenshotManagerBuilder


object Screenshot {
    private val REQUEST_SCREENSHOT_PERMISSION: Int = 1234

    fun takescreenshot(v: View): Bitmap {
        v.isDrawingCacheEnabled = true
        v.buildDrawingCache(true)
        val b = Bitmap.createBitmap(v.drawingCache)
        v.isDrawingCacheEnabled = false
        return b
    }

    fun takeScreenshot(activity: Activity){
        val screenshotManager = ScreenshotManagerBuilder(activity)
            .withCustomActionOrder(ScreenshotActionOrder.pixelCopyFirst()) //optional, ScreenshotActionOrder.pixelCopyFirst() by default
            .withPermissionRequestCode(REQUEST_SCREENSHOT_PERMISSION) //optional, 888 by default
            .build()
    }


}