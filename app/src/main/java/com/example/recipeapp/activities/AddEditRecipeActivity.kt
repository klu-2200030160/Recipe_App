package com.example.recipeapp.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.recipeapp.R
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.services.FirebaseService
import com.example.recipeapp.utils.ImageUtils
import com.example.recipeapp.utils.ToastUtils
import com.example.recipeapp.utils.ValidationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddEditRecipeActivity : AppCompatActivity() {
    private val firebaseService = FirebaseService()
    private lateinit var progressBar: ProgressBar
    private var imageUri: Uri? = null
    private var recipe: Recipe? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_recipe)

        progressBar = findViewById(R.id.progressBar)
        val titleEditText = findViewById<EditText>(R.id.titleEditText)
        val ingredientsEditText = findViewById<EditText>(R.id.ingredientsEditText)
        val instructionsEditText = findViewById<EditText>(R.id.instructionsEditText)
        val cuisineEditText = findViewById<EditText>(R.id.cuisineEditText)
        val prepTimeEditText = findViewById<EditText>(R.id.prepTimeEditText)
        val recipeImageView = findViewById<ImageView>(R.id.recipeImageView)
        val uploadImageButton = findViewById<Button>(R.id.uploadImageButton)
        val saveButton = findViewById<Button>(R.id.saveButton)

        recipe = intent.getParcelableExtra("recipe")
        recipe?.let {
            titleEditText.setText(it.title)
            ingredientsEditText.setText(it.ingredients.joinToString(", "))
            instructionsEditText.setText(it.instructions)
            cuisineEditText.setText(it.cuisine)
            prepTimeEditText.setText(it.prepTime.toString())
            Glide.with(this).load(it.imageUrl).into(recipeImageView)
        }

        uploadImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 100)
        }

        saveButton.setOnClickListener {
            saveRecipe(
                titleEditText.text.toString(),
                ingredientsEditText.text.toString(),
                instructionsEditText.text.toString(),
                cuisineEditText.text.toString(),
                prepTimeEditText.text.toString()
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            imageUri = data?.data
            findViewById<ImageView>(R.id.recipeImageView).setImageURI(imageUri)
        }
    }

    private fun saveRecipe(title: String, ingredients: String, instructions: String, cuisine: String, prepTime: String) {
        if (!ValidationUtils.isNotEmpty(title) || !ValidationUtils.isNotEmpty(ingredients) || !ValidationUtils.isNotEmpty(instructions)) {
            ToastUtils.showShort(this, "All fields are required")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            progressBar.visibility = View.VISIBLE
            val recipeData = Recipe(
                id = recipe?.id ?: "",
                title = title,
                ingredients = ingredients.split(",").map { it.trim() },
                instructions = instructions,
                cuisine = cuisine,
                prepTime = prepTime.toIntOrNull() ?: 0
            )
            val compressedImage = imageUri?.let { ImageUtils.compressImage(this@AddEditRecipeActivity, it) }
            val result = if (recipe == null) {
                firebaseService.addRecipe(recipeData, imageUri)
            } else {
                firebaseService.updateRecipe(recipeData, imageUri)
            }
            result.fold(
                onSuccess = { finish() },
                onFailure = { ToastUtils.showShort(this@AddEditRecipeActivity, it.message ?: "Save failed") }
            )
            progressBar.visibility = View.GONE
        }
    }
}
