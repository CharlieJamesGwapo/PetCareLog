package com.petcare.log

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.petcare.log.databinding.ActivityAddEditPetBinding

class AddEditPetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditPetBinding
    private var petId: String? = null
    private var existing: Pet? = null
    private var vetDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requireLogin()) return
        binding = ActivityAddEditPetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        petId = intent.getStringExtra(MainActivity.EXTRA_PET_ID)
        binding.toolbar.setTitle(if (petId == null) R.string.add_pet else R.string.edit_pet)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.typeInput.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, PET_TYPES))
        binding.typeInput.setText(PET_TYPES[0], false)

        binding.vetInput.setOnClickListener { showDatePicker() }
        binding.vetLayout.setEndIconOnClickListener { showDatePicker() }
        binding.saveButton.setOnClickListener { save() }

        petId?.let { loadPet(it) }
    }

    override fun onStart() {
        super.onStart()
        requireLogin()
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.saveButton.isEnabled = !loading
    }

    private fun loadPet(id: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        setLoading(true)
        petsRef(uid).child(id).get().addOnCompleteListener { task ->
            setLoading(false)
            val pet = if (task.isSuccessful) task.result.getValue(Pet::class.java) else null
            if (pet == null) {
                toast(if (task.isSuccessful) getString(R.string.err_pet_missing) else errorMessage(task.exception))
                finish()
                return@addOnCompleteListener
            }
            existing = pet
            binding.nameInput.setText(pet.name)
            binding.typeInput.setText(pet.type, false)
            binding.ageInput.setText(pet.age.toString())
            vetDate = pet.nextVetVisit
            binding.vetInput.setText(vetDate)
            binding.notesInput.setText(pet.notes)
        }
    }

    private fun showDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.select_vet_date)
        isoToLong(vetDate)?.let { builder.setSelection(it) }
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener {
            vetDate = millisToIso(it)
            binding.vetInput.setText(vetDate)
        }
        picker.show(supportFragmentManager, "vet_date")
    }

    private fun save() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { requireLogin(); return }

        val name = binding.nameInput.text?.toString()?.trim().orEmpty()
        val type = binding.typeInput.text?.toString()?.trim().orEmpty()
        val ageText = binding.ageInput.text?.toString()?.trim().orEmpty()
        val notes = binding.notesInput.text?.toString()?.trim().orEmpty()
        listOf(binding.nameLayout, binding.typeLayout, binding.ageLayout).forEach { it.error = null }

        var ok = true
        if (name.isEmpty()) { binding.nameLayout.error = getString(R.string.err_required); ok = false }
        if (type !in PET_TYPES) { binding.typeLayout.error = getString(R.string.err_required); ok = false }
        val age = ageText.toIntOrNull()
        if (ageText.isEmpty()) { binding.ageLayout.error = getString(R.string.err_required); ok = false }
        else if (age == null || age < 0) { binding.ageLayout.error = getString(R.string.err_age); ok = false }
        if (!ok) return

        val ref = petsRef(uid)
        val id = petId ?: ref.push().key
        if (id == null) { toast(getString(R.string.err_generic)); return }
        val pet = Pet(
            id = id, name = name, type = type, age = age ?: 0,
            nextVetVisit = vetDate, notes = notes,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        setLoading(true)
        ref.child(id).setValue(pet).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                toast(getString(R.string.saved))
                finish()
            } else {
                setLoading(false)
                toast(errorMessage(task.exception))
            }
        }
    }
}
