package com.example.recipeapp.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.withContext

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var firebaseService: FirebaseService
    private lateinit var popularRecyclerView: RecyclerView
    private lateinit var popularAdapter: RecipeAdapter
    private var userRole: String? = null
    private var currentUserId: String? = null

    // Drawer
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView
    private lateinit var searchEditText: EditText

    // Permission request code
    private val STORAGE_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_dashboard)
        checkStoragePermissions()
        firebaseService = FirebaseService(this)

        // Window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Drawer setup
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView1)
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
                R.id.nav_home -> true
                R.id.nav_search -> {
                    val intent = Intent(this, SearchActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
                    true
                }
                R.id.nav_all_recipes -> {
                    val intent = Intent(this, AllRecipesActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_add_recipe -> {
                    val intent = Intent(this, AddEditRecipeActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_UserManagement -> {
                    val intent = Intent(this, UserManagementActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_recipe_management -> {
                    val intent = Intent(this, RecipeManagementActivity::class.java)
                    startActivity(intent)
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
                            startActivity(Intent(this@AdminDashboardActivity, LoginActivity::class.java))
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
        popularAdapter = RecipeAdapter { recipe ->
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
        val menu = navigationView.menu
        menu.findItem(R.id.nav_search)?.isVisible = true
        menu.findItem(R.id.nav_home)?.isVisible = true
        menu.findItem(R.id.nav_all_recipes)?.isVisible = true
        menu.findItem(R.id.nav_add_recipe)?.isVisible = true
        menu.findItem(R.id.nav_UserManagement)?.isVisible = true
        menu.findItem(R.id.nav_recipe_management)?.isVisible = true
        menu.findItem(R.id.nav_profile)?.isVisible = true
        menu.findItem(R.id.nav_logout)?.isVisible = true
    }

    private suspend fun loadUserData() {
        firebaseService.getCurrentUser()?.let { user ->
            userRole = user.role
            currentUserId = user.id
            title = "Admin Dashboard - ${userRole?.replaceFirstChar { it.uppercaseChar() }}"
            setupNavigationMenu()
        } ?: run {
            showToast("User not authenticated")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private suspend fun loadPopularRecipes() {
        try {
            val popularRecipes = withContext(Dispatchers.IO) {
                firebaseService.getPopularRecipes() // Use getPopularRecipes for admin dashboard
            }
            println("AdminDashboardActivity: Fetched ${popularRecipes.size} recipes")
            popularAdapter.submitList(popularRecipes)
            if (popularRecipes.isEmpty()) {
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

    private fun checkStoragePermissions() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, STORAGE_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Storage permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Storage permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}