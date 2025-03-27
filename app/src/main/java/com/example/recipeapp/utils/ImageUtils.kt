package com.example.recipeapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.ByteArrayOutputStream
import android.provider.MediaStore

object ImageUtils {

    // Compress an image URI to a byte array (for Firebase Storage upload)
    fun compressImage(context: Context, imageUri: Uri, maxSize: Int = 1024): ByteArray? {
        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            val scaledBitmap = scaleBitmap(bitmap, maxSize)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream) // 80% quality
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Scale bitmap to a maximum size while maintaining aspect ratio
    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height
        if (width > height) {
            if (width > maxSize) {
                height = (height * maxSize) / width
                width = maxSize
            }
        } else {
            if (height > maxSize) {
                width = (width * maxSize) / height
                height = maxSize
            }
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}