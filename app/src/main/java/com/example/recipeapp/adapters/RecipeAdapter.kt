package com.example.recipeapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipeapp.R
import com.example.recipeapp.models.Recipe

class RecipeAdapter(
    private var recipes: MutableList<Recipe> = mutableListOf(),
    private val onClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.popular_rv_items, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(recipes[position])
    }

    override fun getItemCount(): Int = recipes.size

    fun updateRecipes(newRecipes: List<Recipe>) {
        recipes.clear()
        recipes.addAll(newRecipes)
        notifyDataSetChanged()
    }

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.popular_img)
        private val nameTextView: TextView = itemView.findViewById(R.id.popular_txt)
        private val timeTextView: TextView = itemView.findViewById(R.id.popular_time)

        fun bind(recipe: Recipe) {
            nameTextView.text = recipe.title
            timeTextView.text = if (recipe.prepTime > 0) "⏱ ${recipe.prepTime} Mins" else ""

            Glide.with(itemView.context)
                .load(recipe.imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(imageView)

            itemView.setOnClickListener { onClick(recipe) }
        }
    }
}