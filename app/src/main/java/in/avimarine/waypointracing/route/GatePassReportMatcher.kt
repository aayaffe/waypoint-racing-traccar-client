package `in`.avimarine.waypointracing.route

object GatePassReportMatcher {
    fun matches(
        pass: GatePassing,
        reportSourceEventId: String?,
        reportTimeMillis: Long?,
        reportDeviceId: String?,
    ): Boolean = if (pass.sourceEventId.isNotEmpty()) {
        pass.sourceEventId == reportSourceEventId
    } else {
        pass.time.time == reportTimeMillis && pass.deviceId == reportDeviceId
    }
}
