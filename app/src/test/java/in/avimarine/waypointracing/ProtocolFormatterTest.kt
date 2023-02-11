package `in`.avimarine.waypointracing

import `in`.avimarine.waypointracing.ProtocolFormatter.formatRequest
import android.location.Location
import android.os.Build
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class ProtocolFormatterTest {

    @Test
    fun testFormatRequest() {
        val position = Position("123456789012345", "boat", Location("gps"), BatteryStatus())
        val url = formatRequest("http://localhost:5055", position)
        Assert.assertEquals("http://localhost:5055?id=123456789012345&timestamp=619315200&lat=0.0&lon=0.0&speed=0.0&bearing=0.0&altitude=0.0&accuracy=0.0&batt=0.0&boatname=boat", url)
    }

    @Test
    fun testFormatPathPortRequest() {
        val position = Position("123456789012345", "boat",Location("gps"), BatteryStatus())
        val url = formatRequest("http://localhost:8888/path", position)
        Assert.assertEquals("http://localhost:8888/path?id=123456789012345&timestamp=619315200&lat=0.0&lon=0.0&speed=0.0&bearing=0.0&altitude=0.0&accuracy=0.0&batt=0.0&boatname=boat", url)
    }

    @Test
    fun testFormatAlarmRequest() {
        val position = Position("123456789012345","boat", Location("gps"), BatteryStatus())
        val url = formatRequest("http://localhost:5055/path", position, "alert message")
        Assert.assertEquals("http://localhost:5055/path?id=123456789012345&timestamp=619315200&lat=0.0&lon=0.0&speed=0.0&bearing=0.0&altitude=0.0&accuracy=0.0&batt=0.0&alarm=alert%20message&boatname=boat", url)
    }

}
