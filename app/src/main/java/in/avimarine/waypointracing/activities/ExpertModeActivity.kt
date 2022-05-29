package `in`.avimarine.waypointracing.activities

import `in`.avimarine.waypointracing.ExpertViewModel
import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.databinding.ActivityExpertModeBinding
import `in`.avimarine.waypointracing.route.Route
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi

class ExpertModeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExpertModeBinding
    private var route = Route.emptyRoute()
    @OptIn(ExperimentalCoroutinesApi::class)
    private val viewModel: ExpertViewModel by lazy { ViewModelProvider(this)[ExpertViewModel::class.java] }


    //    private val db: OnlineDatabase = FirestoreDatabase()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpertModeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<SelectBoatFragment>(R.id.fragment_container_view)
            }
        }
        parseRouteIntent(intent)
        viewModel.updateRoute(route)
    }
    private fun parseRouteIntent(i: Intent?){
        val r : Route? = i?.getParcelableExtra("route")
        if (r!=null){
            route = r
        }
        Log.d(TAG, "Received route: $route")
    }
}