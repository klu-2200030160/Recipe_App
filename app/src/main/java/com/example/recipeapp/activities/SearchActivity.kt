package com.example.recipeapp.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
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
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {
    private lateinit var firebaseService: FirebaseService
    private lateinit var searchEditText: EditText
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var searchAdapter: RecipeAdapter
    private val minQueryLength = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        firebaseService = FirebaseService(this)

        // Set up Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Search Recipes"

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText)
        searchRecyclerView = findViewById(R.id.searchRecyclerView)

        // Set up RecyclerView
        searchRecyclerView.layoutManager = LinearLayoutManager(this)
        searchAdapter = RecipeAdapter { recipe ->
            val intent = Intent(this, RecipeDetailActivity::class.java)
            intent.putExtra("recipe", recipe)
            startActivity(intent)
        }
        searchRecyclerView.adapter = searchAdapter

        // Set up search input listener
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length >= minQueryLength) {
                    searchRecipes(query)
                } else {
                    searchAdapter.submitList(emptyList())
                }
            }
        })

        // Focus on EditText
        searchEditText.requestFocus()
    }

    private fun searchRecipes(query: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val recipes = withContext(Dispatchers.IO) {
                    firebaseService.getRecipes().filter { recipe ->
                        recipe.title.contains(query, ignoreCase = true) ||
                                recipe.ingredients.any { it.contains(query, ignoreCase = true) } ||
                                recipe.instructions.contains(query, ignoreCase = true)
                    }
                }
                searchAdapter.submitList(recipes)
                if (recipes.isEmpty()) {
                    showToast("No recipes found for \"$query\"")
                }
            } catch (e: Exception) {
                showToast("Failed to search recipes: ${e.message}")
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}