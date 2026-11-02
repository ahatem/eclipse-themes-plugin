package com.github.eclipsethemes.core.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;

import com.github.eclipsethemes.core.models.PackageMetadata;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.workbench.WorkbenchColor;
import com.github.eclipsethemes.core.workbench.WorkbenchPaletteResolver;

/** The shipped packages are complete, and those that replaced legacy themes keep their names, appearances and identities. */
public class BundledPackageTest {

    @Test public void everyBundledPackageIsCompleteAndKeepsItsIdentity() throws Exception {
        Properties identities = new Properties();
        try (var in = Files.newInputStream(ParserTestSupport.themes().resolve("bundled-identities.properties"))) {
            identities.load(in);
        }
        // Stored selections reference bundled:<ect-id>, and the identity is found by package id and theme name.
        Map<String, Theme> legacyByName = new HashMap<>();
        try (var files = Files.list(ParserTestSupport.legacyThemes())) {
            for (Path file : files.toList()) {
                Theme legacy = new XmlThemeParser().parse(file.toFile());
                legacyByName.put(legacy.getName(), legacy);
            }
        }
        var roles = Arrays.stream(WorkbenchColor.values()).map(WorkbenchColor::schemaName).collect(Collectors.toSet());
        var packageIds = new HashSet<String>();
        try (var files = Files.list(ParserTestSupport.themes())) {
            for (Path file : files.filter(p -> p.toString().endsWith(".ectheme")).sorted().toList()) {
                String base = file.getFileName().toString().replaceFirst("[.]ectheme$", "");
                Theme theme = new ThemePackageParser().parse(file.toFile());

                PackageMetadata metadata = theme.getPackageMetadata().orElseThrow();
                assertEquals(base, "com.github.eclipsethemes." + base.toLowerCase(Locale.ROOT).replace('_', '-'),
                        metadata.packageId());
                assertTrue(base, packageIds.add(metadata.packageId()));
                String identity = identities.getProperty(metadata.packageId());
                Theme legacy = legacyByName.remove(theme.getName());
                if (legacy != null) {
                    assertEquals(base, legacy.getType(), theme.getType());
                    assertEquals(base, legacy.getId(), identity);
                } else {
                    // A theme added later gets a new id of the same form.
                    assertEquals(base, identity, UUID.fromString(identity).toString());
                }

                assertEquals(base, roles, theme.getAuthoredWorkbench().keySet());
                // Authored colors are shown as written: none needed a readability correction.
                assertEquals(base, Set.of(), WorkbenchPaletteResolver.unreadableAuthoredRoles(theme));
                // A theme with an upstream names it and its license; one without says why in its description.
                if (metadata.sourceText().isPresent()) {
                    assertTrue(base, metadata.licenseText().isPresent());
                    assertTrue(base, metadata.copyrightText().isPresent());
                } else {
                    assertFalse(base, theme.getDescription() == null || theme.getDescription().isBlank());
                }
            }
        }
        assertEquals("Legacy themes without a package", Map.of(), legacyByName);
        assertEquals(56, packageIds.size());
        assertEquals(packageIds, identities.stringPropertyNames());
        assertEquals(56, new HashSet<>(identities.values()).size());
    }
}
