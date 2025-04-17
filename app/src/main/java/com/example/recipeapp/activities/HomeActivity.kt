package com.example.recipeapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.adapters.RecipeAdapter
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.services.FirebaseService
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private lateinit var firebaseService: FirebaseService
    private lateinit var popularRecyclerView: RecyclerView
    private lateinit var popularAdapter: RecipeAdapter
    private val recipesList = mutableListOf<Recipe>()
    private var userRole: String? = null
    private var currentUserId: String? = null

    // Drawer
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView
    private lateinit var searchEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        firebaseService = FirebaseService(this)

        // Window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Drawer setup
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuIcon = findViewById(R.id.imageView)
        searchEditText = findViewById(R.id.editTextText)

        // Set up menu icon to toggle drawer
        menuIcon.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Set up search EditText to navigate to SearchActivity
        searchEditText.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }

        // Set up NavigationView menu
        setupNavigationMenu()

        // Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Already in HomeActivity, do nothing or refresh if needed
                    true
                }
                R.id.nav_search -> {
                    val intent = Intent(this, SearchActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
                    true
                }
                R.id.nav_favorites -> {
                    startActivity(Intent(this, FavoritesActivity::class.java))
                    true
                }
                R.id.nav_shopping_list -> {
                    startActivity(Intent(this, ShoppingListActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_logout -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            firebaseService.signOut()
                            startActivity(Intent(this@HomeActivity, LoginActivity::class.java))
                            finish()
                        } catch (e: Exception) {
                            showToast("Logout failed: ${e.message}")
                        }
                    }
                    true
                }
                else -> false
            }.also {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }

        // RecyclerView for popular recipes
        popularRecyclerView = findViewById(R.id.rv_popular)
        popularRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        popularAdapter = RecipeAdapter(recipesList) { recipe ->
            val intent = Intent(this, RecipeDetailActivity::class.java)
            intent.putExtra("recipe", recipe)
            startActivity(intent)
        }
        popularRecyclerView.adapter = popularAdapter

        // Load data
        CoroutineScope(Dispatchers.Main).launch {
            loadUserData()
            loadPopularRecipes()
        }
    }

    private fun setupNavigationMenu() {
        // Apply role-based visibility to menu items
        val menu = navigationView.menu
        // nav_search is visible to all, so no restriction applied
        menu.findItem(R.id.nav_favorites)?.isVisible = true
        menu.findItem(R.id.nav_shopping_list)?.isVisible = true
        // nav_home, nav_profile, nav_logout, nav_search are visible to all
    }

    private suspend fun loadUserData() {
        firebaseService.getCurrentUser()?.let { user ->
            userRole = user.role
            currentUserId = user.id
            title = "Home - ${userRole?.replaceFirstChar { it.uppercaseChar() }}"
            setupNavigationMenu() // Update menu visibility after role is loaded
        } ?: run {
            showToast("User not authenticated")
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
                showToast("No popular recipes found")
            }
        } catch (e: Exception) {
            showToast("Failed to load recipes: ${e.message}")
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}