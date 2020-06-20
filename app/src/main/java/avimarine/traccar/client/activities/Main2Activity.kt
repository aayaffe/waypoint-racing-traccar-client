package avimarine.traccar.client.activities

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import avimarine.traccar.client.*
import avimarine.traccar.client.PositionProvider.PositionListener
import avimarine.traccar.client.route.Route
import avimarine.traccar.client.route.Waypoint
import kotlinx.android.synthetic.main.activity_main2.*
import java.util.*

class Main2Activity : AppCompatActivity(), PositionListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private val magnetic = false
    private lateinit var positionProvider: PositionProvider
    private var nextWpt : Waypoint? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val PERMISSIONS_REQUEST_LOCATION = 2


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        sharedPreferences = getDefaultSharedPreferences(this)
        val route = createTestRoute()
        populateRouteElementSpinner(route)
        positionProvider = PositionProviderFactory.create(this, this)
        setButton(sharedPreferences.getBoolean(MainFragment.KEY_STATUS, false))
        if (sharedPreferences.getBoolean(MainFragment.KEY_STATUS, false)) {
            startTrackingService(true, false)
        }
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
        val l1 = Location("")
        l1.latitude = 30.0
        l1.longitude = 30.0
        val l2 = Location("")
        l2.latitude = 31.0
        l2.longitude = 31.0
        val l3 = Location("")
        l3.latitude = 32.0
        l3.longitude = 32.0

        val wpt1 = Waypoint("wpt1", l1, false)
        val wpt2 = Waypoint("wpt2", l2, false)
        val wpt3 = Waypoint("wpt3", l3, false)

        return Route("TestEvent", Calendar.getInstance().time, arrayListOf(wpt1,wpt2,wpt3),Calendar.getInstance().time)

    }

    fun populateRouteElementSpinner(route: Route){
        val adapter = ArrayAdapter(this,
                R.layout.waypoint_spinner_item, route.elements)
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
        }
        return super.onOptionsItemSelected(item)
    }

    fun startButtonClick(view: View) {
        val sharedPref: SharedPreferences = getDefaultSharedPreferences(this)
        val checked = sharedPref.getBoolean("status", true)
        with (sharedPref.edit()) {
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
        } else{
            Log.w(TAG, "Error: position is null")
        }

    }

    private fun updateUI(position: Position) {
        if (nextWpt!=null) {
            val distWptPort = getDistance(position, nextWpt!!.portWpt)
            val distWptStbd = getDistance(position, nextWpt!!.stbdWpt)
            val dirWptPort = getDirection(position, nextWpt!!.portWpt)
            val dirWptStbd = getDirection(position, nextWpt!!.stbdWpt)
            portBearing.text = getDirString(dirWptPort,magnetic,false,position,position.time.time)
            stbdBearing.text = getDirString(dirWptStbd,magnetic,false,position,position.time.time)
            portDistance.text = getDistString(distWptPort)
            stbdDistance.text = getDistString(distWptStbd)
        }
        cog.text = getDirString(position.course,magnetic,false,position,position.time.time)
        sog.text = getSpeedString(position.speed) //TODO Fix units in the conversion (probably knots)

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
        if (isRunning){
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