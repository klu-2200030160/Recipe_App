package com.example.recipeapp.models

data class Review(
    val id: String = "",
    val recipeId: String = "",
    val userId: String = "",
    val comment: String = "",
    val rating: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
