package com.example.systweaker

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

private const val BG = 0xFF0C1222.toInt()
private const val CARD = 0xFF141B30.toInt()
private const val MUTED = 0xFF8C97B2.toInt()
private const val AMBER = 0xFFFFB020.toInt()
private const val CORAL = 0xFFFF5C7A.toInt()

class GaugeView(ctx: Context) : View(ctx) {
    var maxV = 3000f
    private var shown = 0f
    private var anim: ValueAnimator? = null
    private val rect = RectF()
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = 0xFF1B2440.toInt()
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MUTED; textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    fun setTarget(v: Float) {
        anim?.cancel()
        anim = ValueAnimator.ofFloat(shown, v).apply {
            duration = 700
            addUpdateListener { shown = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onMeasure(w: Int, h: Int) {
        val s = min(MeasureSpec.getSize(w), (340 * resources.displayMetrics.density).toInt())
        setMeasuredDimension(s, s)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val s = w.toFloat()
        val st = s * 0.075f
        track.strokeWidth = st
        arc.strokeWidth = st
        rect.set(st, st, s - st, s - st)
        val sh = SweepGradient(s / 2, s / 2, intArrayOf(AMBER, CORAL, CORAL), floatArrayOf(0f, 0.75f, 1f))
        val m = Matrix()
        m.postRotate(135f, s / 2, s / 2)
        sh.setLocalMatrix(m)
        arc.shader = sh
        big.textSize = s * 0.22f
        small.textSize = s * 0.058f
    }

    override fun onDraw(canvas: Canvas) {
        val s = width.toFloat()
        canvas.drawArc(rect, 135f, 270f, false, track)
        val frac = (shown / maxV).coerceIn(0f, 1f)
        if (frac > 0.005f) canvas.drawArc(rect, 135f, 270f * frac, false, arc)
        canvas.drawText("التيار الحالي", s / 2, s * 0.34f, small)
        canvas.drawText(String.format(Locale.US, "%.0f", shown), s / 2, s * 0.57f, big)
        canvas.drawText("mA", s / 2, s * 0.69f, small)
    }
}

class MainActivity : Activity() {
    private lateinit var bm: BatteryManager
    private val h = Handler(Looper.getMainLooper())
    private var maxMa = 0.0
    private var sumMa = 0.0
    private var n = 0

    private lateinit var gauge: GaugeView
    private lateinit var chipPlug: TextView
    private lateinit var chipStatus: TextView
    private lateinit var tVolt: TextView
    private lateinit var tWatt: TextView
    private lateinit var tTemp: TextView
    private lateinit var tBat: TextView
    private lateinit var tMax: TextView
    private lateinit var tAvg: TextView

    private val tick = object : Runnable {
        override fun run() {
            update()
            h.postDelayed(this, 1000)
        }
    }

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()
    private fun f(fmt: String, vararg a: Any) = String.format(Locale.US, fmt, *a)

    private fun round(color: Int, r: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(r).toFloat()
    }

    private fun chip(): TextView = TextView(this).apply {
        textSize = 13f
        setTextColor(Color.WHITE)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setPadding(dp(14), dp(7), dp(14), dp(7))
        background = round(CARD, 20)
    }

    private fun card(title: String): Pair<View, TextView> {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(16))
            background = round(CARD, 18)
        }
        box.addView(TextView(this).apply {
            text = title; textSize = 12f; setTextColor(MUTED)
        })
        val v = TextView(this).apply {
            text = "--"; textSize = 24f; setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, dp(4), 0, 0)
        }
        box.addView(v)
        return box to v
    }

    private fun row(a: View, b: View): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(a, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            .apply { setMargins(0, 0, dp(6), 0) })
        addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            .apply { setMargins(dp(6), 0, 0, 0) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setBackgroundDrawable(ColorDrawable(BG))
        window.decorView.systemUiVisibility = 0
        window.statusBarColor = BG
        window.navigationBarColor = BG
        bm = getSystemService(BATTERY_SERVICE) as BatteryManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(BG)
            clipToPadding = false
            addView(root)
            setOnApplyWindowInsetsListener { v, ins ->
                v.setPadding(0, ins.systemWindowInsetTop, 0, ins.systemWindowInsetBottom)
                ins
            }
        }
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "AmpMeter"; textSize = 26f; setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        })
        root.addView(TextView(this).apply {
            text = "قياس تيار الشحن لحظياً"; textSize = 14f; setTextColor(MUTED)
            setPadding(0, dp(2), 0, 0)
        })

        gauge = GaugeView(this)
        root.addView(gauge, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(20) })

        chipPlug = chip()
        chipStatus = chip()
        val chips = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(chipPlug)
            addView(chipStatus, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(8) })
        }
        root.addView(chips, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(4); bottomMargin = dp(8) })

        val (c1, v1) = card("الجهد"); tVolt = v1
        val (c2, v2) = card("القدرة"); tWatt = v2
        val (c3, v3) = card("الحرارة"); tTemp = v3
        val (c4, v4) = card("البطارية"); tBat = v4
        val (c5, v5) = card("أعلى تيار"); tMax = v5
        val (c6, v6) = card("متوسط التيار"); tAvg = v6

        val lp = { LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) } }
        root.addView(row(c1, c2), lp())
        root.addView(row(c3, c4), lp())
        root.addView(row(c5, c6), lp())

        root.addView(TextView(this).apply {
            text = "إعادة ضبط الإحصاءات"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(AMBER)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(dp(1), AMBER)
                cornerRadius = dp(16).toFloat()
            }
            setOnClickListener { maxMa = 0.0; sumMa = 0.0; n = 0 }
        }, lp().apply { topMargin = dp(20) })
                val ver = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0"
        }
        val about = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(18), dp(16), dp(18))
            background = round(CARD, 18)
        }
        about.addView(TextView(this).apply {
            text = "تطوير: نورالدين المراكشي"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        })
        about.addView(TextView(this).apply {
            text = "الإصدار $ver"
            textSize = 13f
            setTextColor(MUTED)
            setPadding(0, dp(4), 0, dp(8))
        })
        about.addView(TextView(this).apply {
            text = "صفحتي على فيسبوك"
            textSize = 14f
            setTextColor(AMBER)
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.facebook.com/profile.php?id=100069144503754")))
            }
        })
        root.addView(about, lp().apply { topMargin = dp(20) })
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

        gauge.maxV = when {
            maxMa > 3000 -> 6000f
            maxMa > 1500 -> 3000f
            else -> 2000f
        }
        gauge.setTarget(ma.toFloat())

        chipPlug.text = when (plug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "شاحن AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "لاسلكي"
            else -> "غير موصول"
        }
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING
        chipStatus.text = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "يشحن"
            BatteryManager.BATTERY_STATUS_FULL -> "ممتلئ"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "يفرغ"
            else -> "-"
        }
        chipStatus.setTextColor(if (charging) AMBER else Color.WHITE)

        tVolt.text = f("%.2f V", mv / 1000.0)
        tWatt.text = f("%.1f W", watts)
        tTemp.text = f("%.1f °C", temp)
        tBat.text = "$level%"
        tMax.text = f("%.0f mA", maxMa)
        tAvg.text = f("%.0f mA", avg)
    }
}
