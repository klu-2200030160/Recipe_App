package com.example.recipeapp.utils

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6 // Minimum 6 characters
    }

    fun isNotEmpty(text: String): Boolean {
        return text.trim().isNotEmpty()
    }

    fun isValidRating(rating: Float): Boolean {
        return rating in 0f..5f
    }
}