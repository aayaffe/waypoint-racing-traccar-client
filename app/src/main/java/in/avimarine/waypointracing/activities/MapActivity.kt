package `in`.avimarine.waypointracing.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.activities.ui.main.MapFragment

class MapActivity : AppCompatActivity() {

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