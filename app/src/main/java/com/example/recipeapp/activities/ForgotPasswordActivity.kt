package com.example.recipeapp.activities

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.recipeapp.R
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var updatePasswordButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var togglePasswordVisibility: ToggleButton
    private lateinit var toggleConfirmPasswordVisibility: ToggleButton

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        emailEditText = findViewById(R.id.forgotEmailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        updatePasswordButton = findViewById(R.id.updatePasswordButton)
        progressBar = findViewById(R.id.forgotProgressBar)
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility)
        toggleConfirmPasswordVisibility = findViewById(R.id.toggleConfirmPasswordVisibility)

        firebaseAuth = FirebaseAuth.getInstance()

        togglePasswordVisibility.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        toggleConfirmPasswordVisibility.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                confirmPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                confirmPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            confirmPasswordEditText.setSelection(confirmPasswordEditText.text.length)
        }

        updatePasswordButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE

            firebaseAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result?.signInMethods?.isNotEmpty() == true) {
                        firebaseAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { loginTask ->
                                progressBar.visibility = View.GONE
                                if (loginTask.isSuccessful) {
                                    firebaseAuth.currentUser?.updatePassword(password)
                                        ?.addOnCompleteListener { updateTask ->
                                            if (updateTask.isSuccessful) {
                                                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_LONG).show()
                                                finish()
                                            } else {
                                                Toast.makeText(this, "Error updating password: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                } else {
                                    Toast.makeText(this, "Incorrect email or password.", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Email not registered.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
