package `in`.avimarine.waypointracing.activities

import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.activities.steps.StepperViewModel
import `in`.avimarine.waypointracing.databinding.ActivitySetupWizardBinding
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.aceinteract.android.stepper.StepperNavListener
import com.aceinteract.android.stepper.StepperNavigationView

class SetupWizardActivity : AppCompatActivity(), StepperNavListener {

    private lateinit var binding: ActivitySetupWizardBinding
    lateinit var stepper: StepperNavigationView
    private val viewModel: StepperViewModel by lazy { ViewModelProvider(this)[StepperViewModel::class.java] }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupWizardBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        stepper = findViewById(R.id.stepper)
        stepper.setupWithNavController(findNavController(R.id.frame_stepper))
        stepper.stepperNavListener = this
        binding.buttonNext.setOnClickListener {
            if (validateStep())
                stepper.goToNextStep()
        }

        setupActionBarWithNavController(
            findNavController(R.id.frame_stepper),
            AppBarConfiguration.Builder(
                R.id.step_1_login,
                R.id.step_2_boat,
                R.id.step_3_route,
                R.id.step_4_permissions
            ).build()
        )
    }

    private fun validateStep(): Boolean {
        return viewModel.validateFragment()
    }

    public fun nextStep(){
        stepper.goToNextStep()
    }

    override fun onStepChanged(step: Int) {
        Log.d(TAG, "step = $step")
//        if (step == 1) {
//            stepper.goToPreviousStep()
//        }
    }

    override fun onCompleted() {

//        Toast.makeText(this, "Stepper completed", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (stepper.currentStep == 0) {
            super.onBackPressed()
        } else {
            findNavController(R.id.frame_stepper).navigateUp()
        }
    }
}