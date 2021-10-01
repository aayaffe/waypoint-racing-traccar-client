package `in`.avimarine.waypointracing.activities

import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.TrackingService
import `in`.avimarine.waypointracing.route.GatePassing
import `in`.avimarine.waypointracing.route.GatePassings
import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.route.RouteElement
import `in`.avimarine.waypointracing.ui.RouteElementConcat
import `in`.avimarine.waypointracing.ui.RouteElementFullAdapter
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView

class RouteActivity : AppCompatActivity() {

    private var route: Route? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)

        parseRouteIntent(intent)
        val routeElementAdapter = RouteElementFullAdapter ()

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.adapter = routeElementAdapter

        routeElementAdapter.submitList(createRecList())

    }

    private fun createRecList(): MutableList<RouteElementConcat>? {
        val ret = mutableListOf<RouteElementConcat>()
        val s = sharedPreferences.getString(MainFragment.KEY_GATE_PASSES, "")
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
        route = i?.getParcelableExtra("route")
        Log.d(TAG, "Recieved route: " + route.toString())
    }

}