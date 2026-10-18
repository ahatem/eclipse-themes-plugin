package com.github.eclipsethemes.eclipse;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;

import com.github.eclipsethemes.EclipseThemes;

/** Test-only repository fixtures; no production API or global lifecycle. */
final class RepositoryTestSupport {
    private RepositoryTestSupport() {}

    static ILog quietLog() {
        return (ILog) Proxy.newProxyInstance(ILog.class.getClassLoader(), new Class<?>[] { ILog.class },
                (proxy, method, args) -> null);
    }

    /** Records the message of every logged finding so tests can assert what was reported. */
    static ILog recordingLog(List<String> messages) {
        Objects.requireNonNull(messages, "messages");
        return (ILog) Proxy.newProxyInstance(ILog.class.getClassLoader(), new Class<?>[] { ILog.class },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (args != null && args.length > 0
                            && (name.equals("error") || name.equals("warn") || name.equals("info"))) {
                        messages.add(name + ": " + args[0]);
                    }
                    return null;
                });
    }

    static ActivatorStandIn installActivator() throws Exception {
        return installActivator(quietLog());
    }

    static ActivatorStandIn installActivator(ILog log) throws Exception {
        Field singleton = EclipseThemes.class.getDeclaredField("instance");
        singleton.setAccessible(true);
        Field logger = EclipseThemes.class.getDeclaredField("pluginLog");
        logger.setAccessible(true);
        Object previous = singleton.get(null);
        EclipseThemes standIn = new EclipseThemes();
        logger.set(standIn, Objects.requireNonNull(log, "test logger"));
        // Publish last: failed acquisition above cannot change global state.
        ActivatorStandIn fixture = new ActivatorStandIn(singleton, previous);
        singleton.set(null, standIn);
        return fixture;
    }

    static void installBundled(List<URL> resources) throws Exception {
        Objects.requireNonNull(EclipseThemes.instance(), "installActivator must run first");
        Field field = Plugin.class.getDeclaredField("bundle");
        field.setAccessible(true);
        Bundle bundle = (Bundle) Proxy.newProxyInstance(Bundle.class.getClassLoader(), new Class<?>[] { Bundle.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("findEntries")) return Collections.enumeration(resources);
                    throw new AssertionError(method);
                });
        field.set(EclipseThemes.instance(), bundle);
    }

    static List<String> fileNames(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return files.filter(path -> !path.getFileName().toString().equals(".staging"))
                    .map(path -> path.getFileName().toString()).sorted().toList();
        }
    }

    static List<String> stagedPieces(Path installed) throws IOException {
        Path staging = installed.resolve(".staging");
        return Files.isDirectory(staging) ? fileNames(staging) : List.of();
    }

    static String establishedIdentity(Path directory, String locator) throws IOException {
        return new ThemeIdentityBindings(directory.resolve("theme-identities.properties"))
                .establishedIdentityFor(locator).orElse(null);
    }

    static Path bindingsFile(Path directory) {
        return directory.resolve("theme-identities.properties");
    }

    static final class ActivatorStandIn implements AutoCloseable {
        private final Field singleton;
        private final Object previous;

        private ActivatorStandIn(Field singleton, Object previous) {
            this.singleton = singleton;
            this.previous = previous;
        }

        @Override public void close() throws IllegalAccessException {
            singleton.set(null, previous);
        }
    }
}
