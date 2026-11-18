package com.freekiosk

import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import com.facebook.react.bridge.*
import java.util.Locale

/** Local WebView and camera helpers. No HTTP listener or remote command dispatcher. */
class DeviceUtilitiesModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    companion object { private const val TAG = "DeviceUtilitiesModule" }
    private var cameraPhotoModule: CameraPhotoModule? = null
    private val tts = LazyTts(reactContext.applicationContext, TAG)
    private var ttsVoicesCache: List<Map<String, String>> = emptyList()
    override fun getName() = "DeviceUtilitiesModule"
    private fun cacheTtsVoices(ttsEngine: TextToSpeech) {
        try {
            val voices = mutableListOf<Map<String, String>>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (voice in ttsEngine.voices) {
                    voices.add(mapOf(
                        "name" to voice.name,
                        "lang" to voice.locale.toLanguageTag(),
                        "voiceUri" to voice.name,
                        "localService" to (!voice.isNetworkConnectionRequired).toString(),
                        "default" to (voices.isEmpty()).toString()  // first voice is default
                    ))
                }
            }
            // Fallback: if no voices via API, create voice from default engine info
            if (voices.isEmpty()) {
                val defaultLocale = ttsEngine.defaultVoice?.locale ?: ttsEngine.language ?: Locale.getDefault()
                voices.add(mapOf(
                    "name" to "Default TTS",
                    "lang" to defaultLocale.toLanguageTag(),
                    "voiceUri" to "default",
                    "localService" to "true",
                    "default" to "true"
                ))
            }
            ttsVoicesCache = voices
            Log.d(TAG, "TTS voices cached: ${voices.size} voices")
            for (v in voices) {
                Log.d(TAG, "  Voice: ${v["name"]} (${v["lang"]})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache TTS voices: ${e.message}")
        }
    }

    @ReactMethod
    fun getCamera2Devices(promise: Promise) {
        try {
            if (cameraPhotoModule == null) {
                cameraPhotoModule = CameraPhotoModule(reactContext.applicationContext)
            }
            val cameras = cameraPhotoModule?.getAvailableCameras() ?: emptyList()
            val result = Arguments.createArray()
            cameras.forEach { cam ->
                val cameraMap = Arguments.createMap().apply {
                    putString("id", cam["id"].toString())
                    putString("position", when (cam["facing"]) {
                        "front" -> "front"
                        "back" -> "back"
                        else -> cam["facing"].toString()
                    })
                    putInt("maxWidth", (cam["maxWidth"] as? Int) ?: 0)
                    putInt("maxHeight", (cam["maxHeight"] as? Int) ?: 0)
                }
                result.pushMap(cameraMap)
            }
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Camera2 devices: ${e.message}")
            promise.reject("CAMERA2_ERROR", "Failed to enumerate cameras: ${e.message}")
        }
    }

    @ReactMethod
    fun captureCamera2Photo(cameraFacing: String, quality: Int, promise: Promise) {
        Thread {
            try {
                if (cameraPhotoModule == null) {
                    cameraPhotoModule = CameraPhotoModule(reactContext.applicationContext)
                }
                val inputStream = cameraPhotoModule?.capturePhoto(cameraFacing, quality)
                if (inputStream == null) {
                    promise.reject("CAPTURE_FAILED", "Camera2 photo capture returned null")
                    return@Thread
                }
                // Write to temp file
                val tempFile = java.io.File.createTempFile("motion_cam2_", ".jpg", reactContext.cacheDir)
                tempFile.outputStream().use { out ->
                    inputStream.copyTo(out)
                }
                inputStream.close()
                promise.resolve(tempFile.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Camera2 photo capture failed: ${e.message}")
                promise.reject("CAPTURE_ERROR", "Camera2 capture failed: ${e.message}")
            }
        }.start()
    }

    @ReactMethod
    fun getTtsVoices(promise: Promise) {
        // Binds the engine on the first call and answers once it is up
        tts.use { engine ->
            try {
                if (engine != null && ttsVoicesCache.isEmpty()) cacheTtsVoices(engine)
                val voices = Arguments.createArray()
                for (voice in ttsVoicesCache) {
                    val v = Arguments.createMap()
                    v.putString("name", voice["name"] ?: "")
                    v.putString("lang", voice["lang"] ?: "")
                    v.putString("voiceUri", voice["voiceUri"] ?: "")
                    v.putBoolean("localService", voice["localService"] == "true")
                    v.putBoolean("default", voice["default"] == "true")
                    voices.pushMap(v)
                }
                promise.resolve(voices)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get TTS voices: ${e.message}")
                promise.reject("TTS_ERROR", e.message)
            }
        }
    }

    @ReactMethod
    fun speak(text: String, language: String, voiceUri: String, promise: Promise) {
        try {
            val lang = if (language.isNotEmpty()) language else null
            val voice = if (voiceUri.isNotEmpty() && voiceUri != "default") voiceUri else null
            speakText(text, lang, voice)
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to speak text", e)
            promise.reject("TTS_ERROR", e.message)
        }
    }

    @ReactMethod
    fun stopSpeaking(promise: Promise) {
        try {
            tts.stop()
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop speaking", e)
            promise.reject("TTS_ERROR", e.message)
        }
    }

    private fun detectLocaleForText(text: String): Locale {
        var cjkCount = 0
        var koreanCount = 0
        var japaneseCount = 0
        var arabicCount = 0
        var thaiCount = 0
        var devanagariCount = 0
        var cyrillicCount = 0
        var latinCount = 0
        var totalLetters = 0

        for (char in text) {
            if (!Character.isLetterOrDigit(char)) continue
            totalLetters++
            when {
                // Korean Hangul
                char in '\uAC00'..'\uD7AF' || char in '\u1100'..'\u11FF' || char in '\u3130'..'\u318F' -> koreanCount++
                // Japanese Hiragana + Katakana
                char in '\u3040'..'\u309F' || char in '\u30A0'..'\u30FF' -> japaneseCount++
                // CJK Unified Ideographs (Chinese/Japanese Kanji)
                char in '\u4E00'..'\u9FFF' || char in '\u3400'..'\u4DBF' || char in '\uF900'..'\uFAFF' -> cjkCount++
                // Arabic
                char in '\u0600'..'\u06FF' || char in '\u0750'..'\u077F' -> arabicCount++
                // Thai
                char in '\u0E00'..'\u0E7F' -> thaiCount++
                // Devanagari (Hindi)
                char in '\u0900'..'\u097F' -> devanagariCount++
                // Cyrillic
                char in '\u0400'..'\u04FF' -> cyrillicCount++
                // Latin
                char in 'A'..'Z' || char in 'a'..'z' || char in '\u00C0'..'\u024F' -> latinCount++
            }
        }

        if (totalLetters == 0) return Locale.getDefault()

        // Find the dominant non-Latin script
        val scriptCounts = mapOf(
            "korean" to koreanCount,
            "japanese" to japaneseCount,
            "cjk" to cjkCount,
            "arabic" to arabicCount,
            "thai" to thaiCount,
            "devanagari" to devanagariCount,
            "cyrillic" to cyrillicCount
        )
        val dominant = scriptCounts.maxByOrNull { it.value }

        // If any non-Latin script is present, use that language
        if (dominant != null && dominant.value > 0) {
            return when (dominant.key) {
                "korean" -> Locale.KOREAN
                "japanese" -> Locale.JAPANESE
                "cjk" -> Locale.SIMPLIFIED_CHINESE
                "arabic" -> Locale("ar")
                "thai" -> Locale("th")
                "devanagari" -> Locale("hi")
                "cyrillic" -> Locale("ru")
                else -> Locale.getDefault()
            }
        }

        // All Latin — use device default locale
        return Locale.getDefault()
    }

    private fun parseLocale(language: String): Locale {
        val tag = language.replace("_", "-")
        return try {
            val locale = Locale.forLanguageTag(tag)
            // forLanguageTag returns und (undetermined) for unknown tags — fall back in that case
            if (locale.language.isNotEmpty() && locale.language != "und") locale
            else {
                val parts = tag.split("-")
                when (parts.size) {
                    1 -> Locale(parts[0])
                    2 -> Locale(parts[0], parts[1])
                    else -> Locale(parts[0], parts[1], parts[2])
                }
            }
        } catch (e: Exception) {
            val parts = tag.split("-")
            when (parts.size) {
                1 -> Locale(parts[0])
                2 -> Locale(parts[0], parts[1])
                else -> Locale(parts[0], parts[1], parts[2])
            }
        }
    }

    private fun speakText(text: String, language: String? = null, voiceName: String? = null) {
        // Binds the engine on first use; the text waits until it is up instead of being lost
        tts.use { engine ->
            if (engine == null) {
                Log.w(TAG, "TTS unavailable, dropping: $text")
                return@use
            }
            try {
                // If a specific voice name is requested, try to find and set it
                var voiceSet = false
                if (!voiceName.isNullOrEmpty()) {
                    val matchingVoice = engine.voices?.find { it.name == voiceName }
                    if (matchingVoice != null) {
                        val setResult = engine.setVoice(matchingVoice)
                        voiceSet = true
                        Log.d(TAG, "TTS set voice: $voiceName (result=$setResult)")
                    } else {
                        Log.w(TAG, "TTS voice not found: $voiceName, using locale-based selection")
                    }
                }

                // No voice set: pick one by locale. This used to check `voice == null`, which
                // is practically never true, so a voice that was not found skipped it.
                if (!voiceSet) {
                    val targetLocale = if (!language.isNullOrEmpty()) {
                        parseLocale(language)
                    } else {
                        detectLocaleForText(text)
                    }

                    val result = engine.setLanguage(targetLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "TTS language not supported: $targetLocale, falling back to default")
                        engine.setLanguage(Locale.getDefault())
                    } else {
                        Log.d(TAG, "TTS language set to: $targetLocale")
                    }
                }

                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "freekiosk_tts_${System.currentTimeMillis()}")
                Log.d(TAG, "TTS speaking: $text (voice=$voiceName)")
            } catch (e: Exception) {
                Log.e(TAG, "TTS speak failed: ${e.message}")
            }
        }
    }

    private fun clearWebViewCache(onDone: (() -> Unit)? = null) {
        UiThreadUtil.runOnUiThread {
            try {
                // Clear WebView cache and data
                android.webkit.WebView(reactContext.applicationContext).apply {
                    clearCache(true)
                    clearFormData()
                    clearHistory()
                    destroy()
                }
                // Clear WebStorage (localStorage, IndexedDB, WebSQL)
                android.webkit.WebStorage.getInstance().deleteAllData()
                // Also clear cookies; done once they are really gone
                val cookies = android.webkit.CookieManager.getInstance()
                cookies.removeAllCookies {
                    cookies.flush()
                    Log.d(TAG, "WebView cache, cookies, and storage cleared")
                    onDone?.invoke()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear WebView cache: ${e.message}")
                onDone?.invoke()
            }
        }
    }

    @ReactMethod
    fun clearWebViewSession(promise: Promise) {
        clearWebViewCache { promise.resolve(true) }
    }
    override fun invalidate() {
        tts.shutdown()
        super.invalidate()
    }
}
