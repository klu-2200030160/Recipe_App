package com.example.recipeapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipeapp.R
import com.example.recipeapp.adapters.RecipeAdapter
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.services.FirebaseService
import com.example.recipeapp.utils.ToastUtils
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    private lateinit var firebaseService: FirebaseService
    private lateinit var popularRecyclerView: RecyclerView
    private lateinit var popularAdapter: RecipeAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView
    private lateinit var searchEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        firebaseService = FirebaseService(this)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuIcon = findViewById(R.id.imageView)
        searchEditText = findViewById(R.id.editTextText)

        menuIcon.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        searchEditText.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }
        CategoryRecipesActivity.setupCategoryClicks(this)


        setupNavigationView()
        setupNavigationHeader()

        popularRecyclerView = findViewById(R.id.rv_popular)
        popularRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        popularAdapter = RecipeAdapter { recipe ->
            val intent = Intent(this, RecipeDetailActivity::class.java)
            intent.putExtra("recipe", recipe)
            startActivity(intent)
        }
        popularRecyclerView.adapter = popularAdapter

        CoroutineScope(Dispatchers.Main).launch {
            loadPopularRecipes()
        }
    }

    private fun setupNavigationView() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> true
                R.id.nav_search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
                    true
                }
                R.id.nav_all_recipes -> {
                    startActivity(Intent(this, AllRecipesActivity::class.java))
                    true
                }
                R.id.nav_favorites -> {
                    startActivity(Intent(this, FavoritesActivity::class.java))
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
                            ToastUtils.showShort(this@HomeActivity, "Logout failed: ${e.message}")
                        }
                    }
                    true
                }
                else -> false
            }.also {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
    }

    private fun setupNavigationHeader() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    firebaseService.getCurrentUser()
                }
                if (user == null) {
                    Log.e("HomeActivity", "User is null")
                    ToastUtils.showShort(this@HomeActivity, "Failed to load user data")
                    return@launch
                }
                Log.d("HomeActivity", "User fetched: $user")
                val headerView = navigationView.getHeaderView(0)
                val nameTextView = headerView.findViewById<TextView>(R.id.tv_nav_name)
                val emailTextView = headerView.findViewById<TextView>(R.id.tv_nav_email)
                val profileImageView = headerView.findViewById<ImageView>(R.id.iv_nav_profile_image)
                val welcomeTextView = headerView.findViewById<TextView>(R.id.tv_welcome)

                nameTextView.text = user.name?.takeIf { it.isNotEmpty() } ?: user.username
                emailTextView.text = user.email
                welcomeTextView.text = "Welcome, ${user.name?.takeIf { it.isNotEmpty() } ?: user.username}!"
                if (!user.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(this@HomeActivity)
                        .load(user.profileImageUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(profileImageView)
                } else {
                    profileImageView.setImageResource(R.drawable.ic_profile)
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Failed to load navigation header: ${e.message}", e)
                ToastUtils.showShort(this@HomeActivity, "Failed to load navigation header: ${e.message}")
            }
        }
    }

    private suspend fun loadPopularRecipes() {
        try {
            val popularRecipes = withContext(Dispatchers.IO) {
                firebaseService.getPopularRecipes()
            }
            println("HomeActivity: Fetched ${popularRecipes.size} popular recipes")
            popularAdapter.submitList(popularRecipes)
            if (popularRecipes.isEmpty()) {
                ToastUtils.showShort(this, "No popular recipes found")
            }
        } catch (e: Exception) {
            Log.e("HomeActivity", "Failed to load recipes: ${e.message}", e)
            ToastUtils.showShort(this, "Failed to load recipes: ${e.message}")
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}