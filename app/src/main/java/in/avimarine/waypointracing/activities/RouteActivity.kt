package `in`.avimarine.waypointracing.activities

import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.databinding.ActivityRouteBinding
import `in`.avimarine.waypointracing.route.EventType
import `in`.avimarine.waypointracing.route.GatePassing
import `in`.avimarine.waypointracing.route.GatePassings
import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.ui.RouteElementConcat
import `in`.avimarine.waypointracing.ui.RouteElementFullAdapter
import `in`.avimarine.waypointracing.utils.ScreenShot
import `in`.avimarine.waypointracing.utils.timeStamptoDateString
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import eu.bolt.screenshotty.ScreenshotActionOrder
import eu.bolt.screenshotty.ScreenshotManager
import eu.bolt.screenshotty.ScreenshotManagerBuilder

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
        setPointsDetails(route)
        screenshotManager = ScreenshotManagerBuilder(this)
            .withCustomActionOrder(ScreenshotActionOrder.pixelCopyFirst()) //optional, ScreenshotActionOrder.pixelCopyFirst() by default
            .withPermissionRequestCode(REQUEST_SCREENSHOT_PERMISSION) //optional, 888 by default
            .build()
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
        val gp = GatePassings.getGatePassings(applicationContext)
        for (re in route.elements){
            if (gp.passes.find {it.gateId == re.id} != null) {
                ret += re.points
            }
        }
        return ret

    }

    private fun setDetailsBox(route: Route) {
        binding.idValue.text = route.id
        binding.lastupdateValue.text = timeStamptoDateString(route.lastUpdate.time)
        binding.organizerValue.text = route.organizing
        binding.boatnameValue.text = sharedPreferences.getString(SettingsFragment.KEY_BOAT_NAME, "Undefined")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.send_screenshot_menu_action -> {
                takeScreenshot()
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun createRecList(): MutableList<RouteElementConcat> {
        val ret = mutableListOf<RouteElementConcat>()
        val gp = GatePassings.getGatePassings(applicationContext)
        route.elements.forEachIndexed{ index, re ->
            val rec = RouteElementConcat(re,getLatestGatePass(index,gp))
            ret.add(rec)
        }
        return ret

    }

    private fun getLatestGatePass(ordinal: Int, gps: GatePassings): GatePassing? {
        val thisGps = gps.passes.filter{ it.gateId == ordinal}
        return thisGps.maxByOrNull { it.time.time }
    }

    private fun parseRouteIntent(i: Intent?){
        val r : Route? = i?.getParcelableExtra("route")
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


    private val REQUEST_SCREENSHOT_PERMISSION: Int = 12344321
    private lateinit var screenshotManager : ScreenshotManager


    @Deprecated("Deprecated in Java")
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

}