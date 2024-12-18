package com.sliver.sample.route.navigation.features.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import com.sliver.sample.route.navigation.R
import com.sliver.sample.route.navigation.Root

class MineFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mine, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val showDialog = view.findViewById<TextView>(R.id.show_dialog)
        val openUrl = view.findViewById<TextView>(R.id.open_url)
        val navController = requireActivity().findNavController(R.id.nav_host)
        showDialog.setOnClickListener {
            navController.navigate(Root.Loading("Loading..."), navOptions {
                anim {
                    enter = android.R.anim.slide_in_left
                    exit = android.R.anim.slide_out_right
                    popEnter = android.R.anim.slide_in_left
                    popExit = android.R.anim.slide_out_right
                }
            })
        }
        openUrl.setOnClickListener {
            navController.navigate(Root.WebPage("https://www.baidu.com"), navOptions {
                anim {
                    enter = android.R.anim.slide_in_left
                    exit = android.R.anim.slide_out_right
                    popEnter = android.R.anim.slide_in_left
                    popExit = android.R.anim.slide_out_right
                }
            })
        }
    }

    companion object {
        fun newInstance(): MineFragment {
            return MineFragment()
        }
    }
}