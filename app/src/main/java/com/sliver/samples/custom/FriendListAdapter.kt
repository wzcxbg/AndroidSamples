package com.sliver.samples.custom

import android.view.LayoutInflater
import android.view.ViewGroup
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
        val binding = ItemFriendListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = list[position]
        val binding = holder.binding
        binding.name.text = friend.name
        binding.info.text = friend.info
        //设置内容布局的宽为屏幕宽度
        binding.layoutContent.layoutParams.width = 1080
    }

    override fun getItemCount(): Int {
        return list.size
    }

    data class Friend(val name: String, val info: String)

    class ViewHolder(val binding: ItemFriendListBinding) :
        RecyclerView.ViewHolder(binding.root)

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