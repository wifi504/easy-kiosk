package com.wifi504.easykiosk.engineprobe;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;

public class ProbeInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle report = new Bundle();
        try {
            try (java.io.FileOutputStream out = getTargetContext().openFileOutput("engine-isolation-canary", 0)) {
                out.write("private-host-fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            Intent intent = new Intent(getTargetContext(), ProbeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivitySync(intent);
            long deadline = System.currentTimeMillis() + 90000;
            while (ProbeActivity.result == null && ProbeActivity.failure == null && System.currentTimeMillis() < deadline) {
                Thread.sleep(250);
            }
            if (ProbeActivity.failure != null) throw new AssertionError(ProbeActivity.failure);
            String result = ProbeActivity.result;
            if (result == null) {
                throw new AssertionError("Bundled engine failed: " + result);
            }
            org.json.JSONObject payload = new org.json.JSONObject((String) new org.json.JSONTokener(result).nextValue());
            if (!payload.getString("ua").contains("Chrome/134.") || !payload.getBoolean("js") || !payload.getBoolean("css") || !payload.getBoolean("net")) throw new AssertionError("Modern JS/CSS did not execute: " + payload);
            report.putString("stream", "EMBEDDED_ENGINE_OK " + result + "\n");
            finish(Activity.RESULT_OK, report);
        } catch (Throwable error) {
            report.putString("stream", "EMBEDDED_ENGINE_FAILED " + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, report);
        }
    }
}
