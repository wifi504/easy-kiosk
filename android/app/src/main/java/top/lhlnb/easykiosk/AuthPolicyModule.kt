package top.lhlnb.easykiosk

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class AuthPolicyModule(context: ReactApplicationContext) : ReactContextBaseJavaModule(context) {
    override fun getName() = "AuthPolicyModule"
    override fun getConstants(): Map<String, Any> = mapOf("mode" to AuthPolicy.mode(reactApplicationContext))

    @ReactMethod
    fun prepareTime(promise: Promise) {
        try { NetworkTime.prepare(); promise.resolve(true) }
        catch (_: Exception) { promise.reject("AUTH_TIME_UNAVAILABLE", "网络连接失败") }
    }

    @ReactMethod
    fun verifyTotp(code: String, promise: Promise) {
        try { promise.resolve(AuthPolicy.verify(reactApplicationContext, code)) }
        catch (_: Exception) { promise.reject("AUTH_TIME_UNAVAILABLE", "网络连接失败") }
    }
}
