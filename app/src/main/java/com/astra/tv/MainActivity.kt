package com.astra.tv

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.BitmapDrawable
import android.widget.FrameLayout
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var appGrid: GridLayout

    private val clockTick = object : Runnable {
        override fun run() {
            val now = Date()
            clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            date.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
            handler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        buildScreen()
        loadApps()
        handler.post(clockTick)
    }

    private fun buildScreen() {
        val root = FrameLayout(this)

        // Static bundled wallpaper: decoded once when the home screen is created.
        val wallpaper = ImageView(this).apply {
            setImageResource(R.drawable.wallpaper)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Обои Astra TV"
        }
        root.addView(wallpaper, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Lightweight dark overlay keeps clock and app labels readable.
        val scrim = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x66050B18, 0x22050B18, 0x99050B18.toInt())
            )
        }
        root.addView(scrim, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(54), dp(30), dp(54), dp(24))
            setBackgroundColor(Color.TRANSPARENT)
        }
        root.addView(content, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val brand = TextView(this).apply {
            text = "ASTRA"
            textSize = 18f
            letterSpacing = 0.28f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        top.addView(brand, LinearLayout.LayoutParams(0, dp(44), 1f))

        val timeColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        clock = TextView(this).apply {
            text = "--:--"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        date = TextView(this).apply {
            text = ""
            textSize = 12f
            setTextColor(0xFFB9C6D8.toInt())
        }
        timeColumn.addView(clock)
        timeColumn.addView(date)
        top.addView(timeColumn)
        content.addView(top)

        val heading = TextView(this).apply {
            text = "Приложения"
            textSize = 25f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, dp(66), 0, dp(22))
        }
        content.addView(heading)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        appGrid = GridLayout(this).apply {
            columnCount = 5
            useDefaultMargins = false
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        scroll.addView(appGrid)
        content.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val footer = TextView(this).apply {
            text = "Выберите приложение с помощью пульта"
            textSize = 12f
            setTextColor(0xFF8796AA.toInt())
            setPadding(0, dp(14), 0, 0)
        }
        content.addView(footer)
        setContentView(root)
    }

    @Suppress("DEPRECATION")
    private fun loadApps() {
        appGrid.removeAllViews()
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
            .filter { it.packageName != packageName && pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase(Locale.getDefault()) }

        for (info in apps) {
            appGrid.addView(createAppTile(info))
        }

        if (apps.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Не удалось найти приложения"
                textSize = 18f
                setTextColor(Color.WHITE)
            }
            appGrid.addView(empty)
        } else {
            appGrid.getChildAt(0)?.requestFocus()
        }
    }

    private fun createAppTile(info: ApplicationInfo): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            background = roundedBackground(0x22FFFFFF, dp(20))
            setPadding(dp(8), dp(14), dp(8), dp(12))
            scaleX = 1f
            scaleY = 1f
            setOnFocusChangeListener { view, focused ->
                view.animate().scaleX(if (focused) 1.08f else 1f)
                    .scaleY(if (focused) 1.08f else 1f)
                    .setDuration(130).start()
                view.background = roundedBackground(
                    if (focused) 0x55FFFFFF else 0x22FFFFFF,
                    dp(20)
                )
            }
            setOnClickListener {
                val launch = packageManager.getLaunchIntentForPackage(info.packageName)
                if (launch != null) startActivity(launch)
            }
        }

        val icon = ImageView(this).apply {
            setImageDrawable(packageManager.getApplicationIcon(info))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        tile.addView(icon, LinearLayout.LayoutParams(dp(62), dp(62)))

        val label = TextView(this).apply {
            text = packageManager.getApplicationLabel(info).toString()
            textSize = 13f
            maxLines = 1
            gravity = Gravity.CENTER
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(Color.WHITE)
            setPadding(0, dp(10), 0, 0)
        }
        tile.addView(label, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(30)
        ))

        val params = GridLayout.LayoutParams().apply {
            width = 0
            height = dp(132)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(dp(7), dp(7), dp(7), dp(7))
        }
        tile.layoutParams = params
        return tile
    }

    private fun roundedBackground(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            setStroke(dp(1), 0x30FFFFFF)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(clockTick)
        handler.post(clockTick)
    }

    override fun onPause() {
        handler.removeCallbacks(clockTick)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockTick)
        super.onDestroy()
    }
}
