package com.example.recipeapp.models

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    @PropertyName("id") var id: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("username") val username: String = "",
    @PropertyName("role") val role: String = "customer",
    @PropertyName("name") val name: String = "",
    @PropertyName("age") val age: Int = 0,
    @PropertyName("mobile") val mobile: String = "",
    @PropertyName("profileImageUrl") val profileImageUrl: String = "",
    @PropertyName("favorites") val favorites: List<String> = emptyList(),
    @PropertyName("shoppingList") val shoppingList: List<String> = emptyList()
) : Parcelable