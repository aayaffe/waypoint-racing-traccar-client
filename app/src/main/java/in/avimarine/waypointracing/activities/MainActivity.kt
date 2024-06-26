package `in`.avimarine.waypointracing.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.location.Location
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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.toObject
import `in`.avimarine.androidutils.*
import `in`.avimarine.androidutils.LocationPermissions.Companion.PERMISSIONS_REQUEST_LOCATION_UI
import `in`.avimarine.androidutils.Utils.Companion.getInstalledVersion
import `in`.avimarine.waypointracing.*
import `in`.avimarine.waypointracing.BuildConfig
import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.activities.SetupWizardActivity.Companion.runSetupWizardIfNeeded
import `in`.avimarine.waypointracing.activities.fragments.MapFragment
import `in`.avimarine.waypointracing.database.FirestoreDatabase
import `in`.avimarine.waypointracing.databinding.ActivityMainBinding
import `in`.avimarine.waypointracing.route.*
import `in`.avimarine.waypointracing.ui.LocationViewModel
import `in`.avimarine.waypointracing.ui.RouteElementAdapter
import `in`.avimarine.waypointracing.ui.VersionViewModel
import `in`.avimarine.waypointracing.utils.*
import java.util.*


class MainActivity : AppCompatActivity(), PositionProvider.PositionListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var positionProvider: PositionProvider
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var prefs: Preferences
    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmIntent: PendingIntent
//    private var nextWpt: Int = 0
    private var route = Route.emptyRoute()
    val delayedHandler = Handler(Looper.getMainLooper())
    private var isFirstSpinnerLoad = true
    private lateinit var binding: ActivityMainBinding
    private val debugMode = BuildConfig.DEBUG

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
        binding.versionViewModel = VersionViewModel(getInstalledVersion(this))
        setContentView(view)
        Auth.launchAuthenticationProcess(signInLauncher)
        sharedPreferences = getDefaultSharedPreferences(this.applicationContext)
        prefs = Preferences(sharedPreferences)
        checkVersion()
        if (intent.action == Intent.ACTION_MAIN) {
            val r = RouteLoader.loadRouteFromFile(this)
            if (!r.isEmpty()) {
                FirestoreDatabase.getRoute(r.id, this::isRouteUpdated) { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
            }
            loadRoute(r)
        } else {
            RouteLoader.handleIntent(this, intent, this::loadRoute)
        }
        setEmptyRouteUI(route.isEmpty())
        runSetupWizardIfNeeded(this)

        setButton(prefs.status)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (prefs.status) {
            startTrackingService(checkPermission = true, initialPermission = false)
        }

        binding.time.setLabel(getTimeZoneString())
        if (route.isValidWpt(prefs.nextWpt)) {
            getNextWpt()
        }

        createAlarmIntent()
        binding.routeElementSpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?, position: Int, id: Long
            ) {
                if (isFirstSpinnerLoad) {
                    isFirstSpinnerLoad = false
                    if (route.isValidWpt(prefs.nextWpt)) {
                        parent?.setSelection(prefs.nextWpt)
                    }
                    return
                }
                setNextWpt(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // write code to perform some action
            }
        }
        prefs.expertMode = false
        setOnBackPressed()

        Log.d(TAG, "Save all Locations: ${RemoteConfig.getBool("save_all_locations")}")
    }

    private fun isRouteUpdated(docs: QuerySnapshot?) {
        if (docs == null) {
            Log.d(TAG, "No such route")
            return
        }
        for (doc in docs) {
            val rd = doc.toObject<RouteDetails>()
            val r = Route.fromGeoJson(rd.route)
            if (r.lastUpdate > this.route.lastUpdate) {
                val lastCheckedVersion = prefs.routeUpdatedVersion
                if (lastCheckedVersion == r.lastUpdate.time) {
                    //User already decided not to update route to this version
                    break
                }
                //Show dialog to ask if user wants to load updated route
                val builder = AlertDialog.Builder(this)
                builder.setMessage(R.string.route_updated_dialog_message)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        prefs.routeUpdatedVersion = 0
                        RouteLoader.loadRouteFromString(this, rd.route, this::loadRoute)
                    }
                    .setNegativeButton(R.string.no) { _, _ ->
                        prefs.routeUpdatedVersion = r.lastUpdate.time
                    }
                builder.create().show()
            }
            break
        }

    }

    private fun checkVersion() {
        FirestoreDatabase.getSupportedVersion({
            if (it != null) {
                val ver = it.getLong("ver") ?: -1
                if (ver > getInstalledVersion(this)) {
                    Utils.alertOnUnsupportedVersion(this)
                }
            }
        }, {
            Log.w(TAG, "Failed to get minimal version", it)
        })
    }

    private fun createAlarmIntent() {
        val originalIntent = Intent(this, AutostartReceiver::class.java)
        originalIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        alarmIntent = PendingIntent.getBroadcast(applicationContext, 0, originalIntent, flags)
    }


    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                Log.d(TAG, "Logged in as ${user.displayName}")
                Auth.loadSettingsFromServer(user.uid, this) {
                    Log.w(TAG, "Failed to load settings from server", it)
                    DialogUtils.createDialog(
                        this,
                        R.string.missing_boat_name_title,
                        R.string.missing_boat_name_message,
                        { _, _
                            ->
                            val intent = Intent(this, SettingsActivity::class.java)
                            this.startActivity(intent)
                        },
                        null,
                        null
                    ).show()
                }
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

    @SuppressLint("MissingSuperCall", "False Positive")
    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        if (RouteLoader.handleIntent(this, i, this::loadRoute)) {
            setNextWpt(0)
        }
    }

    private fun createPositionProvider() {
        positionProvider = PositionProviderFactory.create(this, this)
    }


    private fun loadRoute(r: Route?, newRoute: Boolean = false) {
        if (r == null) {
            errorLoadingRoute("Error Loading Route")
            loadRoute(RouteLoader.loadRouteFromFile(this))
            return
        }
        if (newRoute) {
            //Loading new route, reset last checked version
            prefs.routeUpdatedVersion = 0
            prefs.nextWpt = route.getNextNonOptionalWpt(-1)
        }
        route = r
        prefs.currentRoute = route.toString()
        populateRouteElementSpinner(r)
        setEmptyRouteUI(route.isEmpty())
        setActivityTitle(r)
        if (!route.isEmpty()) {
            Toast.makeText(
                applicationContext,
                "Loaded route\n ${route.eventName}",
                Toast.LENGTH_LONG
            ).show()
        }
        createAlarmIntent()
        prefs.status = true
    }

    private fun setActivityTitle(r: Route) {
        if (route.eventType == EventType.WPTRACING) {
            setTitle(
                getString(R.string.title_waypoint_racing),
                r.eventName + " - " + prefs.boatName
            )
        } else {
            setTitle(
                getString(R.string.title_treasure_hunting),
                r.eventName + " - " + prefs.boatName
            )
        }
    }

    private fun errorLoadingRoute(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    }


    override fun onStart() {
        super.onStart()
        val PREFS_NAME = "MyPrefsFile"
        val settings = getSharedPreferences(PREFS_NAME, 0)
        if (settings.getBoolean("my_first_time", true)) {
            settings.edit().putBoolean("my_first_time", false).apply()
            return
        }
        startPositionProvider()
        setActivityTitle(route)
        prefs.status = true
    }

    private fun startPositionProvider() {
        try {
            if (LocationPermissions.arePermissionsGranted(this)) {
                if (!this::positionProvider.isInitialized) {
                    createPositionProvider()
                }
                positionProvider.startUpdates()
            } else {
                LocationPermissions.askForLocationPermission(
                    this,
                    PERMISSIONS_REQUEST_LOCATION_UI,
                    getString(R.string.permission_rationale)
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        if (route.isEmpty()) {
            val r = RouteLoader.loadRouteFromFile(this)
            loadRoute(r)
        }
        startPositionProvider()
        getNextWpt()
        setGPSInterval(1)
        setMainActivityVisibilityStatus(true)
        updateLastPass()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Create new fragment and transaction
            val newFragment = MapFragment()
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.map_fragment_view, newFragment)
            transaction.addToBackStack(null)
            transaction.commit()
            binding.mapFragmentView.visibility = View.VISIBLE
        } else {
            binding.mapFragmentView.visibility = View.GONE
            // Get fragment instance
            val oldFragment = supportFragmentManager.findFragmentById(R.id.map_fragment_view)
            oldFragment?.let {
                // Create new transaction and remove the fragment
                val transaction = supportFragmentManager.beginTransaction()
                transaction.remove(it)
                transaction.commit()
            }
        }
    }

    private fun getNextWpt() {
//        nextWpt = prefs.nextWpt
        if (prefs.nextWpt >= route.elements.size) {
            setNextWpt(0)
        }
        binding.routeElementSpinner.setSelection(prefs.nextWpt)
        route.elements.elementAtOrNull(prefs.nextWpt)?.let { setNextWaypointUI(it) }
    }

    private fun setNextWaypointUI(wpt: RouteElement) {
        if (wpt.routeElementType == RouteElementType.WAYPOINT) {
            if (wpt.proofArea.type == ProofAreaType.QUADRANT) {
                binding.shortestDistanceToGate.visibility = View.INVISIBLE
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
        setGPSInterval(5)//prefs.initialGPSInterval.toInt())
        setMainActivityVisibilityStatus(false)
        stopPositionProvider()
    }

    private fun stopPositionProvider() {
        try {
            if (this::positionProvider.isInitialized) {
                positionProvider.stopUpdates()
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }

    override fun onStop() {
        stopPositionProvider()
        super.onStop()
    }

    private fun setOnBackPressed(){
        val callback = object : OnBackPressedCallback(true /* enabled by default */) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@MainActivity)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Closing Waypoint Racing")
                    .setMessage("Are you sure you want to stop tracking and exit?")
                    .setPositiveButton("Yes") { _, _ ->
                        run {
                            stopTrackingService()
                            prefs.status = false
                            finish()
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
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
        menu.findItem(R.id.expert_mode_menu_action).isVisible = debugMode
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

    private val getRouteStartForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                if (extras != null) {
                    extras.getString("RouteJson")?.let {
                        Log.d(TAG, it)
                        prefs.status = false
                        resetRoute(false)
                        RouteLoader.loadRouteFromString(this, it, this::loadRoute)
                        resetRoute(false)
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
                val bitmap = ScreenShot.takeScreenshot(this)
                ScreenShot.sendSnapshot(bitmap, BuildConfig.APPLICATION_ID, this)
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
                intent.putExtra("route", route)
                this.startActivity(intent)
                return true
            }

            R.id.map_activity_menu_action -> {
                val intent = Intent(this, MapActivity::class.java)
//                intent.putExtra("route", route)
                this.startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun login() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener {
                    Log.d(TAG, "Signed out")
                    setUiForLogin(FirebaseAuth.getInstance().currentUser)
                }
        } else {
            Auth.launchAuthenticationProcess(signInLauncher)
        }
    }

    private fun resetRoute(resetGatePasses: Boolean = true) {
        isFirstSpinnerLoad = true
        setNextWpt(route.getNextNonOptionalWpt(-1))
        if (resetGatePasses) {
            GatePassings.reset(this, route)
        }
        populateRouteElementSpinner(route)
        binding.lastPassTextView.text = ""
        FirestoreDatabase.addEvent(`in`.avimarine.waypointracing.database.EventType.RESET_ROUTE)
    }

    fun startButtonClick(view: View) {
        val checked = prefs.status
        prefs.status = checked.not()
    }

    fun loginButtonClick(view: View) {
        login()
    }

    fun setNextWpt(n: Int) {
//        nextWpt = n
        prefs.nextWpt = n
    }

    private fun setGPSInterval(i: Int) {
        prefs.GPSInterval = i.toString()
    }

    private fun setMainActivityVisibilityStatus(b: Boolean) {
        prefs.uiVisible = b
    }

    override fun onPositionError(error: Throwable) {
        Log.e(TAG, "Position Error: ", error)
    }

    override fun onPositionUpdate(position: Position, location: Location) {
        updateUI(location)
    }

    private fun updateUI(location: Location) {
        val wpt = route.elements.elementAtOrNull(prefs.nextWpt)
        binding.viewmodel = LocationViewModel(location, wpt, sharedPreferences)
        setUiForGPS(true)
        if (wpt != null) {
            binding.location.setTextColor(if (wpt.isInProofArea(location)) Color.GREEN else Color.BLACK)
        } else {
            binding.location.setTextColor(Color.BLACK)
        }
        val interval = (prefs.GPSInterval.toLong()) * 4000 //After four times interval
        delayedHandler.removeCallbacksAndMessages(null)
        delayedHandler.postDelayed({
            setUiForGPS(false)
        }, interval)
        if (prefs.tracking) {
            binding.lastSend.visibility = View.VISIBLE
            val lastLocationSentTime = prefs.lastSend
            if (lastLocationSentTime > 0 && Utils.timeDiffInSeconds(
                    lastLocationSentTime,
                    Date().time
                ) < (prefs.GPSInterval
                    .toInt()) * 1.5
            ) {
                binding.lastSend.setImageResource(R.drawable.btn_rnd_grn)
            } else {
                binding.lastSend.setImageResource(R.drawable.btn_rnd_red)
            }
        } else {
            binding.lastSend.visibility = View.GONE
        }
    }

    private fun setEmptyRouteUI(isEmpty: Boolean) {
        if (prefs.tracking) {
            binding.lastSend.visibility = View.VISIBLE
        } else {
            binding.lastSend.visibility = View.GONE
        }

//        binding.location.setTextColor(Color.BLACK)
        if (isEmpty) {
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
//                binding.startBtn.visibility = View.INVISIBLE
            } else {
//                binding.startBtn.visibility = View.VISIBLE
            }
        }
        setUiForLogin(FirebaseAuth.getInstance().currentUser)
    }

    private fun setUiForGPS(isAvailable: Boolean) {
        if ((isAvailable) && (FirebaseAuth.getInstance().currentUser != null)) {
            binding.portGate.setTextColor(Color.BLACK)
            binding.stbdGate.setTextColor(Color.BLACK)
            binding.cogsog.setTextColor(Color.BLACK)
            binding.location.setTextColor(Color.BLACK)
            binding.time.setTextColor(Color.BLACK)
            binding.shortestDistanceToGate.setTextColor(Color.BLACK)
            binding.vmg.setTextColor(Color.BLACK)
        } else {
            binding.portGate.setTextColor(Color.RED)
            binding.stbdGate.setTextColor(Color.RED)
            binding.cogsog.setTextColor(Color.RED)
            binding.location.setTextColor(Color.RED)
            binding.time.setTextColor(Color.RED)
            binding.shortestDistanceToGate.setTextColor(Color.RED)
            binding.vmg.setTextColor(Color.RED)
            binding.location.setLabel("Accuracy - Unknown")
        }
    }

    private fun setUiForLogin(user: FirebaseUser?) {
        if (user == null) {
            prefs.status = false
//            binding.startBtn.visibility = View.INVISIBLE
            binding.loginBtn.visibility = View.VISIBLE
        } else {
//            binding.startBtn.visibility = View.VISIBLE
            binding.loginBtn.visibility = View.GONE
            prefs.status = true
        }
        invalidateOptionsMenu()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences == null) return
        if (key == SettingsFragment.KEY_STATUS) {
            setButton(prefs.status)
            if (prefs.status) {
                startTrackingService(true, false)
            } else {
                stopTrackingService()
            }
        } else if (key == SettingsFragment.KEY_NEXT_WPT) {
            getNextWpt()
        } else if (key == SettingsFragment.KEY_LAST_SEND) {
            if (Utils.timeDiffInSeconds(
                    prefs.lastSend,
                    Date().time
                ) < (prefs.GPSInterval.toInt()) * 1.5
            ) {
                binding.lastSend.setImageResource(R.drawable.btn_rnd_grn)
            } else {
                binding.lastSend.setImageResource(R.drawable.btn_rnd_red)
            }
        } else if (key == SettingsFragment.KEY_GATE_PASSES) {
            updateLastPass()
        } else if (key == SettingsFragment.KEY_BOAT_NAME) {
            setActivityTitle(route)
        }
    }

    private fun updateLastPass() {
        val gp = GatePassings.getLastGatePass(this, route.id)
        if (gp != null) {
            binding.lastPassTextView.text = getString(
                R.string.lastpass_message,
                gp.gateName,
                timeStampToDateString(gp.time.time)
            )
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

    private fun startTrackingService(checkPermission: Boolean, initialPermission: Boolean) {
        var permission = initialPermission
        if (checkPermission) {
            val requiredPermissions: MutableSet<String> = HashSet()
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
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
            ContextCompat.startForegroundService(this, i)
            createAlarmIntent()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    ALARM_MANAGER_INTERVAL.toLong(), ALARM_MANAGER_INTERVAL.toLong(), alarmIntent
                )
            }
            if (!BatteryOptimizationHelper().requestedExceptions(this)) {
                BatteryOptimizationHelper().requestException(this)
            }
        } else {
            prefs.status = false
        }
    }

    private fun stopTrackingService() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            alarmManager.cancel(alarmIntent)
            Log.d(TAG, "Stopped alarm manager")
        }
        Log.d(TAG, "Stopping service")
        this.stopService(Intent(this, TrackingService::class.java))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_LOCATION || requestCode == LocationPermissions.PERMISSIONS_REQUEST_LOCATION_UI) {
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

    companion object {
        private const val PERMISSIONS_REQUEST_LOCATION = 2
        private const val ALARM_MANAGER_INTERVAL = 15000
    }

}