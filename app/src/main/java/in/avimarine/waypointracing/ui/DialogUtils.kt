package `in`.avimarine.waypointracing.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.widget.Toast
import `in`.avimarine.waypointracing.R

class DialogUtils {
    companion object {
        fun show(context: Context, message: String) {
            val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
            if (toast != null) {
                toast.setText(message)
                toast.show()
            }
        }

        fun showLong(context: Context?, message: String) {
            val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            if (toast != null) {
                toast.setText(message)
                toast.show()
            }
        }

        fun createDialog(
            context: Context?, titleId: Int, messageId: Int,
            positiveButtonListener: DialogInterface.OnClickListener?,
            negativeButtonListener: DialogInterface.OnClickListener?,
            onDismissListener: DialogInterface.OnDismissListener
        ): Dialog {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(titleId)
            builder.setMessage(messageId)
            builder.setPositiveButton(R.string.dlg_ok, positiveButtonListener)
            builder.setNegativeButton(R.string.dlg_cancel, negativeButtonListener)
            builder.setOnDismissListener(onDismissListener)
            return builder.create()
        }
    }
}