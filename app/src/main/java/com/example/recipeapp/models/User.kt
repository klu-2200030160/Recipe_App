package com.example.recipeapp.models

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val role: String = "customer",
    val favorites: List<String> = emptyList(),
    val shoppingList: List<String> = emptyList() // Added for shopping list
) : Parcelable