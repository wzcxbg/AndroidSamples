package com.sliver.samples.floatingwindow.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object FloatingWindowUtils {

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun requestPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }
}