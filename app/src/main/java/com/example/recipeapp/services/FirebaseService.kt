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
        val pattern = Pattern.compile("^[a-zA-Z0-9!@#$%^&*]+$")
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
            try {
                val storageRef = storage.reference.child("recipe_images/$recipeId.jpg")
                storageRef.putFile(it).await()
                val url = storageRef.downloadUrl.await().toString()
                Log.d("FirebaseService", "Recipe image uploaded: $url")
                url
            } catch (e: Exception) {
                Log.e("FirebaseService", "Recipe image upload failed: ${e.message}", e)
                null
            }
        }

        val finalRecipe = recipe.copy(
            id = recipeId,
            createdBy = userId,
            imageUrl = imageUrl ?: recipe.imageUrl
        )
        db.collection("recipes").document(recipeId).set(finalRecipe).await()
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
    // Get recipes by category
    suspend fun getRecipesByCategory(category: String): List<Recipe> {
        return try {
            val snapshot = db.collection("recipes")
                .whereEqualTo("category", category)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(Recipe::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error fetching recipes by category: ${e.message}")
            emptyList()
        }
    }
    // Increment recipe views
    suspend fun incrementRecipeViews(recipeId: String) {
        try {
            val recipeRef = db.collection("recipes").document(recipeId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(recipeRef)
                val currentViews = snapshot.getLong("views") ?: 0
                transaction.update(recipeRef, "views", currentViews + 1)
            }.await()
            Log.d("FirebaseService", "Views incremented for recipe: $recipeId")
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to increment views: ${e.message}", e)
        }
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
    suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        val usersList = mutableListOf<User>()
        try {
            val snapshot = db.collection("users").get().await()
            for (document in snapshot.documents) {
                try {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        user.id = document.id  // ✅ Set Firestore doc ID
                        usersList.add(user)
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseService", "Failed to parse user: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Get all users failed: ${e.message}", e)
        }
        return@withContext usersList
    }



    // Delete user (for admin)
    suspend fun deleteUser(user: User): Boolean {
        return try {
            if (user.id.isBlank()) {
                throw IllegalArgumentException("User ID is blank")
            }
            db.collection("users").document(user.id).delete().await()
            true
        } catch (e: Exception) {
            Log.e("FirebaseService", "Delete user failed: ${e.message}", e)
            false
        }
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
        val finalReview = review.copy(id =reviewId)
        db.collection("reviews").document(reviewId).set(finalReview).await()
        Log.d("FirebaseService", "Review added: $reviewId")
        Result.success(reviewId)
    } catch (e: Exception) {
        Log.e("FirebaseService", "Add review failed: ${e.message}", e)
        Result.failure(e)
    }

    // Update user role (for admin)
    suspend fun updateUserRole(userId: String, role: String): Result<Unit> = try {
        val normalizedRole = role.replaceFirstChar { it.uppercase() } // "chef" -> "Chef"
        db.collection("users").document(userId).update("role", normalizedRole).await()
        Log.d("FirebaseService", "Updated role for $userId to $normalizedRole")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseService", "Failed to update role: ${e.message}", e)
        Result.failure(e)
    }

    // Get recipes by user ID
    suspend fun getRecipesByUser(userId: String): List<Recipe> = try {
        val snapshot = db.collection("recipes")
            .whereEqualTo("createdBy", userId)
            .get().await()
        snapshot.documents.mapNotNull { it.toObject(Recipe::class.java) }
    } catch (e: Exception) {
        Log.e("FirebaseService", "Get recipes by user failed: ${e.message}", e)
        emptyList()
    }
    suspend fun getAllRecipes(): List<Recipe> = try {
        val snapshot = db.collection("recipes").get().await()
        snapshot.documents.mapNotNull { it.toObject(Recipe::class.java) }
    } catch (e: Exception) {
        Log.e("FirebaseService", "Get all recipes failed: ${e.message}", e)
        emptyList()
    }
    suspend fun getRecipeById(id: String): Recipe? {
        return try {
            val snapshot = db.collection("recipes").document(id).get().await()
            snapshot.toObject(Recipe::class.java)?.copy(id = snapshot.id)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error getting recipe: ${e.message}", e)
            null
        }
    }
    suspend fun getUserById(id: String): User? {
        return try {
            val snapshot = db.collection("users").document(id).get().await()
            snapshot.toObject(User::class.java)?.copy(id = snapshot.id)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error getting user: ${e.message}", e)
            null
        }
    }
    // Add recipe to user's favorites
    suspend fun addFavorite(recipeId: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val userRef = db.collection("users").document(userId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val favorites = snapshot.get("favorites") as? MutableList<String> ?: mutableListOf()
                if (!favorites.contains(recipeId)) {
                    favorites.add(recipeId)
                    transaction.update(userRef, "favorites", favorites)
                }
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Remove recipe from user's favorites
    suspend fun removeFavorite(recipeId: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val userRef = db.collection("users").document(userId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val favorites = snapshot.get("favorites") as? MutableList<String> ?: mutableListOf()
                favorites.remove(recipeId)
                transaction.update(userRef, "favorites", favorites)
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Get list of favorite recipe IDs
    suspend fun getFavoriteRecipeIds(): List<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()
            val snapshot = db.collection("users").document(userId).get().await()
            snapshot.get("favorites") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
 }