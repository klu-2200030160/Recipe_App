package com.example.recipeapp.activities

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.recipeapp.R
import com.example.recipeapp.models.User
import com.example.recipeapp.services.FirebaseService
import kotlinx.coroutines.*

class AddEditUserActivity : AppCompatActivity() {

    private lateinit var firebaseService: FirebaseService
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var roleSpinner: Spinner
    private lateinit var saveButton: Button

    private var userId: String? = null
    private val roles = listOf("Chef", "Admin", "Customer")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_user)

        firebaseService = FirebaseService(this)

        usernameEditText = findViewById(R.id.editUsername)
        emailEditText = findViewById(R.id.editEmail)
        roleSpinner = findViewById(R.id.roleSpinner)
        saveButton = findViewById(R.id.btnSaveUser)

        // Set up spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = adapter

        userId = intent.getStringExtra("USER_ID")

        if (userId != null) {
            loadUserData(userId!!)
        }

        saveButton.setOnClickListener {
            val role = roleSpinner.selectedItem.toString()
            if (userId != null) {
                updateUserRole(userId!!, role)
            } else {
                Toast.makeText(this, "User ID or role missing", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserData(userId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val user = withContext(Dispatchers.IO) {
                firebaseService.getUserById(userId)
            }
            if (user != null) {
                usernameEditText.setText(user.username)
                emailEditText.setText(user.email)
                val roleIndex = roles.indexOfFirst { it.equals(user.role, ignoreCase = true) }
                if (roleIndex != -1) {
                    roleSpinner.setSelection(roleIndex)
                }
            } else {
                Toast.makeText(this@AddEditUserActivity, "User not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateUserRole(userId: String, role: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                firebaseService.updateUserRole(userId, role)
            }
            if (result.isSuccess) {
                Toast.makeText(this@AddEditUserActivity, "Role updated", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@AddEditUserActivity, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
