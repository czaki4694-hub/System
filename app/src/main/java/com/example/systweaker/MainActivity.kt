package com.example.systweaker

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val serversUrl =
        "https://raw.githubusercontent.com/czaki4694-hub/System/main/servers.json"

    private var connected = false
    private var selected = 0
    private val names = ArrayList<String>()
    private val flags = ArrayList<String>()

    private lateinit var statusText: TextView
    private lateinit var connectBtn: TextView
    private lateinit var stateText: TextView
    private lateinit var listBox: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun shape(color: String, radius: Int, stroke: String? = null) =
        GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(2), Color.parseColor(stroke))
        }

    private fun isMarocTelecom(): Boolean {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.networkOperator == "60401" || tm.simOperator == "60401"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F1117"))
            setPadding(dp(20), dp(48), dp(20), dp(20))
        }

        root.addView(TextView(this).apply {
            text = "MA VPN"
            textSize = 26f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })

        statusText = TextView(this).apply {
            textSize = 14f
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        root.addView(statusText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) })

        connectBtn = TextView(this).apply {
            text = "ON"
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setOnClickListener { toggle() }
        }
        root.addView(connectBtn, LinearLayout.LayoutParams(dp(170), dp(170)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(36)
        })

        stateText = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#9AA0B4"))
        }
        root.addView(stateText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16); bottomMargin = dp(24) })

        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(listBox) }
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)
        refreshUi()
        loadServers()
    }

    private fun refreshUi() {
        val ok = isMarocTelecom()
        statusText.text = if (ok) "Maroc Telecom detected" else "Maroc Telecom only"
        statusText.setTextColor(Color.parseColor(if (ok) "#3DDC97" else "#FF6B6B"))
        statusText.background = shape(if (ok) "#16301F" else "#3A1A1F", 20)

        val color = if (connected) "#22C55E" else "#3D5AFE"
        connectBtn.background = shape(color, 85)
        connectBtn.text = if (connected) "OFF" else "ON"
        stateText.text = if (connected) "Connected" else "Not connected"
    }

    private fun toggle() {
        if (!isMarocTelecom()) {
            Toast.makeText(this, "Only works on Maroc Telecom", Toast.LENGTH_LONG).show()
            return
        }
        if (names.isEmpty()) {
            Toast.makeText(this, "No servers loaded", Toast.LENGTH_SHORT).show()
            return
        }
        connected = !connected // phase 1: UI only, real tunnel comes in phase 2
        refreshUi()
    }

    private fun loadServers() {
        stateText.text = "Loading servers..."
        thread {
            try {
                val c = URL(serversUrl).openConnection() as HttpURLConnection
                c.connectTimeout = 8000
                c.readTimeout = 8000
                val txt = c.inputStream.bufferedReader().readText()
                val arr = JSONArray(txt)
                val n = ArrayList<String>()
                val f = ArrayList<String>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    n.add(o.getString("name"))
                    f.add(o.optString("flag", ""))
                }
                runOnUiThread {
                    names.clear(); names.addAll(n)
                    flags.clear(); flags.addAll(f)
                    renderList(); refreshUi()
                }
            } catch (e: Exception) {
                runOnUiThread { stateText.text = "Failed to load servers" }
            }
        }
    }

    private fun renderList() {
        listBox.removeAllViews()
        for (i in names.indices) {
            val row = TextView(this).apply {
                text = "${flags[i]}  ${names[i]}"
                textSize = 17f
                setTextColor(Color.WHITE)
                setPadding(dp(18), dp(16), dp(18), dp(16))
                background = if (i == selected) shape("#1B2140", 14, "#3D5AFE")
                else shape("#171A23", 14)
                setOnClickListener { selected = i; renderList() }
            }
            listBox.addView(row, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) })
        }
    }
}
