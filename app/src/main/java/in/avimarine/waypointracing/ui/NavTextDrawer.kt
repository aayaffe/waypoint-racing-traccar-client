package `in`.avimarine.waypointracing.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import `in`.avimarine.waypointracing.R
//import kotlinx.android.synthetic.main.navtextdrawer.view.*
import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.databinding.NavtextdrawerBinding
import android.graphics.Color
import android.os.Build
import android.widget.TextView.AUTO_SIZE_TEXT_TYPE_NONE
import android.widget.TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM


class NavTextDrawer(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private var binding: NavtextdrawerBinding = NavtextdrawerBinding.inflate(LayoutInflater.from(context),this, true)
    private var unitsEnabled = true
    private var labelEnabled = true
    private var dataText: String? = null
    private var unitsText: String? = null
    private var labelText: String? = null
    private var textColor = Color.BLACK


    init {
        setupAttributes(attrs)
    }
    fun setLabel(s: String?) {
        if (s==null){
            binding.labelTv.text = ""
        }
        else {
            binding.labelTv.text = s
        }
    }
    fun setData(s: String?) {
        if (s==null){
            binding.dataTV.text = ""
        }
        else {
            binding.dataTV.text = s
        }
    }
    fun setUnits(s: String?) {
        if (s==null){
            binding.unitsTv.text = ""
        }
        else {
            binding.unitsTv.text = s
        }
    }

    fun setTextColor(c: Int){
        binding.dataTV.setTextColor(c)
    }

    private fun setupAttributes(attrs: AttributeSet?) {
        Log.d(TAG,"SetupAttributes")
        // Obtain a typed array of attributes
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.NavTextDrawer,
                0, 0)
        // Extract custom attributes into member variables
        labelText = typedArray.getString(R.styleable.NavTextDrawer_labelText)
        unitsText = typedArray.getString(R.styleable.NavTextDrawer_unitsText)
        dataText = typedArray.getString(R.styleable.NavTextDrawer_dataText)
        labelEnabled = typedArray.getBoolean(R.styleable.NavTextDrawer_labelEnabled,true)
        unitsEnabled = typedArray.getBoolean(R.styleable.NavTextDrawer_unitsEnabled,true)
        textColor = typedArray.getInt(R.styleable.NavTextDrawer_textColor, Color.BLACK)
        val dataTextSize = typedArray.getDimension(R.styleable.NavTextDrawer_dataTextSize, 12f)
        binding.dataTV.textSize = dataTextSize

        setLabel(labelText)
        setUnits(unitsText)
        setData(dataText)
        setTextColor(textColor)
        if (!labelEnabled){
            binding.labelTv.visibility = View.GONE
        }
        if (!unitsEnabled){
            binding.unitsTv.visibility = View.GONE
        }
        if (typedArray.getBoolean(R.styleable.NavTextDrawer_autoTextSize,false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                binding.dataTV.setAutoSizeTextTypeWithDefaults(AUTO_SIZE_TEXT_TYPE_UNIFORM)
                binding.unitsTv.setAutoSizeTextTypeWithDefaults(AUTO_SIZE_TEXT_TYPE_UNIFORM)
                binding.labelTv.setAutoSizeTextTypeWithDefaults(AUTO_SIZE_TEXT_TYPE_UNIFORM)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                binding.dataTV.setAutoSizeTextTypeWithDefaults(AUTO_SIZE_TEXT_TYPE_NONE)
                binding.unitsTv.setAutoSizeTextTypeWithDefaults(AUTO_SIZE_TEXT_TYPE_NONE)
                binding.labelTv.setAutoSizeTextTypeWithDefaults(AUTO_SIZE_TEXT_TYPE_NONE)
            }
        }
        // TypedArray objects are shared and must be recycled.
        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas?) {
        Log.d(TAG,"onDraw")
        super.onDraw(canvas)

    }

}