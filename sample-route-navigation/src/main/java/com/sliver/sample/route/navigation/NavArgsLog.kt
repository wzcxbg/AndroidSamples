package com.sliver.sample.route.navigation

import android.app.Activity
import android.util.Log
import androidx.fragment.app.Fragment


fun Activity.printNavArgs() {
    val bundle = intent.extras
    for (key in bundle?.keySet() ?: emptySet()) {
        val arg = bundle?.get(key)
        Log.e(this.javaClass.simpleName, "key:$key  val:$arg  ${arg?.javaClass}")
    }
}

fun Fragment.printNavArgs() {
    val bundle = arguments
    for (key in bundle?.keySet() ?: emptySet()) {
        val arg = bundle?.get(key)
        Log.e(this.javaClass.simpleName, "key:$key  val:$arg  ${arg?.javaClass}")
    }
}