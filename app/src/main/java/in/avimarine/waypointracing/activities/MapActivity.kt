package `in`.avimarine.waypointracing.activities

import android.os.Bundle
import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.activities.fragments.MapFragment

class MapActivity : EdgeToEdgeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MapFragment.newInstance())
                .commitNow()
        }
    }
}
