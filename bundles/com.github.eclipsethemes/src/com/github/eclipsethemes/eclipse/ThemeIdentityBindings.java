package com.github.eclipsethemes.eclipse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

/** Workspace-owned identity/locator bindings only; does not manage theme XML. */
class ThemeIdentityBindings {
    private final Path file;

    ThemeIdentityBindings(Path file) {
        this.file = file;
    }

    String identityFor(String locator) throws IOException {
        return identityFor(locator, false);
    }

    void requireUnused(String locator) throws IOException {
        if (readBindings().containsKey(locator)) {
            throw new IOException("Managed theme locator is reserved by an existing identity binding: " + locator);
        }
    }

    String newIdentityFor(String locator) throws IOException {
        return identityFor(locator, true);
    }

    private String identityFor(String locator, boolean requireNew) throws IOException {
        Properties bindings = readBindings();
        String existing = bindings.getProperty(locator);
        if (existing != null) {
            if (requireNew) {
                throw new IOException("Managed theme locator is reserved by an existing identity binding: " + locator);
            }
            if (!existing.startsWith("local:") || existing.substring(6).isBlank()
                    || bindings.values().stream().filter(existing::equals).count() != 1) {
                throw new IOException("Inconsistent theme identity binding for " + locator);
            }
            return existing;
        }

        String generated;
        do { generated = "local:" + UUID.randomUUID(); } while (bindings.containsValue(generated));
        bindings.setProperty(locator, generated);
        Path staged = Files.createTempFile(file.getParent(), "theme-identities-", ".tmp");
        try {
            try (var output = Files.newOutputStream(staged)) {
                bindings.store(output, "Eclipse Themes installed logical identities");
            }
            replace(staged);
        } finally {
            Files.deleteIfExists(staged);
        }
        // Never return a generated ID before its binding has successfully replaced the old file.
        return generated;
    }

    private Properties readBindings() throws IOException {
        Properties bindings = new Properties();
        try (var input = Files.newInputStream(file)) {
            bindings.load(input);
        } catch (NoSuchFileException e) {
            // First admission, with no previous binding data.
        } catch (IllegalArgumentException e) {
            throw new IOException("Unreadable theme identity bindings: " + file, e);
        }
        return bindings;
    }

    void replace(Path staged) throws IOException {
        // No unsafe fallback: an unsupported/failed atomic metadata replacement fails admission.
        Files.move(staged, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
