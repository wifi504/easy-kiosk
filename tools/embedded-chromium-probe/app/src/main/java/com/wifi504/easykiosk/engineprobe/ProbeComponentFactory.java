package com.wifi504.easykiosk.engineprobe;

public class ProbeComponentFactory extends top.lhlnb.easykiosk.EmbeddedChromiumComponentFactory {
    @Override protected void onIsolatedRendererCreated(int uid) {
        try (java.io.FileInputStream input = new java.io.FileInputStream("/data/user/0/com.wifi504.easykiosk.engineprobe/files/engine-isolation-canary")) {
            input.read();
            throw new SecurityException("Isolated renderer read host private file");
        } catch (java.io.IOException expected) {
            if (expected.getMessage() == null || !expected.getMessage().contains("Permission denied"))
                throw new SecurityException("Private fixture must exist and be denied by the OS", expected);
            android.util.Log.i("EasyKioskEngineProbe", "PRIVATE_DATA_ACCESS_DENIED " + uid);
        }
    }
}