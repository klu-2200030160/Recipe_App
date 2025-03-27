package com.example.recipeapp.services

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.example.recipeapp.models.*
import com.google.firebase.firestore.Query

class FirebaseService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()


    // Authentication
    suspend fun signUp(email: String, password: String, username: String, role: String): Result<Unit> {
        return try {
            // Create user with email and password
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User ID not found")

            // Store additional user info in Firestore
            val userData = hashMapOf(
                "username" to username,
                "email" to email,
                "role" to role
            )
            db.collection("users").document(userId).set(userData).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getCurrentUser(): User? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            val role = userDoc.getString("role") ?: "Customer" // Default to "Customer"
            User(id = userId, role = role)
        } catch (e: Exception) {
            null
        }
    }


    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            // Authenticate with Firebase Authentication
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User ID not found")

            // Fetch the user's role from Firestore
            val userDoc = db.collection("users").document(userId).get().await()
            val role = userDoc.getString("role") ?: throw Exception("Role not found in Firestore")

            // Return the role as part of a successful result
            Result.success(role)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }


    suspend fun getUserRole(): String? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            userDoc.getString("role") ?: "Customer"
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUser(userId: String, username: String) {
        db.collection("users").document(userId)
            .update("username", username)
            .await()
    }

    // Recipe Management
    suspend fun addRecipe(recipe: Recipe, imageUri: Uri?): Result<String> {
        if (recipe.title.isEmpty() || recipe.ingredients.isEmpty() || recipe.instructions.isEmpty()) {
            return Result.failure(Exception("All recipe fields are required"))
        }
        return try {
            val recipeId = db.collection("recipes").document().id
            var finalRecipe = recipe.copy(id = recipeId, createdBy = auth.currentUser?.uid ?: "")
            if (imageUri != null) {
                val imageRef = storage.reference.child("recipe_images/$recipeId.jpg")
                imageRef.putFile(imageUri).await()
                val url = imageRef.downloadUrl.await().toString()
                finalRecipe = finalRecipe.copy(imageUrl = url)
            }
            db.collection("reviews").document(recipeId).set(finalRecipe).await()
            Result.success(recipeId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRecipe(recipe: Recipe, imageUri: Uri?): Result<Unit> {
        if (recipe.id.isEmpty()) return Result.failure(Exception("Recipe ID is required"))
        return try {
            var updatedRecipe = recipe
            if (imageUri != null) {
                val imageRef = storage.reference.child("recipe_images/${recipe.id}.jpg")
                imageRef.putFile(imageUri).await()
                val url = imageRef.downloadUrl.await().toString()
                updatedRecipe = recipe.copy(imageUrl = url)
            }
            db.collection("recipes").document(recipe.id).set(updatedRecipe).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecipe(recipeId: String): Result<Unit> {
        if (recipeId.isEmpty()) return Result.failure(Exception("Recipe ID is required"))
        return try {
            db.collection("recipes").document(recipeId).delete().await()
            storage.reference.child("recipe_images/$recipeId.jpg").delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecipes(): List<Recipe> {
        return try {
            val querySnapshot = db.collection("recipes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
            querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Recipe::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Review Management
    suspend fun addReview(review: Review): Result<String> {
        if (review.comment.isEmpty() || review.rating !in 0f..5f) {
            return Result.failure(Exception("Invalid review data"))
        }
        return try {
            val reviewId = db.collection("reviews").document().id
            val finalReview = review.copy(id = reviewId)
            db.collection("reviews").document(reviewId).set(finalReview).await()
            Result.success(reviewId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReviews(recipeId: String): List<Review> {
        return try {
            db.collection("reviews")
                .whereEqualTo("recipeId", recipeId)
                .get().await()
                .toObjects(Review::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Favorites and Shopping List
    suspend fun toggleFavorite(recipeId: String): Result<Unit> {
        val user = getCurrentUser() ?: return Result.failure(Exception("User not logged in"))
        val newFavorites = if (recipeId in user.favorites) {
            user.favorites - recipeId
        } else {
            user.favorites + recipeId
        }
        return try {
            db.collection("users").document(user.id)
                .update("favorites", newFavorites).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToShoppingList(ingredients: List<String>): Result<Unit> {
        val user = getCurrentUser() ?: return Result.failure(Exception("User not logged in"))
        val newList = (user.shoppingList + ingredients).distinct()
        return try {
            db.collection("users").document(user.id)
                .update("shoppingList", newList).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    suspend fun updateShoppingList(userId: String, shoppingList: List<String>) {
        db.collection("users").document(userId)
            .update("shoppingList", shoppingList)
            .await()
    }
    suspend fun uploadImage(fileUri: Uri, recipeId: String): String {
        val storageRef = FirebaseStorage.getInstance().reference.child("recipe_images/$recipeId.jpg")
        storageRef.putFile(fileUri).await()
        return storageRef.downloadUrl.await().toString()
    }
}