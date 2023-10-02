/*
 * Copyright 2020 Ayomide Falobi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package `in`.avimarine.waypointracing.activities.steps
import `in`.avimarine.waypointracing.activities.SetupWizardActivity
import `in`.avimarine.waypointracing.databinding.LoadRouteFragmentBinding
import `in`.avimarine.waypointracing.route.RouteDetails
import `in`.avimarine.waypointracing.route.RouteLoader
import `in`.avimarine.waypointracing.ui.RouteViewHolder
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Fragment for holding and controlling views for the third step.
 */
@ExperimentalCoroutinesApi
class Step3Fragment : SetupFragment() {

    private lateinit var viewBinding: LoadRouteFragmentBinding

    override fun validateFragment(): Boolean {
        val r = RouteLoader.loadRouteFromFile(requireContext())
        return !r.isEmpty()
    }

    /**
     * Setup view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = LoadRouteFragmentBinding.inflate(inflater, container, false)
        viewModel.setCurrentFragment(this)
        populateRoutesList()
        return viewBinding.root
    }

    private fun populateRoutesList(){
        val query: Query = FirebaseFirestore.getInstance()
            .collection("routes")
            .limit(50)
        val options = FirestoreRecyclerOptions.Builder<RouteDetails>()
            .setQuery(query, RouteDetails::class.java).setLifecycleOwner(this)
            .build()
        val adapter = object: FirestoreRecyclerAdapter<RouteDetails, RouteViewHolder>(options){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
                val view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2,parent,false)
                return RouteViewHolder(view)

            }
            override fun onBindViewHolder(holder: RouteViewHolder, position: Int, model: RouteDetails) {
                val tvName: TextView = holder.itemView.findViewById(android.R.id.text1)
                val tvOrganizer: TextView = holder.itemView.findViewById(android.R.id.text2)
                tvName.text = model.name
                tvOrganizer.text = model.organizing
                holder.itemView.setOnClickListener {
                    selectRoute(model.route)
                }
            }
        }

        viewBinding.RoutesRecyclerView.adapter = adapter
        viewBinding.RoutesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        viewBinding.RoutesRecyclerView.itemAnimator = null
    }

    private fun selectRoute(json: String){
        RouteLoader.loadRouteFromString(requireContext(), json) {
            (activity as SetupWizardActivity?)!!.nextStep()
        }
    }
}
