package com.example.recipeapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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

class FavoritesActivity : AppCompatActivity() {
    private val firebaseService = FirebaseService()
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView // Made class-level to access in loadFavorites
    private lateinit var adapter: RecipeAdapter   // Made class-level for clarity
    private lateinit var emptyTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.favoritesRecyclerView)
        emptyTextView = findViewById(R.id.emptyTextView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = RecipeAdapter(emptyList()) { recipe ->
            val intent = Intent(this@FavoritesActivity, RecipeDetailActivity::class.java)
            intent.putExtra("recipe", recipe)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        loadFavorites()
    }


    private fun loadFavorites() {
        CoroutineScope(Dispatchers.Main).launch {
            progressBar.visibility = View.VISIBLE
            try {
                val user = firebaseService.getCurrentUser()
                if (user == null) {
                    ToastUtils.showShort(this@FavoritesActivity, "Please log in to view favorites")
                    startActivity(Intent(this@FavoritesActivity, LoginActivity::class.java))
                    finish()
                    return@launch
                }

                val allRecipes = firebaseService.getRecipes()
                val favorites = allRecipes.filter { it.id in user.favorites }
                adapter.updateRecipes(favorites)
                emptyTextView.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (favorites.isEmpty()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                ToastUtils.showShort(this@FavoritesActivity, "Failed to load favorites: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}