package `in`.avimarine.waypointracing.activities

import `in`.avimarine.waypointracing.*
import `in`.avimarine.waypointracing.databinding.ActivityMainBinding
import `in`.avimarine.waypointracing.route.*
import `in`.avimarine.waypointracing.ui.LocationViewModel
import `in`.avimarine.waypointracing.ui.RouteElementAdapter
import `in`.avimarine.waypointracing.utils.*
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
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
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import eu.bolt.screenshotty.ScreenshotActionOrder
import eu.bolt.screenshotty.ScreenshotManager
import eu.bolt.screenshotty.ScreenshotManagerBuilder
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), PositionProvider.PositionListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

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
    // See: https://developer.android.com/training/basics/intents/result
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        launchAuthenticationProcess()
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
            val intent = Intent(this, SetupWizardActivity::class.java)
            this.startActivity(intent)
        }

        setButton(sharedPreferences.getBoolean(SettingsFragment.KEY_STATUS, false))
        if (sharedPreferences.getBoolean(SettingsFragment.KEY_STATUS, false)) {
            startTrackingService(checkPermission = true, initialPermission = false)
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

    private fun launchAuthenticationProcess() {
        if (FirebaseAuth.getInstance().currentUser!=null){
            return
        }
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build())

        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()
        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                Log.d(TAG, "Logged in as ${user.displayName}")
            }
        } else {
            if (response != null) {
                Log.e(TAG, "Error authenticating ${response.error?.errorCode}")
            } else {
                Log.e(TAG, "User canceled sign in")
            }
        }
        setUiForLogin(FirebaseAuth.getInstance().currentUser)
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
        val PREFS_NAME = "MyPrefsFile"
        val settings = getSharedPreferences(PREFS_NAME, 0)
        if (settings.getBoolean("my_first_time", true)) return
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
        setBoatName()
    }

    private fun setBoatName() {
        binding.boatNameTextView.text = sharedPreferences.getString(SettingsFragment.KEY_BOAT_NAME, "Undefined")
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
        if (FirebaseAuth.getInstance().currentUser == null) {
            menu.findItem(R.id.login_menu_action).icon =
                getDrawable(R.drawable.ic_baseline_login_24)
            menu.findItem(R.id.login_menu_action).title =
                getString(R.string.login)
        } else {
            menu.findItem(R.id.login_menu_action).icon =
                getDrawable(R.drawable.ic_baseline_logout_24)
            menu.findItem(R.id.login_menu_action).title =
                getString(R.string.logout)
        }
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
            R.id.login_menu_action -> {
                login()
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
                val intent = Intent(this, SetupWizardActivity::class.java)
                this.startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun login() {
        if (FirebaseAuth.getInstance().currentUser!=null){
            AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener {
                    Log.d(TAG, "Signed out")
                    setUiForLogin(FirebaseAuth.getInstance().currentUser)
                }
        } else {
            launchAuthenticationProcess()
        }
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

    fun loginButtonClick(view: View) {
        login()
    }

    fun setNextWpt(n: Int){
        nextWpt = n
        val sharedPref: SharedPreferences = getDefaultSharedPreferences(this)
        with(sharedPref.edit()) {
            putInt(SettingsFragment.KEY_NEXT_WPT, n)
            commit()
        }
    }

    private fun setGPSInterval(i: Int){
        val sharedPref: SharedPreferences = getDefaultSharedPreferences(this)
        with(sharedPref.edit()) {
            putString(SettingsFragment.KEY_INTERVAL, i.toString())
            commit()
        }
    }

    private fun setMainActivityVisibilityStatus(b: Boolean){
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
        binding.viewmodel = LocationViewModel(position,wpt,sharedPreferences)
        setUiForGPS(true)
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
            binding.lastSend.visibility = View.GONE
        }
    }

    private fun setEmptyRouteUI(isEmpty: Boolean) {
        if (sharedPreferences.getBoolean(SettingsFragment.KEY_TRACKING, false)) {
            binding.lastSend.visibility = View.VISIBLE
        } else {
            binding.lastSend.visibility = View.GONE
        }
        setBoatName()
//        binding.location.setTextColor(Color.BLACK)
        if (isEmpty){
            binding.routeElementSpinner.visibility = View.INVISIBLE
            binding.nextWptHeader.text = getString(R.string.no_route_loaded)
            binding.portGate.visibility = View.GONE
            binding.stbdGate.visibility = View.GONE
            binding.shortestDistanceToGate.visibility = View.GONE
            binding.vmg.visibility = View.GONE
            binding.startBtn.visibility = View.INVISIBLE

        } else {
            binding.routeElementSpinner.visibility = View.VISIBLE
            binding.nextWptHeader.text = getString(R.string.next_waypoint_gate)
            binding.portGate.visibility = View.VISIBLE
            binding.stbdGate.visibility = View.VISIBLE
            binding.shortestDistanceToGate.visibility = View.VISIBLE
            binding.vmg.visibility = View.VISIBLE
            if (FirebaseAuth.getInstance().currentUser == null) {
                binding.startBtn.visibility = View.INVISIBLE
            } else {
                binding.startBtn.visibility = View.VISIBLE
            }
        }
        setUiForLogin(FirebaseAuth.getInstance().currentUser)
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

    private fun setUiForLogin(user: FirebaseUser?) {
        if (user == null) {
            sharedPreferences.edit().putBoolean(SettingsFragment.KEY_STATUS,  false).apply()
            binding.startBtn.visibility = View.INVISIBLE
            binding.loginBtn.visibility = View.VISIBLE
        } else {
            binding.startBtn.visibility = View.VISIBLE
            binding.loginBtn.visibility = View.GONE
        }
        invalidateOptionsMenu()
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
            setBoatName()
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
            if (!BatteryOptimizationHelper().requestedExceptions(this)) {
                BatteryOptimizationHelper().requestException(this)
            }
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
            onSuccess = { ScreenShot.processScreenshot(it, this) },
            onError = { /*onMakeScreenshotFailed(it)*/ }
        )
    }

    companion object{
        private const val PERMISSIONS_REQUEST_LOCATION = 2
//        private const val ALARM_MANAGER_INTERVAL = 15000
    }

}