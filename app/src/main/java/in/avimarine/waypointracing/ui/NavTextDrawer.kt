package `in`.avimarine.waypointracing.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import `in`.avimarine.waypointracing.R
import kotlinx.android.synthetic.main.navtextdrawer.view.*
import `in`.avimarine.waypointracing.TAG



class NavTextDrawer(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {


    private var unitsEnabled = true
    private var labelEnabled = true
    private var dataText: String? = null
    private var unitsText: String? = null
    private var labelText: String? = null

    init {
        Log.d(TAG, "InInit")
        LayoutInflater.from(context).inflate(R.layout.navtextdrawer, this)
        setupAttributes(attrs)
    }
    fun setLabel(s: String?) {
        if (s==null){
            labelTv.text = ""
        }
        else {
            labelTv.text = s
        }
    }
    fun setData(s: String?) {
        if (s==null){
            dataTV.text = ""
        }
        else {
            dataTV.text = s
        }
    }
    fun setUnits(s: String?) {
        if (s==null){
            unitsTv.text = ""
        }
        else {
            unitsTv.text = s
        }
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
        val dataTextSize = typedArray.getDimension(R.styleable.NavTextDrawer_dataTextSize, 12f)
        dataTV.textSize = dataTextSize
        // TypedArray objects are shared and must be recycled.
        typedArray.recycle()
        setLabel(labelText)
        setUnits(unitsText)
        setData(dataText)

        if (!labelEnabled){
            labelTv.visibility = View.INVISIBLE
        }
        if (!unitsEnabled){
            unitsTv.visibility = View.INVISIBLE
        }
    }

    override fun onDraw(canvas: Canvas?) {
        Log.d(TAG,"onDraw")
        super.onDraw(canvas)

    }

}