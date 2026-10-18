package com.github.eclipsethemes.eclipse.workbench;

import static org.junit.Assert.assertEquals;
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
			Matcher matcher = Pattern.compile("'#([A-Za-z0-9-]+)'").matcher(css);
			while (matcher.find()) {
				referenced.add(matcher.group(1));
			}
			Set<String> expected = new HashSet<>();
			for (WorkbenchColor role : WorkbenchColor.values()) {
				expected.add(role.cssId(family));
			}
			assertEquals(family.key(), expected, referenced);
		}
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
