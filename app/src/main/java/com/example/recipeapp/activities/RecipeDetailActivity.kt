package com.example.recipeapp.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.models.Review
import com.example.recipeapp.services.FirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var firebaseService: FirebaseService
    private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var ingredientsTextView: TextView
    private lateinit var instructionsTextView: TextView
    private lateinit var categoryTextView: TextView
    private lateinit var prepTimeTextView: TextView
    private lateinit var reviewEditText: EditText
    private lateinit var ratingBar: RatingBar
    private lateinit var submitReviewButton: Button
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var reviewAdapter: ReviewAdapter // Assume a RecyclerView adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        firebaseService = FirebaseService(this)
        titleTextView = findViewById(R.id.titleTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)
        ingredientsTextView = findViewById(R.id.ingredientsTextView)
        instructionsTextView = findViewById(R.id.instructionsTextView)
        categoryTextView = findViewById(R.id.categoryTextView)
        prepTimeTextView = findViewById(R.id.prepTimeTextView)
        reviewEditText = findViewById(R.id.reviewEditText)
        ratingBar = findViewById(R.id.ratingBar)
        submitReviewButton = findViewById(R.id.submitReviewButton)
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView)

        // Set up RecyclerView
        reviewAdapter = ReviewAdapter()
        reviewsRecyclerView.adapter = reviewAdapter
        reviewsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Load recipe
        val recipe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("recipe", Recipe::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("recipe")
        }

        if (recipe == null) {
            Log.e("RecipeDetailActivity", "No recipe provided")
            showToast("Error: No recipe provided")
            finish()
            return
        }

        // Display recipe details
        titleTextView.text = recipe.title
        descriptionTextView.text = recipe.description
        ingredientsTextView.text = recipe.ingredients.joinToString("\n")
        instructionsTextView.text = recipe.instructions
        categoryTextView.text = recipe.category
        prepTimeTextView.text = "${recipe.prepTime} minutes"

        // Load reviews
        loadReviews(recipe.id)

        submitReviewButton.setOnClickListener { submitReview(recipe.id) }
    }

    private fun loadReviews(recipeId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val reviews = withContext(Dispatchers.IO) {
                firebaseService.getReviews(recipeId)
            }
            Log.d("RecipeDetailActivity", "Loaded reviews: $reviews")
            reviewAdapter.submitList(reviews)
        }
    }

    private fun submitReview(recipeId: String) {
        val comment = reviewEditText.text.toString().trim()
        val rating = ratingBar.rating

        if (comment.isEmpty()) {
            showToast("Please enter a review")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val user = withContext(Dispatchers.IO) { firebaseService.getCurrentUser() }
            if (user == null) {
                Log.e("RecipeDetailActivity", "No user logged in")
                showToast("Please log in to submit a review")
                finish()
                return@launch
            }

            val review = Review(
                recipeId = recipeId,
                comment = comment,
                rating = rating
            )

            val result: Result<String> = withContext(Dispatchers.IO) {
                firebaseService.addReview(review)
            }

            if (result.isSuccess) {
                showToast("Review submitted successfully")
                reviewEditText.text.clear()
                ratingBar.rating = 0f
                loadReviews(recipeId)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e("RecipeDetailActivity", "Submit review failed: $errorMsg")
                showToast("Failed to submit review: $errorMsg")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

// Placeholder ReviewAdapter (adjust to your implementation)
class ReviewAdapter : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {
    private var reviews: List<Review> = emptyList()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Bind review data to views
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]
        // Bind data (e.g., comment, rating)
    }

    override fun getItemCount(): Int = reviews.size

    fun submitList(newReviews: List<Review>) {
        reviews = newReviews
        notifyDataSetChanged()
    }
}