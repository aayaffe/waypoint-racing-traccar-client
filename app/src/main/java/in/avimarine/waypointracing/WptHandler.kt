package `in`.avimarine.waypointracing

import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.route.RouteElement

interface WptHandler {
    fun onWptUpdate(route: Route, wpt: RouteElement)
}