package com.example.systweaker

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1A1A22"))
            setPadding(48, 48, 48, 48)
        }

        val btn = Button(this).apply {
            text = "Open Radio Info (LTE only)"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#3D5AFE"))
            textSize = 18f
            setOnClickListener { openRadioInfo() }
        }

        layout.addView(
            btn,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(layout)
    }

    private fun openRadioInfo() {
        val targets = listOf(
            "com.android.phone" to "com.android.phone.settings.RadioInfo",
            "com.android.settings" to "com.android.settings.RadioInfo"
        )
        for ((pkg, cls) in targets) {
            try {
                startActivity(Intent().setClassName(pkg, cls))
                return
            } catch (e: Exception) { }
        }
        Toast.makeText(this, "Radio Info not found on this phone", Toast.LENGTH_LONG).show()
    }
}
