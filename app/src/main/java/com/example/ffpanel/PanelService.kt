package com.example.ffpanel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.roundToInt

class PanelService : Service() {

    private val green = Color.parseColor("#00E08A")
    private val ink = Color.parseColor("#06210F")
    private val dark = Color.parseColor("#12161D")
    private val dark2 = Color.parseColor("#1A1F28")
    private val grey = Color.parseColor("#7B8794")

    private lateinit var wm: WindowManager
    private lateinit var prefs: SharedPreferences
    private lateinit var features: Features

    private var button: TextView? = null
    private var panel: LinearLayout? = null
    private var status: TextView? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences("panel", Context.MODE_PRIVATE)
        features = Features(this)
        features.onStatus = { msg -> status?.post { status?.text = msg } }
        startForeground(1, notificacao())
        criarBotao()
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY

    override fun onBind(i: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        panel?.let { runCatching { wm.removeView(it) } }
        button?.let { runCatching { wm.removeView(it) } }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    private fun params() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun circulo(cor: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(cor)
        setStroke(dp(2), green)
    }

    private fun arredondado(cor: Int) = GradientDrawable().apply {
        setColor(cor)
        cornerRadius = dp(9).toFloat()
    }

    private fun margem(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(6) }

    private fun arrastavel(
        alca: View,
        alvo: View,
        p: WindowManager.LayoutParams,
        chave: String,
        onTap: () -> Unit
    ) {
        var sx = 0
        var sy = 0
        var tx = 0f
        var ty = 0f
        var moveu = false

        alca.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    sx = p.x
                    sy = p.y
                    tx = e.rawX
                    ty = e.rawY
                    moveu = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx).toInt()
                    val dy = (e.rawY - ty).toInt()
                    if (abs(dx) > 12 || abs(dy) > 12) moveu = true
                    if (moveu) {
                        p.x = sx + dx
                        p.y = sy + dy
                        wm.updateViewLayout(alvo, p)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (moveu) {
                        prefs.edit()
                            .putInt(chave + "x", p.x)
                            .putInt(chave + "y", p.y)
                            .apply()
                    } else {
                        onTap()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun criarBotao() {
        val p = params().apply {
            width = dp(56)
            height = dp(56)
            x = prefs.getInt("bx", 40)
            y = prefs.getInt("by", 300)
        }

        val v = TextView(this).apply {
            text = "FF"
            setTextColor(green)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = circulo(dark)
        }

        arrastavel(v, v, p, "b") { alternarPainel() }
        wm.addView(v, p)
        button = v
    }

    private fun alternarPainel() {
        panel?.let {
            runCatching { wm.removeView(it) }
            panel = null
            return
        }

        val p = params().apply {
            width = dp(250)
            x = prefs.getInt("px", 110)
            y = prefs.getInt("py", 300)
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
            setPadding(dp(12), dp(8), dp(12), dp(12))
        }

        val cabecalho = TextView(this).apply {
            text = "FF PANEL"
            setTextColor(green)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(6), 0, dp(10))
        }
        box.addView(cabecalho)

        box.addView(interruptor("ESP NAME") { features.espName = it })
        box.addView(interruptor("NO RECOIL 50%") { features.noRecoil = it })
        box.addView(interruptor("AIMBOT LEGIT") { features.aimbot = it })
        box.addView(slider("FOV", 40, 400, features.fov) { features.fov = it })
        box.addView(slider("Smooth", 1, 15, features.smooth) { features.smooth = it })

        status = TextView(this).apply {
            text = "aguardando Shizuku"
            setTextColor(grey)
            textSize = 9f
            setPadding(0, dp(10), 0, 0)
        }
        box.addView(status)

        arrastavel(cabecalho, box, p, "p") { }
        wm.addView(box, p)
        panel = box
    }

    private fun interruptor(rotulo: String, onChange: (Boolean) -> Unit): TextView {
        var ligado = false
        return TextView(this).apply {
            text = rotulo
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(grey)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = arredondado(dark2)
            layoutParams = margem()
            setOnClickListener {
                ligado = !ligado
                setTextColor(if (ligado) ink else grey)
                background = arredondado(if (ligado) green else dark2)
                onChange(ligado)
                features.sync()
            }
        }
    }

    private fun slider(
        rotulo: String,
        min: Int,
        max: Int,
        valor: Int,
        onChange: (Int) -> Unit
    ): LinearLayout {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, 0)
        }

        wrap.addView(TextView(this).apply {
            text = rotulo
            textSize = 10f
            setTextColor(grey)
        })

        wrap.addView(SeekBar(this).apply {
            this.max = max - min
            progress = valor - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, pr: Int, u: Boolean) {
                    onChange(pr + min)
                    features.sync()
                }

                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })

        return wrap
    }

    private fun notificacao(): Notification {
        val canal = "ffpanel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val c = NotificationChannel(
                canal, "FF Panel", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(c)
        }
        return Notification.Builder(this, canal)
            .setContentTitle("FF Panel ativo")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }
}
