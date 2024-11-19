package com.sliver.sample.preference

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val sp = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val recSizePreference = findPreference<Preference>("rec_resize_settings")!!
        recSizePreference.summaryProvider = SummaryProvider<Preference> {
            val recResizeHeight = sp.getString("rec_resize_height", null)
            val recResizeWidth = sp.getString("rec_resize_width", null)
            "按批次中最大的图片宽高比缩放至高度$recResizeHeight，填充宽度至$recResizeWidth"
        }
    }
}