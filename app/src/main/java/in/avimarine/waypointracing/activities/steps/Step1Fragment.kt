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

import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.activities.SetupWizardActivity
import `in`.avimarine.waypointracing.databinding.LoginFragmentBinding
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Fragment for holding and controlling views for the first step.
 */
@ExperimentalCoroutinesApi
class Step1Fragment : SetupFragment() {

    private lateinit var viewBinding: LoginFragmentBinding

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }


    override fun validateFragment(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    /**
     * Setup view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = LoginFragmentBinding.inflate(inflater, container, false)
        setupUI()
        viewModel.setCurrentFragment(this)
        return viewBinding.root
    }


    private fun setupUI() {
        viewBinding.button2.setOnClickListener {
            launchAuthenticationProcess()
        }
    }

    private fun launchAuthenticationProcess() {
        if (FirebaseAuth.getInstance().currentUser!=null){
            return
        }
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build())

        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()
        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                Log.d(TAG, "Logged in as ${user.displayName}")
                (activity as SetupWizardActivity?)!!.nextStep()
            }
        } else {
            if (response != null) {
                Log.e(TAG, "Error authenticating ${response.error?.errorCode}")
            } else {
                Log.e(TAG, "User canceled sign in")
            }
        }
    }
}
