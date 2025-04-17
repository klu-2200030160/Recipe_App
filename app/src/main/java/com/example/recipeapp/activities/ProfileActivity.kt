package com.example.recipeapp.activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.recipeapp.R
import com.example.recipeapp.services.FirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {
    private lateinit var firebaseService: FirebaseService
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        firebaseService = FirebaseService(this)
        progressBar = findViewById(R.id.progressBar)
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val emailTextView = findViewById<TextView>(R.id.emailTextView)
        val saveButton = findViewById<Button>(R.id.saveButton)

        loadProfile()

        saveButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            if (username.isEmpty()) {
                Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveProfile(username)
        }
    }

    private fun loadProfile() {
        CoroutineScope(Dispatchers.Main).launch {
            progressBar.visibility = View.VISIBLE
            val user = firebaseService.getCurrentUser()
            user?.let {
                findViewById<EditText>(R.id.usernameEditText).setText(it.username)
                findViewById<TextView>(R.id.emailTextView).text = it.email
            }
            progressBar.visibility = View.GONE
        }
    }

    private fun saveProfile(username: String) {
        CoroutineScope(Dispatchers.Main).launch {
            progressBar.visibility = View.VISIBLE
            try {
                val user = withContext(Dispatchers.IO) { firebaseService.getCurrentUser() }
                if (user != null) {
                    val email = findViewById<EditText>(R.id.emailTextView).text.toString().trim()
                    if (email.isEmpty()) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Email cannot be empty",
                            Toast.LENGTH_SHORT
                        ).show()
                        progressBar.visibility = View.GONE
                        return@launch
                    }
                    val result = withContext(Dispatchers.IO) {
                        firebaseService.updateUser(user.id, username, email)
                    }
                    if (result.isSuccess) {
                        Toast.makeText(this@ProfileActivity, "Profile updated", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                        Toast.makeText(
                            this@ProfileActivity,
                            "Failed to update profile: $errorMsg",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@ProfileActivity,
                        "User not authenticated",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Failed to update profile: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            progressBar.visibility = View.GONE
        }
    }
}