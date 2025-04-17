package com.example.recipeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    val id: String = "", // Unique ID for the recipe
    val title: String = "", // Recipe name
    val description: String = "", // Optional description
    val ingredients: List<String> = emptyList(), // List of ingredients
    val instructions: String = "", // Cooking instructions
    val category: String = "", // Category (e.g., Breakfast, Dessert)
    val createdBy: String = "", // User ID of the creator
    val imageUrl: String? = null, // URL of the uploaded image (optional)
    var prepTime: Int = 0
) : Parcelable