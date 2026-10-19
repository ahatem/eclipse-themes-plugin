package com.github.eclipsethemes.eclipse.workbench;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.workbench.WorkbenchColor;
import com.github.eclipsethemes.core.workbench.WorkbenchFamily;
import com.github.eclipsethemes.core.workbench.WorkbenchSlots;

/**
 * Keeps the Java mapping, {@code plugin.xml} and the two static CSS files in step. Surefire runs with the
 * bundle directory as the working directory.
 */
public class WorkbenchContributionConsistencyTest {

	private static final Path BUNDLE = Paths.get(".");
	private static final Set<String> PLATFORM_REFERENCES = Set.of("org-eclipse-ui-workbench-TAB_TEXT_FONT",
			"org-eclipse-ui-workbench-TREE_TABLE_FONT", "org-eclipse-ui-workbench-FORM_HEADING_ERROR_COLOR",
			"org-eclipse-ui-workbench-FORM_HEADING_WARNING_COLOR", "org-eclipse-ui-workbench-FORM_HEADING_INFO_COLOR",
			"org-eclipse-debug-ui-VariableTextFont");
	/** The splash-screen rules copied from the stock themes; every other color is a palette role. */
	private static final Set<String> SPLASH_LITERALS = Set.of("#9c9696", "#e1e1e1");

	@Test
	public void pluginXmlDeclaresTheFourThemesWithTheirFamilyStylesheet() throws Exception {
		Document plugin = pluginXml();
		Map<String, String> themes = new LinkedHashMap<>();
		for (Element ext : extensions(plugin, "org.eclipse.e4.ui.css.swt.theme")) {
			NodeList list = ext.getElementsByTagName("theme");
			for (int i = 0; i < list.getLength(); i++) {
				Element theme = (Element) list.item(i);
				themes.put(theme.getAttribute("id"), theme.getAttribute("basestylesheeturi"));
			}
			// Only the two family sheets: any other sheet bound to our ids escapes the checks below.
			assertEquals(0, ext.getElementsByTagName("stylesheet").getLength());
		}
		assertEquals(new HashSet<>(WorkbenchSlots.ownThemeIds()), themes.keySet());
		for (WorkbenchFamily family : WorkbenchFamily.values()) {
			String css = "css/workbench-" + family.key() + ".css";
			assertEquals(css, themes.get(family.themeId()));
			assertEquals(css, themes.get(family.alternateThemeId()));
			assertTrue(css, Files.isRegularFile(BUNDLE.resolve(css)));
		}
	}

	@Test
	public void colorDefinitionsAreNonEditableWithTheFamilyFallbackValues() throws Exception {
		Map<String, Element> definitions = new LinkedHashMap<>();
		for (Element ext : extensions(pluginXml(), "org.eclipse.ui.themes")) {
			NodeList list = ext.getElementsByTagName("colorDefinition");
			for (int i = 0; i < list.getLength(); i++) {
				Element def = (Element) list.item(i);
				definitions.put(def.getAttribute("id"), def);
			}
		}
		Set<String> expected = new HashSet<>();
		for (WorkbenchFamily family : WorkbenchFamily.values()) {
			for (WorkbenchColor role : WorkbenchColor.values()) {
				String id = role.colorId(family);
				expected.add(id);
				Element def = definitions.get(id);
				assertTrue("missing colorDefinition " + id, def != null);
				assertEquals(id, "false", def.getAttribute("isEditable"));
				Color fallback = family.fallbackPalette().get(role);
				assertEquals(id, fallback.toEclipseFormat(), def.getAttribute("value"));
			}
		}
		assertEquals(expected, definitions.keySet());
	}

	@Test
	public void eachCssFileReadsOnlyItsOwnFamilyAndEveryRole() throws Exception {
		for (WorkbenchFamily family : WorkbenchFamily.values()) {
			String css = Files.readString(BUNDLE.resolve("css/workbench-" + family.key() + ".css"), StandardCharsets.UTF_8);
			Set<String> referenced = new HashSet<>();
			Set<String> platform = new HashSet<>();
			Matcher matcher = Pattern.compile("'#([A-Za-z0-9_-]+)'").matcher(css);
			while (matcher.find()) {
				String id = matcher.group(1);
				(id.startsWith("com-github-eclipsethemes-workbench-") ? referenced : platform).add(id);
			}
			Set<String> expected = new HashSet<>();
			for (WorkbenchColor role : WorkbenchColor.values()) {
				expected.add(role.cssId(family));
			}
			assertEquals(family.key(), expected, referenced);
			// Platform definitions read the same way the stock Dark and Light sheets read them (fonts, form headings).
			assertEquals(family.key(), PLATFORM_REFERENCES, platform);
			// Preference and color-definition rules make Eclipse write into other plug-ins' preferences and delete
			// user values on the next theme change (evidence/runtime/rejected-definitions-sheet), so neither may appear.
			assertFalse(family.key(), withoutComments(css).contains("IEclipsePreferences"));
			assertFalse(family.key(), withoutComments(css).contains("ColorDefinition"));
		}
	}

	@Test
	public void noColorIsHardCodedOutsideTheSplashRules() throws Exception {
		for (WorkbenchFamily family : WorkbenchFamily.values()) {
			String css = withoutComments(
					Files.readString(BUNDLE.resolve("css/workbench-" + family.key() + ".css"), StandardCharsets.UTF_8));
			Set<String> literals = new HashSet<>();
			Matcher matcher = Pattern.compile("(?<![\\w'#-])(#[0-9a-fA-F]{3,8})(?![\\w-])").matcher(css);
			while (matcher.find()) {
				literals.add(matcher.group(1).toLowerCase());
			}
			assertEquals(family.key() + " " + literals, SPLASH_LITERALS, literals);
			assertFalse(family.key(), css.matches("(?s).*(rgb|hsl)a?\\(.*"));
		}
	}

	@Test
	public void theTwoFamilySheetsAreIdenticalApartFromTheFamilyName() throws Exception {
		String dark = Files.readString(BUNDLE.resolve("css/workbench-dark.css"), StandardCharsets.UTF_8);
		String light = Files.readString(BUNDLE.resolve("css/workbench-light.css"), StandardCharsets.UTF_8);
		assertEquals(withoutComments(dark), withoutComments(light.replace("-workbench-light-", "-workbench-dark-")));
	}

	@Test
	public void startupExtensionAndCssAreIncludedInTheBuild() throws Exception {
		boolean startup = false;
		for (Element ext : extensions(pluginXml(), "org.eclipse.ui.startup")) {
			NodeList list = ext.getElementsByTagName("startup");
			for (int i = 0; i < list.getLength(); i++) {
				startup |= WorkbenchStartup.class.getName().equals(((Element) list.item(i)).getAttribute("class"));
			}
		}
		assertTrue(startup);
		List<String> build = Files.readAllLines(BUNDLE.resolve("build.properties"));
		assertTrue(build.stream().anyMatch(line -> line.trim().startsWith("css/")));
		assertTrue(Files.readString(BUNDLE.resolve("META-INF/MANIFEST.MF"))
				.contains("org.eclipse.e4.ui.css.swt.theme;resolution:=optional"));
	}

	private static String withoutComments(String css) {
		return css.replaceAll("(?s)/[*].*?[*]/", "");
	}

	private static Document pluginXml() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		return factory.newDocumentBuilder().parse(BUNDLE.resolve("plugin.xml").toFile());
	}

	private static List<Element> extensions(Document plugin, String point) {
		List<Element> result = new java.util.ArrayList<>();
		NodeList list = plugin.getElementsByTagName("extension");
		for (int i = 0; i < list.getLength(); i++) {
			Element element = (Element) list.item(i);
			if (point.equals(element.getAttribute("point"))) {
				result.add(element);
			}
		}
		return result;
	}
}
