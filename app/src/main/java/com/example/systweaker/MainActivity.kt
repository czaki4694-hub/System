package com.example.systweaker

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class MainActivity : Activity() {
    private lateinit var bm: BatteryManager
    private lateinit var tv: TextView
    private val h = Handler(Looper.getMainLooper())
    private var maxMa = 0.0
    private var sumMa = 0.0
    private var n = 0

    private val tick = object : Runnable {
        override fun run() {
            update()
            h.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        tv = TextView(this).apply {
            textSize = 22f
            setPadding(48, 120, 48, 48)
        }
        val reset = Button(this).apply {
            text = "إعادة ضبط الإحصاءات"
            setOnClickListener { maxMa = 0.0; sumMa = 0.0; n = 0 }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(tv)
            addView(reset)
        })
    }

    override fun onResume() { super.onResume(); h.post(tick) }
    override fun onPause() { super.onPause(); h.removeCallbacks(tick) }

    private fun update() {
        val i: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val mv = i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val temp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val plug = i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, 0) ?: 0

        val raw = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val ma = abs(if (abs(raw) > 20000) raw / 1000.0 else raw.toDouble())
        val watts = (mv / 1000.0) * (ma / 1000.0)

        if (plug != 0) {
            if (ma > maxMa) maxMa = ma
            sumMa += ma
            n++
        }
        val avg = if (n > 0) sumMa / n else 0.0
        val plugName = when (plug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "شاحن (AC)"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "لاسلكي"
            else -> "غير موصول"
        }
        val st = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "يشحن"
            BatteryManager.BATTERY_STATUS_FULL -> "ممتلئ"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "يفرغ"
            else -> "-"
        }
        tv.text = "التيار الآن: %.0f mA (%.2f A)\nالجهد: %.2f V\nالقدرة: %.1f W\n\n".format(ma, ma / 1000, mv / 1000.0, watts) +
            "الأعلى: %.0f mA\nالمتوسط: %.0f mA\n\n".format(maxMa, avg) +
            "المصدر: $plugName\nالحالة: $st\nالبطارية: $level%%\nالحرارة: %.1f °C".format(temp)
    }
}
