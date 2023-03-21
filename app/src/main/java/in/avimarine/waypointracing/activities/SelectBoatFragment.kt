package `in`.avimarine.waypointracing.activities

import `in`.avimarine.waypointracing.*
import `in`.avimarine.waypointracing.database.Boat
import `in`.avimarine.waypointracing.database.FirestoreDatabase
import `in`.avimarine.waypointracing.databinding.FragmentSelectBoatBinding
import `in`.avimarine.waypointracing.ui.RouteViewHolder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import `in`.avimarine.androidutils.TAG
import kotlinx.coroutines.ExperimentalCoroutinesApi

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SelectBoatFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SelectBoatFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var viewBinding: FragmentSelectBoatBinding
    private val viewModel: ExpertViewModel by activityViewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        viewBinding = FragmentSelectBoatBinding.inflate(inflater, container, false)
        populateBoatsList()
        viewBinding.addBoatBtn.setOnClickListener{ addBoat() }
        return viewBinding.root
    }

    private fun addBoat() {
        if (viewBinding.boatNameTxtView.text.isBlank()){
            Toast.makeText(requireContext(), "No Name present", Toast.LENGTH_LONG).show()
        }
        val b = Boat(viewBinding.boatNameTxtView.text.toString(), "", "Manual")
        FirestoreDatabase.addBoat(b,viewBinding.boatNameTxtView.text.toString())
    }

    private fun populateBoatsList() {
        val query: Query = FirebaseFirestore.getInstance()
            .collection("boats")
            .limit(50)
        val options = FirestoreRecyclerOptions.Builder<Boat>()
            .setQuery(query, Boat::class.java).setLifecycleOwner(this)
            .build()
        val adapter = object : FirestoreRecyclerAdapter<Boat, RecyclerView.ViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
                val view = LayoutInflater.from(requireContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false)
                return RouteViewHolder(view)

            }

            override fun onBindViewHolder(
                holder: RecyclerView.ViewHolder,
                position: Int,
                model: Boat
            ) {
                val tvName: TextView = holder.itemView.findViewById(android.R.id.text1)
                val tvOrganizer: TextView = holder.itemView.findViewById(android.R.id.text2)
                tvName.text = model.name
                tvOrganizer.text = model.skipperName
                val documentId = snapshots.getSnapshot(position).id
                holder.itemView.setOnClickListener {
                    selectBoat(documentId, model.name)
                }
            }
        }
        viewBinding.BoatsRecyclerView.adapter = adapter
        viewBinding.BoatsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun selectBoat(id: String, name: String) {
        Log.d(TAG, "Selected boat id = $id")
        viewModel.updateBoatName(name)
        parentFragmentManager.commit {
            replace<ManualInputFragment>(R.id.fragment_container_view)
            setReorderingAllowed(true)
            addToBackStack("manualInput") // name can be null
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SelectBoatFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SelectBoatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}