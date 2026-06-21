package com.nexchat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.nexchat.util.NexChatLog
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class NexChatApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging (debug only, silent in release)
        NexChatLog.init()

        Timber.d("NexChatApp starting")

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return

        val messageChannel = NotificationChannel(
            CHANNEL_MESSAGES,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New message notifications"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 150, 100, 150)
            setShowBadge(true)
        }

        val callChannel = NotificationChannel(
            CHANNEL_CALLS,
            "Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming call notifications"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 300, 500)
            setShowBadge(true)
        }

        val generalChannel = NotificationChannel(
            CHANNEL_GENERAL,
            "General",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "General app notifications"
            setShowBadge(false)
        }

        manager.createNotificationChannels(listOf(messageChannel, callChannel, generalChannel))
    }

    companion object {
        const val CHANNEL_MESSAGES = "nexchat_messages"
        const val CHANNEL_CALLS = "nexchat_calls"
        const val CHANNEL_GENERAL = "nexchat_general"
    }
}
