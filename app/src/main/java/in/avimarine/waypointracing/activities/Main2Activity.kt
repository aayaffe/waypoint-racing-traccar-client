package `in`.avimarine.waypointracing.activities

import `in`.avimarine.waypointracing.*
import `in`.avimarine.waypointracing.route.*
import `in`.avimarine.waypointracing.ui.RouteElementAdapter
import `in`.avimarine.waypointracing.ui.dialogs.FirstTimeDialog
import `in`.avimarine.waypointracing.utils.LocationPermissions
import `in`.avimarine.waypointracing.utils.Screenshot
import `in`.avimarine.waypointracing.utils.Utils
import android.Manifest
import android.R.attr
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.allViews
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import eu.bolt.screenshotty.ScreenshotActionOrder
import eu.bolt.screenshotty.ScreenshotBitmap
import eu.bolt.screenshotty.ScreenshotManagerBuilder
import kotlinx.android.synthetic.main.activity_main2.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule
import android.content.pm.PackageInfo

import android.provider.MediaStore

import android.graphics.Bitmap
import android.net.Uri
import eu.bolt.screenshotty.ScreenshotManager
import java.io.ByteArrayOutputStream
import java.lang.Exception
import android.R.attr.bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class Main2Activity : AppCompatActivity(), PositionProvider.PositionListener,
    TrackingController.RouteHandler,
    SharedPreferences.OnSharedPreferenceChangeListener, FirstTimeDialog.FirstTimeDialogListener {

    private val magnetic = false
    private lateinit var positionProvider: PositionProvider
    private lateinit var sharedPreferences: SharedPreferences
    private var nextWpt: Int = 0
    private val PERMISSIONS_REQUEST_LOCATION_TRACKING_SERVICE = 2
    private val PERMISSIONS_REQUEST_LOCATION_UI = 4
    private var route = Route.emptyRoute()
    private var noGPSTimer: Timer = Timer("GPSTIMER", true)
    private var isFirstSpinnerLoad = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenshotManager = ScreenshotManagerBuilder(this)
            .withCustomActionOrder(ScreenshotActionOrder.pixelCopyFirst()) //optional, ScreenshotActionOrder.pixelCopyFirst() by default
            .withPermissionRequestCode(REQUEST_SCREENSHOT_PERMISSION) //optional, 888 by default
            .build()
        sharedPreferences = getDefaultSharedPreferences(this.applicationContext)
        setContentView(R.layout.activity_main2)
        if (intent.action == Intent.ACTION_MAIN) {
            val r = RouteLoader.loadRouteFromFile(this)
            loadRoute(r)
        } else {
            RouteLoader.handleIntent(this, intent, this::loadRoute)
        }
        val PREFS_NAME = "MyPrefsFile"
        val settings = getSharedPreferences(PREFS_NAME, 0)

        if (settings.getBoolean("my_first_time", true)) {
            //the app is being launched for first time
            Log.d(TAG, "First time run")
            // first time task
            val dialog = FirstTimeDialog()
            dialog.show(supportFragmentManager, "FirstTimeDialogFragment")
            // record the fact that the app has been started at least once
            settings.edit().putBoolean("my_first_time", false).apply()
        }

        setButton(sharedPreferences.getBoolean(MainFragment.KEY_STATUS, false))
        if (sharedPreferences.getBoolean(MainFragment.KEY_STATUS, false)) {
            startTrackingService(true, false)
        }
        val mCalendar: Calendar = GregorianCalendar()
        val mTimeZone = mCalendar.timeZone
        val mGMTOffset = mTimeZone.getOffset(mCalendar.timeInMillis)
        time.setLabel(
            "UTC " + (if (mGMTOffset > 0) "+" else "") + TimeUnit.HOURS.convert(
                mGMTOffset.toLong(),
                TimeUnit.MILLISECONDS
            )
        )
        if (isValidWpt(route,nextWpt)){
            getNextWpt()
        }

        routeElementSpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View, position: Int, id: Long
            ) {
                if (isFirstSpinnerLoad) {
                    isFirstSpinnerLoad = false
                    if (isValidWpt(route,nextWpt)){
                        routeElementSpinner.setSelection(nextWpt)
                    }
                    return
                }
                setNextWpt(position)
                val wpt = route.elements[position]
                if (wpt!!.firstTimeInProofArea != -1L) {
                    (parent.getChildAt(0) as TextView).setTextColor(resources.getColor(android.R.color.holo_green_light))
                } else {
                    (parent.getChildAt(0) as TextView).setTextColor(resources.getColor(android.R.color.black))
                }
                sendRouteIntent(route)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
    }

    override fun onNewIntent(i: Intent){
        super.onNewIntent(i)
        RouteLoader.handleIntent(this, i, this::loadRoute)
        setNextWpt(0)
    }



    private fun isValidWpt(route: Route, nextWpt: Int): Boolean {
        return nextWpt<route.elements.size && nextWpt > -1
    }

    private fun createPositionProvider() {
        positionProvider = PositionProviderFactory.create(this, this)
    }

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface
    override fun onDialogPositiveClick(dialog: DialogFragment, boatName: String) {
        with(sharedPreferences.edit()) {
            putString("boat_name", boatName)
            commit()
        }
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // User touched the dialog's negative button
    }

    private fun loadRoute(r: Route?) {
        if (r == null) {
            errorLoadingRoute("Error Loading Route")
            val r = RouteLoader.loadRouteFromFile(this)
            r ?: return
            loadRoute(r)
            return
        }
        route = r
        populateRouteElementSpinner(r)
        setTitle("Waypoint Racing", r.eventName)
        sendRouteIntent(r)

    }

    private fun sendRouteIntent(r: Route) {
        Intent().also { intent ->
            intent.action = TrackingService.ROUTE_ACTION
            intent.putExtra("route", r)
            intent.putExtra("nextwpt", nextWpt)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    private fun setTitle(title: String, subTitle: String) {
        val ab = supportActionBar
        ab?.setTitle(title)
        ab?.setSubtitle(subTitle)
    }


    private fun errorLoadingRoute(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    }


    override fun onStart() {
        super.onStart()
        try {
            if (LocationPermissions.arePermissionsGranted(this)) {
                if (!this::positionProvider.isInitialized) {
                    createPositionProvider()
                }
                positionProvider.startUpdates()
            } else {
                askForLocationPermission(PERMISSIONS_REQUEST_LOCATION_UI)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        if (route.isEmpty()){
            val r = RouteLoader.loadRouteFromFile(this)
            loadRoute(r)
        }
        getNextWpt()
        setGPSInterval(1)
    }

    private fun getNextWpt() {
        nextWpt = sharedPreferences.getInt(MainFragment.KEY_NEXT_WPT, 0)
        routeElementSpinner.setSelection(nextWpt)
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        setGPSInterval(9)
    }

    override fun onStop() {
        try {
            positionProvider.stopUpdates()
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
        super.onStop()
    }

    private fun populateRouteElementSpinner(route: Route) {
        val adapter = RouteElementAdapter(
            this,
            R.layout.waypoint_spinner_item, 0, route.elements
        )
        routeElementSpinner.adapter = adapter
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main2, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings_menu_action -> {
                val intent = Intent(this, MainActivity::class.java)
                this.startActivity(intent)
                return true
            }
            R.id.history_menu_action -> {
                val intent = Intent(this, StatusActivity::class.java)
                this.startActivity(intent)
                return true
            }
            R.id.send_screenshot_menu_action -> {
                takeScreenshot()
            }
            R.id.reset_route_menu_action -> {
                resetRoute()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun resetRoute() {
        setNextWpt(0)
        populateRouteElementSpinner(route)
    }

    fun startButtonClick(view: View) {
        val sharedPref: SharedPreferences = getDefaultSharedPreferences(this)
        val checked = sharedPref.getBoolean(MainFragment.KEY_STATUS, true)
        with(sharedPref.edit()) {
            putBoolean(MainFragment.KEY_STATUS, checked.not())
            commit()
        }
    }

    fun setNextWpt(n: Int){
        nextWpt = n
        val sharedPref: SharedPreferences = getDefaultSharedPreferences(this)
        with(sharedPref.edit()) {
            putInt(MainFragment.KEY_NEXT_WPT, n)
            commit()
        }
    }

    fun setGPSInterval(i: Int){
        val sharedPref: SharedPreferences = getDefaultSharedPreferences(this)
        with(sharedPref.edit()) {
            putString(MainFragment.KEY_INTERVAL, i.toString())
            commit()
        }
    }

    override fun onPositionError(error: Throwable) {
        Log.e(TAG, "Position Error: ", error)
    }

    override fun onPositionUpdate(position: Position) {
//        StatusActivity.addMessage(context.getString(R.string.status_location_update))
        if (!route.isEmpty()) {
            updateUI(position)
        } else {
            Log.w(TAG, "Error: route not initialized")
        }

    }

    private fun updateUI(position: Position) {
        val wpt = route.elements.elementAtOrNull(nextWpt)
        if (wpt != null) {
            val distWptPort = getDistance(position, wpt!!.portWpt)
            val distWptStbd = getDistance(position, wpt!!.stbdWpt)
            val dirWptPort = getDirection(position, wpt!!.portWpt)
            val dirWptStbd = getDirection(position, wpt!!.stbdWpt)
            val portData = getDirString(
                dirWptPort,
                magnetic,
                false,
                position,
                position.time.time
            ) + "/" + getDistString(distWptPort)
            val stbcData = getDirString(
                dirWptStbd,
                magnetic,
                false,
                position,
                position.time.time
            ) + "/" + getDistString(distWptStbd)
            portGate.setData(portData)
            stbdGate.setData(stbcData)
            shortestDistanceToGate.setData(getDistString(pointToLineDist(position.toLocation(),wpt.portWpt,wpt.stbdWpt)))
        } else {
            portGate.setData("-----")
            stbdGate.setData("-----")
        }
        setUiForGPS(true)
        cog.setData(getDirString(position.course, magnetic, false, position, position.time.time))
        sog.setData(getSpeedString(position.speed)) //TODO Fix units in the conversion (probably knots)
        location.setData(getLatString(position.latitude) + "\n" + getLonString(position.longitude))
        time.setData(timeStamptoDateString(position.time.time))
        noGPSTimer.cancel()
        noGPSTimer.purge()
        noGPSTimer = Timer("GPSTIMER", true)
        val interval = (sharedPreferences.getString(MainFragment.KEY_INTERVAL, "600")?.toLong()
            ?: 600) * 4000 //After four times interval
        noGPSTimer.schedule(interval) {
            setUiForGPS(false)
        }
        if (position.mock){
            mockPosition.visibility = View.VISIBLE
        } else {
            mockPosition.visibility = View.INVISIBLE
        }
        if (Utils.timeDiffInSeconds(sharedPreferences.getLong(MainFragment.KEY_LAST_SEND, Long.MAX_VALUE),Date().time) < (sharedPreferences.getString(MainFragment.KEY_INTERVAL,
                8.toString()
            )
                ?.toInt() ?: 8)*1.5) {
            lastSend.setImageResource(R.drawable.btn_rnd_grn)
        } else {
            lastSend.setImageResource(R.drawable.btn_rnd_red)
        }
    }

    private fun setUiForGPS(isAvailable: Boolean) {
        if (isAvailable) {
            portGate.setTextColor(Color.BLACK)
            stbdGate.setTextColor(Color.BLACK)
            cog.setTextColor(Color.BLACK)
            sog.setTextColor(Color.BLACK)
            location.setTextColor(Color.BLACK)
            time.setTextColor(Color.BLACK)
            shortestDistanceToGate.setTextColor(Color.BLACK)
        } else {
            portGate.setTextColor(Color.RED)
            stbdGate.setTextColor(Color.RED)
            cog.setTextColor(Color.RED)
            sog.setTextColor(Color.RED)
            location.setTextColor(Color.RED)
            time.setTextColor(Color.RED)
            shortestDistanceToGate.setTextColor(Color.RED)
        }
    }

//    private fun updateIsInArea(location: Position) {
//        val l = Location("")
//        l.latitude = location.latitude
//        l.longitude = location.longitude
//        val wpt = route.elements[nextWpt]
//        if (wpt != null) {
//            if (wpt!!.isInProofArea(l)) {
//                if (wpt!!.passedGate(location)) {
//                    StatusActivity.addMessage("Passed " + wpt!!.name)
//                    (routeElementSpinner.selectedView as TextView).setTextColor(
//                        resources.getColor(
//                            android.R.color.holo_green_light
//                        )
//                    )
//                    Timer("AdvanceWaypoint", false).schedule(3000) {
//                        runOnUiThread {
//                            if (routeElementSpinner.selectedItemPosition < routeElementSpinner.adapter.count - 1) {
//                                routeElementSpinner.setSelection(routeElementSpinner.selectedItemPosition + 1)
//                            }
//                        }
//                    }
//                }
//            } else {
//            }
//        }
//    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        Log.d(TAG, "Changed Preference: " + key)
        if (key == MainFragment.KEY_STATUS) {
            setButton(sharedPreferences.getBoolean(MainFragment.KEY_STATUS, false))
            if (sharedPreferences.getBoolean(MainFragment.KEY_STATUS, false)) {
                startTrackingService(true, false)
            } else {
                stopTrackingService()
            }
        } else if (key == MainFragment.KEY_NEXT_WPT) {
            getNextWpt()
        } else if (key == MainFragment.KEY_LAST_SEND) {
            if (Utils.timeDiffInSeconds(sharedPreferences.getLong(MainFragment.KEY_LAST_SEND, Long.MAX_VALUE),Date().time) < (sharedPreferences.getString(MainFragment.KEY_INTERVAL,
                    8.toString()
                )
                    ?.toInt() ?: 8)*1.5) {
                lastSend.setImageResource(R.drawable.btn_rnd_grn)
            } else {
                lastSend.setImageResource(R.drawable.btn_rnd_red)
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

    /**
     * Checks for location permissions.
     * returns true if permission granted or requesting permission from user.
     */
    private fun checkLocationPermissions(): Boolean {
        val requiredPermissions: MutableSet<String> = HashSet()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requiredPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (requiredPermissions.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    requiredPermissions.toTypedArray(),
                    PERMISSIONS_REQUEST_LOCATION_TRACKING_SERVICE
                )
            }
            return false
        }
        return true
    }



    private fun askForLocationPermission(permissionRequestCode: Int) {
        val requiredPermissions: MutableSet<String> = HashSet()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requiredPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (requiredPermissions.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(requiredPermissions.toTypedArray(), permissionRequestCode)
            }
        }
    }

    private fun startTrackingService(checkPermission: Boolean, permission: Boolean) {
        var permission = permission
        if (checkPermission) {
            permission = checkLocationPermissions();
        }
        if (permission) {
            val i = Intent(this, TrackingService::class.java)
            if (!route.isEmpty()) {
                i.putExtra("route", route)
            }
            i.putExtra("nextwpt", nextWpt)
            ContextCompat.startForegroundService(this, i)
        } else {
            sharedPreferences.edit().putBoolean(MainFragment.KEY_STATUS, false).apply()
        }
    }

    private fun stopTrackingService() {
        this.stopService(Intent(this, TrackingService::class.java))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION_TRACKING_SERVICE || requestCode == PERMISSIONS_REQUEST_LOCATION_UI) {
            var granted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                    break
                }
            }
            Log.d(TAG, "Permissions granted: $granted")
            if (requestCode == PERMISSIONS_REQUEST_LOCATION_TRACKING_SERVICE) {
                startTrackingService(false, granted)
            } else {
                if (granted) {
                    Log.d(TAG, "Started Updates after permission granted")
                    createPositionProvider()
                    positionProvider.startUpdates()
                }
            }
        }
    }

    override fun onRouteUpdate(index: Int) {
        (routeElementSpinner.selectedView as TextView).setTextColor(
            resources.getColor(
                android.R.color.holo_green_light
            )
        )
        Timer("AdvanceWaypoint", false).schedule(3000) {
            runOnUiThread {
                if (routeElementSpinner.selectedItemPosition < routeElementSpinner.adapter.count - 1) {
                    routeElementSpinner.setSelection(index)
                }
            }
        }
        setNextWpt(index)
    }
    private val REQUEST_SCREENSHOT_PERMISSION: Int = 1234
    private lateinit var screenshotManager : ScreenshotManager


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        screenshotManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun takeScreenshot(){
        val screenshotResult = screenshotManager.makeScreenshot()
        val subscription = screenshotResult.observe(
            onSuccess = { processScreenshot(it) },
            onError = { /*onMakeScreenshotFailed(it)*/ }
        )
    }

    private fun processScreenshot(it: eu.bolt.screenshotty.Screenshot) {
        val bitmap = when (it) {
            is ScreenshotBitmap -> it.bitmap
        }
        sendSnapshot( bitmap)
    }

    private fun sendSnapshot(bitmap: Bitmap) {
        try {
            val cachePath = File(this.getCacheDir(), "images")
            cachePath.mkdirs() // don't forget to make the directory
            val stream =
                FileOutputStream(cachePath.toString() + "/image.png") // overwrites this image every time
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val imagePath: File = File(this.getCacheDir(), "images")
        val newFile = File(imagePath, "image.png")
        val contentUri: Uri =
            FileProvider.getUriForFile(this, "com.example.myapp.fileprovider", newFile)

        if (contentUri != null) {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, contentResolver.getType(contentUri))
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            startActivity(Intent.createChooser(shareIntent, "Choose an app"))
        }
    }

}