package `in`.avimarine.waypointracing.database

import `in`.avimarine.waypointracing.route.GatePassing
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import `in`.avimarine.androidutils.TAG
import `in`.avimarine.waypointracing.Position
import `in`.avimarine.waypointracing.utils.RemoteConfig

class FirestoreDatabase {

    companion object {
        private const val COLLECTION_BOATS = "boats"
        private const val COLLECTION_ROUTES = "routes"
        private const val COLLECTION_REPORTS = "reports"
        private const val COLLECTION_REPORTS_NEW = "reports_new"
        private const val COLLECTION_EVENTS = "events"
        private const val COLLECTION_GENERAL = "general"
        private const val COLLECTION_POSITIONS = "positions"


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
            Firebase.firestore.collection(COLLECTION_ROUTES).limit(10)
                .get()
                .addOnSuccessListener (onSuccess)
                .addOnFailureListener (onFailure)
        }

        fun getRoute(id: String,onSuccess: (QuerySnapshot?) -> Unit, onFailure: OnFailureListener) {
            val docRef = Firebase.firestore.collection(COLLECTION_ROUTES).whereEqualTo("id", id)
            docRef.get()
                .addOnSuccessListener (onSuccess)
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
        }
        fun addBoat(b: Boat, uid: String) {
            val db = Firebase.firestore
            db.collection(COLLECTION_BOATS).document(uid).set(b)
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
            }) {
                val newBoat = Boat(n, "", "")
                addBoat(newBoat, uid)
            }
        }

        fun getBoat(uid: String,onSuccess: (DocumentSnapshot?) -> Unit, onFailure: OnFailureListener) {
            val db = Firebase.firestore
            val docRef = db.collection(COLLECTION_BOATS).document(uid)
            docRef.get()
                .addOnSuccessListener (onSuccess)
                .addOnFailureListener (onFailure)
        }

        fun addManualGatePass(gp: GatePassing, onSuccess: (DocumentReference) -> Unit, onFailure: OnFailureListener) {
            val db = Firebase.firestore
            FirebaseAuth.getInstance().currentUser?.uid ?: return
            db.collection(COLLECTION_REPORTS).document("Manual").set(hashMapOf("id" to "Manual"))
            db.collection(COLLECTION_REPORTS).document("Manual").
            collection(COLLECTION_REPORTS)
                .add(gp)
                .addOnSuccessListener (onSuccess)
                .addOnFailureListener (onFailure)
        }
        fun addGatePass(gp: GatePassing, onSuccess: (DocumentReference) -> Unit, onFailure: OnFailureListener) {
            val db = Firebase.firestore


            if (RemoteConfig.getBool("new_reports_firebase_db")){
                db.collection(COLLECTION_REPORTS_NEW)
                    .add(gp)
                    .addOnSuccessListener (onSuccess)
                    .addOnFailureListener (onFailure)
            }
            else {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
                db.collection(COLLECTION_REPORTS).document(uid).set(hashMapOf("id" to uid))
                db.collection(COLLECTION_REPORTS).document(uid).
                collection(COLLECTION_REPORTS)
                    .add(gp)
                    .addOnSuccessListener (onSuccess)
                    .addOnFailureListener (onFailure)
            }
        }

        fun getOwnReports(routeId: String, gateId: Int, onSuccess: (QuerySnapshot) -> Unit, onFailure: OnFailureListener) {
            val db = Firebase.firestore
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val docRef = db.collection(COLLECTION_REPORTS).document(uid).collection(COLLECTION_REPORTS).whereEqualTo("routeId",routeId).whereEqualTo("gateId", gateId)
            docRef.get()
                .addOnSuccessListener (onSuccess)
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
        }

        fun addEvent(e: EventType, extraData: String = "") {
            val db = Firebase.firestore
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val event = Event(uid, e, System.currentTimeMillis(), extraData)
            db.collection(COLLECTION_EVENTS).document().set(event)

        }

        fun getSupportedVersion(onSuccess: (DocumentSnapshot?) -> Unit, onFailure: OnFailureListener){
            val db = Firebase.firestore
            val docRef = db.collection(COLLECTION_GENERAL).document("MinVersion")
            docRef.get()
                .addOnSuccessListener (onSuccess)
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
        }

        fun addPosition(position: Position, onSuccess: (DocumentReference) -> Unit, onFailure: OnFailureListener) {
            val db = Firebase.firestore
            db.collection(COLLECTION_POSITIONS)
                .add(position)
                .addOnSuccessListener (onSuccess)
                .addOnFailureListener (onFailure)
        }
    }
}