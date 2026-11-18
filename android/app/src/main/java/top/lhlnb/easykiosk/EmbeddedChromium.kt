package top.lhlnb.easykiosk

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import com.norman.webviewup.lib.UpgradeCallback
import com.norman.webviewup.lib.WebViewUpgrade
import com.norman.webviewup.lib.source.UpgradeAssetSource

/** Loads the signed APK's bundled engine before React Native can create a WebView. */
object EmbeddedChromium {
    private val main = Handler(Looper.getMainLooper())
    private val listeners = mutableListOf<(Throwable?) -> Unit>()
    private var started = false
    private var resolved = false
    private var failure: Throwable? = null
    @Volatile var isReady = false
        private set
    @Volatile var userAgent = ""
        private set

    fun isIsolatedProcess(): Boolean = Process.myUid() % 100000 in 90000..99999

    fun prepare(app: Application) {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post { prepare(app) }; return }
        if (started || isIsolatedProcess()) return
        started = true
        val timeout = Runnable { complete(IllegalStateException("Bundled Chromium initialization timed out")) }
        val callback = object : UpgradeCallback {
            override fun onUpgradeProcess(percent: Float) = Unit
            override fun onUpgradeError(error: Throwable) {
                main.removeCallbacks(timeout)
                WebViewUpgrade.removeUpgradeCallback(this)
                complete(error)
            }
            override fun onUpgradeComplete() {
                main.removeCallbacks(timeout)
                WebViewUpgrade.removeUpgradeCallback(this)
                if (resolved) return
                try {
                    userAgent = WebSettings.getDefaultUserAgent(app)
                    val actual = WebView.getCurrentWebViewPackage()?.versionName
                    check(actual == BuildConfig.CHROMIUM_VERSION) { "Unexpected engine: $actual" }
                    check(userAgent.contains("Chrome/" + BuildConfig.CHROMIUM_VERSION.substringBefore('.') + ".")) {
                        "Bundled Chromium UA does not match the pinned major version"
                    }
                    isReady = true
                    Log.i("EasyKioskEngine", "ENGINE_READY version=$actual sha256=${BuildConfig.CHROMIUM_SHA256}")
                    complete(null)
                } catch (error: Throwable) { complete(error) }
            }
        }
        WebViewUpgrade.addUpgradeCallback(callback)
        main.postDelayed(timeout, 180000)
        WebViewUpgrade.upgrade(UpgradeAssetSource(app, "embedded-chromium.apk", BuildConfig.CHROMIUM_SHA256))
    }

    fun whenReady(listener: (Throwable?) -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post { whenReady(listener) }; return }
        if (resolved) listener(failure) else listeners.add(listener)
    }

    private fun complete(error: Throwable?) {
        if (resolved) return
        resolved = true
        failure = error
        if (error != null) Log.e("EasyKioskEngine", "Bundled engine failed; system WebView fallback is disabled", error)
        val pending = listeners.toList()
        listeners.clear()
        pending.forEach { it(error) }
    }
}
