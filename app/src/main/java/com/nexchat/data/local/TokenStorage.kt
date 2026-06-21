package com.nexchat.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nexchat.domain.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure token storage using EncryptedSharedPreferences (AES-256-GCM).
 * Tokens are encrypted at rest — unreadable even on rooted devices
 * without the device credential.
 */
@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        SECURE_PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val json = Json { ignoreUnknownKeys = true }

    // ─── Token Operations ──────────────────────────────────────────────────

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    suspend fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    suspend fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    // ─── User Operations ───────────────────────────────────────────────────

    suspend fun saveUser(user: User) {
        prefs.edit()
            .putString(KEY_USER_JSON, json.encodeToString(user))
            .apply()
    }

    suspend fun getUser(): User? {
        val raw = prefs.getString(KEY_USER_JSON, null) ?: return null
        return try { json.decodeFromString(raw) } catch (_: Exception) { null }
    }

    fun observeUser(): Flow<User?> = flow {
        val raw = prefs.getString(KEY_USER_JSON, null)
        emit(raw?.let { try { json.decodeFromString(it) } catch (_: Exception) { null } })
    }

    // ─── Clear ─────────────────────────────────────────────────────────────

    suspend fun clearAll() {
        prefs.edit().clear().apply()
    }

    // ─── App Lock PIN ──────────────────────────────────────────────────────

    suspend fun hasPin(): Boolean = prefs.getString(KEY_PIN_HASH, null) != null

    suspend fun savePinHash(hash: String) {
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    suspend fun getPinHash(): String? = prefs.getString(KEY_PIN_HASH, null)

    suspend fun clearPin() {
        prefs.edit().remove(KEY_PIN_HASH).apply()
    }

    // ─── Drafts (encrypted) ────────────────────────────────────────────────

    suspend fun saveDraft(conversationId: String, text: String) {
        prefs.edit().putString("draft:$conversationId", text).apply()
    }

    suspend fun getDraft(conversationId: String): String? =
        prefs.getString("draft:$conversationId", null)

    suspend fun clearDraft(conversationId: String) {
        prefs.edit().remove("draft:$conversationId").apply()
    }

    // ─── Biometric preference ──────────────────────────────────────────────

    suspend fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    suspend fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)

    companion object {
        private const val SECURE_PREFS_FILE = "nexchat_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "secure_access_token"
        private const val KEY_REFRESH_TOKEN = "secure_refresh_token"
        private const val KEY_USER_JSON = "secure_user"
        private const val KEY_PIN_HASH = "secure_pin_hash"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}
