package com.example.recipeapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    val id: String = "",
    val title: String = "",
    val ingredients: List<String> = emptyList(),
    val instructions: String = "",
    val imageUrl: String? = null,
    val cuisine: String = "",
    val prepTime: Int = 0,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val ratings: Map<String, Float> = emptyMap()
) : Parcelable