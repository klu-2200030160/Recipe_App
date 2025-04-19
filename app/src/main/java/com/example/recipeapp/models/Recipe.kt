package com.example.recipeapp.models

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    @PropertyName("id") val id: String = "",
    @PropertyName("title") val title: String = "",
    @PropertyName("description") val description: String = "",
    @PropertyName("ingredients") val ingredients: List<String> = emptyList(),
    @PropertyName("instructions") val instructions: String = "",
    @PropertyName("category") val category: String = "",
    @PropertyName("imageUrl") val imageUrl: String = "",
    @PropertyName("createdBy") val createdBy: String = "",
    @PropertyName("prepTime") val prepTime: Int = 0,
    @PropertyName("views") val views: Int = 0
) : Parcelable