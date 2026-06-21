package com.nexchat.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.io.File

/**
 * Root and tamper detection. Checks multiple vectors:
 * - Known root binary paths
 * - Test keys build
 * - Dangerous props
 * - Emulator detection
 */
object SecurityUtils {

    fun isDeviceRooted(): Boolean {
        return checkRootBinaries() || checkTestKeys() || checkProps() || checkSuExists()
    }

    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    fun isDebuggerAttached(): Boolean = Debug.isDebuggerConnected()

    private fun checkRootBinaries(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkTestKeys(): Boolean {
        return Build.TAGS?.contains("test-keys") == true
    }

    private fun checkProps(): Boolean {
        val props = arrayOf(
            "ro.debuggable",
            "ro.secure"
        )
        return try {
            val process = Runtime.getRuntime().exec("getprop ro.debuggable")
            val result = process.inputStream.bufferedReader().readText().trim()
            process.destroy()
            result == "1"
        } catch (_: Exception) {
            false
        }
    }

    private fun checkSuExists(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val result = process.inputStream.bufferedReader().readText()
            process.destroy()
            result.isNotBlank()
        } catch (_: Exception) {
            false
        }
    }
}
