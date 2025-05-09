package com.example.recipeapp.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipeapp.R
import com.example.recipeapp.adapters.RecipeAdapter
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.services.FirebaseService
import kotlinx.coroutines.*
import androidx.recyclerview.widget.RecyclerView

class FavoritesActivity : AppCompatActivity() {

    private lateinit var firebaseService: FirebaseService
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        firebaseService = FirebaseService(this)
        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView)

        setSupportActionBar(findViewById(R.id.toolbar3))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Favourites"
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar3)
        for (i in 0 until toolbar.childCount) {
            val view = toolbar.getChildAt(i)
            if (view is TextView) {
                val typeface = ResourcesCompat.getFont(this, R.font.poppins) // Change to your font
                view.typeface = typeface
                view.textSize = 20f // Optional: set text size
                break
            }
        }

        // Setup Adapter with click handling
        recipeAdapter = RecipeAdapter { recipe ->
            val intent = Intent(this, RecipeDetailActivity::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.putExtra("recipe", recipe)
            } else {
                @Suppress("DEPRECATION")
                intent.putExtra("recipe", recipe)
            }
            startActivity(intent)
            // Optionally add smooth transition animation
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)
        favoritesRecyclerView.adapter = recipeAdapter

        loadFavoriteRecipes()
    }

    private fun loadFavoriteRecipes() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val favoriteIds = withContext(Dispatchers.IO) {
                    firebaseService.getFavoriteRecipeIds()
                }

                if (favoriteIds.isNotEmpty()) {
                    val recipes = withContext(Dispatchers.IO) {
                        firebaseService.getRecipesByIds(favoriteIds)
                    }
                    recipeAdapter.submitList(recipes)
                } else {
                    Toast.makeText(this@FavoritesActivity, "No favorites yet!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FavoritesActivity, "Error loading favorites", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
