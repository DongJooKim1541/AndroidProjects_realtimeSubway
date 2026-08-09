package com.example.gc_last.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.gc_last.R

/** 단일 Activity. 화면 전환은 Navigation Component가 담당한다. */
class MainActivity : AppCompatActivity() {

    private val navController: NavController by lazy { findNavController(R.id.navigation_host) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NavigationUI.setupActionBarWithNavController(this, navController)
    }

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp()
}
