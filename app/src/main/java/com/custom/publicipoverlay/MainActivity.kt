package com.custom.publicipoverlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var tvIp: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnStartOverlay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvIp = findViewById(R.id.tvIp)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnStartOverlay = findViewById(R.id.btnStartOverlay)

        // 申请悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }

        btnRefresh.setOnClickListener {
            fetchPublicIp()
        }

        btnStartOverlay.setOnClickListener {
            val serviceIntent = Intent(this, OverlayService::class.java)
            startForegroundService(serviceIntent)
        }

        fetchPublicIp()
    }

    private fun fetchPublicIp() {
        thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.ipify.org")
                    .build()
                val response = client.newCall(request).execute()
                val ip = response.body?.string() ?: "获取失败"
                runOnUiThread {
                    tvIp.text = "公网IP：$ip"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvIp.text = "公网IP：网络错误"
                }
            }
        }
    }
}
