package `in`.avimarine.waypointracing.activities

import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.route.GatePassing
import `in`.avimarine.waypointracing.route.GatePassings
import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.ui.RouteElementConcat
import `in`.avimarine.waypointracing.ui.RouteElementFullAdapter
import `in`.avimarine.waypointracing.utils.timeStamptoDateString
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_route.*

class RouteActivity : AppCompatActivity() {

    private var route = Route.emptyRoute()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)// showing the back button in action bar

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)

        parseRouteIntent(intent)
        val routeElementAdapter = RouteElementFullAdapter()

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.adapter = routeElementAdapter

        routeElementAdapter.submitList(createRecList())

        setTitle(route.eventName, "")

        if (route.isEmpty()){
            recyclerView.visibility = View.GONE
            route_details_box.visibility = View.GONE
            no_route_header.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            route_details_box.visibility = View.VISIBLE
            no_route_header.visibility = View.GONE
        }

        setDetailsBox(route)

    }

    private fun setDetailsBox(route: Route) {
        id_value.text = route.id
        lastupdate_value.text = timeStamptoDateString(route.lastUpdate.time)
        organizer_value.text = route.organizing
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun createRecList(): MutableList<RouteElementConcat> {
        val ret = mutableListOf<RouteElementConcat>()
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
        route?.elements?.forEachIndexed{ index, re ->
            val rec = RouteElementConcat(re,getLatestGatePass(index,gp))
            ret.add(rec)
        }
        return ret

    }

    private fun getLatestGatePass(ordinal: Int, gps: GatePassings): GatePassing? {
        val thisGps = gps.passes.filter{ it.gateId == ordinal}
        return thisGps.maxByOrNull { it -> it.time.time }
    }

    private fun parseRouteIntent(i: Intent?){
        val r : Route? = i?.getParcelableExtra("route")
        if (r!=null){
            route = r
        }
        Log.d(TAG, "Recieved route: " + route.toString())
    }

    private fun setTitle(title: String, subTitle: String) {
        val ab = supportActionBar
        ab?.setTitle(title)
        ab?.setSubtitle(subTitle)
    }



}