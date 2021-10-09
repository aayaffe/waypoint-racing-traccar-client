package `in`.avimarine.waypointracing.utils

import `in`.avimarine.waypointracing.Position
import android.hardware.GeomagneticField
import android.location.Location
import android.os.Build
import android.text.format.DateFormat
import android.text.format.DateFormat.format
import android.util.Log
import androidx.annotation.RequiresApi
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfJoins.inside
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import net.sf.geographiclib.Geodesic
import net.sf.geographiclib.GeodesicLine
import net.sf.geographiclib.GeodesicMask
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList


/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 03/08/2019.
 */

private val TAG = "Calculations"

fun getDirString(dir: Double, magnetic: Boolean, fromNotation: Boolean, location: Location, time: Long): String {
    return getDirString(dir, magnetic, fromNotation, location.latitude.toFloat(), location.longitude.toFloat(), location.altitude.toFloat(), time)
}

fun getDirString(dir: Double, magnetic: Boolean, fromNotation: Boolean, location: Position, time: Long): String {
    return getDirString(dir, magnetic, fromNotation, location.latitude.toFloat(), location.longitude.toFloat(), location.altitude.toFloat(), time)
}

fun getDirString(dir: Double, magnetic: Boolean, fromNotation: Boolean, latitude: Float, longitude: Float, altitude: Float, time: Long): String {
    var calcDir = dir
    if (magnetic) {
        val geomagneticField = GeomagneticField(
                latitude,
                longitude,
                altitude,
                time
        )
        Log.d(TAG, "Declination is: " + geomagneticField.declination)
        calcDir += geomagneticField.declination
    }
    if (fromNotation) {
        calcDir -= 180
    }
    if (calcDir < 0) {
        calcDir += 360
    }
    return String.format("%03d", Math.round(calcDir)) + if (magnetic) " M" else ""
}


/**
 * Al conversion functions are from Knots
 */
fun toMPerMin(speed: Double): Double {
    return speed * 30.8667
}

fun toKMh(speed: Double): Double {
    return speed * 1.852
}

fun toMPerSec(speed: Double): Double {
    return speed * 0.514444
}

/**
 * Returns the speed in metres per minute
 * @param dist Distance in metres
 * @param firstTime start time in milliseconds
 * @param secondTime end time in millisecond
 * @return Speed in metres per minute
 */
fun getSpeed(dist: Double, firstTime: Long, secondTime: Long): Double {
    val duration = (secondTime - firstTime).toDouble() / (1000 * 60)
    return dist / duration
}

private val geod = Geodesic.WGS84// This matches EPSG4326, which is the coordinate system used by Geolake

/**
 * Get the distance between two points in meters.
 * @param lat1 First point'getDirString latitude
 * @param lon1 First point'getDirString longitude
 * @param lat2 Second point'getDirString latitude
 * @param lon2 Second point'getDirString longitude
 * @return Distance between the first and the second point in meters
 */
fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val line = geod.InverseLine(
        lat1,
        lon1,
        lat2,
        lon2,
        GeodesicMask.DISTANCE_IN or GeodesicMask.LATITUDE or GeodesicMask.LONGITUDE
    )
    return line.Distance()
}

private fun getDistance(p1: Point, p2: Point):Double{
    return getDistance(p1.latitude(),p1.longitude(),p2.latitude(),p2.longitude())
}

/***
 * Returns Distance in meters
 */
fun getDistance(firstLocation : Location, secondLocation: Location): Double {
    return getDistance(firstLocation.latitude, firstLocation.longitude, secondLocation.latitude, secondLocation.longitude)
}

/***
 * Returns Distance in meters
 */
fun getDistance(firstLocation : Position, secondLocation: Location): Double {
    return getDistance(firstLocation.latitude, firstLocation.longitude, secondLocation.latitude, secondLocation.longitude)
}
/***
 * Returns Distance in meters
 */
fun getDistance(firstLocation : Position, secondLocation: Position): Double {
    return getDistance(firstLocation.latitude, firstLocation.longitude, secondLocation.latitude, secondLocation.longitude)
}

/**
 * Get the azimuth between two points in degrees.
 * @param lat1 First point'getDirString latitude
 * @param lon1 First point'getDirString longitude
 * @param lat2 Second point'getDirString latitude
 * @param lon2 Second point'getDirString longitude
 * @return Azimuth between the first and the second point in degrees true
 */
fun getDirection(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val line = geod.InverseLine(
        lat1,
        lon1,
        lat2,
        lon2,
        GeodesicMask.DISTANCE_IN or GeodesicMask.LATITUDE or GeodesicMask.LONGITUDE
    )
    return line.Azimuth()
}
fun getDirection(firstLocation: Location, secondLocation: Location): Double {
    return getDirection(firstLocation.latitude,firstLocation.longitude,secondLocation.latitude,secondLocation.longitude)
}

fun getDirection(firstLocation: Position, secondLocation: Position): Double {
    return getDirection(firstLocation.latitude,firstLocation.longitude,secondLocation.latitude,secondLocation.longitude)
}
fun getDirection(firstLocation: Location, secondLocation: Position): Double {
    return getDirection(firstLocation.latitude,firstLocation.longitude,secondLocation.latitude,secondLocation.longitude)
}

fun getDirection(firstLocation: Position, secondLocation: Location): Double {
    return getDirection(firstLocation.latitude,firstLocation.longitude,secondLocation.latitude,secondLocation.longitude)
}

fun timeStamptoDateString(timestamp: Long): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("(dd)HH:mm:ss")
        return date.format(formatter)
    }
    val calendar = Calendar.getInstance(Locale.getDefault())
    calendar.timeInMillis = timestamp// * 1000L
    val date = format("(dd)HH:mm:ss",calendar).toString()
    return date
}

fun Location.toPoint() : Point {
    return Point.fromLngLat(this.longitude,this.latitude)
}

fun Position.toLocation() : Location {
    val l = Location("")
    l.latitude = this.latitude
    l.longitude = this.longitude
    return l
}


/**
 * Returns Error in degrees (total error equals +/- ret)
 */
fun getDirError(firstLocation: Location, secondLocation: Location, err1: Double, err2: Double): Double {
    val ber = getDirection(firstLocation,secondLocation)
    val tmpLocA = TurfMeasurement.destination(firstLocation.toPoint(),err1,ber-90,TurfConstants.UNIT_METERS)
    val tmpLocB = TurfMeasurement.destination(secondLocation.toPoint(),err2,ber+90,TurfConstants.UNIT_METERS)
    var res = TurfMeasurement.bearing(tmpLocA,tmpLocB)
    res = (res-ber)
    return res
}

/**
 * Returns Error in meters (total error equals +/- ret)
 */
fun getDistError(err1: Double, err2: Double): Double {
    return err1+err2
}

/**
 * Returns a location of [dist] and [dir] from [loc]
 *
 * @param loc
 * @param dir - Degrees true from north
 * @param dist - Distance in Nautical miles
 * @return
 */
fun getLocFromDirDist(loc: Location, dir: Double, dist: Double) : Location{
    val line = GeodesicLine(geod, loc.latitude,loc.longitude, dir, GeodesicMask.DISTANCE_IN or GeodesicMask.LATITUDE or GeodesicMask.LONGITUDE)
    val gd = line.Position(dist * 1852.0)
    val ret = Location("")
    ret.latitude = gd.lat2
    ret.longitude = gd.lon2
    return ret

}

/**
 * Get the distance between a point and a line.
 * @param loc Point's location
 * @param firstLocation Line first location
 * @param secondLocation Line's second location
 * @return Distance between the point and the line in meters
 */
fun pointToLineDist(loc: Location, firstLocation : Location, secondLocation: Location): Double{
    val pointList = arrayListOf(firstLocation.toPoint(),secondLocation.toPoint())
    val p = TurfMisc.nearestPointOnLine(loc.toPoint(),pointList)
    return getDistance(loc.toPoint(),p.geometry() as Point)
}

fun isPointInPolygon(wpts : ArrayList<Location>, loc: Location) : Boolean{
    val poly: Polygon = Polygon.fromLngLats(toListOfListOfLocs(wpts))
    return inside(Point.fromLngLat(loc.longitude,loc.latitude),poly)
}

fun toListOfListOfLocs(wpts : ArrayList<Location>):List<List<Point>>{
    val ret = arrayListOf(arrayListOf<Point>())
    wpts.forEach{
        ret[0].add(Point.fromLngLat(it.longitude,it.latitude))
    }
    return ret
}

fun isBetweenAngles(from: Double, to:Double, angle:Double): Boolean{
    var b2 = to
    var a = angle
    if (to < from) {
        b2 = to + 360.0
    }
    if (angle < from) {
        a = angle + 360
    }
    // compare
    return a in from..b2
}

