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

import `in`.avimarine.waypointracing.Boat
import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.database.FirestoreDatabase
import `in`.avimarine.waypointracing.databinding.LoginFragmentBinding
import `in`.avimarine.waypointracing.databinding.SetBoatFragmentBinding
import `in`.avimarine.waypointracing.route.GatePassing
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Fragment for holding and controlling views for the first step.
 */
@ExperimentalCoroutinesApi
class Step2Fragment : SetupFragment() {

    private lateinit var viewBinding: SetBoatFragmentBinding


    override fun validateFragment(): Boolean {
        if (viewBinding.boatname.text.isBlank()){
            viewBinding.boatname.error = "This must not be blank"
            return false
        }
        else {
            val b = Boat(viewBinding.boatname.text.toString(), viewBinding.sailNumber.text.toString(),viewBinding.skipper.text.toString())
            FirestoreDatabase.addBoat(b, FirebaseAuth.getInstance().currentUser?.uid ?: "") //TODO: Fix issues when user not logged in
        }
        return true
    }

    /**
     * Setup view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = SetBoatFragmentBinding.inflate(inflater, container, false)
        viewModel.setCurrentFragment(this)
        FirestoreDatabase.getBoat(FirebaseAuth.getInstance().currentUser?.uid ?: "", {
            if (it != null) {
                val boat = it.toObject<Boat>()
                if (boat != null) {
                    viewBinding.boatname.setText(boat.name)
                    viewBinding.sailNumber.setText(boat.sailNumber)
                    viewBinding.skipper.setText(boat.skipperName)
                }
            }
        },{
            Log.e(TAG, "Failed to load boat", it)
        })
        return viewBinding.root
    }

}
