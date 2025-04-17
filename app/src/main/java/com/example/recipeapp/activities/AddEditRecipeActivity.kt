package com.example.recipeapp.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.recipeapp.R
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.services.FirebaseService
import kotlinx.coroutines.*

class AddEditRecipeActivity : AppCompatActivity() {

    private lateinit var firebaseService: FirebaseService
    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var ingredientsEditText: EditText
    private lateinit var instructionsEditText: EditText
    private lateinit var categoryEditText: EditText
    private lateinit var prepTimeEditText: EditText
    private lateinit var imagePreview: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var saveButton: Button

    private var selectedImageUri: Uri? = null
    private var editingRecipe: Recipe? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imagePreview.setImageURI(it)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            pickImage()
        } else {
            showToast("Permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_recipe)

        firebaseService = FirebaseService(this)

        nameEditText = findViewById(R.id.nameEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        ingredientsEditText = findViewById(R.id.ingredientsEditText)
        instructionsEditText = findViewById(R.id.instructionsEditText)
        categoryEditText = findViewById(R.id.categoryEditText)
        imagePreview = findViewById(R.id.imagePreview)
        selectImageButton = findViewById(R.id.selectImageButton)
        saveButton = findViewById(R.id.saveButton)
        prepTimeEditText = findViewById(R.id.prepTimeEditText)

        selectImageButton.setOnClickListener { checkPermissionAndPickImage() }
        saveButton.setOnClickListener { saveOrUpdateRecipe() }

        editingRecipe = intent.getParcelableExtra("recipe")
        editingRecipe?.let { loadRecipeForEditing(it) }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> pickImage()
            else -> requestPermissionLauncher.launch(permission)
        }
    }

    private fun pickImage() {
        pickImageLauncher.launch("image/*")
    }

    private fun loadRecipeForEditing(recipe: Recipe) {
        nameEditText.setText(recipe.title)
        descriptionEditText.setText(recipe.description)
        ingredientsEditText.setText(recipe.ingredients.joinToString("\n"))
        instructionsEditText.setText(recipe.instructions)
        categoryEditText.setText(recipe.category)
        prepTimeEditText.setText(recipe.prepTime.toString())
        // Load image using Glide or similar if needed
    }

    private fun saveOrUpdateRecipe() {
        val name = nameEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val ingredients = ingredientsEditText.text.toString().trim().split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val instructions = instructionsEditText.text.toString().trim()
        val category = categoryEditText.text.toString().trim()
        val prepTime = prepTimeEditText.text.toString().trim().toIntOrNull() ?: 0

        if (name.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            showToast("Title, ingredients and instructions are required.")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val user = withContext(Dispatchers.IO) { firebaseService.getCurrentUser() }
            if (user == null) {
                showToast("Please log in first.")
                startActivity(Intent(this@AddEditRecipeActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            val imageUrl = if (selectedImageUri != null) {
                withContext(Dispatchers.IO) {
                    firebaseService.uploadImageToFirebaseStorage(
                        uri = selectedImageUri!!,
                        fileName = "recipe_${System.currentTimeMillis()}.jpg"
                    )
                }
            } else editingRecipe?.imageUrl

            val recipe = Recipe(
                id = editingRecipe?.id ?: "",
                title = name,
                description = description,
                ingredients = ingredients,
                instructions = instructions,
                category = category,
                imageUrl = imageUrl ?: "",
                createdBy = user.id,
                prepTime = prepTime
            )

            val result = if (editingRecipe != null) {
                firebaseService.updateRecipe(recipe, null)
            } else {
                firebaseService.addRecipe(recipe, null)
            }

            if (result.isSuccess) {
                showToast("Recipe ${if (editingRecipe != null) "updated" else "added"} successfully")
                finish()
            } else {
                showToast("Error: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
