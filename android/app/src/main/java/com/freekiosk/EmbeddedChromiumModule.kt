package com.freekiosk

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule

class EmbeddedChromiumModule(context: ReactApplicationContext) : ReactContextBaseJavaModule(context) {
    override fun getName() = "EmbeddedChromiumModule"
    override fun getConstants(): Map<String, Any> = mapOf(
        "version" to BuildConfig.CHROMIUM_VERSION,
        "sha256" to BuildConfig.CHROMIUM_SHA256,
        "userAgent" to EmbeddedChromium.userAgent,
    )
}
