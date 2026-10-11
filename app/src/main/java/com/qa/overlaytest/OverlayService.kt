package com.qa.overlaytest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            removeOverlay()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()

        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_FULL_OPAQUE
        val passThrough = intent?.getBooleanExtra(EXTRA_PASS_THROUGH, true) ?: true
        showOverlay(mode, passThrough)
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val channelId = "overlay_tester"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Overlay tester", NotificationManager.IMPORTANCE_LOW)
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("QA overlay is active")
            .setContentText("Open the app and tap Stop to remove it")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .build()

        val type = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0
        ServiceCompat.startForeground(this, 1, notification, type)
    }

    private fun showOverlay(mode: String, passThrough: Boolean) {
        removeOverlay()

        // Clearly labelled so nobody mistakes it for a real system screen.
        val view = TextView(this).apply {
            text = "QA TEST OVERLAY\n\nSystem update in progress (simulated)\nDo not turn off your device"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1565C0"))
        }

        var width = WindowManager.LayoutParams.MATCH_PARENT
        var height = WindowManager.LayoutParams.MATCH_PARENT
        var gravity = Gravity.TOP
        var alpha = 1.0f

        when (mode) {
            MODE_FULL_OPAQUE -> alpha = 1.0f
            // Android 12+ only lets touches pass through overlays with opacity <= 0.8
            MODE_FULL_80 -> alpha = 0.8f
            MODE_SEMI -> alpha = 0.5f
            MODE_PARTIAL -> {
                height = (resources.displayMetrics.heightPixels * 0.25).toInt()
                gravity = Gravity.BOTTOM
            }
            MODE_TINY -> {
                width = 1
                height = 1
            }
        }

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (passThrough) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

        val params = WindowManager.LayoutParams(
            width, height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            this.alpha = alpha
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager?.addView(view, params)
        overlayView = view
    }

    private fun removeOverlay() {
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.qa.overlaytest.STOP"
        const val EXTRA_MODE = "mode"
        const val EXTRA_PASS_THROUGH = "pass_through"

        const val MODE_FULL_OPAQUE = "full_opaque"
        const val MODE_FULL_80 = "full_80"
        const val MODE_SEMI = "semi_50"
        const val MODE_PARTIAL = "partial_bottom"
        const val MODE_TINY = "tiny_1px"
    }
}
