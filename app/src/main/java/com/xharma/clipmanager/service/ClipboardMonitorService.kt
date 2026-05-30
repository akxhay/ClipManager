package com.xharma.clipmanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.xharma.clipmanager.MainActivity
import com.xharma.clipmanager.data.ClipDatabase
import com.xharma.clipmanager.data.ClipEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ClipboardMonitorService : Service() {

    private lateinit var clipboardManager: ClipboardManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: ClipDatabase

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        // This listener might still fire in the background on some devices,
        // but it won't be able to read the content.
        updateClipboardContent()
    }

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        database = ClipDatabase.getDatabase(this)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipListener)

        createNotificationChannel()
        updateClipboardContent()

        startForegroundService()
    }

    private fun startForegroundService() {
        val notification = createNotification(_lastClipText.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REFRESH) {
            updateClipboardContent()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        _isRunning.value = false
        clipboardManager.removePrimaryClipChangedListener(clipListener)
        super.onDestroy()
    }

    private fun updateClipboardContent() {
        try {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (!text.isNullOrEmpty() && text != _lastClipText.value) {
                    _lastClipText.value = text
                    updateNotification(text)

                    // Save to Database (this will update the UI via Flow)
                    serviceScope.launch {
                        database.clipDao().insert(ClipEntry(text = text))
                    }

                    // Save to x_clipmanager.txt
                    if (_isAutoSaveEnabled.value) {
                        saveToXharmaFile(text)
                    }
                }
            }
        } catch (e: Exception) {
            // Android 10+ will throw SecurityException if reading clipboard in background
            Log.d("ClipMonitor", "Background clipboard read restricted: ${e.message}")
        }
    }

    private fun saveToXharmaFile(text: String) {
        val fileName = "x_clipmanager.txt"
        val file = File("/data/local/tmp", fileName)

        runCatching {
            file.writeText(text)
            Log.d("ClipMonitor", "File saved to: ${file.absolutePath}")
        }.onFailure {
            Log.e("ClipMonitor", "File save failed to /data/local/tmp: ${it.message}")
            Log.e("ClipMonitor", "TIP: If you get EACCES, run: adb shell \"touch ${file.absolutePath} && chmod 666 ${file.absolutePath}\"")
        }
    }

    private fun updateNotification(content: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(content))
    }

    private fun createNotification(content: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent =
            PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        // 🔥 IMPORTANT: This action now starts the Transparent Activity to bypass background restrictions
        val syncIntent = Intent(this, ClipboardSyncActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val syncPendingIntent = PendingIntent.getActivity(
            this, 1, syncIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cleanContent = if (content.length > 60) "${content.take(57)}..." else content

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ClipManager is monitoring")
            .setContentText(cleanContent)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setColor(0xFF6750A4.toInt())
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.stat_notify_sync, "Sync Now", syncPendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(content)
                    .setBigContentTitle("Last Captured Clip")
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Clipboard Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description =
                "Maintains a persistent notification while the clipboard is being monitored"
            setShowBadge(false)
        }
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "clipboard_monitor_channel"
        const val ACTION_REFRESH = "com.xharma.clipmanager.REFRESH"

        private val _lastClipText = MutableStateFlow("No content copied yet")
        val lastClipText = _lastClipText.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _isAutoSaveEnabled = MutableStateFlow(false)
        val isAutoSaveEnabled = _isAutoSaveEnabled.asStateFlow()

        fun setAutoSaveEnabled(enabled: Boolean) {
            _isAutoSaveEnabled.value = enabled
        }

        fun start(context: Context) {
            val intent = Intent(context, ClipboardMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(
                intent
            ) else context.startService(intent)
        }

        fun stop(context: Context) =
            context.stopService(Intent(context, ClipboardMonitorService::class.java))

        fun refresh(context: Context) = context.startService(
            Intent(
                context,
                ClipboardMonitorService::class.java
            ).apply { action = ACTION_REFRESH })
    }
}
