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
import com.example.recipeapp.utils.ToastUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllRecipesActivity : AppCompatActivity() {

    private lateinit var firebaseService: FirebaseService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_recipes)

        firebaseService = FirebaseService(this)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "All Recipes"

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecipeAdapter { recipe ->
            val intent = Intent(this, RecipeDetailActivity::class.java)
            intent.putExtra("recipe", recipe)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        CoroutineScope(Dispatchers.Main).launch {
            loadRecipes()
        }
    }

    private suspend fun loadRecipes() {
        try {
            val recipes = withContext(Dispatchers.IO) {
                firebaseService.getRecipes()
            }
            println("AllRecipesActivity: Fetched ${recipes.size} recipes")
            adapter.submitList(recipes)
            if (recipes.isEmpty()) {
                ToastUtils.showShort(this, "No recipes found")
            }
        } catch (e: Exception) {
            ToastUtils.showShort(this, "Failed to load recipes: ${e.message}")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}