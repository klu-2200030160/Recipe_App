package com.example.recipeapp.activities

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.recipeapp.R
import com.example.recipeapp.adapters.IngredientAdapter
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.models.Review
import com.example.recipeapp.services.FirebaseService
import kotlinx.coroutines.*
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.adapters.ReviewAdapter

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var firebaseService: FirebaseService

    private lateinit var recipeTitle: TextView
    private lateinit var recipeCategory: TextView
    private lateinit var recipeDescription: TextView
    private lateinit var averageRatingBar: RatingBar
    private lateinit var averageRatingTextView: TextView
    private lateinit var prepTime: TextView
    private lateinit var recipeImage: ImageView
    private lateinit var ingredientsRecyclerView: RecyclerView
    private lateinit var recipeInstructions: TextView
    private lateinit var createdBy: TextView

    private lateinit var reviewEditText: EditText
    private lateinit var reviewRatingBar: RatingBar
    private lateinit var submitReviewButton: Button
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var reviewAdapter: ReviewAdapter

    private lateinit var favoriteButton: ImageButton
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        firebaseService = FirebaseService(this)

        // View Initializations
        recipeTitle = findViewById(R.id.recipeTitle)
        recipeCategory = findViewById(R.id.recipeCategory)
        recipeDescription = findViewById(R.id.recipeDescription)
        averageRatingBar = findViewById(R.id.averageRatingBar)
        averageRatingTextView = findViewById(R.id.averageRatingTextView)
        prepTime = findViewById(R.id.prepTime)
        recipeImage = findViewById(R.id.recipeImage)
        ingredientsRecyclerView = findViewById(R.id.ingredientsRecyclerView)
        recipeInstructions = findViewById(R.id.recipeInstructions)
        createdBy = findViewById(R.id.createdBy)

        reviewEditText = findViewById(R.id.reviewEditText)
        reviewRatingBar = findViewById(R.id.reviewRatingBar)
        submitReviewButton = findViewById(R.id.submitReviewButton)
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView)

        favoriteButton = findViewById(R.id.favoriteButton)

        // Setup Ingredients RecyclerView
        ingredientsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Setup Reviews RecyclerView
        reviewAdapter = ReviewAdapter()
        reviewsRecyclerView.adapter = reviewAdapter
        reviewsRecyclerView.layoutManager = LinearLayoutManager(this)

        val recipe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("recipe", Recipe::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("recipe") as? Recipe
        }

        if (recipe == null) {
            showToast("Error: No recipe found.")
            finish()
            return
        }

        // Fill UI with Recipe data
        recipeTitle.text = recipe.title
        recipeCategory.text = "Category: ${recipe.category}"
        recipeDescription.text = recipe.description
        prepTime.text = "⏱ Prep: ${recipe.prepTime} minutes"
        recipeInstructions.text = recipe.instructions
        loadCreatedByUserName(recipe.createdBy)



        Glide.with(this)
            .load(recipe.imageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .into(recipeImage)

        ingredientsRecyclerView.adapter = IngredientAdapter(recipe.ingredients)

        loadReviews(recipe.id)

        // Increment views count
        CoroutineScope(Dispatchers.IO).launch {
            firebaseService.incrementRecipeViews(recipe.id)
        }

        submitReviewButton.setOnClickListener {
            submitReview(recipe.id)
        }

        // Favorite button click
        favoriteButton.setOnClickListener {
            toggleFavorite(recipe.id)
        }
    }

    private fun loadReviews(recipeId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val reviews = withContext(Dispatchers.IO) {
                firebaseService.getReviews(recipeId)
            }

            reviewAdapter.submitList(reviews)

            if (reviews.isNotEmpty()) {
                val avgRating = reviews.map { it.rating }.average().toFloat()
                averageRatingTextView.text = String.format("%.1f/5", avgRating)
                averageRatingBar.rating = avgRating
            } else {
                averageRatingTextView.text = "No ratings"
                averageRatingBar.rating = 0f
            }
        }
    }

    private fun submitReview(recipeId: String) {
        val comment = reviewEditText.text.toString().trim()
        val rating = reviewRatingBar.rating

        if (comment.isEmpty()) {
            showToast("Please enter a review")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val user = withContext(Dispatchers.IO) { firebaseService.getCurrentUser() }
            if (user == null) {
                showToast("Please log in to submit a review")
                finish()
                return@launch
            }

            val review = Review(
                recipeId = recipeId,
                comment = comment,
                rating = rating,
                userId = user.id
            )

            val result = withContext(Dispatchers.IO) {
                firebaseService.addReview(review)
            }

            if (result.isSuccess) {
                showToast("Review submitted successfully")
                reviewEditText.text.clear()
                reviewRatingBar.rating = 0f
                loadReviews(recipeId)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                showToast("Failed to submit review: $errorMsg")
            }
        }
    }
    private fun loadCreatedByUserName(userId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val userSnapshot = withContext(Dispatchers.IO) {
                    firebaseService.getUserById(userId)
                }
                if (userSnapshot != null) {
                    val userName = userSnapshot.username
                    createdBy.text = "Recipe by: $userName"
                } else {
                    createdBy.text = "Recipe by: Unknown"
                }
            } catch (e: Exception) {
                createdBy.text = "Recipe by: Unknown"
            }
        }
    }


    private fun toggleFavorite(recipeId: String) {
        isFavorite = !isFavorite
        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
            CoroutineScope(Dispatchers.IO).launch {
                firebaseService.addFavorite(recipeId)
            }
            showToast("Added to favorites ❤️")
        } else {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border)
            CoroutineScope(Dispatchers.IO).launch {
                firebaseService.removeFavorite(recipeId)
            }
            showToast("Removed from favorites 🤍")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
