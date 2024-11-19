package com.sliver.sample.preference

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class RecSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.rec_settings_preferences, rootKey)
    }
}