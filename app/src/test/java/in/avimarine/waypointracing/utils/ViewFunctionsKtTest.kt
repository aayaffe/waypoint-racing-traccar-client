package `in`.avimarine.waypointracing.utils

import org.junit.Assert
import org.junit.Test


class ViewFunctionsKtTest {

    @Test
    fun testGetCompasspoint() {

        Assert.assertEquals("N","N",getPointOfCompass(315.0,45.0))
        Assert.assertEquals("S","S",getPointOfCompass(180.0,190.0))
        Assert.assertEquals("E","E",getPointOfCompass(80.0,120.0))
        Assert.assertEquals("NW","NW",getPointOfCompass(280.0,340.0))


    }

}