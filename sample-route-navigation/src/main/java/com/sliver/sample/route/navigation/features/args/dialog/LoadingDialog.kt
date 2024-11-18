package com.sliver.sample.route.navigation.features.args.dialog

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.toRoute
import com.sliver.sample.route.navigation.Root
import com.sliver.sample.route.navigation.printNavArgs

class LoadingDialog : DialogFragment() {
    private val viewModel by viewModels<LoadingViewModel> { LoadingViewModel.Factory }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        printNavArgs()

        val loadingRoute = findNavController()
            .getBackStackEntry<Root.Loading>()
            .toRoute<Root.Loading>()

        val loadingRoute2 = viewModel
            .savedStateHandle
            .toRoute<Root.Loading>()

        val loadingRoute3 = Root.Loading(
            arguments?.getString("msg")!!
        )

        Log.e("TAG", "onCreateView: $loadingRoute")
        Log.e("TAG", "onCreateView: $loadingRoute2")
        Log.e("TAG", "onCreateView: $loadingRoute3")


        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.HORIZONTAL
        linearLayout.gravity = Gravity.CENTER

        val progressBar = ProgressBar(
            context, null, 0,
            androidx.appcompat.R.style.Widget_AppCompat_ProgressBar
        )
        progressBar.isIndeterminate = true
        val textView = TextView(context)
        textView.text = loadingRoute.msg
        linearLayout.addView(progressBar)
        linearLayout.addView(textView)
        return linearLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val window = requireDialog().window
        window?.attributes?.width = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes?.height = WindowManager.LayoutParams.WRAP_CONTENT
    }
}