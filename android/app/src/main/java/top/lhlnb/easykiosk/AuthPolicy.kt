package top.lhlnb.easykiosk

import android.content.Context
import org.json.JSONObject

/** Only the policy in the signed APK controls authentication, never mutable app settings. */
object AuthPolicy {
    private fun policy(context: Context): JSONObject = context.assets.open("easykiosk-auth.json").bufferedReader().use {
        JSONObject(it.readText()).also { value -> require(value.getInt("schema") == 1) }
    }

    fun mode(context: Context): String = try {
        when (policy(context).getString("mode")) {
            "pin" -> "pin"
            "totp" -> { Totp.decodeSecret(policy(context).getString("secret")); "totp" }
            else -> "error"
        }
    } catch (_: Exception) { "error" }

    fun verify(context: Context, input: String): Boolean {
        check(mode(context) == "totp") { "Authentication policy unavailable" }
        val key = Totp.decodeSecret(policy(context).getString("secret"))
        // Network I/O happens on the React native-module worker, not the UI thread.
        val unixMillis = NetworkTime.preparedUnixMillis()
        val matched = Totp.matchingStep(key, input, unixMillis) ?: return false
        synchronized(this) {
            val fingerprint = java.security.MessageDigest.getInstance("SHA-256").digest(key).joinToString("") { "%02x".format(it) }
            val prefs = context.getSharedPreferences("EasyKioskTotpReplay", Context.MODE_PRIVATE)
            val last = prefs.getLong(fingerprint, -1)
            if (matched <= last) return false
            check(prefs.edit().putLong(fingerprint, matched).commit()) { "Could not persist authentication state" }
            return true
        }
    }
}
