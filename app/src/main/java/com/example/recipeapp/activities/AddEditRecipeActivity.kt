package com.example.recipeapp.activities

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.recipeapp.R
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.services.FirebaseService
import kotlinx.coroutines.*

class AddEditRecipeActivity : AppCompatActivity() {

    private lateinit var firebaseService: FirebaseService
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var ingredientsEditText: EditText
    private lateinit var instructionsEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var prepTimeEditText: EditText
    private lateinit var imagePreview: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var saveButton: Button

    private var selectedImageUri: Uri? = null
    private var editingRecipe: Recipe? = null

    private val categoryList = listOf("Select a Category","Salad", "Main", "Beverage", "Desserts", "Snacks")

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

        val currentUser = firebaseService.auth.currentUser
        if (currentUser == null) {
            showToast("Please log in to continue")
            finish()
            return
        }

        // Initialize views
        titleEditText = findViewById(R.id.titleEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        ingredientsEditText = findViewById(R.id.ingredientsEditText)
        instructionsEditText = findViewById(R.id.instructionsEditText)
        categorySpinner = findViewById(R.id.categorySpinner)
        prepTimeEditText = findViewById(R.id.prepTimeEditText)
        imagePreview = findViewById(R.id.imagePreview)
        selectImageButton = findViewById(R.id.selectImageButton)
        saveButton = findViewById(R.id.saveButton)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        selectImageButton.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        saveButton.setOnClickListener {
            saveRecipe()
        }

        val recipeId = intent.getStringExtra("RECIPE_ID")
        if (!recipeId.isNullOrBlank()) {
            CoroutineScope(Dispatchers.Main).launch {
                val recipe = withContext(Dispatchers.IO) { firebaseService.getRecipeById(recipeId) }
                if (recipe != null) {
                    editingRecipe = recipe
                    loadRecipeForEditing(recipe)
                } else {
                    showToast("Failed to load recipe for editing")
                }
            }
        }
    }

    private fun loadRecipeForEditing(recipe: Recipe) {
        titleEditText.setText(recipe.title)
        descriptionEditText.setText(recipe.description)
        ingredientsEditText.setText(recipe.ingredients.joinToString("\n"))
        instructionsEditText.setText(recipe.instructions)
        prepTimeEditText.setText(recipe.prepTime.toString())

        val index = categoryList.indexOfFirst { it.equals(recipe.category, ignoreCase = true) }
        if (index >= 0) categorySpinner.setSelection(index)

        if (recipe.imageUrl.isNotBlank()) {
            Glide.with(this)
                .load(recipe.imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(imagePreview)
        }
    }

    private fun saveRecipe() {
        val title = titleEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val ingredients = ingredientsEditText.text.toString().trim()
            .split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val instructions = instructionsEditText.text.toString().trim()
        val category = categorySpinner.selectedItem.toString()
        val prepTime = prepTimeEditText.text.toString().trim().toIntOrNull() ?: 0

        if (title.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            showToast("Title, ingredients, and instructions are required")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val user = withContext(Dispatchers.IO) { firebaseService.getCurrentUser() }

            val normalizedRole = user?.role?.trim()?.lowercase()
            Log.d("AddEditRecipeActivity", "User role: ${user?.role}")

            if (user == null || normalizedRole !in listOf("chef", "admin")) {
                showToast("Only chefs or admins can add or update recipes")
                return@launch
            }

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

            val result = withContext(Dispatchers.IO) {
                firebaseService.saveRecipe(recipe, selectedImageUri)
            }

            if (result.isSuccess) {
                showToast("Recipe ${if (editingRecipe != null) "updated" else "added"} successfully")
                finish()
            } else {
                showToast("Failed to save recipe: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
