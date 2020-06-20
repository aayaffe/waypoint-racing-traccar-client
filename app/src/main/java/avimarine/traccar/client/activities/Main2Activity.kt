package avimarine.traccar.client.activities

import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import avimarine.traccar.client.*
import avimarine.traccar.client.PositionProvider.PositionListener
import avimarine.traccar.client.route.Route
import avimarine.traccar.client.route.Waypoint
import kotlinx.android.synthetic.main.activity_main2.*
import java.util.*

class Main2Activity : AppCompatActivity(), PositionListener  {

    private val magnetic = false
    private lateinit var positionProvider: PositionProvider
    private var nextWpt : Waypoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        val route = createTestRoute()
        populateRouteElementSpinner(route)
        positionProvider = PositionProviderFactory.create(this, this)
    }

    override fun onStart() {
        super.onStart()
        try {
            positionProvider.startUpdates()
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
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
}