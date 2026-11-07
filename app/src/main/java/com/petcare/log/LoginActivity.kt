package com.petcare.log

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.petcare.log.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser != null) {
            goHome()
            return
        }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener { loginWithEmail() }
        binding.googleButton.setOnClickListener { loginWithGoogle() }
        binding.signUpLink.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) goHome()
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.loginButton.isEnabled = !loading
        binding.googleButton.isEnabled = !loading
        binding.signUpLink.isEnabled = !loading
    }

    private fun loginWithEmail() {
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        var ok = true
        if (email.isEmpty()) { binding.emailLayout.error = getString(R.string.err_required); ok = false }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.emailLayout.error = getString(R.string.err_email); ok = false }
        if (password.isEmpty()) { binding.passwordLayout.error = getString(R.string.err_required); ok = false }
        if (!ok) return

        setLoading(true)
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                goHome()
            } else {
                setLoading(false)
                toast(errorMessage(task.exception))
            }
        }
    }

    private fun loginWithGoogle() {
        setLoading(true)
        val option = GetSignInWithGoogleOption.Builder(getString(R.string.default_web_client_id)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        lifecycleScope.launch {
            try {
                val result = CredentialManager.create(this@LoginActivity).getCredential(this@LoginActivity, request)
                val cred = result.credential
                if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCred).addOnCompleteListener { task ->
                        val user = auth.currentUser
                        if (task.isSuccessful && user != null) {
                            saveProfileIfMissing(user, null) { goHome() }
                        } else {
                            setLoading(false)
                            toast(errorMessage(task.exception))
                        }
                    }
                } else {
                    setLoading(false)
                    toast(getString(R.string.err_google))
                }
            } catch (e: GetCredentialCancellationException) {
                setLoading(false)
            } catch (e: GetCredentialException) {
                setLoading(false)
                toast(getString(R.string.err_google))
            } catch (e: Exception) {
                setLoading(false)
                toast(getString(R.string.err_google))
            }
        }
    }
}
