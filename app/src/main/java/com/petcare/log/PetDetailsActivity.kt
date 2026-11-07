package com.petcare.log

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.petcare.log.databinding.ActivityPetDetailsBinding

class PetDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetDetailsBinding
    private var petId: String = ""
    private var listener: ValueEventListener? = null
    private var listeningUid: String? = null
    private var deleting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requireLogin()) return
        binding = ActivityPetDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        petId = intent.getStringExtra(MainActivity.EXTRA_PET_ID).orEmpty()
        if (petId.isEmpty()) { finish(); return }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.inflateMenu(R.menu.menu_details)
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_edit) {
                startActivity(Intent(this, AddEditPetActivity::class.java).putExtra(MainActivity.EXTRA_PET_ID, petId))
                true
            } else false
        }
        binding.deleteButton.setOnClickListener { confirmDelete() }
    }

    override fun onStart() {
        super.onStart()
        if (!requireLogin()) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val l = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pet = snapshot.getValue(Pet::class.java)
                if (pet == null) {
                    if (!deleting) finish()
                    return
                }
                bind(pet)
            }
            override fun onCancelled(error: DatabaseError) {
                toast(getString(R.string.err_load))
            }
        }
        listener = l
        listeningUid = uid
        petsRef(uid).child(petId).addValueEventListener(l)
    }

    override fun onStop() {
        super.onStop()
        val l = listener
        val uid = listeningUid
        if (l != null && uid != null) petsRef(uid).child(petId).removeEventListener(l)
        listener = null
    }

    private fun bind(pet: Pet) {
        binding.toolbar.title = pet.name
        binding.avatar.setImageResource(petIcon(pet.type))
        binding.valType.text = pet.type
        binding.valAge.text = agePlural(pet.age)
        binding.valVet.text = pet.nextVetVisit.ifEmpty { getString(R.string.not_set) }
        binding.valNotes.text = pet.notes.ifEmpty { getString(R.string.none) }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_msg)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> deletePet() }
            .show()
    }

    private fun deletePet() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        deleting = true
        binding.progress.visibility = View.VISIBLE
        binding.deleteButton.isEnabled = false
        petsRef(uid).child(petId).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                toast(getString(R.string.deleted))
                finish()
            } else {
                deleting = false
                binding.progress.visibility = View.GONE
                binding.deleteButton.isEnabled = true
                toast(errorMessage(task.exception))
            }
        }
    }
}
