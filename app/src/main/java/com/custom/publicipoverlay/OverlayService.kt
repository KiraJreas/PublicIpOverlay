package com.custom.publicipoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.concurrent.thread

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var ipTextView: TextView
    private var isRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val CHANNEL_ID = "overlay_ip_channel"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(1, buildNotification())

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 20
        params.y = 100

        ipTextView = TextView(this)
        ipTextView.textSize = 14f
        ipTextView.setPadding(10, 6, 10, 6)
        windowManager.addView(ipTextView, params)

        isRunning = true
        startIpRefreshLoop()
    }

    private fun startIpRefreshLoop() {
        thread {
            while (isRunning) {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("https://api.ipify.org")
                        .build()
                    val response = client.newCall(request).execute()
                    val ip = response.body?.string() ?: "获取失败"
                    val finalIp = ip
                    mainHandler.post {
                        ipTextView.text = "公网IP: $finalIp"
                    }
                } catch (_: Exception) {
                    mainHandler.post {
                        ipTextView.text = "公网IP: 网络错误"
                    }
                }
                Thread.sleep(60000)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "IP 悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("PublicIP Overlay")
                .setContentText("悬浮 IP 显示运行中")
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("PublicIP Overlay")
                .setContentText("悬浮 IP 显示运行中")
                .build()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (::ipTextView.isInitialized) {
            windowManager.removeView(ipTextView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
