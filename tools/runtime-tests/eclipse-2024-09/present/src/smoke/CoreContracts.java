package smoke;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.equinox.app.*;
import org.osgi.framework.Bundle;
import com.github.eclipsethemes.core.parser.RuntimeXmlChecks;

/** Packaged parser and real preference scopes. No workbench, mocked scopes or product exports. */
public class CoreContracts implements IApplication {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("PASS " + message);
    }

    @Override public Object start(IApplicationContext context) throws Exception {
        RuntimeState.verify();
        Bundle product = Platform.getBundle("com.github.eclipsethemes");
        check(product != null, "packaged product resolved");
        product.start();
        for (String id : List.of("org.eclipse.platform", "org.eclipse.osgi", "org.eclipse.core.runtime"))
            System.out.println("RUNTIME " + id + "=" + Platform.getBundle(id).getVersion());
        Path scratch = Path.of(System.getProperty("r1.scratch"));
        String phase = System.getProperty("r1.resetPhase", "exercise");
        if (phase.equals("exercise")) {
            Class<?> activator = product.loadClass("com.github.eclipsethemes.EclipseThemes");
            Object instance = activator.getMethod("instance").invoke(null);
            check(instance != null, "packaged activator started");
            Class<?> repository = product.loadClass("com.github.eclipsethemes.eclipse.ThemeRepository");
            Object catalog = repository.getConstructor(File.class, org.eclipse.core.runtime.ILog.class)
                    .newInstance(scratch.resolve("catalog").toFile(), activator.getMethod("getLogger").invoke(instance));
            repository.getMethod("reload").invoke(catalog);
            check(((List<?>) repository.getMethod("getAllThemes").invoke(catalog)).size() == 33,
                    "packaged catalog loads 33 bundled themes");
            RuntimeXmlChecks.run(product, scratch);
        }
        verifyReset(product, phase.equals("verify"));
        System.out.println(phase.equals("verify") ? "R1 PERSISTENCE COMPLETE" : "R1 CORE COMPLETE");
        return IApplication.EXIT_OK;
    }

    @SuppressWarnings("unchecked") private static void verifyReset(Bundle product, boolean persisted) throws Exception {
        Map<String, IEclipsePreferences> nodes = new LinkedHashMap<>();
        Map<String, Set<String>> keys = new LinkedHashMap<>();
        int count = 0;
        for (String name : List.of("editor.TextEditorThemeAdapter", "editor.JavaEditorThemeAdapter",
                "editor.CppEditorThemeAdapter", "editor.PluginEditorThemeAdapter", "editor.AntEditorThemeAdapter",
                "ui.DebugConsoleThemeAdapter")) {
            Class<?> type = product.loadClass("com.github.eclipsethemes.eclipse.adapters." + name);
            Object adapter = type.getConstructor().newInstance();
            String id = (String) type.getMethod("getPreferencesId").invoke(adapter);
            Set<String> managed = (Set<String>) type.getMethod("getManagedKeys").invoke(adapter);
            IEclipsePreferences node = InstanceScope.INSTANCE.getNode(id);
            if (!persisted) {
                for (String key : managed) node.put(key, "theme override");
                node.put("r1.unrelated.text", "keep\n=\u2603");
                node.put("r1.unrelated.number", "7");
                node.node("r1.child").put("key", "keep child");
                DefaultScope.INSTANCE.getNode(id).put(managed.iterator().next(), "lower scope");
                node.flush();
            }
            nodes.put(id, node); keys.put(id, managed); count += managed.size();
        }
        if (!persisted) {
            Class<?> resetType = product.loadClass("com.github.eclipsethemes.eclipse.preferences.PreferenceReset");
            Object reset = resetType.getConstructor(Function.class).newInstance((Function<String, IEclipsePreferences>) nodes::get);
            for (int i = 0; i < 2; i++) {
                Object result = resetType.getMethod("reset").invoke(reset);
                check((Boolean) result.getClass().getMethod("successful").invoke(result), "real reset succeeds, run " + i);
            }
        }
        for (var entry : nodes.entrySet()) {
            IEclipsePreferences node = entry.getValue();
            check("keep\n=\u2603".equals(node.get("r1.unrelated.text", null)), entry.getKey() + " unrelated text survives");
            check("7".equals(node.get("r1.unrelated.number", null)), entry.getKey() + " unrelated number survives");
            check(node.get("r1.absent", null) == null, entry.getKey() + " unrelated absence survives");
            check("keep child".equals(node.node("r1.child").get("key", null)), entry.getKey() + " child survives");
            for (String key : keys.get(entry.getKey())) check(node.get(key, null) == null, entry.getKey() + "/" + key + " absent");
            if (!persisted) check("lower scope".equals(DefaultScope.INSTANCE.getNode(entry.getKey())
                    .get(keys.get(entry.getKey()).iterator().next(), null)), entry.getKey() + " lower scope survives");
        }
        System.out.println("RESET managed=" + count + " nodes=" + nodes.size() + " persisted=" + persisted);
    }

    @Override public void stop() {}
}
