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
        File[] files = new File(args[0]).listFiles();
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) if (file.getName().startsWith("host-") && file.getName().endsWith(".dex")) {
            for (ClassDef type : DexFileFactory.loadDexFile(file, Opcodes.forApi(29)).getClasses()) host.put(type.getType(), type);
        }
        new File(args[1]).mkdirs();
        StringBuilder report = new StringBuilder();
        for (File file : files) if (file.getName().startsWith("engine-") && file.getName().endsWith(".dex")) {
            DexPool pool = new DexPool(Opcodes.forApi(29));
            for (ClassDef type : DexFileFactory.loadDexFile(file, Opcodes.forApi(29)).getClasses()) {
                ClassDef existing = host.get(type.getType());
                if (existing == null) pool.internClass(type);
                else {
                    if (!shared(type.getType())) throw new IllegalStateException("Unaudited duplicate engine class: " + type.getType());
                    verify(type, existing, host);
                    report.append(type.getType()).append('\n');
                }
            }
            FileDataStore out = new FileDataStore(new File(args[1], file.getName()));
            try { pool.writeTo(out); } finally { out.close(); }
        }
        Files.write(Paths.get(args[2]), report.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("Shared runtime binary API verified; deduplicated classes:\n" + report);
    }
}
