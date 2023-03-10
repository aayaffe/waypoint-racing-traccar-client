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
package `in`.avimarine.waypointracing.activities.steps

import `in`.avimarine.waypointracing.BatteryOptimizationHelper
import `in`.avimarine.waypointracing.databinding.GrantPermissionFragmentBinding
import `in`.avimarine.androidutils.LocationPermissions
import android.Manifest
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Fragment for holding and controlling views for the third step.
 */
@ExperimentalCoroutinesApi
class Step4Fragment : SetupFragment() {

    private lateinit var viewBinding: GrantPermissionFragmentBinding

    override fun validateFragment(): Boolean {
        return BatteryOptimizationHelper().requestedExceptions(requireContext()) && LocationPermissions.arePermissionsGranted(activity as Context)
    }

    /**
     * Setup view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = GrantPermissionFragmentBinding.inflate(inflater, container, false)
        setupUI()
        viewModel.setCurrentFragment(this)
        return viewBinding.root
    }

    private fun setupUI() {
        viewBinding.locationPermissionsBtn.setOnClickListener {
//            getLocationPermissions()
            if (LocationPermissions.arePermissionsGranted(activity as Context)) {
                viewBinding.locationPermissionsBtn.setBackgroundColor(Color.GREEN)
                viewBinding.locationPermissionsBtn.isEnabled = false
            } else {
                activity?.let { it1 ->
                    LocationPermissions.askForLocationPermission(
                        it1,
                        LocationPermissions.PERMISSIONS_REQUEST_LOCATION_UI
                    )
                }
                viewBinding.locationPermissionsBtn.setBackgroundColor(Color.GREEN)

            }
        }
        viewBinding.preventSleepBtn.setOnClickListener {
            getNoSleepException()
        }
    }

    private fun getNoSleepException() {
        BatteryOptimizationHelper().requestException(requireContext())
        viewBinding.preventSleepBtn.setBackgroundColor(Color.GREEN)
        viewBinding.preventSleepBtn.isEnabled = false
    }

//    private fun getLocationPermissions() {
//        activity?.let {
//            if (arePermissionsGranted(activity as Context)) {
//                viewBinding.locationPermissionsBtn.setBackgroundColor(Color.GREEN)
//                viewBinding.locationPermissionsBtn.isEnabled = false
//            } else {
//                permReqLauncher.launch(
//                    PERMISSIONS
//                )
//            }
//        }
//    }
    // util method
//    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean = permissions.all {
//        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
//    }

    companion object {
        var PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }

    private val permReqLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value == true
            }
            if (granted) {
                viewBinding.locationPermissionsBtn.setBackgroundColor(Color.GREEN)
                viewBinding.locationPermissionsBtn.isEnabled = false
            }
        }
}
