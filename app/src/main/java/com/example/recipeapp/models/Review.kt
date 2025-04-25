package com.example.recipeapp.models

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Review(
    @PropertyName("id") val id: String = "",
    @PropertyName("recipeId") val recipeId: String = "",
    @PropertyName("userId") val userId: String = "",
    @PropertyName("comment") val comment: String = "",
    @PropertyName("rating") val rating: Float = 0f
) : Parcelable
