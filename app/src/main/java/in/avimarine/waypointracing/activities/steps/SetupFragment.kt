package `in`.avimarine.waypointracing.activities.steps

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

abstract class SetupFragment : Fragment() {
    abstract fun validateFragment():Boolean
    protected val viewModel: StepperViewModel by lazy { ViewModelProvider(requireActivity())[StepperViewModel::class.java] }


}