package com.github.eclipsethemes.eclipse.preferences;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.github.eclipsethemes.core.models.PackageMetadata;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.eclipse.ThemeRecord;

/** The details pane never turns theme metadata into link markup, a launch or a preview request. */
public class ThemeWebsitePresentationTest {

	@Test public void launchableValuesBecomeLinkMarkup() {
		assertEquals("<a>https://example.com</a>",
				EclipseThemesPreferencePage.websiteText("https://example.com"));
		assertEquals("Surrounding whitespace is removed from the visible link",
				"<a>https://example.com</a>",
				EclipseThemesPreferencePage.websiteText("  https://example.com  "));
	}

	@Test public void unusableValuesStayInertLiteralText() {
		assertEquals("ftp://example.com", EclipseThemesPreferencePage.websiteText("ftp://example.com"));
		assertEquals("javascript:alert(1)", EclipseThemesPreferencePage.websiteText("javascript:alert(1)"));
		assertEquals("example.com", EclipseThemesPreferencePage.websiteText("example.com"));
	}

	@Test public void markupInsideMetadataIsEscapedInsteadOfRendered() {
		String text = EclipseThemesPreferencePage.websiteText("https://example.com/<script>");

		assertEquals("https://example.com/&lt;script&gt;", text);
		assertFalse("Metadata never becomes link markup: " + text, text.contains("<a>"));
	}

	@Test public void absentOrBlankValuesShowThePlaceholder() {
		assertEquals("N/A", EclipseThemesPreferencePage.websiteText(null));
		assertEquals("N/A", EclipseThemesPreferencePage.websiteText(""));
		assertEquals("N/A", EclipseThemesPreferencePage.websiteText("   "));
	}

	@Test public void packageSourceFollowsTheWebsiteLinkPolicy() {
		String notStated = EclipseThemesPreferencePage.NOT_STATED;
		assertEquals("<a>https://github.com/x</a>",
				EclipseThemesPreferencePage.linkText("https://github.com/x", notStated));
		assertEquals("file:///etc/passwd", EclipseThemesPreferencePage.linkText("file:///etc/passwd", notStated));
		assertEquals(notStated, EclipseThemesPreferencePage.linkText(null, notStated));
	}

	@Test public void absentProvenanceIsShownAsNotStated() {
		var metadata = new PackageMetadata("io.example.theme", null, null, null, null);
		assertEquals("Not stated", EclipseThemesPreferencePage.stated(metadata.licenseText()));
		assertEquals("Not stated", EclipseThemesPreferencePage.stated(metadata.copyrightText()));
		assertEquals("MIT", EclipseThemesPreferencePage.stated(
				new PackageMetadata("io.example.theme", null, "MIT", null, null).licenseText()));
	}

	@Test public void packagesNeverProduceAPreviewRequest() {
		Theme packaged = new Theme(null, "Nightfall", "", "", "", ThemeType.DARK, Map.of(),
				new PackageMetadata("io.example.nightfall", null, null, null, null), Map.of());
		ThemeRecord record = ThemeRecord.installed("local:x", "x.ectheme", packaged);
		assertTrue(EclipseThemesPreferencePage.previewUrl(record).isEmpty());
	}
}
