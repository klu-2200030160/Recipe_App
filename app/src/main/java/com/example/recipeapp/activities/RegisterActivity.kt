package com.example.recipeapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.recipeapp.R
import com.example.recipeapp.services.FirebaseService
import com.example.recipeapp.utils.ToastUtils
import com.example.recipeapp.utils.ValidationUtils
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var firebaseService: FirebaseService
    private lateinit var progressBar: ProgressBar
    private lateinit var roleSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        firebaseService = FirebaseService(this)

        // Initialize UI elements
        progressBar = findViewById(R.id.progressBar)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        roleSpinner = findViewById(R.id.roleSpinner)

        // Set up role spinner
        val roles = arrayOf("Select Role", "Chef", "Customer", "Admin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = adapter

        // Register button click listener
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
            val role = roleSpinner.selectedItem.toString()
            registerUser(email, password, username, role)
        }
    }

    private fun registerUser(email: String, password: String, username: String, role: String) {
        if (!ValidationUtils.isValidEmail(email)) {
            ToastUtils.showShort(this, "Invalid email format")
            return
        }
        if (!ValidationUtils.isValidPassword(password)) {
            ToastUtils.showShort(this, "Password must be at least 6 characters")
            return
        }
        if (username.isEmpty()) {
            ToastUtils.showShort(this, "Username cannot be empty")
            return
        }
        if (role == "Select Role") {
            ToastUtils.showShort(this, "Please select a role")
            return
        }

        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                val result = firebaseService.signUp(email, password, username, role)
                if (result.isSuccess) {
                    ToastUtils.showShort(this@RegisterActivity, "Registered as $role")
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else {
                    ToastUtils.showShort(this@RegisterActivity, result.exceptionOrNull()?.message ?: "Registration failed")
                }
            } catch (e: Exception) {
                ToastUtils.showShort(this@RegisterActivity, "Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}