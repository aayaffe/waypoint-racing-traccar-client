package `in`.avimarine.waypointracing.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ContextThemeWrapper
import android.view.View
import android.view.View.MeasureSpec
import android.widget.TextView
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.databinding.ActivityMainBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RaceDeckLayoutTest {
    @Test
    fun positionStripStaysVisibleWithAReservedPlaceholderBeforeFirstFix() {
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(), R.style.AppTheme
        )
        val binding = ActivityMainBinding.inflate(LayoutInflater.from(context))
        binding.raceDeckUiState = RaceDeckUiState()
        binding.executePendingBindings()
        val width = (400 * context.resources.displayMetrics.density).toInt()
        val height = (800 * context.resources.displayMetrics.density).toInt()
        binding.root.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        binding.root.layout(0, 0, width, height)

        assertEquals("POS  —", binding.positionStrip.text.toString())
        assertTrue(
            binding.positionStrip.height >=
                context.resources.getDimensionPixelSize(R.dimen.race_position_min_height),
        )
    }

    @Test
    fun navigationAndPassViewsRenderFromImmutableRaceDeckState() {
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(), R.style.AppTheme
        )
        val binding = ActivityMainBinding.inflate(LayoutInflater.from(context))
        binding.raceDeckUiState = RaceDeckUiState(
            courseName = "Spring Series",
            boatName = "Sea Breeze",
            target = RaceDeckTargetMapper.map("GATE 4", isMark = false, isFinish = false).copy(
                displayLabel = "NEAREST ON GATE",
                accessibilityDescription = "Active target: GATE 4. Open targets.",
            ),
            healthLabels = RaceDeckHealthLabels(
                gps = "GPS ●",
                tracking = "TRACKING ●",
                sync = "SYNC ●",
                device = "DEVICE ✓",
            ),
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
            gpsWarning = "Last valid fix 12 seconds ago.",
            passBanner = RaceDeckPassBannerState(
                name = "GATE 4",
                eventDetails = "14:38:12\n32°49.382'N 034°58.117'E",
                nextTarget = "MARK 5",
                nextTargetLabel = "Next: MARK 5",
                isFinish = false,
                titleText = "GATE PASSED",
                icon = ContextCompat.getDrawable(context, R.drawable.ic_gate_celebration),
                uploadStatusText = "UPLOADED",
            ),
        )
        binding.executePendingBindings()

        assertEquals("038°", binding.activeTargetBearing.text.toString())
        assertEquals("284 m", binding.activeTargetDistance.text.toString())
        assertEquals("NEAREST ON GATE", binding.activeTargetType.text.toString())
        assertEquals("Active target: GATE 4. Open targets.", binding.activeTargetPanel.contentDescription)
        assertEquals("tnum", binding.activeTargetBearing.fontFeatureSettings)
        assertEquals("tnum", binding.activeTargetDistance.fontFeatureSettings)
        assertEquals("Spring Series", binding.raceDeckCourseName.text.toString())
        assertEquals("Sea Breeze", binding.raceDeckBoatName.text.toString())
        assertEquals("GPS ●", binding.raceDeckGpsStatus.text.toString())
        assertEquals("TRACKING ●", binding.raceDeckTrackingStatus.text.toString())
        assertEquals("SYNC ●", binding.raceDeckSyncStatus.text.toString())
        assertEquals("DEVICE ✓", binding.raceDeckDeviceStatus.text.toString())
        assertEquals("POS  32°49.382'N 034°58.117'E", binding.positionStrip.text.toString())
        assertEquals("GATE 4", binding.passCelebrationName.text.toString())
        assertEquals("GATE PASSED", binding.passCelebrationTitle.text.toString())
        assertEquals("14:38:12\n32°49.382'N 034°58.117'E", binding.passCelebrationEventDetails.text.toString())
        assertEquals("Next: MARK 5", binding.passCelebrationNextTarget.text.toString())
        assertEquals("UPLOADED", binding.passCelebrationUploaded.text.toString())
        assertEquals("2 settings may affect tracking", binding.deviceSetupSummary.text.toString())
        assertEquals(View.VISIBLE, binding.deviceSetupBanner.visibility)
        assertEquals(
            ContextCompat.getColor(context, R.color.race_surface_warning),
            (binding.deviceSetupBanner.background as android.graphics.drawable.ColorDrawable).color,
        )
        assertEquals("Last valid fix 12 seconds ago.", binding.gpsWarningMessage.text.toString())
        assertEquals(View.VISIBLE, binding.gpsWarningBanner.visibility)
        assertEquals(View.GONE, binding.trackingOffBanner.visibility)
        assertEquals("14:37:12", binding.raceDeckUiState?.navigation?.time)
        assertTrue(binding.raceTargetsAction.minimumHeight >=
            context.resources.getDimensionPixelSize(R.dimen.race_touch_target_min))
        assertTrue(binding.passCelebrationDismiss.layoutParams.width >=
            context.resources.getDimensionPixelSize(R.dimen.race_touch_target_min))
        assertTrue(binding.passCelebrationDismiss.layoutParams.height >=
            context.resources.getDimensionPixelSize(R.dimen.race_touch_target_min))

        val targetRow = LayoutInflater.from(context).inflate(R.layout.target_sheet_item, null, false)
        assertTrue((targetRow as ViewGroup).getChildAt(0).minimumHeight >=
            context.resources.getDimensionPixelSize(R.dimen.race_touch_target_min))
    }

    @Test
    fun gateAndPositionValuesFitAtThirteenFontScaleOnCompactPortraitWidth() {
        val applicationContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val configuration = Configuration(applicationContext.resources.configuration).apply {
            fontScale = 1.3f
        }
        val context = ContextThemeWrapper(
            applicationContext.createConfigurationContext(configuration), R.style.AppTheme
        )
        context.resources.displayMetrics.scaledDensity =
            context.resources.displayMetrics.density * configuration.fontScale
        val binding = ActivityMainBinding.inflate(LayoutInflater.from(context))
        binding.raceDeckUiState = RaceDeckUiState(
            target = RaceDeckTargetMapper.map("EAST GATE", isMark = false, isFinish = false),
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
        )
        binding.executePendingBindings()
        binding.mapFragmentView.visibility = View.GONE
        binding.gateEndpointsPanel.visibility = View.VISIBLE
        val width = (400 * context.resources.displayMetrics.density).toInt()
        val height = (800 * context.resources.displayMetrics.density).toInt()
        binding.root.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        binding.root.layout(0, 0, width, height)

        val targetContent = binding.activeTargetPanel.getChildAt(0) as ViewGroup
        assertEquals("038°", binding.activeTargetBearing.text.toString())
        assertEquals("284 m", binding.activeTargetDistance.text.toString())
        assertTrue("Target bearing has no measured height", binding.activeTargetBearing.height > 0)
        assertTrue("Target distance has no measured height", binding.activeTargetDistance.height > 0)
        assertTrue(binding.activeTargetName.bottom <= targetContent.height)
        assertTrue(binding.activeTargetType.bottom <= targetContent.height)
        assertTrue(binding.activeTargetBearing.bottom <= targetContent.height)
        assertTrue(binding.activeTargetDistance.right <= targetContent.width)
        listOf(
            binding.activeTargetName,
            binding.activeTargetType,
            binding.activeTargetBearing,
            binding.activeTargetDistance,
            binding.positionStrip,
        ).forEach(::assertTextLinesFit)
        assertTrue(binding.positionStrip.lineCount <= 2)
        val endpointValues = findTextViews(binding.gateEndpointsPanel)
            .filter { it.text.contains("\n") }
        assertEquals(2, endpointValues.size)
        assertTrue(endpointValues.all { it.lineCount <= 2 })
        endpointValues.forEach(::assertTextLinesFit)
        findTextViews(binding.raceDeckMetricsPanel)
            .filter { it.text.toString() in setOf("041°", "5.8 kt", "5.1 kt", "02:48", "14:40:48", "14:37:12") }
            .forEach(::assertTextLinesFit)
    }

    @Test
    fun gateEndpointsStayCompactAndLeaveMetricsVisibleOnTabletLandscape() {
        val applicationContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val configuration = Configuration(applicationContext.resources.configuration).apply {
            fontScale = 1.3f
            smallestScreenWidthDp = 600
        }
        val context = ContextThemeWrapper(
            applicationContext.createConfigurationContext(configuration), R.style.AppTheme
        )
        val binding = ActivityMainBinding.inflate(LayoutInflater.from(context))
        binding.raceDeckUiState = RaceDeckUiState(
            navigation = RaceDeckNavigationState(
                bearing = "038°", distance = "284 m", portEndpoint = "031°\n302 m",
                starboardEndpoint = "046°\n296 m", cog = "041°", sog = "5.8 kt",
                vmg = "5.1 kt", ttg = "02:48", eta = "14:40:48", time = "14:37:12",
                position = "32°49.382'N 034°58.117'E",
            ),
        )
        binding.executePendingBindings()
        binding.mapFragmentView.visibility = View.GONE
        binding.gateEndpointsPanel.visibility = View.VISIBLE
        val width = (1280 * context.resources.displayMetrics.density).toInt()
        val height = (800 * context.resources.displayMetrics.density).toInt()
        binding.root.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        binding.root.layout(0, 0, width, height)

        val compactPanelLimit = (140 * context.resources.displayMetrics.density).toInt()
        assertTrue("Gate endpoint panel expanded over navigation metrics", binding.gateEndpointsPanel.height <= compactPanelLimit)
        assertTrue(binding.positionStrip.top >= binding.gateEndpointsPanel.bottom)
        assertTrue(binding.raceDeckMetricsPanel.top >= binding.positionStrip.bottom)
        assertTrue(binding.raceDeckMetricsPanel.bottom <= binding.root.height)
    }

    @Test
    fun passAndTargetSurfacesUseNightPaletteResources() {
        val applicationContext = ApplicationProvider.getApplicationContext<android.content.Context>()
        val configuration = Configuration(applicationContext.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                Configuration.UI_MODE_NIGHT_YES
        }
        val context = ContextThemeWrapper(
            applicationContext.createConfigurationContext(configuration), R.style.AppTheme
        )
        val binding = ActivityMainBinding.inflate(LayoutInflater.from(context))
        binding.raceDeckUiState = RaceDeckUiState(
            target = RaceDeckTargetMapper.map("MARK 5", isMark = true, isFinish = false),
            passBanner = RaceDeckPassBannerState(
                name = "MARK 4",
                eventDetails = "14:38:12\n32°49.382'N 034°58.117'E",
                nextTarget = "MARK 5",
                nextTargetLabel = "Next: MARK 5",
                isFinish = false,
                titleText = "GATE PASSED",
                icon = ContextCompat.getDrawable(context, R.drawable.ic_gate_celebration),
                uploadStatusText = "UPLOADED",
            ),
        )
        binding.executePendingBindings()
        val panelColor = ContextCompat.getColor(context, R.color.race_surface_panel)

        assertEquals(panelColor, binding.activeTargetPanel.cardBackgroundColor.defaultColor)
        assertEquals(panelColor, binding.passCelebrationCard.cardBackgroundColor.defaultColor)

        val targetRow = LayoutInflater.from(context).inflate(R.layout.target_sheet_item, null, false)
        val targetCard = targetRow.findViewById<com.google.android.material.card.MaterialCardView>(R.id.targetCard)
        assertEquals(panelColor, targetCard.cardBackgroundColor.defaultColor)
        assertEquals(
            ContextCompat.getColor(context, R.color.race_text_primary),
            targetRow.findViewById<TextView>(R.id.targetName).currentTextColor,
        )
    }

    @Test
    fun mapActionUsesTheSameNeutralStyleAsOtherBottomActions() {
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(), R.style.AppTheme
        )
        val binding = ActivityMainBinding.inflate(LayoutInflater.from(context))
        assertEquals(
            binding.raceTargetsAction.backgroundTintList?.defaultColor,
            binding.raceMapAction.backgroundTintList?.defaultColor,
        )
        assertEquals(
            binding.raceTargetsAction.compoundDrawableTintList?.defaultColor,
            binding.raceMapAction.compoundDrawableTintList?.defaultColor,
        )
        assertEquals(
            binding.raceTargetsAction.textColors.defaultColor,
            binding.raceMapAction.textColors.defaultColor,
        )
        assertEquals(R.id.navigation_view, binding.mapFragmentView.accessibilityTraversalAfter)
        assertEquals(R.id.raceBottomActions, binding.mapFragmentView.accessibilityTraversalBefore)
    }

    @Test
    fun restrictedBackgroundUseReceivesCriticalDeviceTreatment() {
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(), R.style.AppTheme
        )
        val binding = ActivityMainBinding.inflate(LayoutInflater.from(context))
        binding.raceDeckUiState = RaceDeckUiState(
            healthLabels = RaceDeckHealthLabels(device = "DEVICE ✕"),
            deviceWarning = context.getString(R.string.race_deck_background_restricted_detail),
            deviceWarningCritical = true,
        )
        binding.executePendingBindings()

        assertEquals("DEVICE ✕", binding.raceDeckDeviceStatus.text.toString())
        assertEquals(
            context.getString(R.string.race_deck_background_restricted_banner),
            binding.deviceSetupTitle.text.toString(),
        )
        assertEquals(
            ContextCompat.getColor(context, R.color.race_surface_critical),
            (binding.deviceSetupBanner.background as android.graphics.drawable.ColorDrawable).color,
        )
        assertEquals(
            ContextCompat.getColor(context, R.color.race_critical),
            binding.deviceSetupTitle.currentTextColor,
        )
    }

    private fun findTextViews(root: ViewGroup): List<TextView> = buildList {
        for (index in 0 until root.childCount) {
            val child = root.getChildAt(index)
            if (child is TextView) add(child)
            if (child is ViewGroup) addAll(findTextViews(child))
        }
    }

    private fun assertTextLinesFit(textView: TextView) {
        val textLayout = textView.layout ?: return
        val availableWidth = (textView.width - textView.paddingLeft - textView.paddingRight).toFloat()
        for (line in 0 until textLayout.lineCount) {
            assertTrue(
                "Text clips horizontally at 1.3× font scale: ${textView.text}",
                textLayout.getLineWidth(line) <= availableWidth,
            )
        }
    }
}
