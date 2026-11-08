override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val layout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.parseColor("#1A1A22"))
        setPadding(48, 48, 48, 48)
    }

    val btn = Button(this).apply {
        text = "فتح Radio Info (LTE only)"
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
import android.graphics.Color
import android.view.Gravity
