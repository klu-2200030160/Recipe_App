package com.example.recipeapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.models.User

class UserAdapter(
    private var users: List<User> = listOf(),
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    fun submitList(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userEmail: TextView = itemView.findViewById(R.id.userEmail)
        private val userRole: TextView = itemView.findViewById(R.id.userRole)
        private val userName: TextView = itemView.findViewById(R.id.userName)
        private val editUserBtn: ImageButton = itemView.findViewById(R.id.editUserBtn)
        private val deleteUserBtn: ImageButton = itemView.findViewById(R.id.deleteUserBtn)

        fun bind(user: User) {
            userEmail.text = user.email
            userRole.text = "Role: ${user.role}"
            userName.text = "Name: ${user.username ?: "N/A"}"
            editUserBtn.setOnClickListener { onEditClick(user) }
            deleteUserBtn.setOnClickListener { onDeleteClick(user) }
        }
    }
}
