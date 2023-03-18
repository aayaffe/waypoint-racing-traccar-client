package `in`.avimarine.waypointracing.route

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import `in`.avimarine.androidutils.TAG
import org.json.JSONException
import java.io.FileInputStream
import java.io.FileNotFoundException


class RouteLoader {

    companion object {
        private const val DEFAULT_FILE_NAME = "myRoute"
        private const val MAX_FILE_SIZE = 10000
        /** Handles intent of new route from file or url.
            return true if intent handles and route loaded. false otherwise
        */
        fun handleIntent(context: Context, intent: Intent, loadRoute: (r: Route?) -> Unit) : Boolean{
            var ret = false
            when (intent.action) {
                Intent.ACTION_SEND -> { //From file/string
                    if ("text/json" == intent.type || "application/json" == intent.type) {
                        val s = handleJsonText(context, intent) // Handle json being sent
                        saveRoute(context, s)
                        loadRoute(Route.fromGeoJson(s))
                        ret = true
                    } else {
                        if (checkIntent(context, intent)) {
                            val s = handleJsonText(context, intent) // Handle json being sent
                            saveRoute(context, s)
                            loadRoute(Route.fromGeoJson(s))
                            ret = true
                        }
                        Log.d(TAG, "Unknown intent type: " + intent.type)
                    }
                }
                Intent.ACTION_VIEW -> { //From URL
                    Log.d(TAG, "Intent action: " + intent.action + " Intent data: " + intent.data + " Intent type: " + intent.type)
                    loadJsonfromUrl(context, intent.data, loadRoute) // Handle json being sent
                    ret = true
                }
                else -> {
                    Log.d(TAG, "Unknown intent action: " + intent.action + " Intent data: " + intent.data)
                }
            }
            return ret
        }

        fun loadRouteFromUrl(context: Context, url:String , loadRoute: (r: Route?) -> Unit){
            Log.d(TAG, "Loading route from url: $url")
            val uri = Uri.parse(url)
            loadJsonfromUrl(context, uri, loadRoute)
        }

        private fun saveRoute(context: Context, s: String) {
            context.openFileOutput(DEFAULT_FILE_NAME, Context.MODE_PRIVATE).use {
                it.write(s.toByteArray())
            }
        }

        fun loadRouteFromFile(context: Context): Route {
            try {
                val text = context.openFileInput(DEFAULT_FILE_NAME).bufferedReader().use { it.readText() }
                return Route.fromGeoJson(text)
            } catch (e: FileNotFoundException) {
                Log.w(TAG, "file not found", e)
            } catch (e: JSONException){
                Log.w(TAG, "Failed to parse JSON", e)
            } catch (e: Exception){
                Log.w(TAG, "Failed to load Route", e)
            }
            return Route.emptyRoute()
        }

        private fun loadJsonfromUrl(context: Context, url: Uri?, loadRoute: (r: Route?) -> Unit) {
            val queue = Volley.newRequestQueue(context)
            val stringRequest = StringRequest(Request.Method.GET, url?.toString(),
                    { response ->
                        Log.d(TAG, "URL response: $response")
                        loadRoute(Route.fromGeoJson(response))
                        saveRoute(context, response)
                    },
                    {
                        Log.e(TAG, "Error loading json from url", it.cause)
                        loadRoute(null)
//                        errorLoadingRoute("Unable to Load Route from given location")
                    }
            )
            stringRequest.retryPolicy = DefaultRetryPolicy(
                12000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            queue.add(stringRequest)
        }

        private fun checkIntent(context: Context, intent: Intent): Boolean {
            val uri: Uri
            intent.clipData?.let { clipData ->
                Log.d(TAG, clipData.toString())
                uri = clipData.getItemAt(0).uri
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    cursor.moveToFirst()
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    val size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
                    Log.d(TAG, "Json filesize: $size")
                    Log.d(TAG, "Json filename: $name")
                    return if ((size < MAX_FILE_SIZE) && ("json" in name)) {
                        true
                    } else {
                        Log.d(TAG, "JSON File too big, or not contains json. size: $size name: $name")
                        //TODO Check for proper file limit and add Toast
                        false
                    }
                }
            }
            return false
        }

        /**
         * Returns text value of intent, empty string if error.
         *
         * @param intent
         * @return text value of intent, empty string if error.
         */
        private fun handleJsonText(context: Context, intent: Intent): String {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                //Handle Simpleshare
                return it
            }
            //Handle shared file share
            val uri = getUri(intent)
            if (uri != null) {
                if (checkIntent(context, intent)) {
                    val inputPFD = try {
                        context.contentResolver.openFileDescriptor(uri, "r")
                    } catch (e: FileNotFoundException) {
                        Log.e("MainActivity", "File not found.", e)
                        return ""
                    }
                    // Get a regular file descriptor for the file
                    val fd = inputPFD?.fileDescriptor
                    val fileStream = FileInputStream(fd)
                    return fileStream.bufferedReader().use { it.readText() }
                } else {
                    return ""
                }
            }
            return ""
        }

        private fun getUri(intent: Intent): Uri? {
            intent.clipData?.let { clipData ->
                Log.d(TAG, clipData.toString())
                return clipData.getItemAt(0).uri
            }
            return null
        }

        fun loadRouteFromString(context: Context, json: String, loadRoute: (r: Route?) -> Unit) {
            loadRoute(Route.fromGeoJson(json))
            saveRoute(context, json)
        }
    }
}