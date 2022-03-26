package `in`.avimarine.waypointracing.ui

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

class RouteElementFullAdapter() :
    ListAdapter<RouteElementConcat, RouteElementFullAdapter.RouteElementConcatViewHolder>(RouteElementConcatDiffCallback) {

    /* ViewHolder for Flower, takes in the inflated view and the onClick behavior. */
    class RouteElementConcatViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.route_element_name)
        private val gatePassTextView: TextView = itemView.findViewById(R.id.last_gate_pass)
        private val passedImageView: ImageView = itemView.findViewById(R.id.passed_image)
        private val typeImageView: ImageView = itemView.findViewById(R.id.route_element_type_image)

        private var currentRec: RouteElementConcat? = null

        /* Bind Route Element particulars. */
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(rec: RouteElementConcat) {
            currentRec = rec
            nameTextView.text = rec.re.name
            if (rec.gp != null) {
                passedImageView.setImageResource(R.drawable.ic_baseline_check_24)
                gatePassTextView.text = "Last Gate Pass: " + timeStamptoDateString(rec.gp.time.time)
            } else {
                passedImageView.setImageResource(R.drawable.ic_baseline_x_24)
                gatePassTextView.text = "Last Gate Pass: Never"
            }
        }
    }

    /* Creates and inflates view and return FlowerViewHolder. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteElementConcatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.route_element_concat_item, parent, false)
        return RouteElementConcatViewHolder(view)
    }

    /* Gets current gate and uses it to bind view. */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RouteElementConcatViewHolder, position: Int) {
        val rec = getItem(position)
        holder.bind(rec)

    }
}

object RouteElementConcatDiffCallback : DiffUtil.ItemCallback<RouteElementConcat>() {
    override fun areItemsTheSame(oldItem: RouteElementConcat, newItem: RouteElementConcat): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: RouteElementConcat, newItem: RouteElementConcat): Boolean {
        return areItemsTheSame(oldItem,newItem)
    }
}