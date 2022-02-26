package `in`.avimarine.waypointracing.activities

import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.databinding.ActivityRouteBinding
import `in`.avimarine.waypointracing.route.GatePassing
import `in`.avimarine.waypointracing.route.GatePassings
import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.ui.RouteElementConcat
import `in`.avimarine.waypointracing.ui.RouteElementFullAdapter
import `in`.avimarine.waypointracing.utils.timeStamptoDateString
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView

class RouteActivity : AppCompatActivity() {

    private var route = Route.emptyRoute()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivityRouteBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
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
            binding.routeDetailsBox.visibility = View.GONE
            binding.noRouteHeader.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            binding.routeDetailsBox.visibility = View.VISIBLE
            binding.noRouteHeader.visibility = View.GONE
        }

        setDetailsBox(route)

    }

    private fun setDetailsBox(route: Route) {
        binding.idValue.text = route.id
        binding.lastupdateValue.text = timeStamptoDateString(route.lastUpdate.time)
        binding.organizerValue.text = route.organizing
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
        route.elements.forEachIndexed{ index, re ->
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