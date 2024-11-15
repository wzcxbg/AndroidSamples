package com.sliver.sample.route.navigation.features.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.toRoute
import com.sliver.sample.route.navigation.R
import com.sliver.sample.route.navigation.Root
import com.sliver.sample.route.navigation.printNavArgs

class SettingsFragment : Fragment() {

    private val viewModel by viewModels<SettingsViewModel> { SettingsViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        printNavArgs()

        val settingsRoute1 = findNavController().getBackStackEntry<Root.Settings>()
            .toRoute<Root.Settings>()

        val settingsRoute2 = viewModel.savedStateHandle.toRoute<Root.Settings>()

        val settingsRoute3 = Root.Settings(arguments?.getString("name")!!)

        Log.e("TAG", "onViewCreated: $settingsRoute1")
        Log.e("TAG", "onViewCreated: $settingsRoute2")
        Log.e("TAG", "onViewCreated: $settingsRoute3")
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}