package com.sliver.samples.viewpager

import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ViewPager2PageAdapter : RecyclerView.Adapter<ViewPager2PageAdapter.ViewPage2PageHolder>() {
    class ViewPage2PageHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    private val colors = listOf(
        0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF, 0xFF000000,
        0xFFFFFFFF, 0xFF8B0000, 0xFF7CFC00, 0xFF87CEEB, 0xFFFFA500, 0xFF800080, 0xFF808080,
        0xFFA52A2A, 0xFFFFC0CB, 0xFF008000, 0xFF000080, 0xFF800000, 0xFF008080, 0xFF808000,
        0xFFC0C0C0, 0xFFA9A9A9, 0xFF696969, 0xFFBDB76B, 0xFF8B4513, 0xFF2E8B57, 0xFF4682B4,
        0xFFD2691E, 0xFF5F9EA0, 0xFF7FFF00, 0xFFDC143C, 0xFF00008B, 0xFF008B8B, 0xFFB8860B,
        0xFFA9A9A9, 0xFF006400, 0xFFBDB76B, 0xFF8B008B, 0xFF556B2F, 0xFFFF8C00, 0xFF9932CC,
        0xFF8B0000, 0xFFE9967A, 0xFF8FBC8F, 0xFF483D8B, 0xFF2F4F4F, 0xFF00CED1, 0xFFFF1493,
        0xFF00BFFF, 0xFF696969, 0xFF1E90FF, 0xFFB22222, 0xFFFFFAF0, 0xFF228B22, 0xFFFF00FF,
        0xFFDCDCDC, 0xFFF8F8FF, 0xFFFFD700, 0xFFDAA520, 0xFF808080, 0xFF00FF00, 0xFFADFF2F,
        0xFFF0FFF0, 0xFFFF69B4, 0xFFCD5C5C, 0xFF4B0082, 0xFFF0E68C, 0xFFE6E6FA, 0xFFFFF0F5,
        0xFF7CFC00, 0xFFFFFACD, 0xFFADD8E6, 0xFFF08080, 0xFFE0FFFF, 0xFFFAFAD2, 0xFFD3D3D3,
        0xFF90EE90, 0xFFFFB6C1, 0xFFFFA07A, 0xFF20B2AA, 0xFF87CEFA, 0xFF778899, 0xFFB0C4DE,
        0xFFFFE4C4, 0xFF0000CD, 0xFFFFEBCD, 0xFF0000FF, 0xFF8A2BE2, 0xFFA52A2A, 0xFFDEB887,
        0xFF5F9EA0, 0xFF7FFF00, 0xFFD2691E, 0xFFFF7F50, 0xFF6495ED, 0xFFFFF8DC, 0xFFDC143C,
        0xFF00FFFF, 0xFF00008B, 0xFF008B8B, 0xFFB8860B, 0xFFA9A9A9, 0xFF006400, 0xFFBDB76B,
        0xFF8B008B, 0xFF556B2F, 0xFFFF8C00, 0xFF9932CC, 0xFF8B0000, 0xFFE9967A, 0xFF8FBC8F,
        0xFF483D8B, 0xFF2F4F4F, 0xFF00CED1, 0xFFFF1493, 0xFF00BFFF, 0xFF696969, 0xFF1E90FF,
        0xFFB22222, 0xFFFFFAF0, 0xFF228B22, 0xFFFF00FF, 0xFFDCDCDC, 0xFFF8F8FF, 0xFFFFD700,
        0xFFDAA520, 0xFF808080, 0xFF00FF00, 0xFFADFF2F, 0xFFF0FFF0, 0xFFFF69B4, 0xFFCD5C5C,
        0xFF4B0082, 0xFFF0E68C, 0xFFE6E6FA, 0xFFFFF0F5, 0xFF7CFC00, 0xFFFFFACD, 0xFFADD8E6,
        0xFFF08080, 0xFFE0FFFF, 0xFFFAFAD2, 0xFFD3D3D3, 0xFF90EE90, 0xFFFFB6C1, 0xFFFFA07A,
        0xFF20B2AA, 0xFF87CEFA, 0xFF778899, 0xFFB0C4DE, 0xFFFFE4C4, 0xFF0000CD, 0xFFFFEBCD,
        0xFF0000FF, 0xFF8A2BE2, 0xFFA52A2A, 0xFFDEB887, 0xFF5F9EA0, 0xFF7FFF00, 0xFFD2691E,
        0xFFFF7F50, 0xFF6495ED, 0xFFFFF8DC, 0xFFDC143C, 0xFF00FFFF, 0xFF00008B, 0xFF008B8B,
        0xFFB8860B, 0xFFA9A9A9, 0xFF006400, 0xFFBDB76B, 0xFF8B008B, 0xFF556B2F, 0xFFFF8C00,
        0xFF9932CC, 0xFF8B0000, 0xFFE9967A, 0xFF8FBC8F, 0xFF483D8B, 0xFF2F4F4F, 0xFF00CED1,
        0xFFFF1493, 0xFF00BFFF, 0xFF696969, 0xFF1E90FF, 0xFFB22222, 0xFFFFFAF0, 0xFF228B22,
        0xFFFF00FF, 0xFFDCDCDC, 0xFFF8F8FF, 0xFFFFD700, 0xFFDAA520, 0xFF808080, 0xFF00FF00,
        0xFFADFF2F, 0xFFF0FFF0, 0xFFFF69B4, 0xFFCD5C5C, 0xFF4B0082, 0xFFF0E68C, 0xFFE6E6FA,
    ).map { it.toInt() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPage2PageHolder {
        val textView = TextView(parent.context)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f)
        textView.layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT,
        )
        return ViewPage2PageHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewPage2PageHolder, position: Int) {
        val colorInt = colors[holder.adapterPosition]
        holder.textView.text = String.format(null, "0x%X", colorInt)
        holder.textView.setBackgroundColor(colorInt)
    }

    override fun getItemCount(): Int {
        return colors.size
    }
}