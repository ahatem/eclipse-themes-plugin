package com.github.eclipsethemes.eclipse;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ThemeRepositoryImportTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void failedExclusiveCopyLeavesNoBindingOrDestinationAndAllowsRetry() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path blockedParent = Files.writeString(directory.resolve("installed_themes"), "not a directory");
        Path source = Files.writeString(directory.resolve("incoming.xml"), "<colorTheme name='Incoming'/>");

        var repository = new ThemeRepository(directory.toFile(), quietLog());
        assertEquals(List.of(), repository.getAllThemes());
        assertFalse("The injected copy failure is reported as failure",
                repository.importTheme(source.toFile()).isPresent());
        assertFalse("No binding may outlive a failed copy",
                Files.exists(directory.resolve("theme-identities.properties")));
        assertFalse("No destination remains after the handled failure",
                Files.exists(blockedParent.resolve("incoming.xml")));
        assertTrue("The failed attempt must not admit a catalog record", repository.getAllThemes().isEmpty());

        // The failed attempt left no reservation: once the obstruction is removed, a retry admits normally.
        Files.delete(blockedParent);
        Files.createDirectory(blockedParent);
        var admitted = repository.importTheme(source.toFile());
        assertTrue(admitted.isPresent());
        assertTrue(admitted.get().getLogicalId().startsWith("local:"));
        assertEquals(List.of(admitted.get()), repository.getAllThemes());
        assertTrue(Files.readString(directory.resolve("theme-identities.properties")).contains("incoming.xml="));
        assertTrue(Files.exists(blockedParent.resolve("incoming.xml")));
    }

    @Test public void failedBindingAfterCopyRemovesOnlyNewDestinationAndPreservesPriorState() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path installed = Files.createDirectory(directory.resolve("installed_themes"));
        Path existing = Files.writeString(installed.resolve("existing.xml"), "<colorTheme name='Existing'/>");
        Path bindingsFile = directory.resolve("theme-identities.properties");
        var seeded = new ThemeIdentityBindings(bindingsFile);
        String priorId = seeded.identityFor("existing.xml");
        byte[] bindingsBefore = Files.readAllBytes(bindingsFile);
        byte[] existingBefore = Files.readAllBytes(existing);
        Path source = Files.writeString(directory.resolve("incoming.xml"), "<colorTheme name='Incoming'/>");

        var failing = new ThemeIdentityBindings(bindingsFile) {
            @Override void replace(Path staged) throws IOException {
                assertTrue("A complete staged write precedes replacement", Files.size(staged) > 0);
                throw new IOException("injected metadata replacement failure");
            }
        };
        var repository = new ThemeRepository(directory.toFile(), quietLog(), failing);
        assertFalse(repository.importTheme(source.toFile()).isPresent());

        assertFalse("Only this attempt's destination is removed", Files.exists(installed.resolve("incoming.xml")));
        assertArrayEquals("Pre-existing managed XML is unchanged", existingBefore, Files.readAllBytes(existing));
        assertArrayEquals("Prior bindings are unchanged", bindingsBefore, Files.readAllBytes(bindingsFile));
        assertTrue("No catalog entry is admitted", repository.getAllThemes().isEmpty());
        assertEquals(priorId, new ThemeIdentityBindings(bindingsFile).identityFor("existing.xml"));
        try (var files = Files.list(installed)) {
            assertEquals("No partial destination remains", List.of("existing.xml"),
                    files.map(path -> path.getFileName().toString()).sorted().toList());
        }
    }

    private static ILog quietLog() {
        return (ILog) Proxy.newProxyInstance(ILog.class.getClassLoader(), new Class<?>[] { ILog.class },
                (proxy, method, args) -> null);
    }
}
