package com.nexchat.data.remote.interceptor

import com.nexchat.data.local.TokenStorage
import com.nexchat.data.remote.api.AuthApi
import com.nexchat.data.remote.dto.RefreshRequest
import com.nexchat.util.NexChatLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auth interceptor with thread-safe token refresh.
 * Uses Mutex to prevent multiple simultaneous refresh attempts.
 * Queues pending requests and retries them with the new token.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val authApi: AuthApi
) : Interceptor {

    private val refreshMutex = Mutex()
    private var lastRefreshToken: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenStorage.getAccessToken() }
        val request = chain.request().newBuilder().apply {
            token?.let { addHeader("Authorization", "Bearer $it") }
        }.build()

        val response = chain.proceed(request)

        if (response.code == 401) {
            return handleUnauthorized(chain, request, response)
        }

        return response
    }

    private fun handleUnauthorized(
        chain: Interceptor.Chain,
        originalRequest: okhttp3.Request,
        unauthorizedResponse: Response
    ): Response {
        // Check if this is a refresh request itself — don't retry
        if (originalRequest.url.encodedPath.contains("auth/refresh")) {
            runBlocking { tokenStorage.clearAll() }
            NexChatLog.logTokenEvent("Refresh token expired, clearing auth")
            return unauthorizedResponse
        }

        val newToken = runBlocking {
            refreshMutex.withLock {
                val currentRefreshToken = tokenStorage.getRefreshToken()

                // Another thread already refreshed with same token
                if (currentRefreshToken == lastRefreshToken && currentRefreshToken != null) {
                    return@withLock tokenStorage.getAccessToken()
                }

                if (currentRefreshToken == null) {
                    tokenStorage.clearAll()
                    return@withLock null
                }

                NexChatLog.logTokenEvent("Refreshing access token")

                try {
                    val result = authApi.refresh(RefreshRequest(currentRefreshToken))
                    val data = result.body()?.data

                    if (data != null) {
                        tokenStorage.saveTokens(data.accessToken, data.refreshToken)
                        lastRefreshToken = data.refreshToken
                        NexChatLog.logTokenEvent("Token refresh successful")
                        data.accessToken
                    } else {
                        NexChatLog.logTokenEvent("Token refresh failed: empty response")
                        tokenStorage.clearAll()
                        null
                    }
                } catch (e: Exception) {
                    NexChatLog.logError("AUTH", "Refresh failed: ${e.message}")
                    tokenStorage.clearAll()
                    null
                }
            }
        }

        unauthorizedResponse.close()

        return if (newToken != null) {
            val retryRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
            chain.proceed(retryRequest)
        } else {
            // Return 401 so UI can navigate to login
            unauthorizedResponse
        }
    }
}
