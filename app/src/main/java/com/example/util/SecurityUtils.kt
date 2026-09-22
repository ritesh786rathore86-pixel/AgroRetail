package com.example.util

import java.security.MessageDigest

object SecurityUtils {
    private const val SALT = "AgroRetail_2026_Secure_Salt#"

    fun hashPin(pin: String): String {
        val input = "$SALT$pin"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, storedHash: String): Boolean {
        if (storedHash.isBlank() || pin.isBlank()) return false
        val computed = hashPin(pin)
        return computed.equals(storedHash, ignoreCase = true)
    }
}
