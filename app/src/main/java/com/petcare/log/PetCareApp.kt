package com.petcare.log

import android.app.Application
import com.google.firebase.auth.FirebaseAuth

class PetCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            // Demo builds only: talk to the local Firebase Emulator Suite (host machine = 10.0.2.2).
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
            appDatabase().useEmulator("10.0.2.2", 9000)
            // Keep the (local) realtime connection alive between screens so slow, scripted demos
            // do not hit the 60 s idle disconnect + slow reconnect.
            var synced: com.google.firebase.database.DatabaseReference? = null
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                synced?.keepSynced(false)
                synced = auth.currentUser?.let { userRef(it.uid).also { r -> r.keepSynced(true) } }
            }
        }
    }
}
