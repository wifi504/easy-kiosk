package com.wifi504.easykiosk.engineprobe;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.TextView;
import com.norman.webviewup.lib.UpgradeCallback;
import com.norman.webviewup.lib.WebViewUpgrade;
import com.norman.webviewup.lib.source.UpgradeAssetSource;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Fixed loopback fixtures only. This is independent of the production application. */
public class ProbeActivity extends Activity {
    public static volatile String result;
    public static volatile String failure;
    public static volatile boolean rendererOutOfProcess;
    private WebView view;
    private ServerSocket server;
    private UpgradeCallback callback;
    private final String fixture = "<html><body>Bundled Chromium<script>" +
        "window.onload=async()=>{try{const data=await fetch('/ping').then(r=>r.json());" +
        "EngineProbe.report(JSON.stringify({ua:navigator.userAgent,js:({x:7})?.x===7," +
        "css:CSS.supports('grid-template-columns','subgrid'),net:data.ok}));}" +
        "catch(e){EngineProbe.fail(String(e));}};</script></body></html>";

    private void failed(Throwable error) {
        failure = Log.getStackTraceString(error);
        Log.e("EasyKioskEngineProbe", failure);
    }
    private void serve() {
        try {
            while (!server.isClosed()) {
                try (Socket client = server.accept()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                    String request = reader.readLine();
                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) { }
                    boolean ping = request != null && request.startsWith("GET /ping ");
                    byte[] body = (ping ? "{\"ok\":true}" : fixture).getBytes(StandardCharsets.UTF_8);
                    String header = "HTTP/1.1 200 OK\r\nContent-Type: " + (ping ? "application/json" : "text/html") +
                        "; charset=utf-8\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n";
                    client.getOutputStream().write(header.getBytes(StandardCharsets.UTF_8));
                    client.getOutputStream().write(body);
                }
            }
        } catch (Exception error) { if (!server.isClosed()) failed(error); }
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView status = new TextView(this);
        status.setText("Preparing bundled Chromium test");
        setContentView(status);
        try {
            server = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
            new Thread(this::serve, "engine-loopback-fixture").start();
        } catch (Exception error) { failed(error); return; }
        callback = new UpgradeCallback() {
            public void onUpgradeProcess(float percent) { }
            public void onUpgradeError(Throwable error) { failed(error); }
            public void onUpgradeComplete() {
                try {
                    view = new WebView(ProbeActivity.this);
                    view.getSettings().setJavaScriptEnabled(true);
                    view.addJavascriptInterface(new Object() {
                        @android.webkit.JavascriptInterface public void report(String payload) {
                            runOnUiThread(() -> { rendererOutOfProcess = view.getWebViewRenderProcess() != null; result = org.json.JSONObject.quote(payload); });
                            Log.i("EasyKioskEngineProbe", "PAGE_RESULT " + result);
                        }
                        @android.webkit.JavascriptInterface public void fail(String message) { failed(new Exception(message)); }
                    }, "EngineProbe");
                    setContentView(view);
                    view.loadUrl("http://127.0.0.1:" + server.getLocalPort() + "/");
                } catch (Throwable error) { failed(error); }
            }
        };
        WebViewUpgrade.addUpgradeCallback(callback);
        try (java.io.InputStream in = getAssets().open("embedded-chromium.json")) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(); byte[] data = new byte[8192]; int count;
            while ((count = in.read(data)) != -1) out.write(data, 0, count);
            String hash = new org.json.JSONObject(out.toString("UTF-8")).getString("sha256");
            WebViewUpgrade.upgrade(new UpgradeAssetSource(this, "embedded-chromium.apk", hash));
        } catch (Exception error) { failed(error); }
    }
    @Override public void onDestroy() {
        if (callback != null) WebViewUpgrade.removeUpgradeCallback(callback);
        if (view != null) view.destroy();
        if (server != null) try { server.close(); } catch (Exception ignored) { }
        super.onDestroy();
    }
}
