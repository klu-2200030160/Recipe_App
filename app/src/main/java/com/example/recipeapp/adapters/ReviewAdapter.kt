package com.example.recipeapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.models.Review

class ReviewAdapter : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    private var reviews: List<Review> = emptyList()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val commentTextView: TextView = itemView.findViewById(R.id.reviewCommentTextView)
        val reviewRatingBar: RatingBar = itemView.findViewById(R.id.reviewRatingBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]
        holder.commentTextView.text = review.comment
        holder.reviewRatingBar.rating = review.rating
    }

    override fun getItemCount(): Int = reviews.size

    fun submitList(newReviews: List<Review>) {
        reviews = newReviews
        notifyDataSetChanged()
    }
}
