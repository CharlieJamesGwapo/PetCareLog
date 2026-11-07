package com.petcare.log

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.petcare.log.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.signUpButton.setOnClickListener { register() }
        binding.loginLink.setOnClickListener { finish() }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.signUpButton.isEnabled = !loading
        binding.loginLink.isEnabled = !loading
    }

    private fun register() {
        val name = binding.nameInput.text?.toString()?.trim().orEmpty()
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val confirm = binding.confirmInput.text?.toString().orEmpty()
        listOf(binding.nameLayout, binding.emailLayout, binding.passwordLayout, binding.confirmLayout).forEach { it.error = null }

        var ok = true
        if (name.isEmpty()) { binding.nameLayout.error = getString(R.string.err_required); ok = false }
        if (email.isEmpty()) { binding.emailLayout.error = getString(R.string.err_required); ok = false }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.emailLayout.error = getString(R.string.err_email); ok = false }
        if (password.isEmpty()) { binding.passwordLayout.error = getString(R.string.err_required); ok = false }
        else if (password.length < 6) { binding.passwordLayout.error = getString(R.string.err_password_len); ok = false }
        if (confirm.isEmpty()) { binding.confirmLayout.error = getString(R.string.err_required); ok = false }
        else if (confirm != password) { binding.confirmLayout.error = getString(R.string.err_password_match); ok = false }
        if (!ok) return

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            val user = auth.currentUser
            if (task.isSuccessful && user != null) {
                val profile = mapOf("name" to name, "email" to email)
                userRef(user.uid).child("profile").setValue(profile).addOnCompleteListener { save ->
                    if (!save.isSuccessful) toast(errorMessage(save.exception))
                    goHome()
                }
            } else {
                setLoading(false)
                toast(errorMessage(task.exception))
            }
        }
    }
}
