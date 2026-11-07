package com.petcare.log

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val PET_TYPES = listOf("Dog", "Cat", "Bird", "Fish", "Rabbit", "Other")

fun petIcon(type: String): Int = when (type) {
    "Dog" -> R.drawable.ic_pet_dog
    "Cat" -> R.drawable.ic_pet_cat
    "Bird" -> R.drawable.ic_pet_bird
    "Fish" -> R.drawable.ic_pet_fish
    "Rabbit" -> R.drawable.ic_pet_rabbit
    else -> R.drawable.ic_pet_other
}

/** Shared database instance (pointed at the local emulator in demo builds, see PetCareApp). */
fun appDatabase(): FirebaseDatabase =
    if (BuildConfig.USE_FIREBASE_EMULATOR) FirebaseDatabase.getInstance(DEMO_DB_URL) else FirebaseDatabase.getInstance()

const val DEMO_DB_URL = "http://10.0.2.2:9000?ns=petcare-log-placeholder-default-rtdb"

fun userRef(uid: String): DatabaseReference =
    appDatabase().getReference("users").child(uid)

fun petsRef(uid: String): DatabaseReference = userRef(uid).child("pets")

/** Returns true if a user is signed in; otherwise redirects to Login and finishes the activity. */
fun Activity.requireLogin(): Boolean {
    if (FirebaseAuth.getInstance().currentUser == null) {
        startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
        return false
    }
    return true
}

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

fun Context.errorMessage(e: Exception?): String =
    e?.localizedMessage?.takeIf { it.isNotBlank() } ?: getString(R.string.err_generic)

fun Activity.goHome() {
    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    finish()
}

/** Writes users/{uid}/profile if it does not exist yet, then calls onDone (success or not). */
fun saveProfileIfMissing(user: FirebaseUser, name: String?, onDone: () -> Unit) {
    val ref = userRef(user.uid).child("profile")
    ref.get().addOnCompleteListener { task ->
        val exists = task.isSuccessful && task.result.exists()
        if (exists) {
            onDone()
        } else {
            val profile = mapOf(
                "name" to (name ?: user.displayName ?: user.email?.substringBefore("@") ?: ""),
                "email" to (user.email ?: "")
            )
            ref.setValue(profile).addOnCompleteListener { onDone() }
        }
    }
}

fun isoToDisplay(iso: String): String = try {
    val src = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dst = SimpleDateFormat("MMM d", Locale.getDefault())
    dst.format(src.parse(iso)!!)
} catch (e: Exception) { iso }

fun isoToLong(iso: String): Long? = try {
    val f = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    f.parse(iso)?.time
} catch (e: Exception) { null }

fun millisToIso(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(ms))

fun agePlural(age: Int) = if (age == 1) "1 yr" else "$age yrs"
