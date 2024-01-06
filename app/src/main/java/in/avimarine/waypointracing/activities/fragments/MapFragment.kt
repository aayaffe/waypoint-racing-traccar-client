package `in`.avimarine.waypointracing.activities.fragments

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
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationLongClickListener
import com.mapbox.maps.plugin.annotation.generated.OnPolylineAnnotationLongClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import `in`.avimarine.androidutils.TAG
import `in`.avimarine.androidutils.Utils.Companion.mapRange
import `in`.avimarine.androidutils.getLatString
import `in`.avimarine.androidutils.getLonString
import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.activities.SettingsFragment
import `in`.avimarine.waypointracing.databinding.FragmentMapBinding
import `in`.avimarine.waypointracing.route.GatePassings
import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.route.RouteElement
import `in`.avimarine.waypointracing.route.RouteElementType
import `in`.avimarine.waypointracing.utils.Preferences
import `in`.avimarine.waypointracing.utils.RouteParser.Companion.parseRoute

class MapFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener,
    OnIndicatorPositionChangedListener {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val mapView get() = binding.mapView
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var lineAnnotationManager: PolylineAnnotationManager? = null
    private var annotationApi: AnnotationPlugin? = null
    var route: Route = Route.emptyRoute()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var prefs: Preferences
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
        prefs = Preferences(sharedPreferences)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val view = binding.root
        createMapIconBitmaps()
        annotationApi = mapView.annotations
        pointAnnotationManager = annotationApi?.createPointAnnotationManager()
        lineAnnotationManager = annotationApi?.createPolylineAnnotationManager()
        mapView.getMapboxMap().loadStyleUri(
            "mapbox://styles/aayaffe/clmbnvfaa018401pjfbco00px"
        ) {
            mapView.scalebar.updateSettings {
                isMetricUnits = true
            }
            mapView.gestures.rotateEnabled = false
            nextWpt = prefs.nextWpt
            route = parseRoute(prefs.currentRoute)
            addRouteWaypoints(route, nextWpt)
            initLocationComponent()
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
        locationComponentPlugin.addOnIndicatorPositionChangedListener(this)
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.user_puck,
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
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.removeOnIndicatorPositionChangedListener(this)
        mapView.onDestroy()
        _binding = null
        super.onDestroyView()
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
        if (key == SettingsFragment.KEY_ROUTE) {
            route = parseRoute(prefs.currentRoute)
            addRouteWaypoints(route, nextWpt)
        }
        if (key == SettingsFragment.KEY_NEXT_WPT) {
            nextWpt = prefs.nextWpt //sharedPreferences.getInt(SettingsFragment.KEY_NEXT_WPT, -1)
            addRouteWaypoints(route, nextWpt)
        }
        if (key == SettingsFragment.KEY_GATE_PASSES) {
            addRouteWaypoints(route, nextWpt,false)
        }

    }

    private fun addRouteWaypoints(route: Route, nextWpt: Int, adjustZoom: Boolean = true) {
        if (route.elements.isEmpty()) {
            return
        }
        val maxPoints = route.elements.maxOf { it.points }
        val minPoints = route.elements.minOf { it.points }
        pointAnnotationManager?.deleteAll()
        lineAnnotationManager?.deleteAll()
        val wpt = route.elements.elementAtOrNull(nextWpt)
        route.elements.forEachIndexed { index, re ->
            val selected = re.id == (wpt?.id ?: false)
            val jsonObject = JsonParser.parseString(re.toGeoJson()).asJsonObject
            jsonObject.getAsJsonObject("properties").apply {
                addProperty("ordinal", index)
            }
            when (re.routeElementType) {
                RouteElementType.WAYPOINT -> {
                    val pointAnnotationOptions =
                        pointAnnotationOptions(re, jsonObject, selected, maxPoints, minPoints)
                    pointAnnotationManager?.create(pointAnnotationOptions)
                }
                RouteElementType.GATE, RouteElementType.FINISH, RouteElementType.START -> {
                    val lineAnnotationOptions = lineAnnotationOptions(re, jsonObject, selected)
                    lineAnnotationManager?.create(lineAnnotationOptions)
                    val pointAnnotationOptions =
                        pointAnnotationOptions(re, jsonObject, selected, maxPoints, minPoints)
                    pointAnnotationManager?.create(pointAnnotationOptions)
                }
            }
        }

        pointAnnotationManager?.addLongClickListener(OnPointAnnotationLongClickListener {
            handleLongClick(it.getData())
            true
        })
        lineAnnotationManager?.addLongClickListener(OnPolylineAnnotationLongClickListener {
            handleLongClick(it.getData())
            true
        })
        if (adjustZoom) {
            adjustZoom()
        }

    }

    private fun handleLongClick(data: JsonElement?) {
        val ordinal = data?.asJsonObject?.get("properties")?.asJsonObject?.get("ordinal")?.asInt ?: -1
        setNextWpt(ordinal)
        val name = data?.asJsonObject?.get("properties")?.asJsonObject?.get("name")
        Toast.makeText(
            requireContext(),
            "Set next waypoint $name",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun pointAnnotationOptions(
        re: RouteElement,
        jsonObject: JsonObject,
        selected: Boolean,
        maxPoints: Double,
        minPoints: Double
    ): List<PointAnnotationOptions> {
        if (re.routeElementType != RouteElementType.WAYPOINT) {
            val portWpt = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(re.portWpt.longitude, re.portWpt.latitude))
                .withData(jsonObject)
                .withIconImage(routeElementToBitmap(re, selected, maxPoints, minPoints))
            val stbWpt = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(re.stbdWpt.longitude, re.stbdWpt.latitude))
                .withData(jsonObject)
                .withIconImage(routeElementToBitmap(re, selected, maxPoints, minPoints))
            return listOf(portWpt, stbWpt)
        } else {
            return listOf(PointAnnotationOptions()
                .withPoint(Point.fromLngLat(re.portWpt.longitude, re.portWpt.latitude))
                .withData(jsonObject)
                .withIconImage(routeElementToBitmap(re, selected, maxPoints, minPoints)))
        }

    }
    private fun lineAnnotationOptions(
        re: RouteElement,
        jsonObject: JsonObject,
        selected: Boolean,
    ): PolylineAnnotationOptions {
        return PolylineAnnotationOptions()
            .withLineColor(routeElementToColor(re, selected))
            .withLineWidth(2.0)
            .withData(jsonObject)
            .withPoints(
                listOf<Point>(
                    Point.fromLngLat(re.portWpt.longitude, re.portWpt.latitude),
                    Point.fromLngLat(re.stbdWpt.longitude, re.stbdWpt.latitude)
                )
            )
    }

    private fun routeElementToColor(re: RouteElement, selected: Boolean): String {
        val gp =
            activity?.let {
                GatePassings.getCurrentRouteGatePassings(
                    it.applicationContext,
                    route.id
                )
            }
        if (gp != null) {
            val latestGp = gp.getLatestGatePassForGate(re.id)
            if (latestGp != null) {
                if (selected) {
                    return "#38761d"
                }
                return "#8fce00"
            }
        }
        if (selected) {
            return "#004aff"
        }
        return "#458B74"
    }

    private fun adjustZoom() {
        val points = pointAnnotationManager?.annotations?.map { it.point }?: emptyList()
        val linePoints = lineAnnotationManager?.annotations?.flatMap { it.points }?: emptyList()
        val padding = EdgeInsets(
            50.0,
            50.0,
            50.0,
            50.0
        )
        if ((points + linePoints).isNotEmpty()) {
            val cameraPosition = mapView.getMapboxMap().cameraForCoordinates(points + linePoints, padding)
            mapView.getMapboxMap().setCamera(cameraPosition)
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
        prefs.nextWpt = nextWpt
    }

    override fun onIndicatorPositionChanged(point: Point) {
        binding.coordinatesTv.text = "${getLatString(point.latitude())}\n${getLonString(point.longitude())}"
    }

}
