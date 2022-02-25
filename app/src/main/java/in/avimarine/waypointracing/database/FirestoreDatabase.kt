package `in`.avimarine.waypointracing.database

import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.route.Route
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirestoreDatabase: OnlineDatabase {
    private val db = Firebase.firestore
    override fun getRoutesNames(onSuccess: (List<String>) -> Unit, onFailure: OnFailureListener){
        getRoutes({
            val names = arrayListOf<String>()
            for (doc in it){
                doc.getString("name")?.let { it1 -> names.add(it1) }
            }
            onSuccess(names)
        }, onFailure)
    }
    override fun getRoutes(onSuccess: (QuerySnapshot) -> Unit, onFailure: OnFailureListener) {
        db.collection("routes").limit(10)
            .get()
            .addOnSuccessListener (onSuccess)
            .addOnFailureListener (onFailure)
    }

    override fun getRoute(id: String,onSuccess: (DocumentSnapshot?) -> Unit, onFailure: OnFailureListener) {
        val docRef = db.collection("routes").document(id)
        docRef.get()
            .addOnSuccessListener (onSuccess)
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }


}