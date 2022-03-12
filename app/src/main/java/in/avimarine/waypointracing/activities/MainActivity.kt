package `in`.avimarine.waypointracing.activities

import `in`.avimarine.waypointracing.*
import `in`.avimarine.waypointracing.databinding.ActivityMainBinding
import `in`.avimarine.waypointracing.route.*
import `in`.avimarine.waypointracing.ui.RouteElementAdapter
import `in`.avimarine.waypointracing.ui.UiData.Companion.getCOGData
import `in`.avimarine.waypointracing.ui.UiData.Companion.getLocationData
import `in`.avimarine.waypointracing.ui.UiData.Companion.getPortData
import `in`.avimarine.waypointracing.ui.UiData.Companion.getShortestDistanceToGateData
import `in`.avimarine.waypointracing.ui.UiData.Companion.getStbdData
import `in`.avimarine.waypointracing.ui.UiData.Companion.getVMGGateData
import `in`.avimarine.waypointracing.ui.dialogs.FirstTimeDialog
import `in`.avimarine.waypointracing.utils.*
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import eu.bolt.screenshotty.ScreenshotActionOrder
import eu.bolt.screenshotty.ScreenshotBitmap
import eu.bolt.screenshotty.ScreenshotManager
import eu.bolt.screenshotty.ScreenshotManagerBuilder
//import kotlinx.android.synthetic.main.activity_main2.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), PositionProvider.PositionListener,
    SharedPreferences.OnSharedPreferenceChangeListener, FirstTimeDialog.FirstTimeDialogListener {

    private lateinit var positionProvider: PositionProvider
    private lateinit var sharedPreferences: SharedPreferences
    private var nextWpt: Int = 0
    private val PERMISSIONS_REQUEST_LOCATION_UI = 4
    private var route = Route.emptyRoute()
//    private var noGPSTimer: Timer = Timer("GPSTIMER", true)
    val delayedHandler = Handler(Looper.getMainLooper())
    private var isFirstSpinnerLoad = true
//    private lateinit var alarmManager: AlarmManager
//    private lateinit var alarmIntent: PendingIntent
    private lateinit var binding: ActivityMainBinding
    private val expertMode = BuildConfig.DEBUG



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        screenshotManager = ScreenshotManagerBuilder(this)
            .withCustomActionOrder(ScreenshotActionOrder.pixelCopyFirst()) //optional, ScreenshotActionOrder.pixelCopyFirst() by default
            .withPermissionRequestCode(REQUEST_SCREENSHOT_PERMISSION) //optional, 888 by default
            .build()
        sharedPreferences = getDefaultSharedPreferences(this.applicationContext)
//        setContentView(R.layout.activity_main2)

//        alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        val originalIntent = Intent(this, AutostartReceiver::class.java)
//        originalIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
//        alarmIntent = PendingIntent.getBroadcast(this, 0, originalIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (intent.action == Intent.ACTION_MAIN) {
            val r = RouteLoader.loadRouteFromFile(this)
            loadRoute(r)
        } else {
            RouteLoader.handleIntent(this, intent, this::loadRoute)
        }
        setEmptyRouteUI(route.isEmpty())
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

        setButton(sharedPreferences.getBoolean(SettingsFragment.KEY_STATUS, false))
        if (sharedPreferences.getBoolean(SettingsFragment.KEY_STATUS, false)) {
            startTrackingService(true, false)
        }
        val mCalendar: Calendar = GregorianCalendar()
        val mTimeZone = mCalendar.timeZone
        val mGMTOffset = mTimeZone.getOffset(mCalendar.timeInMillis)
        binding.time.setLabel(
            "UTC " + (if (mGMTOffset > 0) "+" else "") + TimeUnit.HOURS.convert(
                mGMTOffset.toLong(),
                TimeUnit.MILLISECONDS
            )
        )
        if (route.isValidWpt(nextWpt)){
            getNextWpt()
        }
        binding.routeElementSpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?, position: Int, id: Long
            ) {
                if (isFirstSpinnerLoad) {
                    isFirstSpinnerLoad = false
                    if (route.isValidWpt(nextWpt)){
                        parent?.setSelection(nextWpt)
                    }
                    return
                }
                setNextWpt(position)
                sendRouteIntent(route)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
        with(sharedPreferences.edit()) {
            putBoolean(SettingsFragment.KEY_EXPERT_MODE, false)
            commit()
        }
    }

    /**
     * Used to ignore changing text size.
     */
    override fun attachBaseContext(newBase: Context?) {
        val newOverride = Configuration(newBase?.resources?.configuration)
        newOverride.fontScale = 1.0f
        applyOverrideConfiguration(newOverride)

        super.attachBaseContext(newBase)
    }

    override fun onNewIntent(i: Intent){
        super.onNewIntent(i)
        if (RouteLoader.handleIntent(this, i, this::loadRoute)){
            setNextWpt(0)
        }
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
            loadRoute(RouteLoader.loadRouteFromFile(this))
            return
        }
        route = r
        populateRouteElementSpinner(r)
        setEmptyRouteUI(route.isEmpty())
        if (route.eventType == EventType.WPTRACING) {
            setTitle(getString(R.string.title_waypoint_racing), r.eventName)
        } else {
            setTitle(getString(R.string.title_treasure_hunting), r.eventName)
        }
        if (!route.isEmpty()) {
            Toast.makeText(applicationContext,"Loaded route\n ${route.eventName}",Toast.LENGTH_LONG).show()
        }
        sendRouteIntent(r)
        val s = sharedPreferences.getString(SettingsFragment.KEY_GATE_PASSES, "")
        var gp = GatePassings("")
        if (s != null) {
            gp = try {
                GatePassings.fromJson(s)
            } catch (e: Exception) {
                Log.d(TAG, "Failed to load gate passings", e)
                GatePassings("")
            }
        }
        if (gp.eventId != route.id){
            with(sharedPreferences.edit()) {
                putString(SettingsFragment.KEY_GATE_PASSES, GatePassings(route.id).toJson())
                commit()
            }
        }

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
        setMainActivityVisibilityStatus(true)
        updateLastPass()
    }

    private fun getNextWpt() {
        nextWpt = sharedPreferences.getInt(SettingsFragment.KEY_NEXT_WPT, 0)
        if (nextWpt >= route.elements.size) {
            setNextWpt(0)
        }
        binding.routeElementSpinner.setSelection(nextWpt)
        route.elements.elementAtOrNull(nextWpt)?.let { setNextWaypointUI(it) }
    }

    private fun setNextWaypointUI(wpt: RouteElement) {
        if (wpt.type == RouteElementType.WAYPOINT) {
            if (wpt.proofArea.type == ProofAreaType.QUADRANT) {
                binding.shortestDistanceToGate.visibility = View.VISIBLE
                binding.stbdGate.visibility = View.VISIBLE
                binding.stbdGate.setLabel(getString(R.string.pass_wpt_from))
                binding.stbdGate.setUnits("")
                binding.stbdGate.setData(
                    getPointOfCompass(
                        wpt.proofArea.bearings[0],
                        wpt.proofArea.bearings[1]
                    )
                )
                binding.portGate.setLabel(getString(R.string.waypoint))
                binding.shortestDistanceToGate.setLabel(getString(R.string.dist_to_wpt))
            } else { //CIRCLE proof area
                binding.shortestDistanceToGate.visibility = View.INVISIBLE
                binding.stbdGate.visibility = View.GONE
                binding.portGate.setLabel(getString(R.string.waypoint))
            }
        } else { //Gate
            binding.shortestDistanceToGate.visibility = View.VISIBLE
            binding.stbdGate.visibility = View.VISIBLE
            binding.stbdGate.setData("-----")
            binding.stbdGate.setUnits(getString(R.string.nm))
            binding.stbdGate.setLabel(getString(R.string.stbd_gate))
            binding.portGate.setLabel(getString(R.string.port_gate))
            binding.shortestDistanceToGate.setLabel(getString(R.string.distance_to_gate))
        }
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        setGPSInterval(9)
        setMainActivityVisibilityStatus(false)

    }

    override fun onStop() {
        try {
            if (this::positionProvider.isInitialized){
                positionProvider.stopUpdates()
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
        super.onStop()
    }

    private fun populateRouteElementSpinner(route: Route) {
        val adapter = RouteElementAdapter(
            this,
            R.layout.waypoint_spinner_item, 0, route.elements
        )
        binding.routeElementSpinner.adapter = adapter
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        menu.findItem(R.id.expert_mode_menu_action).isVisible = expertMode
        return true
    }
    private val getRouteStartForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = result.data?.extras
            if (extras != null) {
                extras.getString("RouteJson")?.let {
                    Log.d(TAG, it)
                    sharedPreferences.edit().putBoolean(SettingsFragment.KEY_STATUS,  false).apply()
                    resetRoute()
                    RouteLoader.loadRouteFromString(this, it, this::loadRoute)
                }
            }
        }

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings_menu_action -> {
                val intent = Intent(this, SettingsActivity::class.java)
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
                return true
            }
            R.id.reset_route_menu_action -> {
                resetRoute()
                return true
            }
            R.id.route_activity_menu_action -> {
                val intent = Intent(this, RouteActivity::class.java)
                intent.putExtra("route", route)
                this.startActivity(intent)
                return true
            }
            R.id.download_latest_menu_action -> {
                getRouteStartForResult.launch(Intent(this, LoadRouteActivity::class.java))
                return true
            }
            R.id.expert_mode_menu_action -> {
                val intent = Intent(this, ExpertModeActivity::class.java)
                this.startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun resetRoute() {
        setNextWpt(0)
        GatePassings.reset(this, route)
        populateRouteElementSpinner(route)
        binding.lastPassTextView.text = ""
    }

    fun startButtonClick(view: View) {
        val checked = sharedPreferences.getBoolean(SettingsFragment.KEY_STATUS, true)
        sharedPreferences.edit().putBoolean(SettingsFragment.KEY_STATUS,  checked.not()).apply()
    }

    fun setNextWpt(n: Int){
        nextWpt = n
        val sharedPref: SharedPreferences = getDefaultSharedPreferences(this)
        with(sharedPref.edit()) {
            putInt(SettingsFragment.KEY_NEXT_WPT, n)
            commit()
        }
    }

    fun setGPSInterval(i: Int){
        val sharedPref: SharedPreferences = getDefaultSharedPreferences(this)
        with(sharedPref.edit()) {
            putString(SettingsFragment.KEY_INTERVAL, i.toString())
            commit()
        }
    }

    fun setMainActivityVisibilityStatus(b: Boolean){
        val sharedPref: SharedPreferences = getDefaultSharedPreferences(this)
        with(sharedPref.edit()) {
            putBoolean(SettingsFragment.KEY_IS_UI_VISIBLE, b)
            commit()
        }
    }

    override fun onPositionError(error: Throwable) {
        Log.e(TAG, "Position Error: ", error)
    }

    override fun onPositionUpdate(position: Position) {
        updateUI(position)
    }

    private fun updateUI(position: Position) {
        val wpt = route.elements.elementAtOrNull(nextWpt)
        binding.portGate.setData(getPortData(position,wpt,sharedPreferences))
        binding.stbdGate.setData(getStbdData(position,wpt,sharedPreferences))
        binding.shortestDistanceToGate.setData(getShortestDistanceToGateData(position,wpt))
        setUiForGPS(true)
        binding.cog.setData(getCOGData(position,sharedPreferences))
        binding.sog.setData(getSpeedString(position.speed))
        binding.location.setData(getLocationData(position))
        binding.time.setData(timeStamptoDateString(position.time.time))
        binding.vmg.setData(getVMGGateData(position,wpt))
        if (wpt != null) {
            binding.location.setTextColor(if (wpt.isInProofArea(position.toLocation())) Color.GREEN else Color.BLACK )
        } else {
            binding.location.setTextColor(Color.BLACK)
        }
        val interval = (sharedPreferences.getString(SettingsFragment.KEY_INTERVAL, "600")?.toLong()
            ?: 600) * 4000 //After four times interval
        delayedHandler.removeCallbacksAndMessages(null)
        delayedHandler.postDelayed({
            setUiForGPS(false)
        }, interval)
        binding.mockPosition.visibility = if (position.mock) View.VISIBLE else View.INVISIBLE
        if (sharedPreferences.getBoolean(SettingsFragment.KEY_TRACKING, false)) {
            binding.lastSend.visibility = View.VISIBLE
            val lastLocationSentTime = sharedPreferences.getLong(SettingsFragment.KEY_LAST_SEND, -1)
            if (lastLocationSentTime > 0 && Utils.timeDiffInSeconds(lastLocationSentTime,Date().time) < (sharedPreferences.getString(
                    SettingsFragment.KEY_INTERVAL,
                    8.toString()
                )
                    ?.toInt() ?: 8)*1.5) {
                binding.lastSend.setImageResource(R.drawable.btn_rnd_grn)
            } else {
                binding.lastSend.setImageResource(R.drawable.btn_rnd_red)
            }
        } else {
            binding.lastSend.visibility = View.INVISIBLE
        }
    }

    private fun setEmptyRouteUI(isEmpty: Boolean) {
        if (sharedPreferences.getBoolean(SettingsFragment.KEY_TRACKING, false)) {
            binding.lastSend.visibility = View.VISIBLE
        } else {
            binding.lastSend.visibility = View.INVISIBLE
        }
        binding.boatNameTextView.text = sharedPreferences.getString(SettingsFragment.KEY_BOAT_NAME, "Undefined")
        binding.location.setTextColor(Color.BLACK)
        if (isEmpty){
            binding.routeElementSpinner.visibility = View.INVISIBLE
            binding.nextWptHeader.text = getString(R.string.no_route_loaded)
            binding.portGate.visibility = View.GONE
            binding.stbdGate.visibility = View.GONE
            binding.shortestDistanceToGate.visibility = View.GONE
            binding.vmg.visibility = View.GONE

        } else {
            binding.routeElementSpinner.visibility = View.VISIBLE
            binding.nextWptHeader.text = getString(R.string.next_waypoint_gate)
            binding.portGate.visibility = View.VISIBLE
            binding.stbdGate.visibility = View.VISIBLE
            binding.shortestDistanceToGate.visibility = View.VISIBLE
            binding.vmg.visibility = View.VISIBLE
        }
    }

    private fun setUiForGPS(isAvailable: Boolean) {
        if (isAvailable) {
            binding.portGate.setTextColor(Color.BLACK)
            binding.stbdGate.setTextColor(Color.BLACK)
            binding.cog.setTextColor(Color.BLACK)
            binding.sog.setTextColor(Color.BLACK)
            binding.location.setTextColor(Color.BLACK)
            binding.time.setTextColor(Color.BLACK)
            binding.shortestDistanceToGate.setTextColor(Color.BLACK)
            binding.vmg.setTextColor(Color.BLACK)
        } else {
            binding.portGate.setTextColor(Color.RED)
            binding.stbdGate.setTextColor(Color.RED)
            binding.cog.setTextColor(Color.RED)
            binding.sog.setTextColor(Color.RED)
            binding.location.setTextColor(Color.RED)
            binding.time.setTextColor(Color.RED)
            binding.shortestDistanceToGate.setTextColor(Color.RED)
            binding.vmg.setTextColor(Color.RED)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == SettingsFragment.KEY_STATUS) {
            setButton(sharedPreferences.getBoolean(SettingsFragment.KEY_STATUS, false))
            if (sharedPreferences.getBoolean(SettingsFragment.KEY_STATUS, false)) {
                startTrackingService(true, false)
            } else {
                stopTrackingService()
            }
        } else if (key == SettingsFragment.KEY_NEXT_WPT) {
            getNextWpt()
        } else if (key == SettingsFragment.KEY_LAST_SEND) {
            if (Utils.timeDiffInSeconds(sharedPreferences.getLong(SettingsFragment.KEY_LAST_SEND, Long.MAX_VALUE),Date().time) < (sharedPreferences.getString(
                    SettingsFragment.KEY_INTERVAL,
                    8.toString()
                )
                    ?.toInt() ?: 8)*1.5) {
                binding.lastSend.setImageResource(R.drawable.btn_rnd_grn)
            } else {
                binding.lastSend.setImageResource(R.drawable.btn_rnd_red)
            }
        } else if (key == SettingsFragment.KEY_GATE_PASSES) {
            updateLastPass()
        } else if (key == SettingsFragment.KEY_BOAT_NAME) {
            binding.boatNameTextView.text = sharedPreferences.getString(SettingsFragment.KEY_BOAT_NAME, "Undefined")
        }
    }

    private fun updateLastPass() {
        val gp = GatePassings.getLastGatePass(this)
        if (gp!=null) {
            binding.lastPassTextView.text = getString(R.string.lastpass_message,gp.gateName, timeStamptoDateString(gp.time.time))
        }
    }

    private fun setButton(isRunning: Boolean) {
        if (isRunning) {
            binding.startBtn.background = ContextCompat.getDrawable(this, R.drawable.btn_rect_red)
            binding.startBtn.text = getString(R.string.settings_status_on)
        } else {
            binding.startBtn.background = ContextCompat.getDrawable(this, R.drawable.btn_rnd_grn)
            binding.startBtn.text = getString(R.string.settings_status_off)
        }
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

    private fun startTrackingService(checkPermission: Boolean, initialPermission: Boolean) {
        var permission = initialPermission
        if (checkPermission) {
            val requiredPermissions: MutableSet<String> = HashSet()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            permission = requiredPermissions.isEmpty()
            if (!permission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                        requiredPermissions.toTypedArray(),
                        PERMISSIONS_REQUEST_LOCATION
                    )
                }
                return
            }
        }
        if (permission) {
            val i = Intent(this, TrackingService::class.java)
            i.putExtra("route", route)
            i.putExtra("nextwpt", nextWpt)
            ContextCompat.startForegroundService(this, i)
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
//                alarmManager.setInexactRepeating(
//                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                    ALARM_MANAGER_INTERVAL.toLong(), ALARM_MANAGER_INTERVAL.toLong(), alarmIntent
//                )
//            }
            BatteryOptimizationHelper().requestException(this)
        } else {
            sharedPreferences.edit().putBoolean(SettingsFragment.KEY_STATUS, false).apply()
        }
    }

    private fun stopTrackingService() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
//            alarmManager.cancel(alarmIntent)
//        }
        this.stopService(Intent(this, TrackingService::class.java))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_LOCATION || requestCode == PERMISSIONS_REQUEST_LOCATION_UI) {
            var granted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                    break
                }
            }
            Log.d(TAG, "Permissions granted: $granted")
            if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
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

    private val REQUEST_SCREENSHOT_PERMISSION: Int = 1234
    private lateinit var screenshotManager : ScreenshotManager


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        screenshotManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun takeScreenshot(){
        val screenshotResult = screenshotManager.makeScreenshot()
        screenshotResult.observe(
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
        val imagePath = File(this.getCacheDir(), "images")
        val newFile = File(imagePath, "image.png")
        val contentUri: Uri =
            FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", newFile)
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
        shareIntent.setDataAndType(contentUri, contentResolver.getType(contentUri))
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
        startActivity(Intent.createChooser(shareIntent, "Choose an app"))
    }

    companion object{
        private const val PERMISSIONS_REQUEST_LOCATION = 2
        private const val ALARM_MANAGER_INTERVAL = 15000
    }

}