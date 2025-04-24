package com.example.recipeapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.models.Recipe

class RecipeManageAdapter(
    private var recipes: List<Recipe> = listOf(),
    private val onEditClick: (Recipe) -> Unit,
    private val onDeleteClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeManageAdapter.RecipeViewHolder>() {

    fun submitList(newRecipes: List<Recipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_manage, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(recipes[position])
    }

    override fun getItemCount(): Int = recipes.size

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recipeTitle: TextView = itemView.findViewById(R.id.recipeTitle)
        private val recipeCategory: TextView = itemView.findViewById(R.id.recipeCategory)
        private val editBtn: ImageButton = itemView.findViewById(R.id.editRecipeBtn)
        private val deleteBtn: ImageButton = itemView.findViewById(R.id.deleteRecipeBtn)

        fun bind(recipe: Recipe) {
            recipeTitle.text = recipe.title
            recipeCategory.text = "Category: ${recipe.category}"
            editBtn.setOnClickListener { onEditClick(recipe) }
            deleteBtn.setOnClickListener { onDeleteClick(recipe) }
        }
    }
}
