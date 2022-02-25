package `in`.avimarine.waypointracing.activities

import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.database.FirestoreDatabase
import `in`.avimarine.waypointracing.database.OnlineDatabase
import `in`.avimarine.waypointracing.databinding.ActivityExpertModeBinding
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

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
            val intent = Intent(this, LoadRouteActivity::class.java)
            this.startActivity(intent)
        }


    }
}