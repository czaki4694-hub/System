package com.example.systweaker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        val btn = Button(this).apply {
            text = "4G Only"
            setOnClickListener { openRadioInfo() }
        }
        layout.addView(btn)
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
        Toast.makeText(this, "Radio Info ما كاينش فهاد التيليفون", Toast.LENGTH_LONG).show()
    }
}
