package com.example.recipeapp.activities

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.recipeapp.R
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.services.FirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddEditRecipeActivity : AppCompatActivity() {

    private lateinit var firebaseService: FirebaseService
    private lateinit var titleEditText: EditText
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

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            imagePreview.setImageURI(uri)
            Log.d("AddEditRecipeActivity", "Image selected: $uri")
        } else {
            Log.w("AddEditRecipeActivity", "No image selected")
            showToast("No image selected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_recipe)

        firebaseService = FirebaseService(this)

        // Check if user is logged in
        val currentUser = firebaseService.auth.currentUser
        if (currentUser == null) {
            Log.e("AddEditRecipeActivity", "No user logged in, UID: null")
            showToast("Please log in to continue")
            finish()
            return
        }
        Log.d("AddEditRecipeActivity", "Logged in user UID: ${currentUser.uid}")

        titleEditText = findViewById(R.id.titleEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        ingredientsEditText = findViewById(R.id.ingredientsEditText)
        instructionsEditText = findViewById(R.id.instructionsEditText)
        categoryEditText = findViewById(R.id.categoryEditText)
        prepTimeEditText = findViewById(R.id.prepTimeEditText)
        imagePreview = findViewById(R.id.imagePreview)
        selectImageButton = findViewById(R.id.selectImageButton)
        saveButton = findViewById(R.id.saveButton)

        selectImageButton.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        saveButton.setOnClickListener { saveRecipe() }

        editingRecipe = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("recipe", Recipe::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("recipe")
        }
        editingRecipe?.let { loadRecipeForEditing(it) }

        // Debug user role on start
        CoroutineScope(Dispatchers.Main).launch {
            val user = withContext(Dispatchers.IO) { firebaseService.getCurrentUser() }
            Log.d("AddEditRecipeActivity", "User on start: $user")
        }
    }

    private fun loadRecipeForEditing(recipe: Recipe) {
        titleEditText.setText(recipe.title)
        descriptionEditText.setText(recipe.description)
        ingredientsEditText.setText(recipe.ingredients.joinToString("\n"))
        instructionsEditText.setText(recipe.instructions)
        categoryEditText.setText(recipe.category)
        prepTimeEditText.setText(recipe.prepTime.toString())
    }

    private fun saveRecipe() {
        val title = titleEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val ingredients = ingredientsEditText.text.toString().trim()
            .split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val instructions = instructionsEditText.text.toString().trim()
        val category = categoryEditText.text.toString().trim()
        val prepTime = prepTimeEditText.text.toString().trim().toIntOrNull() ?: 0

        if (title.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            showToast("Title, ingredients, and instructions are required")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            // Check user role
            val user = withContext(Dispatchers.IO) { firebaseService.getCurrentUser() }
            Log.d("AddEditRecipeActivity", "User: $user")
            if (user == null) {
                Log.e("AddEditRecipeActivity", "No user logged in")
                showToast("Please log in to continue")
                finish()
                return@launch
            }
            if (user.role !in listOf("Chef", "Admin")) {
                Log.e("AddEditRecipeActivity", "User role '${user.role}' not authorized")
                showToast("Only chefs or admins can add or update recipes")
                return@launch
            }

            // Create recipe object
            val recipe = Recipe(
                id = editingRecipe?.id ?: "",
                title = title,
                description = description,
                ingredients = ingredients,
                instructions = instructions,
                category = category,
                imageUrl = editingRecipe?.imageUrl ?: "",
                createdBy = user.id,
                prepTime = prepTime
            )

            // Save recipe
            val result = withContext(Dispatchers.IO) {
                firebaseService.saveRecipe(recipe, selectedImageUri)
            }

            if (result.isSuccess) {
                showToast("Recipe ${if (editingRecipe != null) "updated" else "added"} successfully")
                finish()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e("AddEditRecipeActivity", "Save failed: $errorMsg")
                showToast("Failed to save recipe: $errorMsg")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}