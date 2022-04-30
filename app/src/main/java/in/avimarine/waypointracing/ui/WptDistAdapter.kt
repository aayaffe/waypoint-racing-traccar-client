package `in`.avimarine.waypointracing.ui

import `in`.avimarine.waypointracing.ManualInputFragment
import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.utils.timeStamptoDateString
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class WptDistAdapter(maxDistance: Double) :
    ListAdapter<ManualInputFragment.WptDistance, WptDistAdapter.WptDistViewHolder>(WptDiffCallback) {

    val maxDist = maxDistance

    inner class WptDistViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.wpt_name)
        private val latTextView: TextView = itemView.findViewById(R.id.latTextView)
        private val lonTextView: TextView = itemView.findViewById(R.id.lonTextView)
        private val distTextView: TextView = itemView.findViewById(R.id.distTextView)
        private val passedImageView: ImageView = itemView.findViewById(R.id.passed_image)
        private var currentRec: ManualInputFragment.WptDistance? = null

        /* Bind Route Element particulars. */
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(rec: ManualInputFragment.WptDistance) {
            currentRec = rec
            nameTextView.text = rec.name
            distTextView.text = String.format("%.2f", rec.dist)
            lonTextView.text = String.format("%010.06f", rec.lon)
            latTextView.text = String.format("%09.06f", rec.lat)
            if (rec.dist <= maxDist) {
                passedImageView.setImageResource(R.drawable.ic_baseline_check_24)
            } else {
                passedImageView.setImageResource(R.drawable.ic_baseline_x_24)
            }
        }
    }



    /* Creates and inflates view and return FlowerViewHolder. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WptDistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.wptdist_item, parent, false)
        return WptDistViewHolder(view)
    }

    /* Gets current gate and uses it to bind view. */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: WptDistViewHolder, position: Int) {
        val rec = getItem(position)
        holder.bind(rec)

    }
}

object WptDiffCallback : DiffUtil.ItemCallback<ManualInputFragment.WptDistance>() {
    override fun areItemsTheSame(
        oldItem: ManualInputFragment.WptDistance,
        newItem: ManualInputFragment.WptDistance
    ): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: ManualInputFragment.WptDistance,
        newItem: ManualInputFragment.WptDistance
    ): Boolean {
        return areItemsTheSame(oldItem, newItem)
    }
}