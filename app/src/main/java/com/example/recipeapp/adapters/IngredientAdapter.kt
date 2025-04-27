package com.example.recipeapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R

class IngredientAdapter(private val ingredients: List<String>) :
    RecyclerView.Adapter<IngredientAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ingredientName: TextView = itemView.findViewById(R.id.ingredientName)
        val checkIcon: ImageView = itemView.findViewById(R.id.checkIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.ingredientName.text = ingredients[position]
        holder.checkIcon.setImageResource(R.drawable.ic_check_circle_green)
    }

    override fun getItemCount(): Int = ingredients.size
}
