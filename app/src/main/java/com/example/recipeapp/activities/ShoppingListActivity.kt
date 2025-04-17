package com.example.recipeapp.activities

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.R
import com.example.recipeapp.adapters.ShoppingListAdapter
import com.example.recipeapp.services.FirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShoppingListActivity : AppCompatActivity() {
    private lateinit var firebaseService: FirebaseService
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShoppingListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shopping_list)
        firebaseService = FirebaseService(this)

        // Initialize UI elements
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.shoppingListRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter
        adapter = ShoppingListAdapter(emptyList())
        recyclerView.adapter = adapter

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load shopping list
        loadShoppingList()
    }

    private fun loadShoppingList() {
        CoroutineScope(Dispatchers.Main).launch {
            progressBar.visibility = View.VISIBLE
            val user = firebaseService.getCurrentUser()
            user?.shoppingList?.let { shoppingList ->
                adapter.updateItems(shoppingList)
            } ?: run {
                adapter.updateItems(emptyList()) // Clear list if no user or shopping list
            }
            progressBar.visibility = View.GONE
        }
    }
}