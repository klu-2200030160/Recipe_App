
package com.example.recipeapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.adapters.UserAdapter
import com.example.recipeapp.models.User
import com.example.recipeapp.services.FirebaseService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserManagementActivity : AppCompatActivity() {

    private lateinit var userRecyclerView: RecyclerView
    private lateinit var addUserFab: FloatingActionButton
    private lateinit var userAdapter: UserAdapter
    private lateinit var firebaseService: FirebaseService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)


        setSupportActionBar(findViewById(R.id.toolbar1))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "User Management"
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar1)
        for (i in 0 until toolbar.childCount) {
            val view = toolbar.getChildAt(i)
            if (view is TextView) {
                val typeface = ResourcesCompat.getFont(this, R.font.poppins) // Change to your font
                view.typeface = typeface
                view.textSize = 20f // Optional: set text size
                break
            }
        }

        userRecyclerView = findViewById(R.id.userRecyclerView)
        addUserFab = findViewById(R.id.addUserFab)
        firebaseService = FirebaseService(this)
        userAdapter = UserAdapter(
            onEditClick = { user ->
                val Intent= Intent(this, AddEditUserActivity::class.java)
                Intent.putExtra("USER_ID", user.id)
                startActivity(Intent)
            },
            onDeleteClick = { user -> deleteUser(user) }
        )

        userRecyclerView.adapter = userAdapter
        userRecyclerView.layoutManager = LinearLayoutManager(this)

        addUserFab.setOnClickListener {
            Toast.makeText(this, "Add new user", Toast.LENGTH_SHORT).show()
        }

        loadUsers()
    }

    private fun loadUsers() {
        CoroutineScope(Dispatchers.Main).launch {
            val users = withContext(Dispatchers.IO) {
                firebaseService.getAllUsers()
            }
            userAdapter.submitList(users)
        }
    }

    private fun deleteUser(user: User) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                firebaseService.deleteUser(user)
            }
            if (result) {
                showToast("User deleted")
                loadUsers()
            } else {
                showToast("Failed to delete user")
            }
        }
    }


    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
