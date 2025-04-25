package com.example.recipeapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.adapters.RecipeManageAdapter
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.services.FirebaseService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipeManagementActivity : AppCompatActivity() {

    private lateinit var recipeRecyclerView: RecyclerView
    private lateinit var addRecipeFab: FloatingActionButton
    private lateinit var recipeManageAdapter: RecipeManageAdapter
    private lateinit var firebaseService: FirebaseService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_management)

        recipeRecyclerView = findViewById(R.id.recipeRecyclerView)
        addRecipeFab = findViewById(R.id.addRecipeFab)
        firebaseService = FirebaseService(this)

        // Adapter with edit and delete logic
        recipeManageAdapter = RecipeManageAdapter(
            onEditClick = { recipe ->
                val intent = Intent(this, AddEditRecipeActivity::class.java)
                intent.putExtra("RECIPE_ID", recipe.id)
                startActivity(intent)
            },
            onDeleteClick = { recipe -> deleteRecipe(recipe.id) }
        )

        recipeRecyclerView.layoutManager = LinearLayoutManager(this)
        recipeRecyclerView.adapter = recipeManageAdapter

        // Add new recipe
        addRecipeFab.setOnClickListener {
            val intent = Intent(this, AddEditRecipeActivity::class.java)
            startActivity(intent)
        }

        loadRecipes()
    }

    private fun loadRecipes() {
        CoroutineScope(Dispatchers.Main).launch {
            val user = withContext(Dispatchers.IO) { firebaseService.getCurrentUser() }
            if (user != null) {
                val recipes = withContext(Dispatchers.IO) {
                    if (user.role == "Admin") {
                        firebaseService.getAllRecipes()
                    } else {
                        firebaseService.getRecipesByUser(user.id)
                    }
                }
                recipeManageAdapter.submitList(recipes)
            } else {
                Toast.makeText(this@RecipeManagementActivity, "User not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteRecipe(recipeId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                firebaseService.deleteRecipe(recipeId)
            }
            if (result.isSuccess) {
                Toast.makeText(this@RecipeManagementActivity, "Recipe deleted", Toast.LENGTH_SHORT).show()
                loadRecipes()
            } else {
                Toast.makeText(this@RecipeManagementActivity, "Failed to delete recipe", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
