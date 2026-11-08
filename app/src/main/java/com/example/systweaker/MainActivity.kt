package com.example.systweaker

import android.app.Activity
import android.os.Bundle
import android.widget.*
import com.topjohnwu.superuser.Shell

class MainActivity : Activity() {

    private val statusIcons = listOf(
        "bluetooth", "wifi", "mobile", "volume", "alarm_clock",
        "rotate", "hotspot", "cast", "nfc", "airplane", "location"
    )

    private fun sh(cmd: String): String {
        val r = Shell.cmd(cmd).exec()
        return (r.out + r.err).joinToString("\n")
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
        }
        setContentView(ScrollView(this).apply { addView(root) })

        fun title(t: String) = root.addView(TextView(this).apply {
            text = t; textSize = 18f; setPadding(0, 40, 0, 10)
        })

        Shell.getShell()
        val hasRoot = Shell.isAppGrantedRoot() == true
        root.addView(TextView(this).apply {
            text = if (hasRoot) "Root: OK" else "Root: غير ممنوح"
        })

        title("إخفاء أيقونات شريط الحالة")
        val boxes = statusIcons.map { icon ->
            CheckBox(this).apply { text = icon; root.addView(this) }
        }
        val current = sh("settings get secure icon_blacklist").trim()
            .split(",").filter { it.isNotEmpty() && it != "null" }.toSet()
        boxes.forEachIndexed { i, b -> b.isChecked = statusIcons[i] in current }

        root.addView(Button(this).apply {
            text = "تطبيق الأيقونات"
            setOnClickListener {
                val list = boxes.indices.filter { boxes[it].isChecked }
                    .joinToString(",") { statusIcons[it] }
                if (list.isEmpty()) sh("settings delete secure icon_blacklist")
                else sh("settings put secure icon_blacklist $list")
                toast("تم")
            }
        })

        title("خيارات سريعة")
        fun switch(label: String, onCmd: String, offCmd: String) {
            root.addView(Switch(this).apply {
                text = label
                setOnCheckedChangeListener { _, on -> sh(if (on) onCmd else offCmd) }
            })
        }
        switch("نسبة البطارية في شريط الحالة",
            "settings put system status_bar_show_battery_percent 1",
            "settings put system status_bar_show_battery_percent 0")
        switch("الثواني في الساعة",
            "settings put secure clock_seconds 1",
            "settings put secure clock_seconds 0")

        title("Overlay: ارتفاع شريط الحالة (dp)")
        val h = EditText(this).apply { hint = "مثال: 24"; setText("24") }
        root.addView(h)
        root.addView(Button(this).apply {
            text = "تطبيق الارتفاع"
            setOnClickListener {
                val px = (h.text.toString().toIntOrNull() ?: 24) * resources.displayMetrics.density
                sh("cmd overlay fabricate --target android --name sttweak " +
                   "android:dimen/status_bar_height 0x5=${px.toInt()}px")
                sh("cmd overlay enable --user current com.android.shell:sttweak")
                toast("تم (قد يحتاج إعادة تشغيل SystemUI)")
            }
        })
        root.addView(Button(this).apply {
            text = "إزالة الـ Overlay"
            setOnClickListener {
                sh("cmd overlay disable --user current com.android.shell:sttweak")
                toast("تمت الإزالة")
            }
        })

        root.addView(Button(this).apply {
            text = "إعادة تشغيل SystemUI"
            setOnClickListener { sh("killall com.android.systemui") }
        })

        title("أمر مخصص (root)")
        val cmd = EditText(this).apply { hint = "settings list secure" }
        val out = TextView(this).apply { setTextIsSelectable(true) }
        root.addView(cmd)
        root.addView(Button(this).apply {
            text = "تنفيذ"
            setOnClickListener { out.text = sh(cmd.text.toString()) }
        })
        root.addView(out)
    }
}
