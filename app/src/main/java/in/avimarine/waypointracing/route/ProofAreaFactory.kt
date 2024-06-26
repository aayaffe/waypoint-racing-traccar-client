package `in`.avimarine.waypointracing.route

import android.location.Location
import `in`.avimarine.androidutils.geo.Distance
import `in`.avimarine.androidutils.getDirection
import `in`.avimarine.androidutils.getLocFromDirDist


class ProofAreaFactory {
    companion object {

        /**
         * Create a proof quadrant area
         *
         * @param portWpt - Gate Port Wpt
         * @param stbdWpt - Gate Stbd Wpt
         * @param bearing1, bearing2 - bearings in degrees north in a clockwise order
         * @return ProofArea
         */
        fun createProofArea(stbdWpt: Location, portWpt: Location, bearing1: Double, bearing2: Double): ProofArea {
            return ProofArea(arrayListOf(bearing1, bearing2))
        }

        /**
         * Create a proof quadrant area for a passing waypoint
         *
         * @param wpt - Wpt
         * @param bearing1, bearing2 - bearings in degrees north in a clockwise order
         * @return ProofArea
         */
        fun createProofArea(wpt: Location, bearing1: Double, bearing2: Double): ProofArea {
            return ProofArea(arrayListOf(bearing1, bearing2))
        }

        /**
         * Creates a polygon proof area, which starts with [bearing1] with a [dist] from [portWpt],
         * extending to a polygon of [dist] from the gate line and ends with [bearing2] from [stbdWpt]
         * @param portWpt
         * @param stbdWpt
         * @param bearing1 - from [portWpt]
         * @param bearing2 - from [stbdWpt]
         * @param dist - in Nautical miles
         * @return ProofArea
         */
        fun createProofArea(stbdWpt: Location, portWpt: Location, bearing1: Double, bearing2: Double, dist: Distance): ProofArea {
            val wpts = arrayListOf(portWpt)
            val gateDir = getDirection(portWpt, stbdWpt)
            wpts.add(getLocFromDirDist(portWpt, bearing1, dist))
            wpts.add(getLocFromDirDist(portWpt, gateDir - 90, dist))
            wpts.add(getLocFromDirDist(stbdWpt, gateDir + 270, dist))
            wpts.add(getLocFromDirDist(stbdWpt, bearing2, dist))
            wpts.add(stbdWpt)
            return ProofArea(ProofAreaType.POLYGON, arrayListOf(bearing1,bearing2), wpts, dist)
        }

        /**
         * Creates a polygon proof area, with a [dist] from [portWpt],
         * to [dist] from [stbdWpt]
         * @param portWpt
         * @param stbdWpt
         * @param dist - in Nautical miles
         * @return ProofArea
         */
        fun createProofArea(stbdWpt: Location, portWpt: Location, dist: Distance): ProofArea {
            val wpts = arrayListOf(portWpt)
            val gateDir = getDirection(portWpt, stbdWpt)
            wpts.add(getLocFromDirDist(portWpt, gateDir - 90, dist))
            wpts.add(getLocFromDirDist(stbdWpt, gateDir + 270, dist))
            wpts.add(stbdWpt)
            return ProofArea(ProofAreaType.POLYGON, wpts, dist)
        }

        /**
         * Creates a circle proof area, with a [dist]
         * @param dist - in Nautical miles
         * @return ProofArea
         */
        fun createProofArea(dist: Distance): ProofArea {
            return ProofArea(dist)
        }
    }
}