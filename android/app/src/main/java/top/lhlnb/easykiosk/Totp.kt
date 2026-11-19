package top.lhlnb.easykiosk

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** RFC 6238, SHA-1 / six digits / 30 seconds. No device wall-clock dependency. */
object Totp {
    fun decodeSecret(raw: String): ByteArray {
        val normalized = raw.replace(Regex("\\s"), "").uppercase(java.util.Locale.ROOT)
        require(normalized.matches(Regex("[A-Z2-7]+={0,6}"))) { "Invalid Base32 secret" }
        val text = normalized.trimEnd('=')
        val bytes = java.io.ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (char in text) {
            buffer = (buffer shl 5) or "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(char)
            bits += 5
            if (bits >= 8) { bits -= 8; bytes.write((buffer shr bits) and 255) }
            buffer = buffer and ((1 shl bits) - 1)
        }
        require(buffer == 0 && bytes.size() in 16..64 && text.length % 8 in listOf(0, 2, 4, 5, 7)) { "Invalid Base32 secret" }
        if (normalized.contains('=')) require(normalized.length % 8 == 0) { "Invalid Base32 padding" }
        return bytes.toByteArray()
    }

    fun code(key: ByteArray, step: Long): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array())
        val offset = hash.last().toInt() and 15
        val value = ((hash[offset].toInt() and 127) shl 24) or
            ((hash[offset + 1].toInt() and 255) shl 16) or
            ((hash[offset + 2].toInt() and 255) shl 8) or (hash[offset + 3].toInt() and 255)
        return (value % 1_000_000).toString().padStart(6, '0')
    }

    fun matchingStep(key: ByteArray, input: String, unixMillis: Long): Long? {
        if (!input.matches(Regex("[0-9]{6}"))) return null
        val current = unixMillis / 30_000
        return (current + 1 downTo maxOf(0, current - 1)).firstOrNull {
            java.security.MessageDigest.isEqual(code(key, it).toByteArray(), input.toByteArray())
        }
    }
}
