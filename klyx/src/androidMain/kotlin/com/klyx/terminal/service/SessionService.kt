package com.klyx.terminal.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.klyx.R.drawable
import com.klyx.activities.MainActivity
import com.klyx.core.app.UnsafeGlobalAccess
import com.klyx.core.event.EventBus
import com.klyx.core.util.value
import com.klyx.resources.Res
import com.klyx.resources.app_name
import com.klyx.terminal.SessionManager
import com.klyx.terminal.event.NewSessionEvent
import com.klyx.terminal.event.SessionTerminateEvent
import com.klyx.terminal.event.TerminateAllSessionEvent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SessionService : Service() {
    private var daemonRunning = false

    inner class SessionBinder : Binder() {
        val service get() = this@SessionService
    }

    private val binder = SessionBinder()
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?) = binder

    @OptIn(DelicateCoroutinesApi::class)
    override fun onDestroy() {
        GlobalScope.launch { SessionManager.terminateAll() }
        daemonRunning = false
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        EventBus.INSTANCE.subscribe<NewSessionEvent> { updateNotification() }
        EventBus.INSTANCE.subscribe<SessionTerminateEvent> {
            if (SessionManager.sessions.isEmpty()) {
                stopSelf()
                if (daemonRunning) {
                    daemonRunning = false
                }
            } else {
                updateNotification()
            }
        }

        createNotificationChannel()
        val notification = createNotification()
        startForeground(1, notification)

        if (!daemonRunning) {
            daemonRunning = true
        }

        if (wakeLock == null) {
            wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SessionService::WakeLock"
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class, UnsafeGlobalAccess::class)
    @SuppressLint("WakelockTimeout", "Wakelock")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_EXIT -> {
                GlobalScope.launch {
                    SessionManager.terminateAll()
                    if (daemonRunning) {
                        daemonRunning = false
                    }
                    stopSelf()
                    EventBus.INSTANCE.post(TerminateAllSessionEvent)
                }
            }

            ACTION_WAKE_LOCK -> {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                } else {
                    wakeLock?.acquire()
                }
                updateNotification()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_NOTIFICATION_TAP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val exitIntent = Intent(this, SessionService::class.java).apply { action = ACTION_EXIT }
        val wakeLockIntent = Intent(this, SessionService::class.java).apply { action = ACTION_WAKE_LOCK }

        val exitPendingIntent = PendingIntent.getService(
            this, 1, exitIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val wakelockPendingIntent = PendingIntent.getService(
            this, 1, wakeLockIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("${Res.string.app_name.value} Terminal")
            .setContentText(getNotificationContentText(wakeLock?.isHeld == true))
            .setSmallIcon(drawable.terminal)
            .setContentIntent(pendingIntent)
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    "Exit",
                    exitPendingIntent
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    if (wakeLock?.isHeld == true) {
                        "Release Wake Lock"
                    } else {
                        "Acquire Wake Lock"
                    },
                    wakelockPendingIntent
                ).build()
            )
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Session Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification for Terminal Service"
        }
        notificationManager.createNotificationChannel(channel)
    }

    @JvmSynthetic
    private fun updateNotification() {
        runCatching {
            val notification = createNotification()
            notificationManager.notify(1, notification)
        }.onFailure { it.printStackTrace() }
    }

    private fun getNotificationContentText(wakelock: Boolean): String {
        val count = SessionManager.sessions.size
        val s = if (count > 1) "sessions" else "session"
        val wakeHeld = if (wakelock) " (wake lock held)" else ""
        return "$count $s running$wakeHeld"
    }

    companion object {
        private const val CHANNEL_ID = "session_service_channel"

        private const val ACTION_EXIT = "com.klyx.terminal.ACTION_EXIT"
        private const val ACTION_WAKE_LOCK = "com.klyx.terminal.ACTION_WAKE_LOCK"

        const val ACTION_NOTIFICATION_TAP = "com.klyx.terminal.ACTION_NOTIFICATION_TAP"
    }
}
