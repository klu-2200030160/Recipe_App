package com.example.recipeapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipeapp.R
import com.example.recipeapp.adapters.ReviewAdapter
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.models.Review
import com.example.recipeapp.services.FirebaseService
import com.example.recipeapp.utils.DateUtils
import com.example.recipeapp.utils.ToastUtils
import com.example.recipeapp.utils.ValidationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecipeDetailActivity : AppCompatActivity() {
    private lateinit var firebaseService: FirebaseService
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recipe_detail)
        firebaseService = FirebaseService(this)

        // Initialize UI elements
        progressBar = findViewById(R.id.progressBar)
        val titleTextView = findViewById<TextView>(R.id.titleTextView)
        val recipeImageView = findViewById<ImageView>(R.id.recipeImageView)
        val ingredientsTextView = findViewById<TextView>(R.id.ingredientsTextView)
        val instructionsTextView = findViewById<TextView>(R.id.instructionsTextView)
        val reviewEditText = findViewById<EditText>(R.id.reviewEditText)
        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val submitReviewButton = findViewById<Button>(R.id.submitReviewButton)
        val reviewsRecyclerView = findViewById<RecyclerView>(R.id.reviewsRecyclerView)

        // Set up RecyclerView
        reviewsRecyclerView.layoutManager = LinearLayoutManager(this)
        reviewsRecyclerView.adapter = ReviewAdapter(emptyList())

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get recipe from intent
        val recipe = intent.getParcelableExtra<Recipe>("recipe")
        if (recipe == null) {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Populate UI with recipe data
        titleTextView.text = recipe.title
        Glide.with(this)
            .load(recipe.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery) // Default placeholder
            .error(android.R.drawable.ic_menu_close_clear_cancel) // Default error image
            .into(recipeImageView)
        ingredientsTextView.text = recipe.ingredients.joinToString("\n")
        instructionsTextView.text = recipe.instructions

        // Load reviews
        loadReviews(recipe.id)

        // Submit review button
        submitReviewButton.setOnClickListener {
            val comment = reviewEditText.text.toString().trim()
            val rating = ratingBar.rating

            if (comment.isEmpty()) {
                Toast.makeText(this, "Please enter a review comment", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (rating == 0f) {
                Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitReview(recipe.id, comment, rating)
        }
    }

    private fun loadReviews(recipeId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            progressBar.visibility = View.VISIBLE
            try {
                val reviews = firebaseService.getReviews(recipeId)
                (findViewById<RecyclerView>(R.id.reviewsRecyclerView).adapter as ReviewAdapter).updateReviews(reviews)
            } catch (e: Exception) {
                Toast.makeText(this@RecipeDetailActivity, "Failed to load reviews: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun submitReview(recipeId: String, comment: String, rating: Float) {
        if (!ValidationUtils.isNotEmpty(comment)) {
            ToastUtils.showShort(this, "Please enter a review comment")
            return
        }
        if (!ValidationUtils.isValidRating(rating)) {
            ToastUtils.showShort(this, "Rating must be between 0 and 5")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            progressBar.visibility = View.VISIBLE
            val user = firebaseService.getCurrentUser() // Moved inside coroutine
            val review = Review(
                recipeId = recipeId,
                userId = user?.id ?: "",
                comment = comment,
                rating = rating,
                timestamp = DateUtils.getCurrentTimestamp()
            )
            firebaseService.addReview(review).fold(
                onSuccess = {
                    loadReviews(review.recipeId)
                    findViewById<EditText>(R.id.reviewEditText).text.clear()
                    findViewById<RatingBar>(R.id.ratingBar).rating = 0f
                    ToastUtils.showShort(this@RecipeDetailActivity, "Review submitted")
                },
                onFailure = { ToastUtils.showShort(this@RecipeDetailActivity, it.message ?: "Review submission failed") }
            )
            progressBar.visibility = View.GONE
        }
    }
}