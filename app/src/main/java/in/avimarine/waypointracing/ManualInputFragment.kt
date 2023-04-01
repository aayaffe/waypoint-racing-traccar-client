package `in`.avimarine.waypointracing

import `in`.avimarine.waypointracing.database.FirestoreDatabase
import `in`.avimarine.waypointracing.databinding.FragmentManualInputBinding
import `in`.avimarine.waypointracing.route.GatePassing
import `in`.avimarine.waypointracing.ui.WptDistAdapter
import `in`.avimarine.androidutils.units.GeoCoordinatesFormat
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.santalu.maskara.Mask
import com.santalu.maskara.MaskChangedListener
import com.santalu.maskara.MaskStyle
import `in`.avimarine.androidutils.*
import `in`.avimarine.androidutils.units.DistanceUnits
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ManualInputFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManualInputFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var viewBinding: FragmentManualInputBinding
    private var currentFormat = GeoCoordinatesFormat.D
    private lateinit var sharedPreferences: SharedPreferences
    private val viewModel: ExpertViewModel by activityViewModels()
    private var gateId = -1
    private var gateName = ""




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext().applicationContext)

    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        viewBinding = FragmentManualInputBinding.inflate(inflater, container, false)
        setRadioButtons()
        val maskLat = Mask(
            value = "__.______°",
            character = '_',
            style = MaskStyle.PERSISTENT
        )
        val listenerLat = MaskChangedListener(maskLat)
        viewBinding.latInput.addTextChangedListener(listenerLat)

        val maskLon = Mask(
            value = "___.______°",
            character = '_',
            style = MaskStyle.PERSISTENT
        )
        val listenerLon = MaskChangedListener(maskLon)
        viewBinding.lonInput.addTextChangedListener(listenerLon)

        val maskTime = Mask(
            value = "__:__:__",
            character = '_',
            style = MaskStyle.PERSISTENT
        )
        val listenerTime = MaskChangedListener(maskTime)
        viewBinding.timeInput.addTextChangedListener(listenerTime)

        val maskDate = Mask(
            value = "__/__/__",
            character = '_',
            style = MaskStyle.PERSISTENT
        )
        val listenerDate = MaskChangedListener(maskDate)
        viewBinding.dateInput.addTextChangedListener(listenerDate)
        viewBinding.calculateBtn.setOnClickListener{ populateRankedWptList()}
        viewBinding.sendBtn.setOnClickListener{ send()}
        viewBinding.sendBtn.isEnabled = false
        viewBinding.boatNameTextView.text = viewModel.boatName.value
        setDate(Date())
        return viewBinding.root
    }

    private fun send() {
        val lat = validateLatitude(viewBinding.latInput.unMasked, currentFormat)
        val lon = validateLongitude(viewBinding.lonInput.unMasked, currentFormat)
        if (lat == null || lon == null) {
            Toast.makeText(requireContext(), "Location Error", Toast.LENGTH_LONG).show()
            return
        }
        val l = createLocation(lat, lon)
        val time = validateDateTime(viewBinding.timeInput.unMasked,viewBinding.dateInput.unMasked)
        if (time == -1L) {
            Toast.makeText(requireContext(), "Date or time Error", Toast.LENGTH_LONG).show()
            return
        }
        val gp = GatePassing(
            viewModel.route.value.eventName,
            viewModel.route.value.id,
            viewModel.route.value.lastUpdate,
            "-444",
            viewModel.boatName.value,
            gateId,
            gateName,
            Date(time),
            Position("-444", viewModel.boatName.value, l, BatteryStatus())
        )
        FirestoreDatabase.addManualGatePass(gp, { documentReference ->
            Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
            Toast.makeText(requireContext(), "Uploaded successfully", Toast.LENGTH_LONG).show()
        },{ e ->
            Log.e(TAG, "Error adding gatepass", e)
            Toast.makeText(requireContext(), "--FAILED-- to upload", Toast.LENGTH_LONG).show()
        }
        )
        viewBinding.sendBtn.isEnabled = false
    }

    private fun validateDateTime(time: String, date: String): Long {
        return try{
            val timestamp = SimpleDateFormat("ddMMyyHHmmss").parse(date+time).time
            timestamp
        } catch (e: Exception){
            Log.w(TAG, "Error validating datetime", e)
            -1
        }
    }

    private fun setDate(d: Date) {
        val format = SimpleDateFormat("ddMMyy")
        viewBinding.dateInput.setText(format.format(d))
    }
    private fun setRadioButtons() {
        viewBinding.DRadioButton.setOnClickListener { changeFormat(GeoCoordinatesFormat.D) }
        viewBinding.DMRadioButton.setOnClickListener { changeFormat(GeoCoordinatesFormat.DM) }
        viewBinding.DMSRadioButton.setOnClickListener { changeFormat(GeoCoordinatesFormat.DMS) }
    }

    private fun changeFormat(format: GeoCoordinatesFormat) {
        val lat = getLatitude(currentFormat)
        val lon = getLongitude(currentFormat)
        when (format){
            GeoCoordinatesFormat.D -> {
                val maskLat = Mask(
                    value = "__.______°",
                    character = '_',
                    style = MaskStyle.PERSISTENT
                )
                val listenerLat = MaskChangedListener(maskLat)
                viewBinding.latInput.addTextChangedListener(listenerLat)

                val maskLon = Mask(
                    value = "___.______°",
                    character = '_',
                    style = MaskStyle.PERSISTENT
                )
                val listenerLon = MaskChangedListener(maskLon)
                viewBinding.lonInput.addTextChangedListener(listenerLon)

            }
            GeoCoordinatesFormat.DM -> {
                val maskLat = Mask(
                    value = "__° __.____''",
                    character = '_',
                    style = MaskStyle.PERSISTENT
                )
                val listenerLat = MaskChangedListener(maskLat)
                viewBinding.latInput.addTextChangedListener(listenerLat)
                val maskLon = Mask(
                    value = "___° __.____''",
                    character = '_',
                    style = MaskStyle.PERSISTENT
                )
                val listenerLon = MaskChangedListener(maskLon)
                viewBinding.lonInput.addTextChangedListener(listenerLon)
            }
            GeoCoordinatesFormat.DMS -> {
                val maskLat = Mask(
                    value = "__° __' __''",
                    character = '_',
                    style = MaskStyle.PERSISTENT
                )
                val listenerLat = MaskChangedListener(maskLat)
                viewBinding.latInput.addTextChangedListener(listenerLat)
                val maskLon = Mask(
                    value = "___° __' __''",
                    character = '_',
                    style = MaskStyle.PERSISTENT
                )
                val listenerLon = MaskChangedListener(maskLon)
                viewBinding.lonInput.addTextChangedListener(listenerLon)
            }
        }
        viewBinding.latInput.setText(formatLatMasked(lat,format))
        viewBinding.lonInput.setText(formatLonMasked(lon,format))
        currentFormat = format
    }

    private fun formatLatMasked(lat: Double, format: GeoCoordinatesFormat): String {
        when (format){
            GeoCoordinatesFormat.D -> {
                return String.format("%09.06f", lat).replace(".","")
            }
            GeoCoordinatesFormat.DM -> {
                val deg = lat.toInt()
                val min = (lat - deg) * 60
                return String.format("%02d", deg) + String.format("%07.04f", min).replace(".","")
            }
            GeoCoordinatesFormat.DMS -> {
                var deg = lat.toInt()
                var min = (lat - deg) * 60
                var sec = (min - min.toInt()) * 60
                if (sec.roundToInt()>=60) {
                    sec = 0.0
                    min += 1.0
                }
                if (min>=60){
                    min = 0.0
                    deg += 1
                }
                return String.format("%02d", deg) + String.format("%02d", min.toInt()) + String.format("%02d", sec.roundToInt())
            }
        }
    }

    private fun formatLonMasked(lon: Double, format: GeoCoordinatesFormat): String {
        when (format){
            GeoCoordinatesFormat.D -> {
                return String.format("%010.06f", lon).replace(".","")
            }
            GeoCoordinatesFormat.DM -> {
                val deg = lon.toInt()
                val min = (lon - deg) * 60
                return String.format("%03d", deg) + String.format("%07.04f", min).replace(".","")
            }
            GeoCoordinatesFormat.DMS -> {
                var deg = lon.toInt()
                var min = (lon - deg) * 60
                var sec = (min - min.toInt()) * 60
                if (sec.roundToInt()>=60) {
                    sec = 0.0
                    min += 1.0
                }
                if (min>=60){
                    min = 0.0
                    deg += 1
                }
                return String.format("%03d", deg) + String.format("%02d", min.toInt()) + String.format("%02d", sec.roundToInt())
            }
        }
    }

    private fun getLatitude(format: GeoCoordinatesFormat): Double {
        Log.d(TAG, "Format = $format")
        val lat = viewBinding.latInput.unMasked
        Log.d(TAG, "unmasked lat = $lat")
        val ret = validateLatitude(lat, format)
        Log.d(TAG, "Validated Lat = $ret")
        return ret?:0.0
    }
    private fun getLongitude(format: GeoCoordinatesFormat): Double {
        Log.d(TAG, "Format = $format")
        val lon = viewBinding.lonInput.unMasked
        Log.d(TAG, "unmasked lon = $lon")
        val ret = validateLongitude(lon, format)
        Log.d(TAG, "Validated Lon = $ret")
        return ret?:0.0
    }

    private fun validateLatitude(lat: String, format: GeoCoordinatesFormat): Double? {
        when (format) {
            GeoCoordinatesFormat.DMS -> {
                try{
                    val deg = lat.substring(0,2).toInt()
                    if (deg > 90) return null
                    val min = lat.substring(2,4).toInt()
                    if (min > 59) return null
                    val sec = lat.substring(4,6).toInt()
                    if (sec > 59) return null
                    return deg.toDouble() + min.toDouble() / 60 + sec.toDouble() / 3600
                } catch (e: Exception){
                    Log.w(TAG, "Error validating latitude", e)
                    return null
                }
            }
            GeoCoordinatesFormat.DM -> {
                try{
                    val deg = lat.substring(0,2).toInt()
                    if (deg > 90) return null
                    val min = (lat.substring(2,4) + "." + lat.substring(4)).toDouble()
                    if (min > 60) return null
                    return deg.toDouble() + min / 60
                } catch (e: Exception){
                    Log.w(TAG, "Error validating latitude", e)
                    return null
                }
            }
            GeoCoordinatesFormat.D -> {
                try{
                    val deg = (lat.substring(0,2)+ "." + lat.substring(2)).toDouble()
                    if (deg > 90) return null
                    return deg
                } catch (e: Exception){
                    Log.w(TAG, "Error validating latitude", e)
                    return null
                }
            }
        }
    }

    private fun validateLongitude(lon: String, format: GeoCoordinatesFormat): Double? {
        when (format) {
            GeoCoordinatesFormat.DMS -> {
                try{
                    val deg = lon.substring(0,3).toInt()
                    if (deg > 180) return null
                    val min = lon.substring(3,5).toInt()
                    if (min > 59) return null
                    val sec = lon.substring(5,7).toInt()
                    if (sec > 59) return null
                    return deg.toDouble() + min.toDouble() / 60 + sec.toDouble() / 3600
                } catch (e: Exception){
                    Log.w(TAG, "Error validating longitude", e)
                    return null
                }
            }
            GeoCoordinatesFormat.DM -> {
                try{
                    val deg = lon.substring(0,3).toInt()
                    if (deg > 180) return null
                    val min = (lon.substring(3,5) + "." + lon.substring(5)).toDouble()
                    if (min > 60) return null
                    return deg.toDouble() + min / 60
                } catch (e: Exception){
                    Log.w(TAG, "Error validating longitude", e)
                    return null
                }
            }
            GeoCoordinatesFormat.D -> {
                try{
                    val deg = (lon.substring(0,3)+ "." + lon.substring(3)).toDouble()
                    if (deg > 180) return null
                    return deg
                } catch (e: Exception){
                    Log.w(TAG, "Error validating longitude", e)
                    return null
                }
            }
        }
    }

    private fun populateRankedWptList() {
        val adapter = WptDistAdapter(0.06)
        val lat = validateLatitude(viewBinding.latInput.unMasked, currentFormat)
        val lon = validateLongitude(viewBinding.lonInput.unMasked, currentFormat)
        if (lat == null || lon == null) {
            Toast.makeText(requireContext(), "Location Error", Toast.LENGTH_LONG).show()
            return
        }
        val l = createLocation(lat, lon)
        val wptList = createWptDistList(l)
        adapter.submitList(wptList)
        viewBinding.wptsRecyclerView.adapter = adapter
        viewBinding.wptsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        val wpt = wptList.getOrNull(0)
        if (wpt!=null) {
            gateName = wpt.name
            gateId = wpt.id
        } else {
            gateName = ""
            gateId = -1
        }
        viewBinding.sendBtn.isEnabled = true
    }

    private fun createWptDistList(l: Location): MutableList<WptDistance> {
        val ret = arrayListOf<WptDistance>()
        val route = viewModel.route.value
        for (elem in route.elements){
            val dist = getDistance(elem.stbdWpt.latitude, elem.stbdWpt.longitude, l.latitude, l.longitude).getValue(DistanceUnits.NauticalMiles)
            ret.add(WptDistance(elem.name, elem.stbdWpt.latitude, elem.stbdWpt.longitude, dist, elem.id))
        }
        ret.sortBy{ it.dist }
        return ret
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ManualInputFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ManualInputFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    data class WptDistance(
        val name: String,
        val lat: Double,
        val lon: Double,
        val dist: Double,
        val id: Int,
    )
}

