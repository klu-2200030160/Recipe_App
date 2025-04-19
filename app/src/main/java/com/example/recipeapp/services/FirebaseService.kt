package com.example.recipeapp.services

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.recipeapp.models.Recipe
import com.example.recipeapp.models.Review
import com.example.recipeapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class FirebaseService(context: Context) {

    val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Validate username (no special characters)
    private fun isValidUsername(username: String): Boolean {
        val pattern = Pattern.compile("^[a-zA-Z0-9]+$")
        return pattern.matcher(username).matches() && username.length >= 3
    }

    // Validate email (must contain @gmail.com)
    private fun isValidEmail(email: String): Boolean {
        return email.endsWith("@gmail.com") && email.length >= 10
    }

    // Validate password (no special characters, min length 6)
    private fun isValidPassword(password: String): Boolean {
        val pattern = Pattern.compile("^[a-zA-Z0-9]+$")
        return pattern.matcher(password).matches() && password.length >= 6
    }

    // Get current user
    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: run {
            Log.e("FirebaseService", "No authenticated user")
            return@withContext null
        }
        try {
            val doc = db.collection("users").document(userId).get().await()
            if (!doc.exists()) {
                Log.e("FirebaseService", "User document does not exist for UID: $userId")
                return@withContext null
            }
            val user = doc.toObject(User::class.java)?.copy(id = userId)
            Log.d("FirebaseService", "Fetched user: $user")
            user
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to fetch user: ${e.message}", e)
            null
        }
    }

    // Sign up user with validation
    suspend fun signUp(email: String, password: String, username: String, role: String): Result<Unit> = try {
        if (!isValidEmail(email)) throw Exception("Email must end with @gmail.com")
        if (!isValidUsername(username)) throw Exception("Username must contain only letters and numbers, minimum 3 characters")
        if (!isValidPassword(password)) throw Exception("Password must contain only letters and numbers, minimum 6 characters")

        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val userId = authResult.user?.uid ?: throw Exception("User ID not found")
        val normalizedRole = role.lowercase()
        val userData = hashMapOf(
            "email" to email,
            "username" to username,
            "role" to normalizedRole,
            "name" to "",
            "age" to 0,
            "mobile" to "",
            "profileImageUrl" to "",
            "favorites" to emptyList<String>(),
            "shoppingList" to emptyList<String>()
        )
        db.collection("users").document(userId).set(userData).await()
        Log.d("FirebaseService", "User signed up: $userId, role: $normalizedRole")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseService", "Sign-up failed: ${e.message}", e)
        Result.failure(e)
    }

    // Sign in user
    suspend fun signIn(email: String, password: String): Result<String> = try {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val userId = authResult.user?.uid ?: throw Exception("User ID not found")
        val userDoc = db.collection("users").document(userId).get().await()
        val role = userDoc.getString("role") ?: throw Exception("Role not found")
        Log.d("FirebaseService", "User signed in: $userId, role: $role")
        Result.success(role)
    } catch (e: Exception) {
        Log.e("FirebaseService", "Sign-in failed: ${e.message}", e)
        Result.failure(e)
    }

    // Sign out user
    suspend fun signOut() {
        try {
            auth.signOut()
            Log.d("FirebaseService", "User signed out")
        } catch (e: Exception) {
            Log.e("FirebaseService", "Sign-out failed: ${e.message}", e)
        }
    }

    // Save or update recipe
    suspend fun saveRecipe(recipe: Recipe, imageUri: Uri?): Result<String> = try {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val recipeId = recipe.id.ifEmpty { db.collection("recipes").document().id }
        Log.d("FirebaseService", "Attempting to save recipe: $recipeId for user: $userId")

        val imageUrl = imageUri?.let {
            withContext(Dispatchers.IO) {
                val storageRef = storage.reference.child("recipe_images/$recipeId.jpg")
                storageRef.putFile(it).await()
                val url = storageRef.downloadUrl.await().toString()
                Log.d("FirebaseService", "Image uploaded: $url")
                url
            }
        } ?: recipe.imageUrl

        val finalRecipe = recipe.copy(
            id = recipeId,
            createdBy = userId,
            imageUrl = imageUrl
        )
        Log.d("FirebaseService", "Writing recipe to Firestore: $finalRecipe")
        db.collection("recipes").document(recipeId).set(finalRecipe).await()
        Log.d("FirebaseService", "Recipe saved: $recipeId")
        Result.success(recipeId)
    } catch (e: Exception) {
        Log.e("FirebaseService", "Save recipe failed: ${e.message}", e)
        Result.failure(e)
    }

    // Delete recipe
    suspend fun deleteRecipe(recipeId: String): Result<Unit> = try {
        db.collection("recipes").document(recipeId).delete().await()
        storage.reference.child("recipe_images/$recipeId.jpg").delete().await()
        Log.d("FirebaseService", "Recipe deleted: $recipeId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseService", "Delete recipe failed: ${e.message}", e)
        Result.failure(e)
    }

    // Get all recipes
    suspend fun getRecipes(): List<Recipe> = try {
        val snapshot = db.collection("recipes").get().await()
        snapshot.documents.mapNotNull { it.toObject(Recipe::class.java) }
    } catch (e: Exception) {
        Log.e("FirebaseService", "Get recipes failed: ${e.message}", e)
        emptyList()
    }

    // Get popular recipes (sorted by review count)
    suspend fun getPopularRecipes(): List<Recipe> = try {
        val recipes = getRecipes()
        val recipeReviewCounts = mutableMapOf<String, Int>()
        recipes.forEach { recipe ->
            val reviews = getReviews(recipe.id)
            recipeReviewCounts[recipe.id] = reviews.size
        }
        recipes.sortedByDescending { recipeReviewCounts[it.id] ?: 0 }.take(10)
    } catch (e: Exception) {
        Log.e("FirebaseService", "Get popular recipes failed: ${e.message}", e)
        emptyList()
    }

    // Get favorite recipe IDs
    suspend fun getFavoriteRecipeIds(userId: String): List<String> = try {
        val doc = db.collection("users").document(userId).get().await()
        doc.toObject(User::class.java)?.favorites ?: emptyList()
    } catch (e: Exception) {
        Log.e("FirebaseService", "Get favorite recipe IDs failed: ${e.message}", e)
        emptyList()
    }

    // Get recipes by IDs
    suspend fun getRecipesByIds(recipeIds: List<String>): List<Recipe> {
        return try {
            if (recipeIds.isEmpty()) return emptyList()
            val snapshot = db.collection("recipes")
                .whereIn("id", recipeIds.take(10))
                .get().await()
            snapshot.documents.mapNotNull { it.toObject(Recipe::class.java) }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Get recipes by IDs failed: ${e.message}", e)
            emptyList()
        }
    }

    // Update user profile
    suspend fun updateUser(userId: String, username: String?, email: String?, name: String?, age: Int?, mobile: String?, profileImageUri: Uri?): Result<Unit> = try {
        if (username != null && !isValidUsername(username)) throw Exception("Invalid username")
        if (email != null && !isValidEmail(email)) throw Exception("Invalid email")

        val updates = mutableMapOf<String, Any>()
        username?.let { updates["username"] = it }
        email?.let { updates["email"] = it }
        name?.let { updates["name"] = it }
        age?.let { updates["age"] = it }
        mobile?.let { updates["mobile"] = it }

        val profileImageUrl = profileImageUri?.let {
            withContext(Dispatchers.IO) {
                val storageRef = storage.reference.child("profile_images/$userId.jpg")
                storageRef.putFile(it).await()
                storageRef.downloadUrl.await().toString()
            }
        }
        profileImageUrl?.let { updates["profileImageUrl"] = it }

        if (updates.isNotEmpty()) {
            db.collection("users").document(userId).update(updates).await()
            if (email != null && auth.currentUser?.email != email) {
                auth.currentUser?.updateEmail(email)?.await()
            }
        }
        Log.d("FirebaseService", "User updated: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseService", "Update user failed: ${e.message}", e)
        Result.failure(e)
    }

    // Get all users (for admin)
    suspend fun getAllUsers(): List<User> = try {
        val snapshot = db.collection("users").get().await()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(User::class.java)?.copy(id = doc.id)
        }
    } catch (e: Exception) {
        Log.e("FirebaseService", "Get all users failed: ${e.message}", e)
        emptyList()
    }

    // Delete user (for admin)
    suspend fun deleteUser(userId: String): Result<Unit> = try {
        db.collection("users").document(userId).delete().await()
        storage.reference.child("profile_images/$userId.jpg").delete().await()
        Log.d("FirebaseService", "User deleted: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseService", "Delete user failed: ${e.message}", e)
        Result.failure(e)
    }

    // Get reviews for a recipe
    suspend fun getReviews(recipeId: String): List<Review> = try {
        val snapshot = db.collection("reviews")
            .whereEqualTo("recipeId", recipeId)
            .get().await()
        snapshot.toObjects(Review::class.java)
    } catch (e: Exception) {
        Log.e("FirebaseService", "Get reviews failed: ${e.message}", e)
        emptyList()
    }

    // Add a review
    suspend fun addReview(review: Review): Result<String> = try {
        if (review.comment.isEmpty() || review.rating !in 0f..5f) {
            throw Exception("Invalid review")
        }
        val reviewId = db.collection("reviews").document().id
        val finalReview = review.copy(id = reviewId)
        db.collection("reviews").document(reviewId).set(finalReview).await()
        Log.d("FirebaseService", "Review added: $reviewId")
        Result.success(reviewId)
    } catch (e: Exception) {
        Log.e("FirebaseService", "Add review failed: ${e.message}", e)
        Result.failure(e)
    }

    // Update user role (for admin)
    suspend fun updateUserRole(userId: String, role: String): Result<Unit> = try {
        val normalizedRole = role.lowercase()
        db.collection("users").document(userId).update("role", normalizedRole).await()
        Log.d("FirebaseService", "Updated role for $userId to $normalizedRole")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseService", "Failed to update role: ${e.message}", e)
        Result.failure(e)
    }
}