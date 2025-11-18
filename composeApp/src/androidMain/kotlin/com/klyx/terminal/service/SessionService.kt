package com.klyx.terminal.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import com.klyx.R.drawable
import com.klyx.activities.TerminalActivity
import com.klyx.core.value
import com.klyx.res.Res
import com.klyx.res.app_name
import com.klyx.terminal.TerminalSessionId
import com.klyx.core.terminal.currentUser
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

class SessionService : Service() {
    private val sessions = hashMapOf<TerminalSessionId, TerminalSession>()
    private var daemonRunning = false

    val sessionList = mutableStateListOf<TerminalSessionId>()
    var currentSession by mutableStateOf("main")

    inner class SessionBinder : Binder() {
        val service get() = this@SessionService

        fun createSession(
            id: TerminalSessionId,
            client: TerminalSessionClient,
            activity: TerminalActivity,
            userName: String? = currentUser
        ): TerminalSession {
            requireNotNull(userName) { "User name cannot be null" }

            return com.klyx.terminal.createSession(
                user = userName,
                client = client,
                activity = activity,
                sessionId = id
            ).also {
                sessions[id] = it
                sessionList.add(id)
                updateNotification()
            }
        }

        fun getSession(id: TerminalSessionId) = sessions[id]

        fun terminateSession(id: TerminalSessionId) {
            sessions[id]?.apply {
                if (emulator != null) {
                    finishIfRunning()
                }
            }

            sessions.remove(id)
            sessionList.remove(id)

            if (sessions.isEmpty()) {
                stopSelf()
                if (daemonRunning) {
                    daemonRunning = false
                }
            } else {
                updateNotification()
            }
        }
    }

    private val binder = SessionBinder()
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?) = binder

    override fun onDestroy() {
        sessions.values.forEach { it.finishIfRunning() }
        daemonRunning = false
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }

    override fun onCreate() {
        super.onCreate()
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_EXIT -> {
                sessions.values.forEach { it.finishIfRunning() }
                if (daemonRunning) {
                    daemonRunning = false
                }
                stopSelf()
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
        val intent = Intent(this, TerminalActivity::class.java)
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

    private fun updateNotification() {
        runCatching {
            val notification = createNotification()
            notificationManager.notify(1, notification)
        }.onFailure { it.printStackTrace() }
    }

    private fun getNotificationContentText(wakelock: Boolean): String {
        val count = sessions.size
        val s = if (count > 1) "sessions" else "session"
        val wakeHeld = if (wakelock) " (wake lock held)" else ""
        return "$count $s running$wakeHeld"
    }

    companion object {
        private const val CHANNEL_ID = "session_service_channel"

        private const val ACTION_EXIT = "com.klyx.terminal.ACTION_EXIT"
        private const val ACTION_WAKE_LOCK = "com.klyx.terminal.ACTION_WAKE_LOCK"
    }
}
