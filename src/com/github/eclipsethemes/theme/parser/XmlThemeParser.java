package com.github.eclipsethemes.theme.parser;

import com.github.eclipsethemes.EclipseThemes;
import com.github.eclipsethemes.theme.mapper.LegacyEclipsePropertyMapper;
import com.github.eclipsethemes.theme.models.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;

public class XmlThemeParser implements ThemeParser {

	/**
	 * @param inputStream The stream to parse.
	 * @param sourceFile  The original file, which can be null.
	 */
	public Theme parse(InputStream inputStream, File sourceFile) throws ThemeParseException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(inputStream);

			Element root = document.getDocumentElement();
			String id = root.getAttribute("ect-id");
			String name = root.getAttribute("name");
			String author = root.getAttribute("author");
			String website = root.getAttribute("website");
			String description = root.getAttribute("description");
			String type = root.getAttribute("type");

			Theme theme = new Theme(id, name, author, website, description, sourceFile, ThemeType.from(type));

			NodeList colorNodes = root.getChildNodes();
			for (int i = 0; i < colorNodes.getLength(); i++) {
				Node node = colorNodes.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) node;

					String newTagName = LegacyEclipsePropertyMapper.mapProperty(element.getTagName());
					TokenKey key = TokenKey.byId(newTagName);

					if (key == null) {
						EclipseThemes.instance().getLogger()
								.info("Could match any key with tag = " + element.getTagName());
						continue;
					}

					TokenBuilder tokenBuilder = new TokenBuilder().setKey(key)
							.setHexColor(element.getAttribute("color"));

					if (element.hasAttribute("bold")) {
						tokenBuilder.setBold(Boolean.parseBoolean(element.getAttribute("bold")));
					}
					if (element.hasAttribute("italic")) {
						tokenBuilder.setItalic(Boolean.parseBoolean(element.getAttribute("italic")));
					}

					theme.addToken(tokenBuilder.build());
				}
			}
			return theme;
		} catch (Exception e) {
			throw new ThemeParseException("Failed to parse theme from stream", e);
		}
	}

}
