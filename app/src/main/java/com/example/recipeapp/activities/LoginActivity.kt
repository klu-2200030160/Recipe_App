package com.example.recipeapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.recipeapp.R
import com.example.recipeapp.services.FirebaseService
import com.example.recipeapp.utils.ToastUtils
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var firebaseService: FirebaseService
    private lateinit var progressBar: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        firebaseService = FirebaseService(this)

        // Initialize UI elements
        progressBar = findViewById(R.id.progressBar)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerText = findViewById<TextView>(R.id.registerTextView)
        val forgotPasswordTextView = findViewById<TextView>(R.id.ForgotPassword)


        // Set login button click listener
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            loginUser(email, password)
        }

        // Set register text click listener
        registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        forgotPasswordTextView.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

    }

    private fun loginUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            ToastUtils.showShort(this, "Email and password cannot be empty")
            return
        }

        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE

                // Attempt to sign in and get the role
                val signInResult = firebaseService.signIn(email, password)
                if (signInResult.isSuccess) {
                    val role = signInResult.getOrNull() ?: "Customer" // Fallback (shouldn't happen due to exception)
                    ToastUtils.showShort(this@LoginActivity, "Logged in as $role")

                    // Navigate to role-specific dashboard
                    val intent = when (role.lowercase()) {
                        "chef" -> Intent(this@LoginActivity, ChefDashboardActivity::class.java)
                        "admin" -> Intent(this@LoginActivity, AdminDashboardActivity::class.java)
                        else -> Intent(this@LoginActivity, HomeActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    val exception = signInResult.exceptionOrNull()
                    ToastUtils.showShort(
                        this@LoginActivity,
                        "Login failed: ${exception?.message ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                ToastUtils.showShort(this@LoginActivity, "Unexpected error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}