package com.example.gc_last.main

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.gc_last.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NavigationUI.setupActionBarWithNavController(
            this, findNavController(R.id.navigation_host)
        )
    }

    override fun onSupportNavigateUp() = findNavController(R.id.navigation_host).navigateUp()
}
