package `in`.avimarine.waypointracing.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import `in`.avimarine.androidutils.ScreenShot
import `in`.avimarine.androidutils.TAG
import `in`.avimarine.androidutils.timeStampToDateString
import `in`.avimarine.waypointracing.BuildConfig
import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.databinding.ActivityRouteBinding
import `in`.avimarine.waypointracing.route.EventType
import `in`.avimarine.waypointracing.route.GatePassings
import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.ui.RouteElementConcat
import `in`.avimarine.waypointracing.ui.RouteElementFullAdapter

class RouteActivity : AppCompatActivity(),  SharedPreferences.OnSharedPreferenceChangeListener {

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

        val recyclerView: RecyclerView = findViewById(R.id.route_recycler_view)
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
        setPointsDetails(route)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_route, menu)
        return true
    }

    private fun setPointsDetails(route: Route) {
        if (route.eventType == EventType.TREASUREHUNT) {
            val points = calculatePoints(route)
            binding.pointsValue.text = points.toInt().toString()
            binding.pointsValue.visibility = View.VISIBLE
            binding.pointsTitle.visibility = View.VISIBLE
        } else{
            binding.pointsValue.visibility = View.GONE
            binding.pointsTitle.visibility = View.GONE
        }
    }

    private fun calculatePoints(route: Route): Double {
        var ret = 0.0
        val gp = GatePassings.getCurrentRouteGatePassings(applicationContext, route.id)
        for (re in route.elements){
            if (gp.passes.find {it.gateId == re.id} != null) {
                ret += re.points
            }
        }
        return ret

    }

    private fun setDetailsBox(route: Route) {
        binding.lastupdateValue.text = timeStampToDateString(route.lastUpdate.time)
        binding.organizerValue.text = route.organizing
        binding.boatnameValue.text = sharedPreferences.getString(SettingsFragment.KEY_BOAT_NAME, "Undefined")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.share_route_results_menu_action -> {
                return shareRouteResults()
            }
            R.id.send_screenshot_menu_action -> {
                val bitmap = ScreenShot.takeScreenshot(this)
                ScreenShot.sendSnapshot(bitmap, BuildConfig.APPLICATION_ID, this)
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun shareRouteResults(): Boolean {
        val results = createRecList()
        var text = "Results for ${route.eventName}, *${sharedPreferences.getString(SettingsFragment.KEY_BOAT_NAME,"Unknown Boat")}*: \n"
        for (r in results){
            text = if (r.gp?.time == null) {
                val t = "* " + r.re.name + ": " + "Never" + " " + (if (r.gp?.mock == true) "M" else "") + "\n"
                text.plus(t)
            }else {
                val t = "* " + r.re.name + ": " + timeStampToDateString(r.gp.time.time) + " " + (if (r.gp.mock) "M" else "") + "\n"
                text.plus(t)
            }
        }
        text = text.plus(text.hashCode().toString().takeLast(3))
        text = "DO NOT EDIT THIS MESSAGE\n ===== \n$text\n ===== \n"
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Route results")
        shareIntent.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(shareIntent, "Share route results"))
        return true
    }

    private fun createRecList(): MutableList<RouteElementConcat> {
        val ret = mutableListOf<RouteElementConcat>()
        val gp = GatePassings.getCurrentRouteGatePassings(applicationContext, route.id)
        route.elements.forEach { re ->
            val rec = RouteElementConcat(re,gp.getLatestGatePassForGate(re.id))
            ret.add(rec)
        }
        return ret

    }

    private fun parseRouteIntent(i: Intent?){
        val r = i?.let { IntentCompat.getParcelableExtra(it, "route", Route::class.java) }
        if (r!=null){
            route = r
        }
        Log.d(TAG, "Received route: $route")
    }

    private fun setTitle(title: String, subTitle: String) {
        val ab = supportActionBar
        ab?.title = title
        ab?.subtitle = subTitle
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences == null) return
        if (key == SettingsFragment.KEY_GATE_PASSES) {
            val recyclerView: RecyclerView = findViewById(R.id.route_recycler_view)
            val routeElementAdapter = recyclerView.adapter as RouteElementFullAdapter
            routeElementAdapter.submitList(createRecList())
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
}