/*
 * Copyright 2020 Ayomide Falobi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package `in`.avimarine.waypointracing

import `in`.avimarine.waypointracing.activities.StepperSettings
import `in`.avimarine.waypointracing.route.Route
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * View model for managing a stepper activity.
 */
@ExperimentalCoroutinesApi
class ExpertViewModel : ViewModel() {

    private val _route = MutableStateFlow(Route())
    private val _boatName = MutableStateFlow(String())
    /**
     * Public immutable accessor for [_route].
     */
    val route: StateFlow<Route> get() = _route
    val boatName: StateFlow<String> get() = _boatName


    fun updateRoute(newRoute: Route) {
        _route.value = newRoute
    }

    fun updateBoatName(newName: String) {
        _boatName.value = newName
    }


}
