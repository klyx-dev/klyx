package com.klyx.platform.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.klyx.MainActivity
import com.klyx.R
import com.klyx.core.event.subscribeIn
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import com.klyx.data.terminal.TerminalSessionManager
import com.klyx.event.GlobalEventBus
import com.klyx.event.terminal.NewSessionEvent
import com.klyx.event.terminal.SessionTerminateEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class TerminalService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    @OptIn(UnsafeGlobalAccess::class)
    private val sessionManager: TerminalSessionManager by lazy { GlobalApp.global() }

    private var daemonRunning = false
    private var wakeLock: PowerManager.WakeLock? = null

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private val binder = LocalBinder(WeakReference(this))

    class LocalBinder(private val service: WeakReference<TerminalService>) : Binder() {
        fun getService(): TerminalService? = service.get()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        if (!daemonRunning) {
            daemonRunning = true
        }

        if (wakeLock == null) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TerminalService::WakeLock"
            )
        }

        GlobalEventBus.subscribeIn<NewSessionEvent>(serviceScope) {
            updateNotification()
        }

        GlobalEventBus.subscribeIn<SessionTerminateEvent>(serviceScope) { event ->
            if (sessionManager.sessions.value.isEmpty()) {
                daemonRunning = false
                stopSelf()
            } else {
                updateNotification()
            }
        }
    }

    fun onBound() {
        if (!daemonRunning) {
            daemonRunning = true
        }
    }

    @SuppressLint("WakelockTimeout")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_EXIT -> {
                serviceScope.launch {
                    sessionManager.terminateAll()
                    daemonRunning = false
                    stopSelf()
                }
            }

            ACTION_WAKE_LOCK -> {
                wakeLock?.let { lock ->
                    if (lock.isHeld) {
                        lock.release()
                    } else {
                        lock.acquire()
                    }
                    updateNotification()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        daemonRunning = false
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }

        serviceJob.cancel()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_NOTIFICATION_TAP
        }
        val pendingTapIntent = PendingIntent.getActivity(
            this, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val exitIntent = Intent(this, TerminalService::class.java).apply { action = ACTION_EXIT }
        val exitPendingIntent = PendingIntent.getService(
            this, 1, exitIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val wakeLockIntent =
            Intent(this, TerminalService::class.java).apply { action = ACTION_WAKE_LOCK }
        val wakeLockPendingIntent = PendingIntent.getService(
            this,
            2,
            wakeLockIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val wakeLockTitle =
            if (wakeLock?.isHeld == true) "Release Wake Lock" else "Acquire Wake Lock"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Terminal")
            .setContentText(getNotificationContentText(wakeLock?.isHeld == true))
            .setSmallIcon(R.drawable.terminal_2_24px)
            .setContentIntent(pendingTapIntent)
            .addAction(
                NotificationCompat.Action.Builder(null, "Exit", exitPendingIntent).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(null, wakeLockTitle, wakeLockPendingIntent)
                    .build()
            )
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Terminal Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification for background Terminal sessions"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateNotification() {
        runCatching {
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }.onFailure { it.printStackTrace() }
    }

    private fun getNotificationContentText(isWakeLockHeld: Boolean): String {
        val count = sessionManager.sessions.value.size
        val s = if (count == 1) "session" else "sessions"
        val wakeHeld = if (isWakeLockHeld) " (wake lock held)" else ""
        return "$count $s running$wakeHeld"
    }

    companion object {
        private const val CHANNEL_ID = "terminal_service_channel"
        private const val NOTIFICATION_ID = 1

        private const val ACTION_EXIT = "com.klyx.terminal.ACTION_EXIT"
        private const val ACTION_WAKE_LOCK = "com.klyx.terminal.ACTION_WAKE_LOCK"
        const val ACTION_NOTIFICATION_TAP = "com.klyx.terminal.ACTION_NOTIFICATION_TAP"
    }
}
