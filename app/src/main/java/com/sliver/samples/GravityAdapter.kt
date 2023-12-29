package com.sliver.samples

import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.recyclerview.widget.RecyclerView

class GravityAdapter : RecyclerView.Adapter<GravityAdapter.ViewHolder>() {
    class ViewHolder(val textView: CheckedTextView) : RecyclerView.ViewHolder(textView)

    private val gravities = arrayOf(
        GravityItem(Gravity.FILL, "Gravity.FILL", false),
        GravityItem(Gravity.FILL_HORIZONTAL, "Gravity.FILL_HORIZONTAL", false),
        GravityItem(Gravity.FILL_VERTICAL, "Gravity.FILL_VERTICAL", false),
        GravityItem(Gravity.START, "Gravity.START", false),
        GravityItem(Gravity.END, "Gravity.END", false),
        GravityItem(Gravity.TOP, "Gravity.TOP", false),
        GravityItem(Gravity.BOTTOM, "Gravity.BOTTOM", false),
        GravityItem(Gravity.CENTER, "Gravity.CENTER", false),
        GravityItem(Gravity.CENTER_HORIZONTAL, "Gravity.CENTER_HORIZONTAL", false),
        GravityItem(Gravity.CENTER_VERTICAL, "Gravity.CENTER_VERTICAL", false),
        GravityItem(Gravity.DISPLAY_CLIP_HORIZONTAL, "Gravity.DISPLAY_CLIP_HORIZONTAL", false),
        GravityItem(Gravity.DISPLAY_CLIP_VERTICAL, "Gravity.DISPLAY_CLIP_VERTICAL", false),
        GravityItem(Gravity.CLIP_HORIZONTAL, "Gravity.CLIP_HORIZONTAL", false),
        GravityItem(Gravity.CLIP_VERTICAL, "Gravity.CLIP_VERTICAL", false),
        GravityItem(Gravity.NO_GRAVITY, "Gravity.NO_GRAVITY", false),
    )
    private var listener: GravityListener? = null

    fun setGravityChangedListener(listener: GravityListener) {
        this.listener = listener
    }

    fun getSelectedItems(): List<GravityItem> {
        return gravities.filter { it.selected }
    }

    fun getSelectedGravity(): Int {
        return gravities.filter { it.selected }
            .map { it.gravity }
            .reduceOrNull { acc, i -> acc.or(i) }
            ?: Gravity.NO_GRAVITY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val typedValue = TypedValue()
        val context = parent.context
        context.theme.resolveAttribute(
            android.R.attr.listChoiceIndicatorMultiple,
            typedValue, true
        )
        val textView = CheckedTextView(context)
        textView.gravity = Gravity.CENTER_VERTICAL
        textView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
            typedValue.resourceId,
            0, 0, 0,
        )
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gravityItem = gravities[holder.adapterPosition]
        holder.textView.text = gravityItem.description
        holder.textView.isChecked = gravityItem.selected
        holder.textView.setOnClickListener {
            gravityItem.selected = !gravityItem.selected
            notifyItemChanged(holder.adapterPosition)
            listener?.onGravityChanged(getSelectedGravity())
        }
    }

    override fun getItemCount(): Int {
        return gravities.size
    }

    interface GravityListener {
        fun onGravityChanged(gravity: Int)
    }

    data class GravityItem(
        val gravity: Int,
        val description: String,
        var selected: Boolean,
    )
}