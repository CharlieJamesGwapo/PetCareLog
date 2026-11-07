package com.petcare.log

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.petcare.log.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PetAdapter
    private var petsListener: ValueEventListener? = null
    private var listeningUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requireLogin()) return
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = PetAdapter { pet ->
            startActivity(Intent(this, PetDetailsActivity::class.java).putExtra(EXTRA_PET_ID, pet.id))
        }
        binding.petList.layoutManager = LinearLayoutManager(this)
        binding.petList.adapter = adapter

        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_logout) { confirmLogout(); true } else false
        }
        binding.fabAdd.setOnClickListener { startActivity(Intent(this, AddEditPetActivity::class.java)) }
    }

    override fun onStart() {
        super.onStart()
        if (!requireLogin()) return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        loadGreeting(user.uid, user.displayName)
        startListening(user.uid)
    }

    override fun onStop() {
        super.onStop()
        stopListening()
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }

    private fun loadGreeting(uid: String, fallback: String?) {
        binding.greeting.text = getString(R.string.greeting_fmt, fallback ?: "")
        userRef(uid).child("profile").child("name").get().addOnSuccessListener {
            val name = it.getValue(String::class.java)
            if (!name.isNullOrBlank() && !isDestroyed) binding.greeting.text = getString(R.string.greeting_fmt, name)
        }
    }

    private fun startListening(uid: String) {
        stopListening()
        binding.progress.visibility = View.VISIBLE
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pets = snapshot.children.mapNotNull { child ->
                    child.getValue(Pet::class.java)?.also { if (it.id.isEmpty()) it.id = child.key ?: "" }
                }.sortedByDescending { it.createdAt }
                adapter.submit(pets)
                binding.progress.visibility = View.GONE
                binding.emptyState.visibility = if (pets.isEmpty()) View.VISIBLE else View.GONE
                binding.petList.visibility = if (pets.isEmpty()) View.GONE else View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progress.visibility = View.GONE
                toast(getString(R.string.err_load))
            }
        }
        petsListener = listener
        listeningUid = uid
        petsRef(uid).addValueEventListener(listener)
    }

    private fun stopListening() {
        val l = petsListener
        val uid = listeningUid
        if (l != null && uid != null) petsRef(uid).removeEventListener(l)
        petsListener = null
        listeningUid = null
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.logout) { _, _ ->
                stopListening()
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }
            .show()
    }

    companion object {
        const val EXTRA_PET_ID = "pet_id"
    }
}
