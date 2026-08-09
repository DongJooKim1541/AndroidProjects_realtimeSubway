package com.example.gc_last.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gc_last.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 시작 화면. [SPLASH_DELAY_MS] 후 검색 화면으로 넘어간다. */
class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_splash, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 이전 구현은 Dispatchers.IO에서 Thread.sleep으로 스레드를 붙잡은 뒤 runOnUiThread로
        // 넘어갔고, 대기 중 화면을 벗어나면 findNavController()가 예외를 던졌다.
        // viewLifecycleOwner 스코프의 delay로 바꾸면 화면이 사라질 때 자동으로 취소된다.
        viewLifecycleOwner.lifecycleScope.launch {
            delay(SPLASH_DELAY_MS)
            findNavController().navigate(R.id.action_global_searchFragment)
        }
    }

    companion object {
        private const val SPLASH_DELAY_MS = 3_000L
    }
}
