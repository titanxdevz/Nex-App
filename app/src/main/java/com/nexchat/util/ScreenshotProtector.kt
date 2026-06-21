package com.nexchat.util

import android.app.Activity
import android.view.WindowManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Screenshot and screen recording protection.
 * Adds FLAG_SECURE to prevent content from appearing in
 * recent apps, screenshots, and screen recordings.
 */
@Singleton
class ScreenshotProtector @Inject constructor() {

    /**
     * Block screenshots/screen recording for this activity.
     */
    fun protect(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    /**
     * Allow screenshots/screen recording again.
     */
    fun allow(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
