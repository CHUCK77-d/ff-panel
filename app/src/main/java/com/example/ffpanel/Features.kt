package com.example.ffpanel

import android.content.Context
import android.util.Log

class Features(private val ctx: Context) {

    var espName = false
    var noRecoil = false
    var aimbot = false
    var fov = 120
    var smooth = 4

    var onStatus: ((String) -> Unit)? = null

    fun sync() {
        val linha = "esp=$espName recoil=$noRecoil aim=$aimbot fov=$fov smooth=$smooth"
        Log.d("FFPanel", linha)
        onStatus?.invoke(linha)
    }
}
