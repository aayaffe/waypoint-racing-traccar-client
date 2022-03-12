package `in`.avimarine.waypointracing.utils

import `in`.avimarine.waypointracing.BuildConfig
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import eu.bolt.screenshotty.ScreenshotBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ScreenShot {
    companion object {
        fun processScreenshot(it: eu.bolt.screenshotty.Screenshot, context: Context) {
            val bitmap = when (it) {
                is ScreenshotBitmap -> it.bitmap
            }
            sendSnapshot(bitmap, context)
        }

        fun sendSnapshot(bitmap: Bitmap, context: Context) {
            try {
                val cachePath = File(context.getCacheDir(), "images")
                cachePath.mkdirs() // don't forget to make the directory
                val stream =
                    FileOutputStream(cachePath.toString() + "/image.png") // overwrites this image every time
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val imagePath = File(context.getCacheDir(), "images")
            val newFile = File(imagePath, "image.png")
            val contentUri: Uri =
                FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    newFile
                )
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, context.contentResolver.getType(contentUri))
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            context.startActivity(Intent.createChooser(shareIntent, "Choose an app"))
        }
    }
}