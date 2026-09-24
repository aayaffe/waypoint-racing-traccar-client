package `in`.avimarine.waypointracing.activities

import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

abstract class EdgeToEdgeActivity : AppCompatActivity() {

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applySystemBarInsets()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        applySystemBarInsets()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        applySystemBarInsets()
    }

    private fun applySystemBarInsets() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return
        }

        val content = findViewById<ViewGroup>(android.R.id.content)
        val root = content.getChildAt(0) ?: return
        val initialPadding = Insets.of(
            root.paddingLeft,
            root.paddingTop,
            root.paddingRight,
            root.paddingBottom
        )
        val actionBarHeight = supportActionBar?.let {
            val value = TypedValue()
            if (theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, value, true)) {
                TypedValue.complexToDimensionPixelSize(value.data, resources.displayMetrics)
            } else {
                0
            }
        } ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = initialPadding.left + systemBars.left,
                top = initialPadding.top + systemBars.top + actionBarHeight,
                right = initialPadding.right + systemBars.right,
                bottom = initialPadding.bottom + systemBars.bottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(root)
    }
}
