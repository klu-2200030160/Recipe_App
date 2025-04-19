package com.example.recipeapp.models

import com.google.firebase.firestore.PropertyName

data class Review(
    @PropertyName("id") val id: String = "",
    @PropertyName("recipeId") val recipeId: String = "",
    @PropertyName("comment") val comment: String = "",
    @PropertyName("rating") val rating: Float = 0f
)