package com.sliver.samples.custom

import android.view.ViewGroup
import android.widget.HorizontalScrollView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

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
        return ViewHolder(slideMenuItemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = list[position]

    }

    override fun getItemCount(): Int {
        return list.size
    }

    data class Friend(val name: String, val info: String)

    class ViewHolder(val view: SlideMenuItemView) :
        RecyclerView.ViewHolder(view)

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