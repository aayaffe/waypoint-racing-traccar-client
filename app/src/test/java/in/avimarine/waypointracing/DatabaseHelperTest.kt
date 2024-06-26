package `in`.avimarine.waypointracing

import `in`.avimarine.waypointracing.database.DatabaseHelper
import android.location.Location
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import `in`.avimarine.androidutils.BatteryStatus
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class DatabaseHelperTest {
    @Test
    fun test() {

        val databaseHelper = DatabaseHelper(ApplicationProvider.getApplicationContext())

        var position: Position? = Position("123456789012345", "userid", "boatname", Location("gps"), BatteryStatus())


        Assert.assertNull(databaseHelper.selectPosition())

        databaseHelper.insertPosition(position!!)

        position = databaseHelper.selectPosition()

        Assert.assertNotNull(position)

        databaseHelper.deletePosition(position!!.id)

        Assert.assertNull(databaseHelper.selectPosition())

    }

}
