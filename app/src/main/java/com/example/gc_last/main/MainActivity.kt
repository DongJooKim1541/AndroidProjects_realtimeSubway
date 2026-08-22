package com.example.gc_last.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.gc_last.R

/** 단일 Activity. 화면 전환은 Navigation Component가 담당한다. */
class MainActivity : AppCompatActivity() {

    private val navController: NavController by lazy { findNavController(R.id.navigation_host) }

    /**
     * 뒤로가기 화살표를 어느 화면에도 띄우지 않는다.
     *
     * 기기의 뒤로가기로 충분하고, 화살표까지 두면 제목 옆이 좁아진다. 검색 화면에서는
     * 뒤로 갈 곳도 없다(스플래시는 백스택에서 빠지고, 뒤로가기는 앱 종료를 묻는다).
     */
    private val appBarConfiguration by lazy {
        AppBarConfiguration(
            setOf(
                R.id.splashFragment,
                R.id.searchFragment,
                R.id.resultFragment,
                R.id.saveFragment,
                R.id.savedTimeTableFragment
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // 검색 화면은 맨 위가 바로 탭이어야 한다. 제목만 있는 막대가 화면을 잡아먹는다.
        // 나머지 화면은 어느 역/무엇을 보는 화면인지 제목이 필요하므로 그대로 둔다.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val hideBar = destination.id == R.id.searchFragment || destination.id == R.id.splashFragment
            if (hideBar) supportActionBar?.hide() else supportActionBar?.show()
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        NavigationUI.navigateUp(navController, appBarConfiguration)
}
