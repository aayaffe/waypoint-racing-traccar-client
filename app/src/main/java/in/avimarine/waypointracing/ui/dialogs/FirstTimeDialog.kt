package `in`.avimarine.waypointracing.ui.dialogs

import `in`.avimarine.waypointracing.R
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment

class FirstTimeDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;
            val v =  inflater.inflate(R.layout.dialog_firsttime, null)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(v)
                    // Add action buttons
                    .setPositiveButton(R.string.set
                    ) { _, _ ->
                        listener.onDialogPositiveClick(
                            this,
                            v.findViewById<EditText>(R.id.boatname).text.toString()
                        )
                    }
                .setNegativeButton(R.string.later
                ) { _, _ ->
                    listener.onDialogNegativeClick(this)
                }
            builder.setTitle("Enter the boat's name").create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    // Use this instance of the interface to deliver action events
    internal lateinit var listener: FirstTimeDialogListener

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface FirstTimeDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment, boatName: String)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as FirstTimeDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((context.toString() +
                    " must implement NoticeDialogListener"))
        }
    }
}