package com.example.recipeapp.services

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.recipeapp.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseService(context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val contentResolver: ContentResolver = context.contentResolver

    // Upload image to Firebase Storage and get the download URL
    suspend fun uploadImageToFirebaseStorage(uri: Uri, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val storageRef = FirebaseStorage.getInstance().reference.child("recipe_images/$fileName")
                storageRef.putFile(uri).await()
                storageRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                Log.e("FirebaseStorage", "Upload failed: ${e.message}", e)
                null
            }
        }
    }

    suspend fun signUp(email: String, password: String, username: String, role: String): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User ID not found")
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

    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User ID not found")
            val userDoc = db.collection("users").document(userId).get().await()
            val role = userDoc.getString("role") ?: throw Exception("Role not found")
            Result.success(role)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): User? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            val doc = db.collection("users").document(userId).get().await()
            val role = doc.getString("role") ?: "customer"
            User(userId, role)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun signOut() {
        try {
            auth.signOut()
        } catch (_: Exception) {}
    }

    suspend fun addRecipe(recipe: Recipe, imageUri: Uri?): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            val recipeId = recipe.id.ifEmpty { db.collection("recipes").document().id }
            val imageUrl = imageUri?.let {
                uploadImageToFirebaseStorage(it, "recipe_$recipeId.jpg")
            }
            val finalRecipe = recipe.copy(
                id = recipeId,
                createdBy = userId,
                imageUrl = imageUrl ?: recipe.imageUrl
            )
            db.collection("recipes").document(recipeId).set(finalRecipe).await()
            Result.success(recipeId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRecipe(recipe: Recipe, imageUri: Uri?): Result<Unit> {
        return try {
            val imageUrl = imageUri?.let {
                uploadImageToFirebaseStorage(it, "recipe_${recipe.id}.jpg")
            }
            val updated = recipe.copy(imageUrl = imageUrl ?: recipe.imageUrl)
            db.collection("recipes").document(recipe.id).set(updated).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecipe(recipeId: String): Result<Unit> {
        return try {
            db.collection("recipes").document(recipeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecipes(): List<Recipe> {
        return try {
            db.collection("recipes").get().await()
                .documents.mapNotNull { it.toObject(Recipe::class.java) }
        } catch (e: Exception) {
            emptyList()
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

    suspend fun addReview(review: Review): Result<String> {
        return try {
            if (review.comment.isEmpty() || review.rating !in 0f..5f) {
                return Result.failure(Exception("Invalid review"))
            }
            val reviewId = db.collection("reviews").document().id
            val finalReview = review.copy(id = reviewId)
            db.collection("reviews").document(reviewId).set(finalReview).await()
            Result.success(reviewId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(recipeId: String): Result<Unit> {
        val user = getCurrentUser() ?: return Result.failure(Exception("User not logged in"))
        return try {
            val userDoc = db.collection("users").document(user.id).get().await()
            val favorites = userDoc.get("favorites") as? List<String> ?: emptyList()
            val updated = if (recipeId in favorites) favorites - recipeId else favorites + recipeId
            db.collection("users").document(user.id).update("favorites", updated).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToShoppingList(ingredients: List<String>): Result<Unit> {
        val user = getCurrentUser() ?: return Result.failure(Exception("User not logged in"))
        return try {
            val doc = db.collection("users").document(user.id).get().await()
            val existing = doc.get("shoppingList") as? List<String> ?: emptyList()
            val updated = (existing + ingredients).distinct()
            db.collection("users").document(user.id).update("shoppingList", updated).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateShoppingList(userId: String, shoppingList: List<String>) {
        db.collection("users").document(userId).update("shoppingList", shoppingList).await()
    }

    suspend fun getFavoriteRecipeIds(userId: String): List<String> {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.get("favorites") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRecipesByIds(recipeIds: List<String>): List<Recipe> {
        if (recipeIds.isEmpty()) return emptyList()

        return try {
            val querySnapshot = db.collection("recipes")
                .whereIn("id", recipeIds.take(10)) // Firebase supports max 10 IDs at a time
                .get()
                .await()

            querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Recipe::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "getRecipesByIds failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun updateUser(userId: String, username: String?, email: String?): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()
            username?.let { updates["username"] = it }
            email?.let { updates["email"] = it }
            if (updates.isNotEmpty()) {
                db.collection("users").document(userId).update(updates).await()
                if (email != null && auth.currentUser?.email != email) {
                    auth.currentUser?.updateEmail(email)?.await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}