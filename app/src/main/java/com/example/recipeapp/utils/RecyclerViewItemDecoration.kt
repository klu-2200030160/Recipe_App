package com.example.recipeapp.utils

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewItemDecoration(private val context: Context, private val marginInDp: Int = 16) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val marginInPx = dpToPx(context, marginInDp)
        outRect.apply {
            left = marginInPx
            right = marginInPx
            top = marginInPx
            bottom = marginInPx
        }
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}