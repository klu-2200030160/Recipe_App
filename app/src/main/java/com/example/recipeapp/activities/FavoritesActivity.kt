package com.example.recipeapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.services.FirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesActivity : AppCompatActivity() {

    private lateinit var firebaseService: FirebaseService
    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter // Assume a RecyclerView adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        firebaseService = FirebaseService(this)
        recyclerView = findViewById(R.id.recyclerView)

        // Set up RecyclerView
        recipeAdapter = RecipeAdapter { recipe ->
            // Handle recipe click (e.g., open RecipeDetailActivity)
            val intent = Intent(this, RecipeDetailActivity::class.java)
            intent.putExtra("recipe", recipe)
            startActivity(intent)
        }
        recyclerView.adapter = recipeAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load favorite recipes
        loadFavoriteRecipes()
    }

    private fun loadFavoriteRecipes() {
        CoroutineScope(Dispatchers.Main).launch {
            val user = withContext(Dispatchers.IO) { firebaseService.getCurrentUser() }
            if (user == null) {
                Log.e("FavoritesActivity", "No user logged in")
                showToast("Please log in to view favorites")
                finish()
                return@launch
            }

            val favoriteIds = withContext(Dispatchers.IO) {
                firebaseService.getFavoriteRecipeIds(user.id)
            }
            Log.d("FavoritesActivity", "Favorite IDs: $favoriteIds")

            val recipes = withContext(Dispatchers.IO) {
                firebaseService.getRecipesByIds(favoriteIds)
            }
            Log.d("FavoritesActivity", "Favorite recipes: $recipes")

            recipeAdapter.submitList(recipes)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

// Placeholder RecipeAdapter (adjust to your implementation)
class RecipeAdapter(private val onClick: (Recipe) -> Unit) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {
    private var recipes: List<Recipe> = emptyList()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Bind recipe data to views
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipes[position]
        // Bind data (e.g., title, image)
        holder.itemView.setOnClickListener { onClick(recipe) }
    }

    override fun getItemCount(): Int = recipes.size

    fun submitList(newRecipes: List<Recipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }
}