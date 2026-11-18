package com.wifi504.easykiosk.engineprobe;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import com.norman.webviewup.lib.UpgradeCallback;
import com.norman.webviewup.lib.WebViewUpgrade;
import com.norman.webviewup.lib.source.UpgradeAssetSource;

/** App-scoped loader experiment only. Not the production EasyKiosk application. */
public class ProbeActivity extends Activity {
    public static volatile String result;
    public static volatile String failure;
    private WebView view;
    private UpgradeCallback callback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView status = new TextView(this);
        status.setText("Preparing bundled Chromium test");
        setContentView(status);
        callback = new UpgradeCallback() {
            public void onUpgradeProcess(float percent) { }
            public void onUpgradeError(Throwable error) {
                failure = Log.getStackTraceString(error);
                Log.e("EasyKioskEngineProbe", failure);
            }
            public void onUpgradeComplete() {
                try {
                    view = new WebView(ProbeActivity.this);
                    view.getSettings().setJavaScriptEnabled(true);
                    setContentView(view);
                    view.setWebViewClient(new WebViewClient() {
                        @Override public void onPageFinished(WebView webView, String url) {
                            // Check actual engine UA and modern JS/CSS, not an overridden UA.
                            webView.evaluateJavascript("JSON.stringify({ua:navigator.userAgent,js:({x:7})?.x===7,css:CSS.supports('grid-template-columns','subgrid')})", value -> {
                                result = value;
                                Log.i("EasyKioskEngineProbe", "PAGE_RESULT " + value);
                            });
                        }
                    });
                    view.loadDataWithBaseURL("https://engine-probe.invalid/", "<html><body>Bundled Chromium</body></html>", "text/html", "UTF-8", null);
                } catch (Throwable error) { onUpgradeError(error); }
            }
        };
        WebViewUpgrade.addUpgradeCallback(callback);
        WebViewUpgrade.upgrade(new UpgradeAssetSource(this, "chromium-probe.apk", "134.0.6998.39-x86"));
    }
    @Override public void onDestroy() {
        if (callback != null) WebViewUpgrade.removeUpgradeCallback(callback);
        if (view != null) view.destroy();
        super.onDestroy();
    }
}
