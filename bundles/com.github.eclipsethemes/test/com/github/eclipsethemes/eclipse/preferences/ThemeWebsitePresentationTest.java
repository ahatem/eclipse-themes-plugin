package com.github.eclipsethemes.eclipse.preferences;

import static org.junit.Assert.*;

import org.junit.Test;

/** The details pane never turns theme metadata into link markup or a launch. */
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
}
