package `in`.avimarine.waypointracing.activities

import `in`.avimarine.waypointracing.database.EventType
import `in`.avimarine.waypointracing.database.FirestoreDatabase
import `in`.avimarine.waypointracing.databinding.ActivityLoadRouteBinding
import `in`.avimarine.waypointracing.route.RouteDetails
import `in`.avimarine.waypointracing.ui.RouteViewHolder
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


class LoadRouteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoadRouteBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadRouteBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        populateRoutesList()
        setTitle("Select route", "")
    }

    private fun populateRoutesList(){
        val query: Query = FirebaseFirestore.getInstance()
            .collection("routes")
            .limit(50)
        val options = FirestoreRecyclerOptions.Builder<RouteDetails>()
            .setQuery(query, RouteDetails::class.java).setLifecycleOwner(this)
            .build()
        val adapter = object: FirestoreRecyclerAdapter<RouteDetails, RouteViewHolder> (options){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
                val view = LayoutInflater.from(this@LoadRouteActivity).inflate(android.R.layout.simple_list_item_2,parent,false)
                return RouteViewHolder(view)

            }

            override fun onBindViewHolder(holder: RouteViewHolder, position: Int, model: RouteDetails) {
                val tvName: TextView = holder.itemView.findViewById(android.R.id.text1)
                val tvOrganizer: TextView = holder.itemView.findViewById(android.R.id.text2)
                tvName.text = model.name
                tvOrganizer.text = model.organizing
                holder.itemView.setOnClickListener {
                    selectRoute(model.route, model.name, model.lastUpdate)
                }
            }
        }

        binding.RoutesRecyclerView.adapter = adapter
        binding.RoutesRecyclerView.layoutManager = LinearLayoutManager(this)
    }
    private fun setTitle(title: String, subTitle: String) {
        val ab = supportActionBar
        ab?.title = title
        ab?.subtitle = subTitle
    }

    private fun selectRoute(json: String, name: String, lastUpdate: String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Are you sure?")
        builder.setMessage("Loading new route will delete all previous mark passings and reset route")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes"){ _, _ ->
            val i = Intent()
            i.putExtra("RouteJson", json)
            setResult(RESULT_OK, i)
            FirestoreDatabase.addEvent(EventType.LOAD_ROUTE, "$name $lastUpdate")
            finish()
        }
        builder.setNegativeButton("No"){ _, _ ->
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
}