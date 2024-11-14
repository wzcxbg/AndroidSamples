package com.sliver.sample.route.navigation.features.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sliver.sample.route.navigation.Main
import com.sliver.sample.route.navigation.R
import com.sliver.sample.route.navigation.setupMainFragmentNavGraph

class MainFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navHostFragment = childFragmentManager.findFragmentById(R.id.nav_host)
                as NavHostFragment
        val navController = navHostFragment.navController
        navController.setupMainFragmentNavGraph()

        val navigationView = view.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        navigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> navController.navigate(Main.Home())
                R.id.star -> navController.navigate(Main.Star())
                R.id.mine -> navController.navigate(Main.Mine())
            }
            true
        }
    }

    companion object {
        fun newInstance(): MainFragment {
            return MainFragment()
        }
    }
}