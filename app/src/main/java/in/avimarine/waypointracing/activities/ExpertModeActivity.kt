package `in`.avimarine.waypointracing.activities

import `in`.avimarine.waypointracing.Position
import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.database.FirestoreDatabase
import `in`.avimarine.waypointracing.database.OnlineDatabase
import `in`.avimarine.waypointracing.databinding.ActivityExpertModeBinding
import `in`.avimarine.waypointracing.route.GatePassing
import android.content.Intent
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class ExpertModeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExpertModeBinding
    private val db: OnlineDatabase = FirestoreDatabase()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpertModeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.loadRoutesBtn.setOnClickListener {
            db.getRoutesNames({
                binding.editTextTextMultiLine.text = it.joinToString("\n")
            },
                {
                    Log.w(TAG, "Error getting documents.", it)
                })
            db.getRoutes({
                for (document in it) {
                    Log.d(TAG, "${document.id} => ${document.data}")
                }
            },
                {
                    Log.w(TAG, "Error getting documents.", it)
                })
            db.getRoute("MX71bw0uvRoK0TyF6yMd", {
                if (it != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${it.data}")
                } else {
                    Log.d(TAG, "No such document")
                }
            },
                {
                    Log.w(TAG, "Error getting documents.", it)
                })
        }
        binding.loadRoutesBtn2.setOnClickListener {
            val gp = GatePassing(1, "EventName", "RouteId", "deviceId", "BoatName", 12, "Gate12", Date(), 32.0,35.0)
                val db = Firebase.firestore
                db.collection("reports")
                    .add(gp)
                    .addOnSuccessListener { documentReference ->
                        Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error adding document", e)
                    }

        }


    }
}