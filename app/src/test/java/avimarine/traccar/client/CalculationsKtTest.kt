package avimarine.traccar.client

import android.location.Location
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalculationsKtTest {

    @Test
    fun getLocFromDirDist() {
        val loc1 = Location("")
        val distInNms = 1.0
        loc1.latitude = 32.0
        loc1.longitude = 32.0

        val loc2 = getLocFromDirDist(loc1, 15.0, distInNms)
        val dist: Float = loc1.distanceTo(loc2)
        Assert.assertEquals("Distance difference is " + Math.abs(100 - (dist / (distInNms*1852)) * 100) + "%", distInNms*1852, dist.toDouble(), 6.0)
    }

}