package com.sliver.samples.viewpager

import androidx.recyclerview.widget.LinearLayoutManager
import com.sliver.samples.base.BaseFragment
import com.sliver.samples.databinding.FragmentViewPager2PageBinding

class ViewPager2PageFragment : BaseFragment<FragmentViewPager2PageBinding>() {

    override fun initView() {
        binding.recyclerView.adapter = ViewPager2PageAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    companion object {
        fun newInstance(): ViewPager2PageFragment {
            return ViewPager2PageFragment()
        }
    }
}