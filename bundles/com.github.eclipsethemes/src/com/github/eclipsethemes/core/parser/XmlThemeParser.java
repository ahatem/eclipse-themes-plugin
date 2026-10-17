package com.github.eclipsethemes.core.parser;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.core.exceptions.ThemeParseException;
import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.ThemeType;
import com.github.eclipsethemes.core.models.TokenBuilder;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenOptions;
import com.github.eclipsethemes.core.parser.mapper.LegacyEclipsePropertyMapper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.SAXException;
import java.io.File;
import java.io.InputStream;
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XmlThemeParser implements ThemeParser {

	private final Supplier<DocumentBuilderFactory> factorySupplier;
	private final Consumer<String> mappingWarning;

	public XmlThemeParser() {
		this(message -> {});
	}

	/** Internal loading-boundary sink; no per-parse state is retained on the parser. */
	public XmlThemeParser(Consumer<String> mappingWarning) {
		this(DocumentBuilderFactory::newInstance, mappingWarning);
	}

	XmlThemeParser(Supplier<DocumentBuilderFactory> factorySupplier) {
		this(factorySupplier, message -> {});
	}

	private XmlThemeParser(Supplier<DocumentBuilderFactory> factorySupplier, Consumer<String> mappingWarning) {
		this.factorySupplier = factorySupplier;
		this.mappingWarning = java.util.Objects.requireNonNull(mappingWarning);
	}

	private record Candidate(String source, int occurrence, Token token) {
		boolean canonical() { return source.equals(token.getTokenName()); }
		String location() { return source + "#" + occurrence; }
	}

	/**
	 * @param inputStream The stream to parse.
	 * @param sourceFile  The original file, which can be null.
	 */
	public Theme parse(InputStream inputStream, File sourceFile) throws ThemeParseException {
		try {
			DocumentBuilder builder = SecureXmlBuilder.create(factorySupplier);
			Document document = builder.parse(inputStream);

			Element root = document.getDocumentElement();
			String id = root.getAttribute("ect-id");
			String name = root.getAttribute("name");
			String author = root.getAttribute("author");
			String website = root.getAttribute("website");
			String description = root.getAttribute("description");
			String type = root.getAttribute("type");

			Theme theme = new Theme(id, name, author, website, description, sourceFile, ThemeType.from(type));

			// Pre-populate root tokens so Theme.get() always has a fallback via inheritance.
			// These are overwritten if the XML defines them explicitly.
			theme.addToken(new TokenBuilder().setKey(TokenKey.BACKGROUND).setColor(Color.ofRgb(30, 30, 30)).build());
			theme.addToken(new TokenBuilder().setKey(TokenKey.FOREGROUND).setColor(Color.ofRgb(212, 212, 212)).build());

			NodeList colorNodes = root.getChildNodes();
			Map<TokenKey, List<Candidate>> candidates = new LinkedHashMap<>();
			int occurrence = 0;
			for (int i = 0; i < colorNodes.getLength(); i++) {
				Node node = colorNodes.item(i);
				if (node.getNodeType() != Node.ELEMENT_NODE) continue;
				Element element = (Element) node;
				occurrence++;

				String newTagName = LegacyEclipsePropertyMapper.mapProperty(element.getTagName());
				TokenKey key = TokenKey.byId(newTagName);

				if (key == null) {
					EclipseThemes.instance().getLogger()
							.info("Could not match any key with tag = " + element.getTagName());
					continue;
				}

				TokenBuilder tokenBuilder = new TokenBuilder().setKey(key);
				String colorAttr = element.getAttribute("color");
				if (colorAttr == null || colorAttr.isBlank()) {
					continue;
				}

				try {
					tokenBuilder.setColor(Color.ofHex(colorAttr));
				} catch (IllegalArgumentException e) {
					EclipseThemes.instance().getLogger()
							.warn("Invalid color '" + colorAttr + "' for tag " + element.getTagName());
					continue;
				}

				if (element.hasAttribute("bold")) {
					tokenBuilder.setBold(Boolean.parseBoolean(element.getAttribute("bold")));
				}

				if (element.hasAttribute("italic")) {
					tokenBuilder.setItalic(Boolean.parseBoolean(element.getAttribute("italic")));
				}

				if (element.hasAttribute("underline")) {
					tokenBuilder.setUnderline(Boolean.parseBoolean(element.getAttribute("underline")));
				}

				if (element.hasAttribute("strikethrough")) {
					tokenBuilder.setStrikethrough(Boolean.parseBoolean(element.getAttribute("strikethrough")));
				}

				candidates.computeIfAbsent(key, unused -> new ArrayList<>())
						.add(new Candidate(element.getTagName(), occurrence, tokenBuilder.build()));
			}
			for (var entry : candidates.entrySet()) {
				List<Candidate> accepted = entry.getValue();
				Candidate winner = accepted.get(0);
				for (Candidate candidate : accepted) {
					if (candidate.canonical() || !winner.canonical()) winner = candidate;
				}
				theme.addToken(winner.token());
				for (Candidate candidate : accepted) {
					if (!candidate.token().getColor().equals(winner.token().getColor())
						|| !candidate.token().getOptions().orElse(TokenOptions.EMPTY)
							.equals(winner.token().getOptions().orElse(TokenOptions.EMPTY))) {
						mappingWarning.accept("Theme mapping conflict for role " + entry.getKey().getName()
								+ ": " + candidate.location() + " versus " + winner.location()
								+ "; selected " + winner.location());
					}
				}
			}
			return theme;
		} catch (ThemeParseException e) {
			throw e;
		} catch (SAXException e) {
			throw new ThemeParseException("Failed to parse theme XML: " + e.getMessage(), e);
		} catch (Exception e) {
			throw new ThemeParseException("Failed to parse theme from stream", e);
		}
	}

}
