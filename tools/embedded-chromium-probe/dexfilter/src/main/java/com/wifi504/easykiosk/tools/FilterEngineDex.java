package com.wifi504.easykiosk.tools;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.writer.pool.DexPool;
import org.jf.dexlib2.writer.io.FileDataStore;

/** Drop only audited shared AndroidX definitions from the engine; unknown conflicts fail. */
public final class FilterEngineDex {
    public static void main(String[] args) throws Exception {
        Set<String> host = new HashSet<>();
        for (File file : new File(args[0]).listFiles()) {
            if (!file.getName().startsWith("host-") || !file.getName().endsWith(".dex")) continue;
            for (ClassDef type : DexFileFactory.loadDexFile(file, Opcodes.forApi(29)).getClasses()) host.add(type.getType());
        }
        DexPool pool = new DexPool(Opcodes.forApi(29));
        StringBuilder report = new StringBuilder();
        for (ClassDef type : DexFileFactory.loadDexFile(new File(args[1]), Opcodes.forApi(29)).getClasses()) {
            if (host.contains(type.getType())) {
                if (!type.getType().startsWith("Landroidx/") && !type.getType().startsWith("Landroid/support/")) {
                    throw new IllegalStateException("Unaudited duplicate engine class: " + type.getType());
                }
                report.append(type.getType()).append('\n');
            } else pool.internClass(type);
        }
        try (FileDataStore store = new FileDataStore(new File(args[2]))) { pool.writeTo(store); }
        Files.write(Paths.get(args[3]), report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        System.out.println("Shared AndroidX engine definitions deduplicated:\n" + report);
    }
}
