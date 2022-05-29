package `in`.avimarine.waypointracing.database

import `in`.avimarine.waypointracing.Boat
import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.route.GatePassing
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

class FirestoreDatabase {

    companion object {
        fun getRoutesNames(onSuccess: (List<String>) -> Unit, onFailure: OnFailureListener){
            getRoutes({
                val names = arrayListOf<String>()
                for (doc in it){
                    doc.getString("name")?.let { it1 -> names.add(it1) }
                }
                onSuccess(names)
            }, onFailure)
        }
        fun getRoutes(onSuccess: (QuerySnapshot) -> Unit, onFailure: OnFailureListener) {
            Firebase.firestore.collection("routes").limit(10)
                .get()
                .addOnSuccessListener (onSuccess)
                .addOnFailureListener (onFailure)
        }

        fun getRoute(id: String,onSuccess: (DocumentSnapshot?) -> Unit, onFailure: OnFailureListener) {
            val docRef = Firebase.firestore.collection("routes").document(id)
            docRef.get()
                .addOnSuccessListener (onSuccess)
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
        }
        fun addBoat(b: Boat, uid: String) {
            val db = Firebase.firestore
            db.collection("boats").document(uid).set(b)
        }
        fun updateBoatName(n:String, uid: String){
            getBoat(FirebaseAuth.getInstance().currentUser?.uid ?: "", {
                if (it != null) {
                    val boat = it.toObject<Boat>()
                    if (boat != null) {
                        val newBoat = Boat(n, boat.sailNumber, boat.skipperName)
                        addBoat(newBoat, uid)
                    } else {
                        val newBoat = Boat(n, "","")
                        addBoat(newBoat, uid)
                    }
                } else {
                    val newBoat = Boat(n, "","")
                    addBoat(newBoat, uid)
                }
            },{
                val newBoat = Boat(n, "","")
                addBoat(newBoat, uid)
            })
        }

        fun getBoat(uid: String,onSuccess: (DocumentSnapshot?) -> Unit, onFailure: OnFailureListener) {
            val db = Firebase.firestore
            val docRef = db.collection("boats").document(uid)
            docRef.get()
                .addOnSuccessListener (onSuccess)
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
        }

        fun addManualGatePass(gp: GatePassing, onSuccess: (DocumentReference) -> Unit, onFailure: OnFailureListener) {
            val db = Firebase.firestore
            FirebaseAuth.getInstance().currentUser?.uid ?: return
            db.collection("reports").document("Manual").set(hashMapOf("id" to "Manual"))
            db.collection("reports").document("Manual").
            collection("reports")
                .add(gp)
                .addOnSuccessListener (onSuccess)
                .addOnFailureListener (onFailure)
        }
        fun addGatePass(gp: GatePassing, onSuccess: (DocumentReference) -> Unit, onFailure: OnFailureListener) {
            val db = Firebase.firestore
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            db.collection("reports").document(uid).set(hashMapOf("id" to uid))
            db.collection("reports").document(uid).
            collection("reports")
                .add(gp)
                .addOnSuccessListener (onSuccess)
                .addOnFailureListener (onFailure)
        }
    }


}