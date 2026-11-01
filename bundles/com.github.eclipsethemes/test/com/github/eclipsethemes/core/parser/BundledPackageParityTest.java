package com.github.eclipsethemes.core.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;

/** The shipped packages must be indistinguishable from the legacy documents they replaced. */
public class BundledPackageParityTest {

    @Test public void everyBundledPackageEqualsItsLegacySnapshot() throws Exception {
        Properties identities = new Properties();
        try (var in = Files.newInputStream(ParserTestSupport.themes().resolve("bundled-identities.properties"))) {
            identities.load(in);
        }
        var packageIds = new HashSet<String>();
        int checked = 0;
        try (var files = Files.list(ParserTestSupport.legacyThemes())) {
            for (Path legacyFile : files.sorted().toList()) {
                String base = legacyFile.getFileName().toString().replaceFirst("[.]xml$", "");
                Path packageFile = ParserTestSupport.themes().resolve(base + ".ectheme");
                assertTrue("Missing package for " + base, Files.exists(packageFile));

                Theme legacy = new XmlThemeParser().parse(legacyFile.toFile());
                Theme packaged = new ThemePackageParser().parse(packageFile.toFile());

                assertEquals(base, legacy.getName(), packaged.getName());
                assertEquals(base, legacy.getType(), packaged.getType());
                assertEquals(base, legacy.getAuthor(), packaged.getAuthor());
                assertEquals(base, legacy.getWebsite(), packaged.getWebsite());
                assertEquals(base, snapshot(legacy), snapshot(packaged));
                assertTrue(base, packaged.getAuthoredWorkbench().isEmpty());
                assertTrue(base, packaged.getFonts().isEmpty());

                String packageId = packaged.getPackageMetadata().orElseThrow().packageId();
                assertEquals("com.github.eclipsethemes." + base.toLowerCase(java.util.Locale.ROOT).replace('_', '-'), packageId);
                assertTrue(base, packageIds.add(packageId));
                assertEquals(base, legacy.getId(), identities.getProperty(packageId));
                checked++;
            }
        }
        assertEquals(33, checked);
        assertEquals(33, identities.stringPropertyNames().size());
        assertEquals(packageIds, identities.stringPropertyNames());
    }

    /** Token has no value equality; compare what a token means: role, color and style flags. */
    private static Map<TokenKey, List<Object>> snapshot(Theme theme) {
        Map<TokenKey, List<Object>> result = new HashMap<>();
        theme.getTokens().forEach((key, token) -> result.put(key, List.of(token.getColor(), token.getOptions())));
        return result;
    }
}
