package com.example.recipeapp.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.example.recipeapp.R
import com.example.recipeapp.services.FirebaseService
import com.example.recipeapp.utils.ToastUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var firebaseService: FirebaseService
    private lateinit var nameEditText: EditText
    private lateinit var ageEditText: EditText
    private lateinit var mobileEditText: EditText
    private lateinit var profileImageView: ImageView
    private lateinit var saveButton: Button
    private lateinit var selectImageButton: Button
    private var profileImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        firebaseService = FirebaseService(this)

        // Initialize views
        nameEditText = findViewById(R.id.et_name)
        ageEditText = findViewById(R.id.et_age)
        mobileEditText = findViewById(R.id.et_mobile)
        profileImageView = findViewById(R.id.iv_profile_image)
        saveButton = findViewById(R.id.btn_save_profile)
        selectImageButton = findViewById(R.id.btn_select_image)

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        for (i in 0 until toolbar.childCount) {
            val view = toolbar.getChildAt(i)
            if (view is TextView) {
                val typeface = ResourcesCompat.getFont(this, R.font.poppins) // Change to your font
                view.typeface = typeface
                view.textSize = 20f // Optional: set text size
                break
            }
        }

        // Load current user data
        loadUserData()

        // Handle image selection
        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Handle save button
        saveButton.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadUserData() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    firebaseService.getCurrentUser()
                }
                user?.let {
                    nameEditText.setText(it.name ?: "")
                    ageEditText.setText(it.age?.toString() ?: "")
                    mobileEditText.setText(it.mobile ?: "")

                    if (!it.profileImageUrl.isNullOrEmpty()) {
                        // Load image using Glide
                        Glide.with(this@ProfileActivity)
                            .load(it.profileImageUrl)
                            .placeholder(R.drawable.ic_profile) // while loading show default
                            .error(R.drawable.ic_profile) // in case of error show default
                            .into(profileImageView)
                    }
                }
            } catch (e: Exception) {
                ToastUtils.showLong(this@ProfileActivity, "Failed to load user data: ${e.message}")
            }
        }
    }


    private fun saveProfile() {
        val name = nameEditText.text.toString().trim()
        val age = ageEditText.text.toString().trim().toIntOrNull()
        val mobile = mobileEditText.text.toString().trim()

        // Validation
        if (name.isEmpty()) {
            nameEditText.error = "Name is required"
            return
        }
        if (age != null && (age < 0 || age > 150)) {
            ageEditText.error = "Invalid age"
            return
        }
        if (mobile.isNotEmpty() && !mobile.matches(Regex("^[0-9]{10}$"))) {
            mobileEditText.error = "Invalid mobile number (10 digits required)"
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                val currentUser = firebaseService.getCurrentUser() ?: throw Exception("User not found")

                // Update user profile in Firebase
                val result = firebaseService.updateUser(
                    userId = userId,
                    username = null, // Not updating username
                    email = null, // Not updating email
                    name = name,
                    age = age,
                    mobile = mobile,
                    profileImageUri = profileImageUri
                )

                if (result.isSuccess) {
                    ToastUtils.showShort(this@ProfileActivity, "Profile updated successfully")
                    finish() // Close activity on success
                } else {
                    throw result.exceptionOrNull() ?: Exception("Unknown error")
                }
            } catch (e: Exception) {
                ToastUtils.showLong(this@ProfileActivity, "Failed to update profile: ${e.message}")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            profileImageUri = data.data
            profileImageView.setImageURI(profileImageUri)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private val auth = FirebaseAuth.getInstance()
}