package com.sliver.samples.custom

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sliver.samples.databinding.ItemFriendListBinding

class FriendListAdapter : RecyclerView.Adapter<FriendListAdapter.ViewHolder>() {
    private val list = ArrayList<Friend>()

    fun setItems(newItems: List<Friend>) {
        val oldItems = ArrayList(list)
        val diffCallback = DiffCallback(oldItems, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.list.clear()
        this.list.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val slideMenuItemView = SlideMenuItemView(parent.context)
        slideMenuItemView.layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT,
        )
        val contentBinding = ItemFriendListBinding.inflate(
            LayoutInflater.from(parent.context),
            slideMenuItemView.getLayoutContent(), true
        )

        val menuView = TextView(parent.context)
        menuView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        menuView.text = "Delete"
        menuView.gravity = Gravity.CENTER
        menuView.minWidth = 120
        menuView.background = ColorDrawable(Color.RED)
        slideMenuItemView.getLayoutMenu().addView(menuView)
        return ViewHolder(slideMenuItemView, contentBinding, menuView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = list[position]
        val binding = holder.contentBinding
        binding.name.text = friend.name
        binding.info.text = friend.info
    }

    override fun getItemCount(): Int {
        return list.size
    }

    data class Friend(val name: String, val info: String)

    class ViewHolder(
        val rootView: SlideMenuItemView,
        val contentBinding: ItemFriendListBinding,
        val menuView: TextView
    ) : RecyclerView.ViewHolder(rootView)

    class DiffCallback(
        private val oldItems: List<Friend>,
        private val newItems: List<Friend>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldItems.size
        }

        override fun getNewListSize(): Int {
            return newItems.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] === newItems[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}