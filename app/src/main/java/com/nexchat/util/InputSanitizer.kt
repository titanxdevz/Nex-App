package com.nexchat.util

import java.text.Normalizer
import java.util.regex.Pattern

/**
 * Input sanitization for user-generated content.
 * Prevents XSS, injection, and abuse in group names, messages, bios, etc.
 */
object InputSanitizer {

    private val HTML_PATTERN = Pattern.compile("<[^>]*>")
    private val SCRIPT_PATTERN = Pattern.compile("(?i)<script[^>]*>.*?</script>")
    private val EVENT_HANDLER_PATTERN = Pattern.compile("(?i)on\\w+\\s*=")
    private val JS_URL_PATTERN = Pattern.compile("(?i)javascript:")
    private val SQL_INJECTION_PATTERN = Pattern.compile("(?i)(\\b(select|insert|update|delete|drop|union|exec|execute|alter|create|grant|revoke)\\b)")

    /**
     * Strip all HTML tags from input. Used for display names, group names, bios.
     */
    fun stripHtml(input: String): String {
        return HTML_PATTERN.matcher(input).replaceAll("")
    }

    /**
     * Sanitize for safe display — strip HTML, normalize Unicode, trim.
     */
    fun sanitizeDisplay(input: String): String {
        return stripHtml(input)
            .let { Normalizer.normalize(it, Normalizer.Form.NFKC) }
            .trim()
            .take(MAX_DISPLAY_LENGTH)
    }

    /**
     * Sanitize message content — strip scripts but allow markdown.
     * Messages use markdown for formatting, so we only strip dangerous HTML.
     */
    fun sanitizeMessage(input: String): String {
        return input
            .replace(SCRIPT_PATTERN.toRegex(), "")
            .replace(EVENT_HANDLER_PATTERN.toRegex(), "")
            .replace(JS_URL_PATTERN.toRegex(), "")
            .trim()
            .take(MAX_MESSAGE_LENGTH)
    }

    /**
     * Sanitize username — alphanumeric, underscores, dots only.
     */
    fun sanitizeUsername(input: String): String {
        return input
            .lowercase()
            .replace("[^a-z0-9._]".toRegex(), "")
            .take(MAX_USERNAME_LENGTH)
    }

    /**
     * Sanitize group/community name.
     */
    fun sanitizeGroupName(input: String): String {
        return stripHtml(input).trim().take(MAX_GROUP_NAME_LENGTH)
    }

    /**
     * Check if input contains potentially dangerous content.
     */
    fun isSuspicious(input: String): Boolean {
        return SCRIPT_PATTERN.matcher(input).find() ||
                EVENT_HANDLER_PATTERN.matcher(input).find() ||
                JS_URL_PATTERN.matcher(input).find()
    }

    /**
     * Validate email format.
     */
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validate password strength.
     * Min 8 chars, at least 1 letter and 1 number.
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        if (!password.any { it.isLetter() }) return false
        if (!password.any { it.isDigit() }) return false
        return true
    }

    fun getPasswordStrength(password: String): PasswordStrength {
        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when {
            score <= 2 -> PasswordStrength.WEAK
            score <= 4 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }

    const val MAX_DISPLAY_LENGTH = 50
    const val MAX_MESSAGE_LENGTH = 4096
    const val MAX_USERNAME_LENGTH = 30
    const val MAX_GROUP_NAME_LENGTH = 100
}

enum class PasswordStrength { WEAK, MEDIUM, STRONG }
