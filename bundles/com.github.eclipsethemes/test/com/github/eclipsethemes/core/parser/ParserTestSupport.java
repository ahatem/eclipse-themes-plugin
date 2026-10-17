package com.github.eclipsethemes.core.parser;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.core.runtime.ILog;
import org.junit.rules.ExternalResource;
import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.models.Theme;

/** Test-only inputs and reversible warning-path lifecycle; no production API. */
final class ParserTestSupport {
    private ParserTestSupport() {}

    static ByteArrayInputStream xml(String input) {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    static Path themes() {
        Path local = Path.of("themes");
        return Files.isDirectory(local) ? local : Path.of("bundles/com.github.eclipsethemes/themes");
    }

    static Theme bundled(String name) throws Exception {
        try (var files = Files.list(themes())) {
            for (Path file : files.sorted().toList()) {
                Theme theme = new XmlThemeParser().parse(file.toFile());
                if (theme.getName().equals(name)) return theme;
            }
        }
        throw new AssertionError("Missing theme " + name);
    }

    static ILog quietLog() {
        return (ILog) Proxy.newProxyInstance(ILog.class.getClassLoader(), new Class<?>[] { ILog.class },
                (proxy, method, args) -> null);
    }

    static LoggerStandIn installLogger(ILog log) throws Exception {
        Objects.requireNonNull(log, "test logger");
        Field singleton = EclipseThemes.class.getDeclaredField("instance");
        singleton.setAccessible(true);
        Field logger = EclipseThemes.class.getDeclaredField("pluginLog");
        logger.setAccessible(true);
        Object previous = singleton.get(null);
        EclipseThemes standIn = new EclipseThemes();
        logger.set(standIn, log);
        // Publish last: failed acquisition above cannot change global state.
        LoggerStandIn fixture = new LoggerStandIn(singleton, previous);
        singleton.set(null, standIn);
        return fixture;
    }

    static ExternalResource loggerRule() {
        return new ExternalResource() {
            private LoggerStandIn fixture;
            @Override protected void before() throws Throwable { fixture = installLogger(quietLog()); }
            @Override protected void after() {
                try { if (fixture != null) fixture.close(); }
                catch (Exception e) { throw new AssertionError("Cannot restore test activator", e); }
            }
        };
    }

    static final class LoggerStandIn implements AutoCloseable {
        private final Field singleton;
        private final Object previous;
        private LoggerStandIn(Field singleton, Object previous) { this.singleton = singleton; this.previous = previous; }
        @Override public void close() throws IllegalAccessException { singleton.set(null, previous); }
    }
}
