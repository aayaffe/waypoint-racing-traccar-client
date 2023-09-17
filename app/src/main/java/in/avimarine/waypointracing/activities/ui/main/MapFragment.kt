package `in`.avimarine.waypointracing.activities.ui.main

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.gson.JsonParser
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationLongClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import `in`.avimarine.androidutils.TAG
import `in`.avimarine.androidutils.Utils.Companion.mapRange
import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.activities.SettingsFragment
import `in`.avimarine.waypointracing.databinding.FragmentMainBinding
import `in`.avimarine.waypointracing.route.GatePassings
import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.route.RouteElement
import `in`.avimarine.waypointracing.utils.RouteParser.Companion.parseRoute

class MapFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val mapView get() = binding.mapView
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var annotationApi: AnnotationPlugin? = null
    var route: Route = Route.emptyRoute()
    private lateinit var sharedPreferences: SharedPreferences
    private var bp_selected: Bitmap? = null
    private var bp_selected_green: Bitmap? = null
    private var bp_green: Bitmap? = null
    private var bp_gold: Bitmap? = null
    private var bp_silver: Bitmap? = null
    private var bp_bronze: Bitmap? = null
    private var nextWpt = -1



    companion object {
        fun newInstance() = MapFragment()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        val view = binding.root
        createMapIconBitmaps()
        annotationApi = mapView.annotations
        pointAnnotationManager = annotationApi?.createPointAnnotationManager()
        mapView.getMapboxMap().loadStyleUri(
            "mapbox://styles/aayaffe/clmbnvfaa018401pjfbco00px"
        ) {
            mapView.scalebar.updateSettings { isMetricUnits = true }
            mapView.gestures.rotateEnabled = false
            nextWpt = sharedPreferences.getInt(SettingsFragment.KEY_NEXT_WPT, -1)
            sharedPreferences.getString(SettingsFragment.KEY_ROUTE, null)?.let {
                route = parseRoute(sharedPreferences)
                addRouteWaypoints(route, nextWpt)
                initLocationComponent()
            }
        }

        return view
    }

    private fun createMapIconBitmaps() {
        bp_selected = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_selected_wpt)
            ?.toBitmap()
        bp_selected_green = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_selected_wpt_green)
            ?.toBitmap()
        bp_green = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_green_wpt)
            ?.toBitmap()
        bp_gold = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_gold_wpt)
            ?.toBitmap()
        bp_silver = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_silver_wpt)
            ?.toBitmap()
        bp_bronze = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_bronze_wpt)
            ?.toBitmap()
    }

    private fun initLocationComponent() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.user_puck,
                ),
                shadowImage = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.mapbox_user_icon_shadow,
                ),
                scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
    }

    override fun onStart() {
        super.onStart()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        mapView.onDestroy()
        _binding = null
        super.onDestroyView()
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        if (key == SettingsFragment.KEY_ROUTE) {
            sp?.let {
                route = parseRoute(it)
                addRouteWaypoints(route, nextWpt)
            }
        }
        if (key == SettingsFragment.KEY_NEXT_WPT) {
            sp?.let {
                nextWpt = sharedPreferences.getInt(SettingsFragment.KEY_NEXT_WPT, -1)
                addRouteWaypoints(route, nextWpt,false)
            }
        }
        if (key == SettingsFragment.KEY_GATE_PASSES) {
            addRouteWaypoints(route, nextWpt,false)
        }

    }

    private fun addRouteWaypoints(route: Route, nextWpt: Int, adjustZoom: Boolean = true) {
        val maxPoints = route.elements.maxOf { it.points }
        val minPoints = route.elements.minOf { it.points }
        pointAnnotationManager?.deleteAll()
        val wpt = route.elements.elementAtOrNull(nextWpt)
        route.elements.forEachIndexed { index, re ->
            val selected = re.id == (wpt?.id ?: false)
            val jsonObject = JsonParser.parseString(re.toGeoJson()).asJsonObject
            jsonObject.getAsJsonObject("properties").apply {
                addProperty("ordinal", index)
            }
                val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(re.portWpt.longitude, re.portWpt.latitude))
                .withData(jsonObject)
                .withIconImage(routeElementToBitmap(re, selected, maxPoints, minPoints))
            pointAnnotationManager?.create(pointAnnotationOptions)
        }

        pointAnnotationManager?.addLongClickListener(OnPointAnnotationLongClickListener {
            setNextWpt(it.getData()?.asJsonObject?.get("properties")?.asJsonObject?.get("ordinal")?.asInt ?: -1)
            Toast.makeText(
                requireContext(),
                "Set next waypoint ${it.getData()?.asJsonObject?.get("properties")?.asJsonObject?.get("name")}",
                Toast.LENGTH_SHORT
            ).show()
            true
        })
        if (adjustZoom) {
            val points = pointAnnotationManager?.annotations?.map { it.point }
            val padding = EdgeInsets(
                100.0,
                100.0,
                100.0,
                100.0
            )
            if (points != null) {
                val cameraPosition = mapView.getMapboxMap().cameraForCoordinates(points, padding)
                mapView.getMapboxMap().setCamera(cameraPosition)
            }
        }

    }

    private fun routeElementToBitmap(
        routeElement: RouteElement,
        selected: Boolean,
        maxPoints: Double,
        minPoints: Double
    ): Bitmap {
        val gp =
            activity?.let {
                GatePassings.getCurrentRouteGatePassings(
                    it.applicationContext,
                    route.id
                )
            }
        if (gp != null) {
            val latestGp = gp.getLatestGatePassForGate(routeElement.id)
            if (latestGp != null) {
                if (selected) {
                    return bp_selected_green!!
                }
                return bp_green!!
            }
        }

        if (selected) {
            return bp_selected!!
        }

        return when (mapRange(routeElement.points, Pair(minPoints, maxPoints), Pair(1, 3))) {
            3 -> bp_gold!!
            2 -> bp_silver!!
            1 -> bp_bronze!!
            else -> bp_bronze!!
        }
    }

    private fun setNextWpt(nextWpt: Int) {
        val sharedPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        with(sharedPref.edit()) {
            putInt(SettingsFragment.KEY_NEXT_WPT, nextWpt)
            commit()
        }
    }

}
