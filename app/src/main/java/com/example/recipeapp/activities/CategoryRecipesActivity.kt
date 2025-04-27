package com.example.recipeapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.adapters.RecipeAdapter
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.services.FirebaseService
import com.example.recipeapp.utils.RecyclerViewItemDecoration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat

class CategoryRecipesActivity : AppCompatActivity() {

    private lateinit var category: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecipeAdapter
    private lateinit var firebaseService: FirebaseService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_recipes)

        category = intent.getStringExtra("CATEGORY") ?: ""

        // Set up Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = category
        for (i in 0 until toolbar.childCount) {
            val view = toolbar.getChildAt(i)
            if (view is TextView) {
                val typeface = ResourcesCompat.getFont(this, R.font.poppins) // Change to your font
                view.typeface = typeface
                view.textSize = 20f // Optional: set text size
                break
            }
        }

        // Set up RecyclerView
        firebaseService = FirebaseService(this)
        recyclerView = findViewById(R.id.recyclerViewCategory)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2-column grid
        recyclerView.addItemDecoration(RecyclerViewItemDecoration(this, 16)) // 16dp margin decoration

        adapter = RecipeAdapter { recipe ->
            val intent = Intent(this, RecipeDetailActivity::class.java)
            intent.putExtra("recipe", recipe)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        loadCategoryRecipes()
    }

    private fun loadCategoryRecipes() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val recipes = withContext(Dispatchers.IO) {
                    firebaseService.getRecipesByCategory(category)
                }
                adapter.submitList(recipes)
                if (recipes.isEmpty()) {
                    Toast.makeText(this@CategoryRecipesActivity, "No recipes found for $category", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CategoryRecipesActivity, "Failed to load recipes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun setupCategoryClicks(activity: AppCompatActivity) {
            val categories = mapOf(
                R.id.salad to "Salad",
                R.id.MainDish to "Main",
                R.id.Drinks to "Beverage",
                R.id.Desserts to "Desserts",
                R.id.Snacks to "Snacks"
            )
            for ((viewId, categoryName) in categories) {
                activity.findViewById<ImageView>(viewId)?.setOnClickListener {
                    val intent = Intent(activity, CategoryRecipesActivity::class.java)
                    intent.putExtra("CATEGORY", categoryName)
                    activity.startActivity(intent)
                }
            }
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
