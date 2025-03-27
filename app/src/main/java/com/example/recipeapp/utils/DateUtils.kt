package com.example.recipeapp.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    // Convert timestamp to readable date string
    fun formatTimestamp(timestamp: Long, pattern: String = "dd MMM yyyy, HH:mm"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // Get current timestamp
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }
}