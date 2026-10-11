package com.qa.overlaytest

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private val modes = linkedMapOf(
        "Full screen, opaque (alpha 1.0)" to OverlayService.MODE_FULL_OPAQUE,
        "Full screen, alpha 0.8" to OverlayService.MODE_FULL_80,
        "Semi-transparent (alpha 0.5)" to OverlayService.MODE_SEMI,
        "Partial, bottom 25% (covers buttons)" to OverlayService.MODE_PARTIAL,
        "Tiny 1x1 px" to OverlayService.MODE_TINY
    )

    private lateinit var group: RadioGroup
    private lateinit var passThrough: CheckBox
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        status = TextView(this).apply { textSize = 16f }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "1. Grant 'Display over other apps'"
            setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        })

        group = RadioGroup(this)
        modes.keys.forEachIndexed { i, label ->
            group.addView(RadioButton(this).apply {
                text = label
                id = i + 1000
            })
        }
        group.check(1000)
        root.addView(group)

        passThrough = CheckBox(this).apply {
            text = "Touch pass-through (taps reach the app underneath)"
            isChecked = true
        }
        root.addView(passThrough)

        root.addView(Button(this).apply {
            text = "2. Start overlay (5 sec delay)"
            setOnClickListener { startWithDelay() }
        })

        root.addView(Button(this).apply {
            text = "Stop overlay"
            setOnClickListener {
                startService(
                    Intent(this@MainActivity, OverlayService::class.java)
                        .setAction(OverlayService.ACTION_STOP)
                )
            }
        })

        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        status.text = if (Settings.canDrawOverlays(this))
            "Overlay permission: GRANTED" else "Overlay permission: NOT granted"
    }

    // The delay gives you time to switch to the banking app first.
    private fun startWithDelay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Grant overlay permission first", Toast.LENGTH_SHORT).show()
            return
        }
        val label = (group.findViewById<RadioButton>(group.checkedRadioButtonId)).text.toString()
        val mode = modes[label] ?: OverlayService.MODE_FULL_OPAQUE
        val intent = Intent(this, OverlayService::class.java)
            .putExtra(OverlayService.EXTRA_MODE, mode)
            .putExtra(OverlayService.EXTRA_PASS_THROUGH, passThrough.isChecked)

        Toast.makeText(this, "Overlay starts in 5 seconds", Toast.LENGTH_SHORT).show()
        window.decorView.postDelayed({ ContextCompat.startForegroundService(this, intent) }, 5000)
    }
}
