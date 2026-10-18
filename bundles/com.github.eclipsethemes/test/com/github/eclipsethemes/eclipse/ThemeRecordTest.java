package com.github.eclipsethemes.eclipse;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenKey;

/** Catalog record coordinates: explicit origin, locator rule, identity equality. */
public class ThemeRecordTest {

	@Test public void bundledRecordCarriesOriginAndNoManagedLocator() {
		ThemeRecord record = ThemeRecord.bundled("bundled:curated", theme("Curated", "#112233"));

		assertEquals(ThemeOrigin.BUNDLED, record.origin());
		assertEquals("bundled:curated", record.logicalId());
		assertTrue(record.locator().isEmpty());
		assertEquals("Curated", record.theme().getName());
	}

	@Test public void installedRecordCarriesOriginAndManagedLocator() {
		ThemeRecord record = ThemeRecord.installed("local:generated", "generated.xml", theme("Imported", "#334455"));

		assertEquals(ThemeOrigin.INSTALLED, record.origin());
		assertEquals("local:generated", record.logicalId());
		assertEquals(Optional.of("generated.xml"), record.locator());
		assertEquals("Imported", record.theme().getName());
	}

	@Test public void equalityUsesLogicalIdentityOnly() {
		ThemeRecord admitted = ThemeRecord.installed("local:established", "first.xml", theme("Old", "#112233"));
		ThemeRecord replaced = ThemeRecord.installed("local:established", "first.xml", theme("Renamed", "#445566"));
		ThemeRecord other = ThemeRecord.installed("local:other", "second.xml", theme("Old", "#112233"));

		assertEquals(admitted, replaced);
		assertEquals(admitted.hashCode(), replaced.hashCode());
		assertNotEquals(admitted, other);
		assertEquals("One catalog entry per logical identity", 1, new HashSet<>(java.util.List.of(admitted, replaced)).size());
		assertEquals("Duplicate display and source claims stay independent", 2,
				new HashSet<>(java.util.List.of(admitted, other)).size());
	}

	@Test public void invalidCoordinatesAreRejected() {
		Theme theme = theme("Name", "#112233");

		assertThrows("Logical identity is required", IllegalArgumentException.class,
				() -> ThemeRecord.bundled(" ", theme));
		assertThrows("A bundled record cannot claim managed storage", IllegalArgumentException.class,
				() -> new ThemeRecord("bundled:x", ThemeOrigin.BUNDLED, Optional.of("x.xml"), theme));
		assertThrows("An installed record must name its managed file", IllegalArgumentException.class,
				() -> new ThemeRecord("local:x", ThemeOrigin.INSTALLED, Optional.empty(), theme));
		assertThrows("An installed record must name its managed file", IllegalArgumentException.class,
				() -> ThemeRecord.installed("local:x", " ", theme));
		assertThrows(NullPointerException.class, () -> ThemeRecord.bundled("bundled:x", null));
	}

	private static Theme theme(String name, String background) {
		return new Theme("source", name, "", "", "", ThemeType.LIGHT,
				Map.of(TokenKey.BACKGROUND, new Token(TokenKey.BACKGROUND, Color.ofHex(background), null)));
	}
}
