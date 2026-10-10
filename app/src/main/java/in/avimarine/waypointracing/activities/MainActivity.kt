package `in`.avimarine.waypointracing.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.ActivityManager
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.media.RingtoneManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.HapticFeedbackConstants
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import androidx.constraintlayout.widget.ConstraintLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.toObject
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import `in`.avimarine.androidutils.*
import `in`.avimarine.androidutils.Utils.Companion.getInstalledVersion
import `in`.avimarine.waypointracing.*
import `in`.avimarine.waypointracing.BuildConfig
import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.activities.SetupWizardActivity.Companion.runSetupWizardIfNeeded
import `in`.avimarine.waypointracing.activities.fragments.MapFragment
import `in`.avimarine.waypointracing.database.FirestoreDatabase
import `in`.avimarine.waypointracing.databinding.ActivityMainBinding
import `in`.avimarine.waypointracing.event.EventAssignmentResolver
import `in`.avimarine.waypointracing.route.*
import `in`.avimarine.waypointracing.ui.LocationViewModel
import `in`.avimarine.waypointracing.ui.RaceDeckFormatter
import `in`.avimarine.waypointracing.ui.RaceDeckHealthMapper
import `in`.avimarine.waypointracing.ui.RaceDeckHealthTone
import `in`.avimarine.waypointracing.ui.DeviceReadiness
import `in`.avimarine.waypointracing.ui.DeviceReadinessIssue
import `in`.avimarine.waypointracing.ui.RouteElementAdapter
import `in`.avimarine.waypointracing.ui.VersionViewModel
import `in`.avimarine.waypointracing.ui.TargetSelection
import `in`.avimarine.waypointracing.ui.RaceDeckTargetMapper
import `in`.avimarine.waypointracing.ui.RaceDeckTargetLabel
import `in`.avimarine.waypointracing.ui.RaceDeckAction
import `in`.avimarine.waypointracing.ui.GpsAlertPolicy
import `in`.avimarine.waypointracing.ui.RaceDeckUiState
import `in`.avimarine.waypointracing.utils.*
import java.util.*


class MainActivity : EdgeToEdgeActivity(), PositionProvider.PositionListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var positionProvider: PositionProvider
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var prefs: Preferences
    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmIntent: PendingIntent
//    private var nextWpt: Int = 0
    private var route = Route.emptyRoute()
    val delayedHandler = Handler(Looper.getMainLooper())
    private val notificationRestoreHandler = Handler(Looper.getMainLooper())
    private val deviceReadinessHandler = Handler(Looper.getMainLooper())
    private val gpsWarningHandler = Handler(Looper.getMainLooper())
    private val gpsLossAlertHandler = Handler(Looper.getMainLooper())
    private var isFirstSpinnerLoad = true
    private lateinit var binding: ActivityMainBinding
    private val debugMode = BuildConfig.DEBUG
    private var activityStarted = false
    private var activityResumed = false
    private var batterySaverReceiverRegistered = false
    private val batterySaverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                updateBatterySaverBanner()
            }
        }
    }
    private var notificationPermissionRequestInFlight = false
    private var notificationWarningShown = false
    private var lastShownPassToken: String? = null
    private var celebrationRoute: Route? = null
    private var celebrationPass: GatePassing? = null
    private var latestRaceDeckLocation: Location? = null
    private var lastKnownGpsAvailable = false
    private var lastGpsFixTimeMillis = 0L
    private var gpsAlertsSilencedForSession = false
    private var gpsOutageAlertPlayed = false
    private var batterySaverDismissedForSession = false
    private var portraitMapExpanded = false
    private var raceDeckUiState = RaceDeckUiState()
    private val hidePassCelebration = Runnable { dismissPassCelebration() }
    private val refreshDeviceReadiness = object : Runnable {
        override fun run() {
            updateNotificationPermissionBanner()
            updateBatterySaverBanner()
            deviceReadinessHandler.postDelayed(this, DEVICE_READINESS_REFRESH_INTERVAL_MS)
        }
    }
    private val refreshGpsWarningAge = object : Runnable {
        override fun run() {
            updateGpsWarningMessage()
            if (binding.gpsWarningBanner.isVisible) {
                gpsWarningHandler.postDelayed(this, GPS_WARNING_REFRESH_INTERVAL_MS)
            }
        }
    }
    private val playProlongedGpsAlert = Runnable {
        if (!gpsAlertsSilencedForSession && binding.gpsWarningBanner.isVisible && !gpsOutageAlertPlayed) {
            binding.gpsWarningBanner.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            RingtoneManager.getRingtone(this, Settings.System.DEFAULT_NOTIFICATION_URI)?.play()
            gpsOutageAlertPlayed = true
        }
    }
    private val raceDeckClockTicker = object : Runnable {
        override fun run() {
            binding.raceDeckClock.text = RaceDeckFormatter.clock(Date().time)
            binding.raceDeckClock.postDelayed(this, 1_000)
        }
    }

    // See: https://developer.android.com/training/basics/intents/result
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        binding.versionViewModel = VersionViewModel(getInstalledVersion(this))
        setContentView(view)
        binding.notificationPermissionButton.setOnClickListener { openNotificationSettings() }
        binding.batterySaverButton.setOnClickListener { openBatterySaverSettings() }
        binding.passCelebrationDismiss.setOnClickListener { dismissPassCelebration() }
        binding.passCelebrationAction.setOnClickListener {
            dismissPassCelebration()
            showTargetSheet()
        }
        binding.activeTargetPanel.setOnClickListener { dispatchRaceDeckAction(RaceDeckAction.OPEN_TARGETS) }
        binding.raceTargetsAction.setOnClickListener { dispatchRaceDeckAction(RaceDeckAction.OPEN_TARGETS) }
        binding.raceMapAction.setOnClickListener { dispatchRaceDeckAction(RaceDeckAction.TOGGLE_MAP) }
        binding.raceMoreAction.setOnClickListener { dispatchRaceDeckAction(RaceDeckAction.OPEN_MORE) }
        binding.raceSelectCourseButton.setOnClickListener { dispatchRaceDeckAction(RaceDeckAction.SELECT_COURSE) }
        binding.deviceSetupReview.setOnClickListener { showDeviceSetupPanel() }
        binding.silenceGpsAlerts.setOnClickListener {
            gpsAlertsSilencedForSession = true
            binding.silenceGpsAlerts.visibility = View.GONE
            gpsLossAlertHandler.removeCallbacks(playProlongedGpsAlert)
        }
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
        updateRaceDeckHeader(route)
        if (intent.action == ACTION_CONFIRM_STOP_TRACKING) showStopTrackingConfirmation()
        runSetupWizardIfNeeded(this)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

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

    private fun dispatchRaceDeckAction(action: RaceDeckAction) {
        when (action) {
            RaceDeckAction.OPEN_TARGETS -> showTargetSheet()
            RaceDeckAction.OPEN_MORE -> showRaceMoreMenu()
            RaceDeckAction.SELECT_COURSE -> getRouteStartForResult.launch(
                Intent(this, LoadRouteActivity::class.java)
            )
            RaceDeckAction.TOGGLE_MAP -> {
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    startActivity(Intent(this, MapActivity::class.java))
                } else if (!shouldShowPortraitMap()) {
                    startActivity(Intent(this, MapActivity::class.java))
                } else {
                    portraitMapExpanded = !portraitMapExpanded
                    raceDeckUiState = raceDeckUiState.copy(mapExpanded = portraitMapExpanded)
                    configureRaceDeckMapLayout(landscape = false, showPortraitMap = true)
                    applyPortraitMapExpansion()
                    binding.raceMapAction.setText(
                        if (portraitMapExpanded) R.string.race_deck_collapse_map else R.string.race_deck_map
                    )
                }
            }
        }
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
        ensureTrackingRunning()
    }

    @SuppressLint("MissingSuperCall", "False Positive")
    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        setIntent(i)
        if (i.action == ACTION_CONFIRM_STOP_TRACKING) {
            showStopTrackingConfirmation()
            return
        }
        if (RouteLoader.handleIntent(this, i, this::loadRoute)) {
            setNextWpt(0)
        }
    }

    private fun createPositionProvider() {
        positionProvider = PositionProviderFactory.create(this, this)
    }


    private fun loadRoute(
        r: Route?,
        newRoute: Boolean = false,
        showReadinessReview: Boolean = false,
    ) {
        if (r == null) {
            route = Route.emptyRoute()
            prefs.currentRoute = route.toString()
            populateRouteElementSpinner(route)
            setEmptyRouteUI(true)
            setActivityTitle(route)
            updateRaceDeckHeader(route)
            errorLoadingRoute(getString(R.string.race_deck_restore_failed))
            return
        }
        showRestoringNavigation()
        if (newRoute) {
            //Loading new route, reset last checked version
            prefs.routeUpdatedVersion = 0
        }
        route = r
        if (newRoute) {
            prefs.nextWpt = route.getNextNonOptionalWpt(-1)
        } else if (!route.isEmpty()) {
            prefs.nextWpt = TargetSelection.validOrFallback(
                prefs.nextWpt,
                route.elements.size,
                route.getNextNonOptionalWpt(-1),
            )
        }
        prefs.currentRoute = route.toString()
        if (FirebaseAuth.getInstance().currentUser != null &&
            RemoteConfig.getBool("event_scoped_location_uploads")) {
            EventAssignmentResolver.refreshForRoute(route, prefs)
        } else {
            prefs.clearEventSession()
        }
        populateRouteElementSpinner(r)
        setEmptyRouteUI(route.isEmpty())
        setActivityTitle(r)
        updateRaceDeckHeader(r)
        if (!route.isEmpty()) {
            Toast.makeText(
                applicationContext,
                "Loaded route\n ${route.eventName}",
                Toast.LENGTH_LONG
            ).show()
        }
        createAlarmIntent()
        ensureTrackingRunning()
        if (showReadinessReview) showPreRaceReadinessReview()
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

    private fun updateRaceDeckHeader(route: Route) {
        raceDeckUiState = raceDeckUiState.copy(
            courseName = if (route.isEmpty()) {
                getString(R.string.no_route_loaded)
            } else {
                route.eventName
            },
            boatName = prefs.boatName,
        )
        binding.raceDeckCourseName.text = raceDeckUiState.courseName
        binding.raceDeckBoatName.text = raceDeckUiState.boatName
        updateRaceDeckHealth(gpsAvailable = false)
    }

    private fun updateRaceDeckHealth(gpsAvailable: Boolean) {
        lastKnownGpsAvailable = gpsAvailable
        val trackingActive = prefs.status
        val syncRecent = trackingActive && prefs.lastSend > 0 &&
            Utils.timeDiffInSeconds(prefs.lastSend, Date().time) < prefs.GPSInterval.toInt() * 1.5
        val networkOnline = NetworkManager(this, null).isOnline
        val deviceIssues = deviceReadinessIssues()
        val deviceReady = deviceIssues.isEmpty()
        val health = RaceDeckHealthMapper.map(gpsAvailable, trackingActive, syncRecent)
        raceDeckUiState = raceDeckUiState.copy(health = health)

        binding.raceDeckGpsStatus.setText(
            if (health.gps == RaceDeckHealthTone.HEALTHY) R.string.race_deck_gps_ok else R.string.race_deck_gps_lost
        )
        binding.raceDeckTrackingStatus.setText(
            if (health.tracking == RaceDeckHealthTone.HEALTHY) R.string.race_deck_tracking_on else R.string.race_deck_tracking_off
        )
        binding.raceDeckSyncStatus.setText(
            when {
                !networkOnline -> R.string.race_deck_sync_offline
                health.sync == RaceDeckHealthTone.HEALTHY -> R.string.race_deck_sync_online
                else -> R.string.race_deck_sync_pending
            }
        )
        binding.raceDeckDeviceStatus.setText(
            if (deviceReady) R.string.race_deck_device_ready else R.string.race_deck_device_warning
        )
        binding.raceDeckGpsStatus.setTextColor(ContextCompat.getColor(this, colorForHealthTone(health.gps)))
        binding.raceDeckTrackingStatus.setTextColor(ContextCompat.getColor(this, colorForHealthTone(health.tracking)))
        binding.raceDeckSyncStatus.setTextColor(ContextCompat.getColor(
            this,
            if (!networkOnline) R.color.race_warning else colorForHealthTone(health.sync),
        ))
        binding.raceDeckDeviceStatus.setTextColor(ContextCompat.getColor(
            this,
            if (deviceReady) R.color.race_success else R.color.race_warning
        ))
        val batterySaverOnly = deviceIssues.all { it == DeviceReadinessIssue.BATTERY_SAVER }
        binding.deviceSetupBanner.visibility = if (
            FirebaseAuth.getInstance().currentUser != null && deviceIssues.isNotEmpty() &&
                !(batterySaverOnly && batterySaverDismissedForSession)
        ) View.VISIBLE else View.GONE
        binding.deviceSetupSummary.text = resources.getQuantityString(
            R.plurals.race_deck_device_issue_count,
            deviceIssues.size,
            deviceIssues.size,
        )
        binding.trackingOffBanner.visibility = if (
            FirebaseAuth.getInstance().currentUser != null && !trackingActive
        ) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun refreshRaceDeckHealth() {
        updateRaceDeckHealth(lastKnownGpsAvailable)
    }

    private fun deviceReadinessIssues() = DeviceReadiness.issues(
        notificationsEnabled = notificationsVisible(),
        batteryOptimizationIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        },
        backgroundRestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getSystemService(ActivityManager::class.java).isBackgroundRestricted
        } else {
            false
        },
        batterySaverEnabled = getSystemService(PowerManager::class.java).isPowerSaveMode,
    )

    private fun showDeviceSetupPanel() {
        val issues = deviceReadinessIssues()
        if (issues.isEmpty()) return
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 8, 32, 8)
        }
        issues.forEach { issue ->
            content.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 12, 0, 12)
                addView(TextView(this@MainActivity).apply {
                    text = deviceIssueLabel(issue)
                    textSize = 17f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                addView(TextView(this@MainActivity).apply {
                    text = getString(deviceIssueDescription(issue))
                    textSize = 14f
                })
                addView(Button(this@MainActivity).apply {
                    text = getString(deviceIssueAction(issue))
                    minHeight = (48 * resources.displayMetrics.density).toInt()
                    setOnClickListener { openDeviceIssueSettings(issue) }
                })
            })
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.race_deck_device_setup_title)
            .setView(content)
            .setNegativeButton(android.R.string.cancel, null)
        if (issues.all { it == DeviceReadinessIssue.BATTERY_SAVER }) {
            dialog.setNeutralButton(R.string.race_deck_dismiss_for_session) { _, _ ->
                batterySaverDismissedForSession = true
                refreshRaceDeckHealth()
            }
        }
        dialog.show()
    }

    private fun deviceIssueLabel(issue: DeviceReadinessIssue): String = getString(when (issue) {
        DeviceReadinessIssue.BACKGROUND_RESTRICTED -> R.string.race_deck_background_restricted
        DeviceReadinessIssue.NOTIFICATIONS_DISABLED -> R.string.race_deck_notifications_disabled
        DeviceReadinessIssue.BATTERY_OPTIMIZED -> R.string.race_deck_battery_optimized
        DeviceReadinessIssue.BATTERY_SAVER -> R.string.race_deck_battery_saver
    })

    private fun deviceIssueDescription(issue: DeviceReadinessIssue): Int = when (issue) {
        DeviceReadinessIssue.BACKGROUND_RESTRICTED -> R.string.race_deck_background_restricted_detail
        DeviceReadinessIssue.NOTIFICATIONS_DISABLED -> R.string.race_deck_notifications_disabled_detail
        DeviceReadinessIssue.BATTERY_OPTIMIZED -> R.string.race_deck_battery_optimized_detail
        DeviceReadinessIssue.BATTERY_SAVER -> R.string.race_deck_battery_saver_detail
    }

    private fun deviceIssueAction(issue: DeviceReadinessIssue): Int = when (issue) {
        DeviceReadinessIssue.NOTIFICATIONS_DISABLED -> R.string.race_deck_enable_notifications
        DeviceReadinessIssue.BACKGROUND_RESTRICTED -> R.string.race_deck_fix_background_use
        DeviceReadinessIssue.BATTERY_OPTIMIZED -> R.string.race_deck_fix_battery_setting
        DeviceReadinessIssue.BATTERY_SAVER -> R.string.race_deck_view_settings
    }

    private fun openDeviceIssueSettings(issue: DeviceReadinessIssue) {
        when (issue) {
            DeviceReadinessIssue.NOTIFICATIONS_DISABLED -> openNotificationSettings()
            DeviceReadinessIssue.BATTERY_SAVER -> openBatterySaverSettings()
            DeviceReadinessIssue.BACKGROUND_RESTRICTED,
            DeviceReadinessIssue.BATTERY_OPTIMIZED -> startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.parse("package:$packageName"))
            )
        }
    }

    private fun colorForHealthTone(tone: RaceDeckHealthTone): Int = when (tone) {
        RaceDeckHealthTone.HEALTHY -> R.color.race_success
        RaceDeckHealthTone.WARNING -> R.color.race_warning
        RaceDeckHealthTone.CRITICAL -> R.color.race_critical
    }

    private fun errorLoadingRoute(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    }

    private fun showRestoringNavigation() {
        binding.restoringNavigationBanner.visibility = View.VISIBLE
        binding.restoringNavigationBanner.post {
            binding.restoringNavigationBanner.visibility = View.GONE
        }
    }


    override fun onStart() {
        super.onStart()
        notificationRestoreHandler.removeCallbacksAndMessages(null)
        activityStarted = true
        setActivityTitle(route)
        ensureTrackingRunning()
        startPositionProvider()
    }

    private fun startPositionProvider() {
        try {
            if (LocationPermissions.arePermissionsGranted(this)) {
                if (!this::positionProvider.isInitialized) {
                    createPositionProvider()
                }
                positionProvider.startUpdates()
            }
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        binding.raceDeckClock.removeCallbacks(raceDeckClockTicker)
        raceDeckClockTicker.run()
        registerBatterySaverReceiver()
        updateNotificationPermissionBanner()
        updateBatterySaverBanner()
        deviceReadinessHandler.removeCallbacks(refreshDeviceReadiness)
        deviceReadinessHandler.postDelayed(refreshDeviceReadiness, DEVICE_READINESS_REFRESH_INTERVAL_MS)
        lastShownPassToken = GatePassings.getLastGatePass(this, route.id)?.let(PassUploadStatus::token)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        celebrationPass?.let(::updatePassCelebrationStatus)
        if (route.isEmpty()) {
            val r = RouteLoader.loadRouteFromFile(this)
            loadRoute(r)
        }
        startPositionProvider()
        if (FirebaseAuth.getInstance().currentUser != null &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionIfNeeded()
        }
        getNextWpt()
        setGPSInterval(1)
        setMainActivityVisibilityStatus(true)
        updateLastPass()
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val showPortraitMap = landscape || shouldShowPortraitMap()
        configureRaceDeckMapLayout(landscape, showPortraitMap)
        if (showPortraitMap && supportFragmentManager.findFragmentById(R.id.map_fragment_view) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.map_fragment_view, MapFragment())
                .commit()
        }
        binding.mapFragmentView.visibility = if (showPortraitMap) View.VISIBLE else View.GONE
    }

    private fun shouldShowPortraitMap(): Boolean =
        resources.configuration.screenHeightDp >= MIN_PORTRAIT_MAP_HEIGHT_DP

    private fun configureRaceDeckMapLayout(landscape: Boolean, showPortraitMap: Boolean) {
        val navigation = binding.navigationView.layoutParams as ConstraintLayout.LayoutParams
        val map = binding.mapFragmentView.layoutParams as ConstraintLayout.LayoutParams
        val actions = binding.raceBottomActions.layoutParams as ConstraintLayout.LayoutParams
        if (!landscape && !showPortraitMap) {
            binding.navigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.race_surface_background))
            navigation.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            navigation.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            navigation.startToStart = ConstraintLayout.LayoutParams.UNSET
            navigation.endToStart = ConstraintLayout.LayoutParams.UNSET
            navigation.endToEnd = ConstraintLayout.LayoutParams.UNSET
            navigation.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            navigation.bottomToTop = ConstraintLayout.LayoutParams.UNSET
            navigation.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            actions.width = 0
            actions.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            actions.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        } else if (landscape) {
            binding.navigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.race_surface_background))
            navigation.width = 0
            navigation.height = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            navigation.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            navigation.endToStart = R.id.map_fragment_view
            navigation.endToEnd = ConstraintLayout.LayoutParams.UNSET
            navigation.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            navigation.bottomToTop = ConstraintLayout.LayoutParams.UNSET
            navigation.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

            map.width = 0
            map.height = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            map.startToStart = ConstraintLayout.LayoutParams.UNSET
            map.startToEnd = R.id.navigation_view
            map.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            map.topToBottom = ConstraintLayout.LayoutParams.UNSET
            map.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            map.bottomToTop = ConstraintLayout.LayoutParams.UNSET
            map.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            map.matchConstraintDefaultWidth = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_PERCENT
            map.matchConstraintPercentWidth = if (resources.configuration.smallestScreenWidthDp >= 600) 0.62f else 0.45f

            actions.width = 0
            actions.startToStart = R.id.navigation_view
            actions.endToEnd = R.id.navigation_view
        } else {
            binding.navigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.race_surface_background))
            val guideline = binding.portraitMapGuideline.layoutParams as ConstraintLayout.LayoutParams
            // In map mode the map should occupy all space above the persistent action bar.
            // A zero-position guideline collapses the navigation pane rather than leaving a
            // blank strip above the map.
            guideline.guidePercent = if (portraitMapExpanded) 0f else 0.70f
            binding.portraitMapGuideline.layoutParams = guideline
            navigation.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            navigation.height = 0
            navigation.startToStart = ConstraintLayout.LayoutParams.UNSET
            navigation.endToStart = ConstraintLayout.LayoutParams.UNSET
            navigation.endToEnd = ConstraintLayout.LayoutParams.UNSET
            navigation.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            navigation.bottomToTop = R.id.portraitMapGuideline
            navigation.bottomToBottom = ConstraintLayout.LayoutParams.UNSET

            map.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            map.height = 0
            map.startToEnd = ConstraintLayout.LayoutParams.UNSET
            map.startToStart = ConstraintLayout.LayoutParams.UNSET
            map.endToEnd = ConstraintLayout.LayoutParams.UNSET
            map.topToTop = ConstraintLayout.LayoutParams.UNSET
            map.topToBottom = R.id.portraitMapGuideline
            map.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            map.bottomToTop = R.id.raceBottomActions
            map.matchConstraintDefaultWidth = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD
            map.matchConstraintDefaultHeight = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD
            map.verticalWeight = 0f

            actions.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            actions.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            actions.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        }
        binding.navigationView.layoutParams = navigation
        binding.mapFragmentView.layoutParams = map
        binding.raceBottomActions.layoutParams = actions
    }

    private fun applyPortraitMapExpansion() {
        if (route.isEmpty()) return
        binding.raceDeckMetricsPanel.visibility = if (portraitMapExpanded) View.GONE else View.VISIBLE
    }

    private fun getNextWpt() {
//        nextWpt = prefs.nextWpt
        if (!route.isEmpty()) {
            prefs.nextWpt = TargetSelection.validOrFallback(
                prefs.nextWpt,
                route.elements.size,
                route.getNextNonOptionalWpt(-1),
            )
        }
        binding.routeElementSpinner.setSelection(prefs.nextWpt)
        route.elements.elementAtOrNull(prefs.nextWpt)?.let { setNextWaypointUI(it) }
    }

    private fun setNextWaypointUI(wpt: RouteElement) {
        binding.activeTargetPanel.animate().cancel()
        binding.activeTargetPanel.alpha = 0f
        binding.activeTargetPanel.animate().alpha(1f).setDuration(TARGET_CHANGE_ANIMATION_MS).start()
        val targetState = RaceDeckTargetMapper.map(
            wpt.name,
            isMark = wpt.routeElementType == RouteElementType.WAYPOINT,
            isFinish = wpt.routeElementType == RouteElementType.FINISH,
        )
        raceDeckUiState = raceDeckUiState.copy(target = targetState)
        binding.activeTargetName.text = targetState.name
        binding.activeTargetPanel.contentDescription = getString(
            R.string.race_deck_active_target_description,
            wpt.name,
        )
        if (!targetState.showsEndpoints) {
            binding.activeTargetType.setText(R.string.race_deck_next_mark)
            // The instrument must not expose proof-sector geometry for marks.
            binding.portGate.visibility = View.GONE
            binding.stbdGate.visibility = View.GONE
            binding.shortestDistanceToGate.visibility = View.GONE
            // Header and position strip replace these legacy mark-state cards.
            binding.time.visibility = View.GONE
            binding.location.visibility = View.GONE
            binding.eta.visibility = View.GONE
            binding.navScrollView.visibility = View.GONE
            binding.raceDeckMetricsPanel.visibility = View.VISIBLE
            binding.gateEndpointsPanel.visibility = View.GONE
            setEndpointInstrumentVisible(false)
        } else { //Gate
            binding.activeTargetType.setText(when (targetState.label) {
                RaceDeckTargetLabel.NEAREST_FINISH -> R.string.race_deck_nearest_on_finish
                RaceDeckTargetLabel.NEAREST_GATE -> R.string.race_deck_nearest_on_gate
                RaceDeckTargetLabel.NEXT_MARK -> R.string.race_deck_next_mark
            })
            binding.portGate.visibility = View.VISIBLE
            binding.shortestDistanceToGate.visibility = View.VISIBLE
            binding.stbdGate.visibility = View.VISIBLE
            binding.time.visibility = View.VISIBLE
            binding.location.visibility = View.VISIBLE
            binding.eta.visibility = View.VISIBLE
            binding.navScrollView.visibility = View.GONE
            binding.raceDeckMetricsPanel.visibility = View.VISIBLE
            binding.gateEndpointsPanel.visibility = View.VISIBLE
            setEndpointInstrumentVisible(true)
            binding.stbdGate.setData("-----")
            binding.stbdGate.setUnits(getString(R.string.nm))
            binding.stbdGate.setLabel(getString(R.string.race_deck_stbd))
            binding.portGate.setLabel(getString(R.string.race_deck_port))
            binding.shortestDistanceToGate.setLabel(
                if (wpt.routeElementType == RouteElementType.FINISH) {
                    getString(R.string.race_deck_nearest_on_finish)
                } else {
                    getString(R.string.race_deck_nearest_on_gate)
                }
            )
        }
    }

    /** Re-anchor the legacy metric grid when the endpoint row is absent for a mark. */
    private fun setEndpointInstrumentVisible(visible: Boolean) {
        val params = binding.cogsog.layoutParams as ConstraintLayout.LayoutParams
        if (visible) {
            params.topToTop = ConstraintLayout.LayoutParams.UNSET
            params.topToBottom = R.id.portGate
        } else {
            params.topToBottom = ConstraintLayout.LayoutParams.UNSET
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        }
        binding.cogsog.layoutParams = params
    }

    override fun onPause() {
        activityResumed = false
        binding.raceDeckClock.removeCallbacks(raceDeckClockTicker)
        deviceReadinessHandler.removeCallbacks(refreshDeviceReadiness)
        gpsWarningHandler.removeCallbacks(refreshGpsWarningAge)
        gpsLossAlertHandler.removeCallbacks(playProlongedGpsAlert)
        unregisterBatterySaverReceiver()
        dismissPassCelebration()
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
        activityStarted = false
        // The foreground service continues recording while the activity is in the background.
        stopPositionProvider()
        super.onStop()
        if (prefs.status) {
            notificationRestoreHandler.postDelayed(
                { TrackingService.refreshNotificationIfRunning() },
                NOTIFICATION_RESTORE_DELAY_MS,
            )
        }
    }

    private fun setOnBackPressed(){
        val callback = object : OnBackPressedCallback(true /* enabled by default */) {
            override fun handleOnBackPressed() {
                confirmCloseApp()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun showStopTrackingConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.race_deck_stop_tracking_title)
            .setMessage(R.string.race_deck_stop_tracking_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.race_deck_stop_tracking_confirm) { _, _ ->
                prefs.status = false
                stopTrackingService()
            }
            .show()
    }

    private fun showPreRaceReadinessReview() {
        val issueCount = deviceReadinessIssues().size
        if (issueCount == 0) return
        AlertDialog.Builder(this)
            .setTitle(R.string.race_deck_pre_race_title)
            .setMessage(resources.getQuantityString(
                R.plurals.race_deck_device_issue_count,
                issueCount,
                issueCount,
            ))
            .setPositiveButton(R.string.race_deck_review_settings) { _, _ -> showDeviceSetupPanel() }
            .setNegativeButton(R.string.race_deck_continue_anyway, null)
            .show()
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
        configureRaceMenu(menu)
        return true
    }

    private fun configureRaceMenu(menu: Menu) {
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
    }

    private fun showRaceMoreMenu() {
        PopupMenu(this, binding.raceMoreAction).apply {
            menuInflater.inflate(R.menu.main, menu)
            configureRaceMenu(menu)
            setOnMenuItemClickListener { item -> onOptionsItemSelected(item) }
            show()
        }
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
                        RouteLoader.loadRouteFromString(this, it) { loadedRoute ->
                            loadRoute(loadedRoute, showReadinessReview = true)
                        }
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
        lastGpsFixTimeMillis = Date().time
        latestRaceDeckLocation = location
        val wpt = route.elements.elementAtOrNull(prefs.nextWpt)
        binding.viewmodel = LocationViewModel(location, wpt, sharedPreferences)
        setUiForGPS(true)
        updateRaceDeckHealth(gpsAvailable = true)
        if (wpt != null) {
            binding.location.setTextColor(if (wpt.isInProofArea(location)) Color.GREEN else Color.BLACK)
        } else {
            binding.location.setTextColor(Color.BLACK)
        }
        val interval = (prefs.GPSInterval.toLong()) * 4000 //After four times interval
        delayedHandler.removeCallbacksAndMessages(null)
        delayedHandler.postDelayed({
            setUiForGPS(false)
            updateRaceDeckHealth(gpsAvailable = false)
        }, interval)
        if (prefs.tracking) {
            binding.lastSend.visibility = View.GONE
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
        binding.lastSend.visibility = View.GONE

//        binding.location.setTextColor(Color.BLACK)
        if (isEmpty) {
            binding.routeElementSpinner.visibility = View.INVISIBLE
            binding.nextWptHeader.text = getString(R.string.no_route_loaded)
            binding.portGate.visibility = View.GONE
            binding.stbdGate.visibility = View.GONE
            binding.shortestDistanceToGate.visibility = View.GONE
            binding.vmg.visibility = View.GONE
            binding.eta.visibility = View.GONE
            binding.activeTargetPanel.visibility = View.GONE
            binding.raceDeckMetricsPanel.visibility = View.GONE
            binding.navScrollView.visibility = View.GONE
            binding.gateEndpointsPanel.visibility = View.GONE
            binding.raceSelectCourseButton.visibility = View.VISIBLE
        } else {
            binding.routeElementSpinner.visibility = View.VISIBLE
            binding.nextWptHeader.text = getString(R.string.next_waypoint_gate)
            binding.vmg.visibility = View.VISIBLE
            binding.eta.visibility = View.VISIBLE
            binding.activeTargetPanel.visibility = View.VISIBLE
            binding.raceDeckMetricsPanel.visibility = View.VISIBLE
            binding.nextWptHeader.setText(R.string.race_deck_active_target)
            binding.raceSelectCourseButton.visibility = View.GONE
        }
        setUiForLogin(FirebaseAuth.getInstance().currentUser)
    }

    private fun setUiForGPS(isAvailable: Boolean) {
        gpsWarningHandler.removeCallbacks(refreshGpsWarningAge)
        gpsLossAlertHandler.removeCallbacks(playProlongedGpsAlert)
        updateGpsWarningMessage()
        binding.gpsWarningBanner.visibility = if (
            !isAvailable && FirebaseAuth.getInstance().currentUser != null
        ) View.VISIBLE else View.GONE
        if (!isAvailable && binding.gpsWarningBanner.isVisible) {
            binding.silenceGpsAlerts.visibility = if (gpsAlertsSilencedForSession) View.GONE else View.VISIBLE
            gpsWarningHandler.postDelayed(refreshGpsWarningAge, GPS_WARNING_REFRESH_INTERVAL_MS)
            if (!gpsAlertsSilencedForSession && !gpsOutageAlertPlayed) {
                gpsLossAlertHandler.postDelayed(
                    playProlongedGpsAlert,
                    GpsAlertPolicy.alertDelayMillis((prefs.GPSInterval.toLong()) * 4_000),
                )
            }
        } else if (isAvailable) {
            gpsAlertsSilencedForSession = false
            gpsOutageAlertPlayed = false
        }
        if ((isAvailable) && (FirebaseAuth.getInstance().currentUser != null)) {
            binding.portGate.setTextColor(Color.BLACK)
            binding.stbdGate.setTextColor(Color.BLACK)
            binding.cogsog.setTextColor(Color.BLACK)
            binding.location.setTextColor(Color.BLACK)
            binding.time.setTextColor(Color.BLACK)
            binding.shortestDistanceToGate.setTextColor(Color.BLACK)
            binding.vmg.setTextColor(Color.BLACK)
            binding.eta.setTextColor(Color.BLACK)
        } else {
            binding.portGate.setTextColor(Color.RED)
            binding.stbdGate.setTextColor(Color.RED)
            binding.cogsog.setTextColor(Color.RED)
            binding.location.setTextColor(Color.RED)
            binding.time.setTextColor(Color.RED)
            binding.shortestDistanceToGate.setTextColor(Color.RED)
            binding.vmg.setTextColor(Color.RED)
            binding.eta.setTextColor(Color.RED)
            binding.location.setLabel("Accuracy - Unknown")
        }
    }

    private fun updateGpsWarningMessage() {
        binding.gpsWarningMessage.text = if (lastGpsFixTimeMillis > 0) {
            getString(
                R.string.race_deck_gps_lost_since,
                ((Date().time - lastGpsFixTimeMillis).coerceAtLeast(0) / 1_000),
            )
        } else {
            getString(R.string.race_deck_gps_lost_message)
        }
    }

    private fun setUiForLogin(user: FirebaseUser?) {
        if (user == null) {
            prefs.status = false
            binding.loginBtn.visibility = View.VISIBLE
        } else {
            binding.loginBtn.visibility = View.GONE
        }
        updateRaceDeckHealth(gpsAvailable = false)
        invalidateOptionsMenu()
    }

    private fun showTargetSheet() {
        if (route.isEmpty()) return

        val tablet = resources.configuration.smallestScreenWidthDp >= 600
        val dialog: Dialog = if (tablet) Dialog(this) else BottomSheetDialog(this)
        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 16)
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.race_surface_panel))
        }
        sheet.addView(TextView(this).apply {
            text = getString(R.string.race_deck_targets)
            textSize = 32f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.race_text_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        sheet.addView(TextView(this).apply {
            text = getString(R.string.race_deck_target_hint)
            textSize = 17f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.race_text_secondary))
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { setMargins(0, 4, 0, 18) })
        val passes = GatePassings.getCurrentRouteGatePassings(this, route.id)
        val list = android.widget.ListView(this).apply {
            divider = null
            setSelector(android.R.color.transparent)
            clipToPadding = false
            adapter = object : android.widget.ArrayAdapter<RouteElement>(
                this@MainActivity,
                R.layout.target_sheet_item,
                route.elements,
            ) {
                override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                    val row = convertView ?: layoutInflater.inflate(
                        R.layout.target_sheet_item, parent, false
                    )
                    val target = getItem(position) ?: return row
                    val pass = passes.getLatestGatePassForGate(target.id)
                    val isCurrent = position == prefs.nextWpt
                    val card = row.findViewById<com.google.android.material.card.MaterialCardView>(R.id.targetCard)
                    card.setCardBackgroundColor(ContextCompat.getColor(
                        this@MainActivity,
                        if (isCurrent) R.color.race_target_active_background else R.color.race_surface_panel,
                    ))
                    card.strokeColor = ContextCompat.getColor(
                        this@MainActivity,
                        if (isCurrent) R.color.race_target_active_stroke else R.color.race_surface_panel_alt,
                    )
                    card.strokeWidth = (if (isCurrent) 2 else 1) * resources.displayMetrics.density.toInt()

                    row.findViewById<TextView>(R.id.targetNumber).text = (position + 1).toString()
                    row.findViewById<TextView>(R.id.targetName).text = target.name
                    row.findViewById<TextView>(R.id.targetType).text = targetSheetType(target)

                    val distance = row.findViewById<TextView>(R.id.targetDistance)
                    val current = row.findViewById<TextView>(R.id.targetCurrent)
                    val passedIcon = row.findViewById<android.widget.ImageView>(R.id.targetPassedIcon)
                    val passedAt = row.findViewById<TextView>(R.id.targetPassedAt)
                    val upload = row.findViewById<TextView>(R.id.targetUploadStatus)
                    if (pass == null) {
                        distance.visibility = View.VISIBLE
                        distance.text = targetSheetDistance(target) ?: "—"
                        current.visibility = if (isCurrent) View.VISIBLE else View.GONE
                        passedIcon.visibility = View.GONE
                        passedAt.visibility = View.GONE
                        upload.visibility = View.GONE
                    } else {
                        distance.visibility = View.GONE
                        current.visibility = View.GONE
                        passedIcon.visibility = View.VISIBLE
                        passedAt.visibility = View.VISIBLE
                        passedAt.text = getString(
                            R.string.race_deck_target_passed_at,
                            RaceDeckFormatter.clock(pass.time.time),
                        )
                        upload.visibility = View.VISIBLE
                        when (PassUploadStatus.get(this@MainActivity, pass)) {
                            PassUploadStatus.State.UPLOADED -> {
                                upload.setText(R.string.race_deck_target_uploaded)
                                upload.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.race_success))
                            }
                            PassUploadStatus.State.PENDING -> {
                                upload.setText(R.string.race_deck_target_pending)
                                upload.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.race_text_secondary))
                            }
                            PassUploadStatus.State.FAILED -> {
                                upload.setText(R.string.race_deck_target_failed)
                                upload.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.race_critical))
                            }
                        }
                    }
                    return row
                }
            }
            setOnItemClickListener { _, _, position, _ ->
                setNextWpt(position)
                binding.routeElementSpinner.setSelection(position)
                getNextWpt()
                dialog.dismiss()
            }
        }
        sheet.addView(list, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ))
        dialog.setContentView(sheet)
        dialog.setOnShowListener {
            if (dialog is BottomSheetDialog) {
                val bottomSheet = dialog.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                ) ?: return@setOnShowListener
                bottomSheet.layoutParams.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                dialog.window?.apply {
                    setGravity(Gravity.END)
                    setLayout(
                        (360 * resources.displayMetrics.density).toInt(),
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            }
        }
        dialog.show()
    }

    private fun targetSheetDistance(target: RouteElement): String? =
        latestRaceDeckLocation?.let { location ->
            val targetDistance = if (target.routeElementType == RouteElementType.WAYPOINT) {
                getDistance(location, target.portWpt)
            } else {
                pointToLineDist(location, target.portWpt, target.stbdWpt)
            }
            getDistString(targetDistance)
        }

    private fun targetSheetType(target: RouteElement): String = when (target.routeElementType) {
        RouteElementType.WAYPOINT -> "Mark"
        RouteElementType.FINISH -> "Finish"
        RouteElementType.GATE, RouteElementType.START -> "Gate"
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences == null) return
        if (key == SettingsFragment.KEY_STATUS) {
            if (!prefs.status) {
                stopTrackingService()
            }
            refreshRaceDeckHealth()
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
            refreshRaceDeckHealth()
        } else if (key == SettingsFragment.KEY_GATE_PASSES) {
            updateLastPass()
            val pass = GatePassings.getLastGatePass(this, route.id)
            val token = pass?.let(PassUploadStatus::token)
            if (pass != null && token != lastShownPassToken) {
                lastShownPassToken = token
                val isFinish = route.elements.any {
                    it.id == pass.gateId && it.routeElementType == RouteElementType.FINISH
                }
                runOnUiThread {
                    if (activityResumed) showPassCelebration(pass, isFinish)
                }
            }
        } else if (celebrationPass?.let { key == PassUploadStatus.key(it) } == true) {
            celebrationPass?.let { pass ->
                runOnUiThread {
                    if (celebrationPass == pass && binding.passCelebrationCard.isVisible) {
                        updatePassCelebrationStatus(pass)
                    }
                }
            }
        } else if (key == SettingsFragment.KEY_BOAT_NAME) {
            setActivityTitle(route)
        }
    }

    private fun showPassCelebration(pass: GatePassing, isFinish: Boolean) {
        celebrationRoute = route
        celebrationPass = pass
        val accent = ContextCompat.getColor(
            this, if (isFinish) R.color.pass_finish_accent else R.color.pass_gate_accent
        )
        binding.passCelebrationCard.strokeColor = accent
        binding.passCelebrationIcon.setImageResource(
            if (isFinish) R.drawable.ic_finish_celebration else R.drawable.ic_gate_celebration
        )
        binding.passCelebrationTitle.setText(
            if (isFinish) R.string.finish_pass_notification_title else R.string.gate_pass_notification_title
        )
        binding.passCelebrationName.text = pass.gateName
        binding.passCelebrationEventDetails.text = RaceDeckFormatter.passDetails(
            pass.time.time,
            pass.latitude,
            pass.longitude
        )
        val nextTarget = route.elements.elementAtOrNull(prefs.nextWpt)?.name
        binding.passCelebrationNextTarget.visibility = if (
            !nextTarget.isNullOrBlank() && nextTarget != pass.gateName
        ) View.VISIBLE else View.GONE
        binding.passCelebrationNextTarget.text = nextTarget?.let {
            getString(R.string.pass_next_target, it)
        }
        updatePassCelebrationStatus(pass)
        binding.passCelebrationAction.setText(R.string.race_deck_view_targets)
        binding.passCelebrationAction.backgroundTintList = ColorStateList.valueOf(accent)

        binding.passCelebrationCard.removeCallbacks(hidePassCelebration)
        binding.passCelebrationCard.animate().cancel()
        binding.passCelebrationCard.alpha = 0f
        binding.passCelebrationCard.translationY = -12 * resources.displayMetrics.density
        binding.passCelebrationCard.visibility = View.VISIBLE
        binding.passCelebrationCard.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        binding.passCelebrationCard.animate().alpha(1f).translationY(0f).setDuration(220).start()
        binding.passCelebrationCard.postDelayed(hidePassCelebration, 10000)
    }

    private fun dismissPassCelebration() {
        celebrationRoute = null
        celebrationPass = null
        binding.passCelebrationCard.removeCallbacks(hidePassCelebration)
        binding.passCelebrationCard.animate().cancel()
        binding.passCelebrationCard.visibility = View.GONE
    }

    private fun updatePassCelebrationStatus(pass: GatePassing) {
        binding.passCelebrationUploaded.setText(when (PassUploadStatus.get(this, pass)) {
            PassUploadStatus.State.PENDING -> R.string.pass_upload_pending
            PassUploadStatus.State.UPLOADED -> R.string.pass_uploaded_checked
            PassUploadStatus.State.FAILED -> R.string.pass_upload_failed_status
        })
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

    private fun confirmCloseApp() {
        AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.close_app_title)
            .setMessage(R.string.close_app_message)
            .setPositiveButton(R.string.close_app_confirm) { _, _ -> closeApp() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun closeApp() {
        prefs.status = false
        stopTrackingService()
        finishAndRemoveTask()
    }

    private fun ensureTrackingRunning() {
        updateNotificationPermissionBanner()
        updateBatterySaverBanner()
        if (FirebaseAuth.getInstance().currentUser == null) {
            prefs.status = false
            if (activityStarted) stopTrackingService()
            return
        }
        prefs.status = true
        if (activityStarted) {
            startTrackingService(checkPermission = true, initialPermission = false)
            if (activityResumed && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionIfNeeded()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (notificationPermissionRequestInFlight || notificationWarningShown) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionRequestInFlight = true
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSIONS_REQUEST_NOTIFICATIONS)
        } else if (!notificationsVisible()) {
            showNotificationSettingsDialog()
        }
    }

    private fun notificationsVisible(): Boolean {
        return TrackingService.canShowTrackingNotification(this)
    }

    private fun updateNotificationPermissionBanner() {
        val hidden = !notificationsVisible()
        if (!hidden) notificationWarningShown = false
        binding.notificationPermissionBanner.visibility = View.GONE
        refreshRaceDeckHealth()
    }

    private fun openNotificationSettings() {
        val settingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.parse("package:$packageName"))
        }
        startActivity(settingsIntent)
    }

    private fun updateBatterySaverBanner() {
        binding.batterySaverBanner.visibility = View.GONE
        if (!getSystemService(PowerManager::class.java).isPowerSaveMode) {
            batterySaverDismissedForSession = false
        }
        refreshRaceDeckHealth()
    }

    private fun registerBatterySaverReceiver() {
        if (batterySaverReceiverRegistered) return
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(batterySaverReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(batterySaverReceiver, filter)
        }
        batterySaverReceiverRegistered = true
    }

    private fun unregisterBatterySaverReceiver() {
        if (!batterySaverReceiverRegistered) return
        unregisterReceiver(batterySaverReceiver)
        batterySaverReceiverRegistered = false
    }

    private fun openBatterySaverSettings() {
        try {
            startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun showNotificationSettingsDialog() {
        if (notificationWarningShown || isFinishing) return
        notificationWarningShown = true
        AlertDialog.Builder(this)
            .setTitle(R.string.tracking_notification_hidden_title)
            .setMessage(R.string.tracking_notification_hidden_message)
            .setPositiveButton(R.string.tracking_notification_settings) { _, _ -> openNotificationSettings() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
        if (requestCode == PERMISSIONS_REQUEST_NOTIFICATIONS) {
            notificationPermissionRequestInFlight = false
            updateNotificationPermissionBanner()
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED && notificationsVisible()) {
                ensureTrackingRunning()
            } else {
                showNotificationSettingsDialog()
            }
        } else if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            var granted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                    break
                }
            }
            Log.d(TAG, "Permissions granted: $granted")
            if (granted) {
                startTrackingService(false, true)
                startPositionProvider()
                if (activityResumed) requestNotificationPermissionIfNeeded()
            } else {
                prefs.status = false
                Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val ACTION_CONFIRM_STOP_TRACKING = "in.avimarine.waypointracing.CONFIRM_STOP_TRACKING"
        private const val PERMISSIONS_REQUEST_LOCATION = 2
        private const val PERMISSIONS_REQUEST_NOTIFICATIONS = 3
        private const val ALARM_MANAGER_INTERVAL = 15000
        private const val NOTIFICATION_RESTORE_DELAY_MS = 300L
        private const val DEVICE_READINESS_REFRESH_INTERVAL_MS = 15_000L
        private const val GPS_WARNING_REFRESH_INTERVAL_MS = 1_000L
        private const val TARGET_CHANGE_ANIMATION_MS = 200L
        private const val MIN_PORTRAIT_MAP_HEIGHT_DP = 800
    }

}
