package com.freekiosk;

import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Process;
import android.util.Log;
import androidx.core.app.CoreComponentFactory;
import dalvik.system.InMemoryDexClassLoader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.json.JSONArray;
import org.json.JSONObject;

/** Gives isolated renderers their own engine classes, read only from the installed signed APK. */
public class EmbeddedChromiumComponentFactory extends CoreComponentFactory {
    private static boolean isolated() {
        int id = Process.myUid() % 100000;
        return id >= 90000 && id <= 99999;
    }
    private static byte[] read(ZipFile apk, String name) throws Exception {
        ZipEntry entry = apk.getEntry(name);
        if (entry == null || entry.getSize() < 0 || entry.getSize() > 32 * 1024 * 1024)
            throw new SecurityException("Invalid installed engine entry: " + name);
        try (InputStream in = apk.getInputStream(entry); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[8192]; int count;
            while ((count = in.read(bytes)) != -1) out.write(bytes, 0, count);
            return out.toByteArray();
        }
    }
    private static String sha256(byte[] bytes) throws Exception {
        StringBuilder result = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes))
            result.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return result.toString();
    }
    @Override public ClassLoader instantiateClassLoader(ClassLoader original, ApplicationInfo info) {
        if (!isolated()) return original;
        try (ZipFile apk = new ZipFile(info.sourceDir)) {
            JSONArray entries = new JSONArray(new String(read(apk, "assets/embedded-renderer/index.json"), StandardCharsets.UTF_8));
            ByteBuffer[] dex = new ByteBuffer[entries.length()];
            if (dex.length == 0) throw new SecurityException("No installed renderer code");
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                String name = entry.getString("name");
                if (!name.matches("classes[0-9]*\\.dex")) throw new SecurityException("Invalid engine DEX name");
                byte[] bytes = read(apk, "assets/embedded-renderer/" + name);
                if (!sha256(bytes).equals(entry.getString("sha256"))) throw new SecurityException("Installed engine DEX checksum mismatch");
                dex[i] = ByteBuffer.wrap(bytes);
            }
            // Boot parent keeps engine AndroidX/Kotlin and host dependencies independent.
            return new InMemoryDexClassLoader(dex, info.nativeLibraryDir, Application.class.getClassLoader());
        } catch (Exception error) { throw new IllegalStateException("Cannot load installed isolated Chromium", error); }
    }
    @Override public Application instantiateApplication(ClassLoader loader, String name)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (isolated()) return new Application();
        return super.instantiateApplication(loader, name);
    }
    @Override public Service instantiateService(ClassLoader loader, String name, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (name.startsWith("com.norman.webviewup.lib.sandbox.stub.StubSandboxedProcessService")) {
            int uid = Process.myUid();
            if (!isolated() || !(loader instanceof InMemoryDexClassLoader))
                throw new SecurityException("Renderer must use isolated UID and independent installed code: " + uid);
            Log.i("EasyKioskEngine", "ISOLATED_RENDERER_UID " + uid + " independent_classloader=true");
            onIsolatedRendererCreated(uid);
            return (Service) loader.loadClass("org.chromium.content.app.SandboxedProcessService0").newInstance();
        }
        if (isolated()) throw new SecurityException("Unexpected isolated component: " + name);
        return super.instantiateService(loader, name, intent);
    }
    protected void onIsolatedRendererCreated(int uid) { }
}