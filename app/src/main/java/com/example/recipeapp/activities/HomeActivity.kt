package com.example.recipeapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.adapters.RecipeAdapter
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.services.FirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeActivity : AppCompatActivity() {
    private val firebaseService = FirebaseService()
    private lateinit var popularRecyclerView: RecyclerView
    private lateinit var popularAdapter: RecipeAdapter
    private val recipesList = mutableListOf<Recipe>()
    private var userRole: String? = null
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup RecyclerView for popular recipes
        popularRecyclerView = findViewById(R.id.rv_popular)
        popularRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        popularAdapter = RecipeAdapter(recipesList) { recipe ->
            val intent = Intent(this, RecipeDetailActivity::class.java)
            intent.putExtra("recipe", recipe)
            startActivity(intent)
        }
        popularRecyclerView.adapter = popularAdapter

        // Load user data and recipes
        CoroutineScope(Dispatchers.Main).launch {
            loadUserData()
            loadPopularRecipes()
        }
    }

    private suspend fun loadUserData() {
        firebaseService.getCurrentUser()?.let { user ->
            userRole = user.role
            currentUserId = user.id
            title = "Home - ${userRole?.capitalize()}"
            invalidateOptionsMenu() // Update menu based on role
        } ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private suspend fun loadPopularRecipes() {
        try {
            val popularRecipes = firebaseService.getRecipes()
            recipesList.clear()
            recipesList.addAll(popularRecipes)
            popularAdapter.notifyDataSetChanged()
            if (recipesList.isEmpty()) {
                Toast.makeText(this, "No popular recipes found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load recipes: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        menu?.findItem(R.id.action_add_recipe)?.isVisible = userRole == "chef"
        menu?.findItem(R.id.action_favorites)?.isVisible = userRole == "customer" || userRole == "chef"
        menu?.findItem(R.id.action_shopping_list)?.isVisible = userRole == "customer"
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_recipe -> {
                startActivity(Intent(this, AddEditRecipeActivity::class.java))
                true
            }
            R.id.action_favorites -> {
                startActivity(Intent(this, FavoritesActivity::class.java))
                true
            }
            R.id.action_shopping_list -> {
                startActivity(Intent(this, ShoppingListActivity::class.java))
                true
            }
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_sign_out -> {
                CoroutineScope(Dispatchers.Main).launch {
                    firebaseService.signOut()
                    startActivity(Intent(this@HomeActivity, LoginActivity::class.java))
                    finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}