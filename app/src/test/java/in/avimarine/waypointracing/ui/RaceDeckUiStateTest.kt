package `in`.avimarine.waypointracing.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RaceDeckUiStateTest {
    @Test
    fun stateKeepsHeaderTargetAndHealthIndependent() {
        val target = RaceDeckTargetMapper.map("MARK 5", isMark = true, isFinish = false)
        val state = RaceDeckUiState(courseName = "Spring Series", boatName = "Sea Breeze", target = target)
        assertEquals("Spring Series", state.courseName)
        assertEquals("Sea Breeze", state.boatName)
        assertEquals(RaceDeckTargetLabel.NEXT_MARK, state.target?.label)
    }

    @Test
    fun stateCarriesNavigationWarningsAndPassPresentationWithoutDetectionRules() {
        val pass = RaceDeckPassBannerState(
            name = "GATE 4",
            eventDetails = "14:38:12\n32°49.382'N 034°58.117'E",
            nextTarget = "MARK 5",
            isFinish = false,
        )
        val state = RaceDeckUiState(
            navigation = RaceDeckNavigationState(
                bearing = "038°",
                distance = "284 m",
                portEndpoint = "031°\n302 m",
                starboardEndpoint = "046°\n296 m",
                cog = "041°",
                sog = "5.8 kt",
                vmg = "5.1 kt",
                ttg = "02:48",
                eta = "14:40:48",
                time = "14:37:12",
                position = "32°49.382'N 034°58.117'E",
            ),
            deviceWarning = "2 settings may affect tracking",
            trackingWarningVisible = false,
            gpsWarning = "Last valid fix 12 seconds ago.",
            passBanner = pass,
        )

        assertEquals("038°", state.navigation.bearing)
        assertEquals("046°\n296 m", state.navigation.starboardEndpoint)
        assertEquals("14:40:48", state.navigation.eta)
        assertEquals("2 settings may affect tracking", state.deviceWarning)
        assertEquals("GATE 4", state.passBanner?.name)
        assertEquals("MARK 5", state.passBanner?.nextTarget)
    }

    @Test
    fun expandedMapRetainsRoomForCriticalNavigationAndUsesSafeFallbackBeforeMeasurement() {
        assertEquals(0.42f, RaceDeckLayoutPolicy.expandedMapTopFraction(0, 0, 16), 0f)
        assertEquals(0.40f, RaceDeckLayoutPolicy.expandedMapTopFraction(1_000, 384, 16), 0.001f)
        assertEquals(0.90f, RaceDeckLayoutPolicy.expandedMapTopFraction(1_000, 900, 16), 0f)
        assertEquals(0.836f, RaceDeckLayoutPolicy.expandedMapTopFraction(1_000, 820, 16), 0.001f)
    }

    @Test
    fun responsivePolicyUsesMapThresholdAndPhoneTabletSplits() {
        assertEquals(false, RaceDeckLayoutPolicy.shouldShowPortraitMap(799))
        assertEquals(true, RaceDeckLayoutPolicy.shouldShowPortraitMap(800))
        assertEquals(false, RaceDeckLayoutPolicy.usesTargetsSidePanel(599))
        assertEquals(true, RaceDeckLayoutPolicy.usesTargetsSidePanel(600))
        assertEquals(0.45f, RaceDeckLayoutPolicy.landscapeMapWidthFraction(599), 0f)
        assertEquals(0.62f, RaceDeckLayoutPolicy.landscapeMapWidthFraction(600), 0f)
        assertEquals(true, RaceDeckLayoutPolicy.showTargetSelector(landscape = false, screenHeightDp = 800))
        assertEquals(false, RaceDeckLayoutPolicy.showTargetSelector(landscape = false, screenHeightDp = 799))
        assertEquals(false, RaceDeckLayoutPolicy.showTargetSelector(landscape = true, screenHeightDp = 900))
        assertEquals(0.70f, RaceDeckLayoutPolicy.portraitMapTopFraction(0, 0, 16), 0f)
        assertEquals(0.72f, RaceDeckLayoutPolicy.portraitMapTopFraction(1_000, 700, 20), 0f)
        assertEquals(0.90f, RaceDeckLayoutPolicy.portraitMapTopFraction(1_000, 950, 20), 0f)
        assertEquals(0.70f, RaceDeckLayoutPolicy.portraitMapTopFraction(0, 0, 16), 0f)
        assertEquals(0.72f, RaceDeckLayoutPolicy.portraitMapTopFraction(1_000, 700, 20), 0f)
        assertEquals(0.90f, RaceDeckLayoutPolicy.portraitMapTopFraction(1_000, 950, 20), 0f)
    }

    @Test
    fun passBannerDismissesWithinTheTenSecondLimit() {
        assertEquals(10_000L, RaceDeckPassBannerState.MAX_DISPLAY_DURATION_MILLIS)
    }
}
