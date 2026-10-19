package com.github.eclipsethemes.eclipse;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ThemeIdentityBindingsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void reconstructionReusesPersistedIdsWithoutSourceOrContentInputs() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("identities.properties");
        String first = new ThemeIdentityBindings(file).identityFor("a.xml");
        String second = new ThemeIdentityBindings(file).identityFor("b.xml");
        assertTrue(first.startsWith("local:"));
        assertNotEquals(first, second);
        assertEquals(first, new ThemeIdentityBindings(file).identityFor("a.xml"));
        assertEquals(second, new ThemeIdentityBindings(file).identityFor("b.xml"));
    }

    @Test public void failedReplacementPreservesOldBytesAndDoesNotReturnGeneratedIdentity() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("identities.properties");
        String old = new ThemeIdentityBindings(file).identityFor("old.xml");
        byte[] before = Files.readAllBytes(file);
        var failing = new ThemeIdentityBindings(file) {
            @Override void replace(Path staged) throws IOException {
                assertTrue("A complete staged write precedes replacement", Files.size(staged) > 0);
                throw new IOException("injected metadata replacement failure");
            }
        };
        for (int attempt = 0; attempt < 2; attempt++) {
            assertThrows(IOException.class, () -> failing.identityFor("new.xml"));
            assertArrayEquals(before, Files.readAllBytes(file));
            assertEquals(old, failing.identityFor("old.xml"));
        }
        try (var files = Files.list(file.getParent())) {
            assertEquals("No failed staged data becomes a live binding", 1, files.count());
        }
        var reconstructed = new ThemeIdentityBindings(file);
        assertEquals(old, reconstructed.identityFor("old.xml"));
        assertNotEquals(old, reconstructed.identityFor("new.xml"));
    }

    @Test public void establishedLookupNeverGeneratesIdentityOrBindingData() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("identities.properties");
        var bindings = new ThemeIdentityBindings(file);
        assertTrue(bindings.establishedIdentityFor("a.xml").isEmpty());
        assertFalse("A read-only lookup must not create binding storage", Files.exists(file));
        String identity = bindings.newIdentityFor("a.xml");
        assertEquals(identity, bindings.establishedIdentityFor("a.xml").orElseThrow());
        assertTrue(bindings.establishedIdentityFor("b.xml").isEmpty());
    }

    @Test public void newIdentityRefusesReservedLocatorWithoutTouchingBindings() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("identities.properties");
        var bindings = new ThemeIdentityBindings(file);
        String established = bindings.newIdentityFor("a.xml");
        byte[] before = Files.readAllBytes(file);
        assertThrows(IOException.class, () -> bindings.newIdentityFor("a.xml"));
        assertArrayEquals(before, Files.readAllBytes(file));
        assertEquals(established, bindings.establishedIdentityFor("a.xml").orElseThrow());
    }

    @Test public void removalRequiresEstablishedExactBindingAndLeavesOtherBindingsAlone() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("identities.properties");
        var bindings = new ThemeIdentityBindings(file);
        String removed = bindings.newIdentityFor("a.xml");
        String retained = bindings.newIdentityFor("b.xml");
        assertThrows(IOException.class, () -> bindings.remove("a.xml", "local:different"));
        assertEquals(removed, bindings.establishedIdentityFor("a.xml").orElseThrow());
        assertThrows(IOException.class, () -> bindings.remove("missing.xml", removed));
        bindings.remove("a.xml", removed);
        assertTrue(bindings.establishedIdentityFor("a.xml").isEmpty());
        assertEquals(retained, bindings.establishedIdentityFor("b.xml").orElseThrow());
        assertThrows("Removal is not silently repeatable", IOException.class,
                () -> bindings.remove("a.xml", removed));
    }

    @Test public void restoreReestablishesExactBindingAndRejectsConflictingIdentity() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("identities.properties");
        var bindings = new ThemeIdentityBindings(file);
        String identity = bindings.newIdentityFor("a.xml");
        String other = bindings.newIdentityFor("b.xml");
        bindings.remove("a.xml", identity);
        bindings.restore("a.xml", identity);
        assertEquals(identity, bindings.establishedIdentityFor("a.xml").orElseThrow());
        assertThrows(IOException.class, () -> bindings.restore("a.xml", "local:conflict"));
        assertEquals(identity, bindings.establishedIdentityFor("a.xml").orElseThrow());
        assertEquals(other, bindings.establishedIdentityFor("b.xml").orElseThrow());
    }

    @Test public void malformedOrConflictingBindingsAreNotRegeneratedOver() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("identities.properties");
        Files.writeString(file, "bad=\\uQQQQ\n");
        byte[] before = Files.readAllBytes(file);
        assertThrows(IOException.class, () -> new ThemeIdentityBindings(file).identityFor("bad"));
        assertArrayEquals(before, Files.readAllBytes(file));
        Files.writeString(file, "a.xml=local:duplicate\nb.xml=local:duplicate\nc.xml=local:unaffected\n");
        before = Files.readAllBytes(file);
        assertThrows(IOException.class, () -> new ThemeIdentityBindings(file).identityFor("a.xml"));
        assertEquals("local:unaffected", new ThemeIdentityBindings(file).identityFor("c.xml"));
        assertArrayEquals(before, Files.readAllBytes(file));
    }
}
