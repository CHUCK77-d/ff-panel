package com.example.ffpanel

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 140, 64, 64)
        }

        box.addView(TextView(this).apply {
            text = "FF Panel"
            textSize = 26f
        })

        box.addView(TextView(this).apply {
            text = "1. Conceda a permissao de sobreposicao\n2. Inicie o painel\n3. Volte para o Free Fire"
            textSize = 14f
            setPadding(0, 24, 0, 40)
        })

        box.addView(Button(this).apply {
            text = "Conceder permissao"
            setOnClickListener { pedirPermissao() }
        })

        box.addView(Button(this).apply {
            text = "Iniciar painel"
            setOnClickListener { iniciar() }
        })

        box.addView(Button(this).apply {
            text = "Parar painel"
            setOnClickListener {
                stopService(Intent(this@MainActivity, PanelService::class.java))
            }
        })

        setContentView(box)
    }

    private fun pedirPermissao() {
        if (Settings.canDrawOverlays(this)) return
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun iniciar() {
        if (!Settings.canDrawOverlays(this)) {
            pedirPermissao()
            return
        }
        startService(Intent(this, PanelService::class.java))
    }
}
