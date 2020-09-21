package com.example.gc_last.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gc_last.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//Splash 화면
class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO){
            Thread.sleep(3000)
            requireActivity().runOnUiThread{
                findNavController().navigate(R.id.action_global_searchFragment)
            }
        }
    }

}
