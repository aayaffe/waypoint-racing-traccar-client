package `in`.avimarine.waypointracing.activities.ui.main

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.PuckBearingSource
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationLongClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.locationcomponent.location2
import `in`.avimarine.androidutils.TAG
import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.TrackingService
import `in`.avimarine.waypointracing.activities.SettingsFragment
import `in`.avimarine.waypointracing.activities.StatusActivity
import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.utils.RouteParser.Companion.parseRoute
import org.json.JSONObject

class MapFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    var mapView: MapView? = null
    var route: Route = Route.emptyRoute()
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        fun newInstance() = MapFragment()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        mapView = view.findViewById(R.id.mapView)
        mapView?.getMapboxMap()?.loadStyleUri(
            "mapbox://styles/aayaffe/clmbnvfaa018401pjfbco00px"
        ) {
            mapView!!.gestures.rotateEnabled = false
            mapView!!.location.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
            mapView!!.location2.puckBearingSource = PuckBearingSource.HEADING
            sharedPreferences.getString(SettingsFragment.KEY_ROUTE, null)?.let {
                route = parseRoute(sharedPreferences)
                addRouteWaypoints(route)
            }
        }
        val cameraPosition = CameraOptions.Builder()
            .zoom(8.0)
            .center(Point.fromLngLat(32.0, 35.0))
            .build()
        mapView?.getMapboxMap()?.setCamera(cameraPosition)
        return view
    }

    override fun onStart() {
        super.onStart()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        if (key == SettingsFragment.KEY_ROUTE) {
            sp?.let {
                route = parseRoute(it)
                addRouteWaypoints(route)
            }
        }
    }

    private fun addRouteWaypoints(route: Route) {
        val bitmap = AppCompatResources.getDrawable(requireContext(), R.drawable.red_marker)
            ?.toBitmap()
        val annotationApi = mapView?.annotations
        val pointAnnotationManager = annotationApi?.createPointAnnotationManager(mapView!!)
        pointAnnotationManager?.deleteAll()
        for (e in route.elements) {
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(e.portWpt.longitude, e.portWpt.latitude))
                .withData(JsonParser.parseString(e.toGeoJson()))
                .withIconImage(bitmap!!)
            pointAnnotationManager?.create(pointAnnotationOptions)
        }
        pointAnnotationManager?.addLongClickListener(OnPointAnnotationLongClickListener {
            Toast.makeText(
                requireContext(),
                "Long click on ${it.getData()?.asJsonObject?.get("properties")?.asJsonObject?.get("name")}",
                Toast.LENGTH_SHORT
            ).show()
            true
        })
    }

}
