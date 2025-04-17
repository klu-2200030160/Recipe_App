package com.example.recipeapp.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.adapters.RecipeAdapter
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.services.FirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {
    private lateinit var firebaseService: FirebaseService
    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        firebaseService = FirebaseService(this)

        recyclerView = findViewById(R.id.rv_favorites)
        recyclerView.layoutManager = LinearLayoutManager(this)

        recipeAdapter = RecipeAdapter(mutableListOf()) { recipe ->
            val intent = Intent(this, RecipeDetailActivity::class.java)
            intent.putExtra("recipe", recipe)
            startActivity(intent)
        }
        recyclerView.adapter = recipeAdapter

        loadFavoriteRecipes()
    }

    private fun loadFavoriteRecipes() {
        CoroutineScope(Dispatchers.Main).launch {
            val user = firebaseService.getCurrentUser()
            if (user == null) {
                recipeAdapter.updateRecipes(emptyList())
                return@launch
            }

            // Fetch favorite recipe IDs
            val favorites = firebaseService.getFavoriteRecipeIds(user.id)

            // Fetch recipes matching favorite IDs
            val recipes = firebaseService.getRecipesByIds(favorites)

            recipeAdapter.updateRecipes(recipes)
        }
    }
}