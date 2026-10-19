package com.github.eclipsethemes.core.parser;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Plugin;
import org.junit.Test;
import org.osgi.framework.Bundle;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.eclipse.ThemeRepository;

public class RepositoryMappingWarningTest {
    @Test public void bundledInstalledAndImportRoutesEmitWarningsAndReturnAcceptedThemes() throws Exception {
        var directory = Files.createTempDirectory("mapping-repository-");
        var source = directory.resolve("source.xml");
        Files.writeString(source, "<colorTheme name='Conflict'><deprecated color='#123456'/>"
                + "<deprecatedMember color='#abcdef' bold='true'/></colorTheme>");
        List<String> warnings = new ArrayList<>();
        ILog log = (ILog) Proxy.newProxyInstance(ILog.class.getClassLoader(), new Class<?>[] {ILog.class},
                (proxy, method, args) -> { if (method.getName().equals("warn")) warnings.add((String) args[0]); return null; });
        Field bundleField = Plugin.class.getDeclaredField("bundle");
        bundleField.setAccessible(true);
        try (var logger = ParserTestSupport.installLogger(ParserTestSupport.quietLog())) {
            var repository = new ThemeRepository(directory.toFile(), log);
            assertTrue(repository.importTheme(source.toFile()).successful());
            assertEquals(1, warnings.size());
            var bundle = (Bundle) Proxy.newProxyInstance(Bundle.class.getClassLoader(), new Class<?>[] {Bundle.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("findEntries")) return Collections.enumeration(List.of(source.toUri().toURL()));
                        throw new AssertionError(method);
                    });
            bundleField.set(EclipseThemes.instance(), bundle);
            warnings.clear();
            repository.reload();
            assertEquals(1, repository.getAllThemes().size());
            assertEquals(2, warnings.stream().filter(w -> w.startsWith("Theme mapping conflict")).count());
            for (String warning : warnings.stream().filter(w -> w.startsWith("Theme mapping conflict")).toList()) {
                assertTrue(warning.contains("deprecatedMember#2"));
                assertTrue(warning.contains("selected deprecated#1"));
            }
        } finally {
            try (var files = Files.walk(directory)) {
                for (var file : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
    }
}
