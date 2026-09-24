package com.example.utils

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import android.util.Base64

data class TextStats(
    val characterCount: Int,
    val characterNoSpacesCount: Int,
    val wordCount: Int,
    val lineCount: Int,
    val readingTimeSeconds: Int
)

object TextToolUtils {

    fun computeStats(text: String): TextStats {
        val chars = text.length
        val charsNoSpaces = text.count { !it.isWhitespace() }
        val words = if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
        val lines = if (text.isEmpty()) 0 else text.lines().size
        // Average reading speed 200 words per minute -> ~3.33 words/sec
        val readingTime = if (words == 0) 0 else ((words / 200.0) * 60).toInt().coerceAtLeast(1)

        return TextStats(
            characterCount = chars,
            characterNoSpacesCount = charsNoSpaces,
            wordCount = words,
            lineCount = lines,
            readingTimeSeconds = readingTime
        )
    }

    fun toUpperCase(text: String): String = text.uppercase()

    fun toLowerCase(text: String): String = text.lowercase()

    fun toTitleCase(text: String): String {
        return text.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    fun toCamelCase(text: String): String {
        val words = text.split("[^a-zA-Z0-9]+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) return ""
        return words.first().lowercase() + words.drop(1).joinToString("") {
            it.lowercase().replaceFirstChar { char -> char.titlecase() }
        }
    }

    fun toKebabCase(text: String): String {
        return text.trim()
            .replace("[^a-zA-Z0-9]+".toRegex(), "-")
            .lowercase()
            .trim('-')
    }

    fun encodeBase64(text: String): String {
        return Base64.encodeToString(text.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }

    fun decodeBase64(text: String): String {
        return try {
            val bytes = Base64.decode(text, Base64.DEFAULT)
            String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            "Invalid Base64 string"
        }
    }

    fun encodeUrl(text: String): String {
        return try {
            URLEncoder.encode(text, "UTF-8")
        } catch (e: Exception) {
            text
        }
    }

    fun decodeUrl(text: String): String {
        return try {
            URLDecoder.decode(text, "UTF-8")
        } catch (e: Exception) {
            text
        }
    }

    fun computeHash(text: String, algorithm: String): String {
        return try {
            val digest = MessageDigest.getInstance(algorithm)
            val hashBytes = digest.digest(text.toByteArray(StandardCharsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Hash error"
        }
    }
}
