package avimarine.traccar.client.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import avimarine.traccar.client.*
import avimarine.traccar.client.PositionProvider.PositionListener
import avimarine.traccar.client.route.*
import avimarine.traccar.client.ui.RouteElementAdapter
import avimarine.traccar.client.utils.Screenshot
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main2.*
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule


class Main2Activity : AppCompatActivity(), PositionListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private val magnetic = false
    private lateinit var positionProvider: PositionProvider
    private var nextWpt: RouteElement? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val PERMISSIONS_REQUEST_LOCATION = 2
    private lateinit var route: Route
    private val MAXFILESIZE = 10000
    private val DEFAULTFILENAME = "myRoute"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        sharedPreferences = getDefaultSharedPreferences(this)
        initRoute()
        positionProvider = PositionProviderFactory.create(this, this)
        setButton(sharedPreferences.getBoolean(MainFragment.KEY_STATUS, false))
        if (sharedPreferences.getBoolean(MainFragment.KEY_STATUS, false)) {
            startTrackingService(true, false)
        }
        val mCalendar: Calendar = GregorianCalendar()
        val mTimeZone = mCalendar.timeZone
        val mGMTOffset = mTimeZone.getOffset(mCalendar.timeInMillis)
        time.setLabel("UTC " + (if (mGMTOffset > 0) "+" else "") + TimeUnit.HOURS.convert(mGMTOffset.toLong(), TimeUnit.MILLISECONDS))
        routeElementSpinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>,
                                        view: View, position: Int, id: Long) {
                nextWpt = route.elements[position]
                if (nextWpt!!.firstTimeInProofArea != -1L) {
                    (parent.getChildAt(0) as TextView).setTextColor(resources.getColor(android.R.color.holo_green_light))
                } else {
                    (parent.getChildAt(0) as TextView).setTextColor(resources.getColor(android.R.color.black))
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
    }

    private fun initRoute() {
        when {
            intent?.action == Intent.ACTION_SEND -> {
                if ("text/json" == intent.type || "application/json" == intent.type) {
                    val s = handleJsonText(intent) // Handle json being sent
                    this.openFileOutput(DEFAULTFILENAME, Context.MODE_PRIVATE).use {
                        it.write(s.toByteArray())
                    }
                    loadRoute(s)
                } else {
                    if (checkIntent(intent)) {
                        val s = handleJsonText(intent) // Handle json being sent
                        this.openFileOutput(DEFAULTFILENAME, Context.MODE_PRIVATE).use {
                            it.write(s.toByteArray())
                        }
                        loadRoute(s)
                    }
                    Log.d(TAG, "Unknown intent type: " + intent?.type)
                }
            }
            intent?.action == Intent.ACTION_VIEW -> {
                Log.d(TAG, "Intent action: " + intent.action + " Intent data: " + intent.data + " Intent type: " + intent.type)
                loadJsonfromUrl(intent.data) // Handle json being sent
                return
            }
            else -> {
                Log.d(TAG, "Unknown intent action: " + intent.action + " Intent data: " + intent.data)
            }
        }
        val text: String
        if (!::route.isInitialized) {
            try {
                text = this.openFileInput(DEFAULTFILENAME).bufferedReader().use { it.readText() }
                loadRoute(text)
            } catch (e: FileNotFoundException) {
                Log.d(TAG, "file not found", e)
            }
        }
    }

    private fun loadRoute(geojson: String?) {
        geojson?: return
        route = Route.fromGeoJson(geojson)
        populateRouteElementSpinner(route)
    }

    private fun loadJsonfromUrl(url: Uri?) {
        val queue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(Request.Method.GET, url?.toString(),
                Response.Listener<String> { response ->
                    loadRoute(response)
                },
                Response.ErrorListener {

                    Log.e(TAG,"Error loading json from url", it.cause)
                    errorLoadingRoute("Unable to Load Route from given location")
                }
        )
        queue.add(stringRequest)
    }

    private fun errorLoadingRoute(s: String) {
        Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    }

    private fun checkIntent(intent: Intent): Boolean {
        val uri: Uri
        intent.clipData?.let { clipData ->
            Log.d(TAG, clipData.toString())
            uri = clipData.getItemAt(0).uri
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                val name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                val size = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))
                Log.d(TAG, "Json filesize: " + size)
                Log.d(TAG, "Json filename: " + name)
                return if ((size < MAXFILESIZE) && ("json" in name)) {
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
    private fun handleJsonText(intent: Intent): String {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            //Handle Simpleshare
            return it
        }
        //Handle shared file share
        val uri = getUri(intent)
        if (uri != null) {
            if (checkIntent(intent)) {
                val inputPFD = try {
                    contentResolver.openFileDescriptor(uri, "r")
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

    override fun onStart() {
        super.onStart()
        try {
            positionProvider.startUpdates()
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        try {
            positionProvider.stopUpdates()
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
        super.onStop()
    }

    private fun createTestRoute(): Route {
        val accoEast = Location("")
        accoEast.latitude = 32.921666667
        accoEast.longitude = 35.05
        val accoWest = Location("")
        accoWest.latitude = 32.921666667
        accoWest.longitude = 35.025
        val nahariyaWest = Location("")
        nahariyaWest.latitude = 33.01
        nahariyaWest.longitude = 35.058333333
        val nahariyaEast = Location("")
        nahariyaEast.latitude = 33.01
        nahariyaEast.longitude = 35.08333333
        val achziv = Location("")
        achziv.latitude = 33.05
        achziv.longitude = 35.086666666
        val finishEast = Location("")
        finishEast.latitude = 32.8371666666667
        finishEast.longitude = 35.02455
        val finishWest = Location("")
        finishWest.latitude = 32.8371666666667
        finishWest.longitude = 35.0200666666667

        val acco = Gate("Acco Gate", accoEast, accoWest, true, 315.0, 45.0, 0.25)
        val nahariya = Gate("Nahariya Gate", nahariyaEast, nahariyaWest, true, 315.0, 45.0, 0.25)
        val achzivWpt = Waypoint("Achziv Turning Point", achziv, true, 315.0, 45.0)
        val finish = Finish("Finish Line", finishWest, finishEast, 0.15)
        return Route("TestEvent", Calendar.getInstance().time, arrayListOf(acco, nahariya, achzivWpt, finish), Calendar.getInstance().time)

    }

    fun populateRouteElementSpinner(route: Route) {
        val adapter = RouteElementAdapter(this,
                R.layout.waypoint_spinner_item, 0, route.elements)
        routeElementSpinner.adapter = adapter
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main2, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.getItemId()
        if (id == R.id.settings_menu_action) {
            val intent = Intent(this, MainActivity::class.java)
            this.startActivity(intent)
            return true
        }
        if (id == R.id.history_menu_action) {
            val intent = Intent(this, StatusActivity::class.java)
            this.startActivity(intent)
            return true
        } else if (id == R.id.send_screenshot_menu_action) {
            val b = Screenshot.takescreenshotOfRootView(this.findViewById<View>(android.R.id.content).rootView)

        }
        return super.onOptionsItemSelected(item)
    }

    fun startButtonClick(view: View) {
        val sharedPref: SharedPreferences = getDefaultSharedPreferences(this)
        val checked = sharedPref.getBoolean("status", true)
        with(sharedPref.edit()) {
            putBoolean("status", checked.not())
            commit()
        }

    }

    override fun onPositionError(error: Throwable?) {
        Log.e(TAG, "Position Error: ", error)
    }

    override fun onPositionUpdate(position: Position?) {
//        StatusActivity.addMessage(context.getString(R.string.status_location_update))
        if (position != null) {
            updateUI(position)
        } else {
            Log.w(TAG, "Error: position is null")
        }

    }

    private fun updateUI(position: Position) {
        if (nextWpt != null) {
            val distWptPort = getDistance(position, nextWpt!!.portWpt)
            val distWptStbd = getDistance(position, nextWpt!!.stbdWpt)
            val dirWptPort = getDirection(position, nextWpt!!.portWpt)
            val dirWptStbd = getDirection(position, nextWpt!!.stbdWpt)
            val portData = getDirString(dirWptPort, magnetic, false, position, position.time.time) + "/" + getDistString(distWptPort)
            val stbcData = getDirString(dirWptStbd, magnetic, false, position, position.time.time) + "/" + getDistString(distWptStbd)
            portGate.setData(portData)
            stbdGate.setData(stbcData)
            updateIsInArea(position)
        }
        cog.setData(getDirString(position.course, magnetic, false, position, position.time.time))
        sog.setData(getSpeedString(position.speed)) //TODO Fix units in the conversion (probably knots)
        location.setData(getLatString(position.latitude) + "\n" + getLonString(position.longitude))
        time.setData(timeStamptoDateString(position.time.time))


    }

    private fun updateIsInArea(location: Position) {
        val l = Location("")
        l.latitude = location.latitude
        l.longitude = location.longitude
        if (nextWpt != null) {
            if (nextWpt!!.isInProofArea(l)) {
                if (nextWpt!!.passedGate(location)) {
                    StatusActivity.addMessage("Passed " + nextWpt!!.name)
                    (routeElementSpinner.selectedView as TextView).setTextColor(resources.getColor(android.R.color.holo_green_light))
                    Timer("AdvanceWaypoint", false).schedule(3000) {
                        runOnUiThread {
                            if (routeElementSpinner.selectedItemPosition < routeElementSpinner.adapter.count - 1) {
                                routeElementSpinner.setSelection(routeElementSpinner.selectedItemPosition + 1)
                            }
                        }
                    }
                }
            } else {
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        Log.d(TAG, "Changed Preference: " + key)
        if (key == MainFragment.KEY_STATUS) {
            setButton(sharedPreferences.getBoolean(MainFragment.KEY_STATUS, false))
            if (sharedPreferences.getBoolean(MainFragment.KEY_STATUS, false)) {
                startTrackingService(true, false)
            } else {
                stopTrackingService()
            }
        }
    }

    private fun setButton(isRunning: Boolean) {
        if (isRunning) {
            start_btn.background = ContextCompat.getDrawable(this, R.drawable.btn_rnd_red)
            start_btn.text = "Stop"
        } else {
            start_btn.background = ContextCompat.getDrawable(this, R.drawable.btn_rnd_grn)
            start_btn.text = "Start"
        }
    }

    private fun startTrackingService(checkPermission: Boolean, permission: Boolean) {
        var permission = permission
        if (checkPermission) {
            val requiredPermissions: MutableSet<String> = HashSet()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            permission = requiredPermissions.isEmpty()
            if (!permission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(requiredPermissions.toTypedArray(), PERMISSIONS_REQUEST_LOCATION)
                }
                return
            }
        }
        if (permission) {
            ContextCompat.startForegroundService(this, Intent(this, TrackingService::class.java))
        } else {
            sharedPreferences.edit().putBoolean(MainFragment.KEY_STATUS, false).apply()
        }
    }

    private fun stopTrackingService() {
        this.stopService(Intent(this, TrackingService::class.java))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            var granted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                    break
                }
            }
            startTrackingService(false, granted)
        }
    }
}