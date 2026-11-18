package com.wifi504.easykiosk.tools;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jf.dexlib2.*;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.writer.pool.DexPool;
import org.jf.dexlib2.writer.io.FileDataStore;

/** Reuse shared runtimes only when the host preserves the engine's public binary API. */
public final class FilterEngineDex {
    private static String signature(Method method) {
        return method.getName() + method.getParameterTypes().toString() + method.getReturnType();
    }
    private static boolean shared(String name) {
        return name.startsWith("Landroidx/") || name.startsWith("Landroid/support/") ||
            name.startsWith("Lkotlin/") || name.startsWith("Lkotlinx/") ||
            name.startsWith("Lorg/jetbrains/annotations/") || name.startsWith("Lorg/intellij/lang/annotations/") ||
            name.equals("Lcom/google/common/util/concurrent/ListenableFuture;");
    }
    private static void verify(ClassDef engine, ClassDef host, Map<String, ClassDef> classes) {
        Set<String> methods = new HashSet<>();
        Set<String> fields = new HashSet<>();
        ClassDef current = host;
        Set<String> visited = new HashSet<>();
        while (current != null && visited.add(current.getType())) {
            for (Method method : current.getMethods()) methods.add(signature(method));
            for (Field field : current.getFields()) fields.add(field.getName() + ":" + field.getType());
            current = classes.get(current.getSuperclass());
        }
        for (Method method : engine.getMethods()) {
            if ((method.getAccessFlags() & (AccessFlags.PUBLIC.getValue() | AccessFlags.PROTECTED.getValue())) != 0 &&
                !method.getName().equals("<clinit>") && !methods.contains(signature(method))) {
                throw new IllegalStateException("Shared runtime API mismatch: " + engine.getType() + " " + signature(method));
            }
        }
        for (Field field : engine.getFields()) {
            if ((field.getAccessFlags() & (AccessFlags.PUBLIC.getValue() | AccessFlags.PROTECTED.getValue())) != 0 &&
                !fields.contains(field.getName() + ":" + field.getType())) {
                throw new IllegalStateException("Shared runtime field mismatch: " + engine.getType() + " " + field.getName());
            }
        }
    }
    public static void main(String[] args) throws Exception {
        Map<String, ClassDef> host = new HashMap<>();
        Map<String, ClassDef> engine = new HashMap<>();
        Map<File, List<ClassDef>> shards = new LinkedHashMap<>();
        File[] files = new File(args[0]).listFiles();
        if (files == null) throw new IllegalArgumentException("DEX input directory missing");
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) if (file.getName().endsWith(".dex") &&
                (file.getName().startsWith("host-") || file.getName().startsWith("engine-"))) {
            List<ClassDef> types = new ArrayList<>();
            Map<String, ClassDef> destination = file.getName().startsWith("host-") ? host : engine;
            for (ClassDef type : DexFileFactory.loadDexFile(file, Opcodes.forApi(29)).getClasses()) {
                types.add(type); destination.put(type.getType(), type);
            }
            shards.put(file, types);
        }
        Set<String> engineBoundary = new HashSet<>();
        StringBuilder report = new StringBuilder();
        List<String> errors = new ArrayList<>();
        for (ClassDef type : engine.values()) {
            ClassDef existing = host.get(type.getType());
            if (existing == null) continue;
            try {
                if (type.getType().startsWith("Lorg/chromium/support_lib_boundary/")) {
                    // AndroidX and WebView share this public reflection boundary. The engine's
                    // definition is authoritative only if it preserves every host-side API.
                    verify(existing, type, engine);
                    engineBoundary.add(type.getType());
                    report.append("KEEP_ENGINE_BOUNDARY ").append(type.getType()).append('\n');
                } else {
                    if (!shared(type.getType())) throw new IllegalStateException("Unaudited duplicate engine class: " + type.getType());
                    verify(type, existing, host);
                    report.append("REUSE_HOST_RUNTIME ").append(type.getType()).append('\n');
                }
            } catch (IllegalStateException error) { errors.add(error.getMessage()); }
        }
        Files.write(Paths.get(args[2]), report.toString().getBytes(StandardCharsets.UTF_8));
        if (!errors.isEmpty()) throw new IllegalStateException(String.join("\n", errors));
        new File(args[1]).mkdirs();
        for (Map.Entry<File, List<ClassDef>> shard : shards.entrySet()) {
            boolean fromHost = shard.getKey().getName().startsWith("host-");
            DexPool pool = new DexPool(Opcodes.forApi(29));
            for (ClassDef type : shard.getValue()) {
                boolean keep = fromHost ? !engineBoundary.contains(type.getType()) :
                    !host.containsKey(type.getType()) || engineBoundary.contains(type.getType());
                if (keep) pool.internClass(type);
            }
            FileDataStore out = new FileDataStore(new File(args[1], shard.getKey().getName()));
            try { pool.writeTo(out); } finally { out.close(); }
        }
        System.out.println("Shared runtime and WebView boundary binary APIs verified:\n" + report);
    }
}