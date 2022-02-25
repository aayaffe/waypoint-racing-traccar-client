package `in`.avimarine.waypointracing.database

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

interface OnlineDatabase {
    fun getRoutes(onSuccess: (QuerySnapshot) -> Unit, onFailure: OnFailureListener)
    fun getRoute(id: String, onSuccess: (DocumentSnapshot?) -> Unit, onFailure: OnFailureListener)
    fun getRoutesNames(onSuccess: (List<String>) -> Unit, onFailure: OnFailureListener)
}