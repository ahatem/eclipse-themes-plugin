package com.github.eclipsethemes.core.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.github.eclipsethemes.core.models.Color;
import com.github.eclipsethemes.core.models.Token;
import com.github.eclipsethemes.core.models.TokenBuilder;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.core.models.TokenOptions;
import com.github.eclipsethemes.core.parser.mapper.LegacyEclipsePropertyMapper;
import com.github.eclipsethemes.core.validation.ThemeDiagnostic;

/**
 * The legacy token rules shared by {@code <colorTheme>} children and the package {@code <editor>} section: alias
 * mapping, canonical precedence, last-wins at equal rank, colour grammar, style flags and their warnings.
 */
final class LegacyTokenInterpreter {

	private final String source;
	private final Consumer<ThemeDiagnostic> sink;

	LegacyTokenInterpreter(String source, Consumer<ThemeDiagnostic> sink) {
		this.source = source;
		this.sink = sink;
	}

	private record Candidate(String source, int occurrence, Token token) {
		boolean canonical() { return source.equals(token.getTokenName()); }
		String location() { return source + "#" + occurrence; }
	}

	/** Interprets the direct child elements of {@code container} and puts each resolved token into {@code tokens}. */
	void interpret(Element container, Map<TokenKey, Token> tokens) {
		NodeList colorNodes = container.getChildNodes();
		Map<TokenKey, List<Candidate>> candidates = new LinkedHashMap<>();
		int occurrence = 0;
		for (int i = 0; i < colorNodes.getLength(); i++) {
			Node node = colorNodes.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) continue;
			Element element = (Element) node;
			occurrence++;

			if (element.getElementsByTagName("*").getLength() > 0) {
				warning(ThemeDiagnostic.NESTED_ELEMENTS, "Theme element '<" + element.getTagName()
						+ ">' contains nested elements; only its own attributes were interpreted",
						element.getTagName(), null);
			}

			String newTagName = LegacyEclipsePropertyMapper.mapProperty(element.getTagName());
			TokenKey key = TokenKey.byId(newTagName);

			if (key == null) {
				warning(ThemeDiagnostic.UNKNOWN_ELEMENT, "Unknown theme element '<" + element.getTagName()
						+ ">'; it was ignored", element.getTagName(), null);
				continue;
			}

			TokenBuilder tokenBuilder = new TokenBuilder().setKey(key);
			Color color = color(element);
			if (color == null) {
				continue;
			}
			tokenBuilder.setColor(color);

			if (element.hasAttribute("bold")) {
				tokenBuilder.setBold(styleFlag(element, "bold"));
			}

			if (element.hasAttribute("italic")) {
				tokenBuilder.setItalic(styleFlag(element, "italic"));
			}

			if (element.hasAttribute("underline")) {
				tokenBuilder.setUnderline(styleFlag(element, "underline"));
			}

			if (element.hasAttribute("strikethrough")) {
				tokenBuilder.setStrikethrough(styleFlag(element, "strikethrough"));
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
			tokens.put(entry.getKey(), winner.token());
			for (Candidate candidate : accepted) {
				if (!candidate.token().getColor().equals(winner.token().getColor())
					|| !candidate.token().getOptions().orElse(TokenOptions.EMPTY)
						.equals(winner.token().getOptions().orElse(TokenOptions.EMPTY))) {
					warning(ThemeDiagnostic.MAPPING_CONFLICT, "Theme mapping conflict for role "
							+ entry.getKey().getName() + ": " + candidate.location() + " versus "
							+ winner.location() + "; selected " + winner.location(), null, null);
				}
			}
		}
	}

	/** The element's {@code color} in the documented grammar, or {@code null} after a warning. */
	Color color(Element element) {
		String colorAttr = element.getAttribute("color");
		if (colorAttr.isBlank()) {
			warning(ThemeDiagnostic.MISSING_COLOR, "Theme element '<" + element.getTagName()
					+ ">' has no usable 'color' attribute; the token was ignored", element.getTagName(), "color");
			return null;
		}
		try {
			return Color.ofHex(colorAttr);
		} catch (IllegalArgumentException e) {
			warning(ThemeDiagnostic.INVALID_COLOR, "Theme element '<" + element.getTagName()
					+ ">' has an unaccepted 'color' value '" + colorAttr + "'; the token was ignored",
					element.getTagName(), "color");
			return null;
		}
	}

	private boolean styleFlag(Element element, String attribute) {
		String raw = element.getAttribute(attribute);
		if (raw.equalsIgnoreCase("true")) {
			return true;
		}
		if (raw.equalsIgnoreCase("false")) {
			return false;
		}
		warning(ThemeDiagnostic.STYLE_FLAG, "Theme element '<" + element.getTagName()
				+ ">' has a non-boolean '" + attribute + "' value '" + raw
				+ "'; it was treated as false", element.getTagName(), attribute);
		return false;
	}

	private void warning(String kind, String message, String element, String attribute) {
		sink.accept(new ThemeDiagnostic(ThemeDiagnostic.Severity.WARNING, kind, message, source, element, attribute));
	}
}
