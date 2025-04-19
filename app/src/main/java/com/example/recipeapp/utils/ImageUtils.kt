package com.example.recipeapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    fun compressImage(context: Context, uri: Uri): Uri {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val compressedBytes = outputStream.toByteArray()
        outputStream.close()

        val tempFile = File(context.cacheDir, "compressed_image_${System.currentTimeMillis()}.jpg")
        val fileOutputStream = FileOutputStream(tempFile)
        fileOutputStream.write(compressedBytes)
        fileOutputStream.close()

        return Uri.fromFile(tempFile)
    }
}