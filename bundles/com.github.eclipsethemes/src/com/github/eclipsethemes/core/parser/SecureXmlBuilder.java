package com.github.eclipsethemes.core.parser;

import java.util.function.Supplier;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.EntityResolver2;
import org.xml.sax.helpers.DefaultHandler;
import com.github.eclipsethemes.core.exceptions.ThemeParseException;

/** Parser-local JAXP policy. Never retry with fewer controls or change provider discovery. */
final class SecureXmlBuilder {
    private SecureXmlBuilder() { }

    static DocumentBuilder create(Supplier<DocumentBuilderFactory> supplier) throws ThemeParseException {
        DocumentBuilderFactory factory;
        try {
            factory = supplier.get();
            if (factory == null) throw new ParserConfigurationException("Provider returned no factory");
        } catch (FactoryConfigurationError e) {
            throw new ThemeParseException("XML security configuration failed at factory discovery", e);
        } catch (Exception e) {
            throw new ThemeParseException("XML security configuration failed at factory discovery", e);
        }

        feature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Apache/SAX provider controls: verify support on each supported provider,
        // without importing an implementation or changing class-loader selection.
        feature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        feature(factory, "http://xml.org/sax/features/external-general-entities", false);
        feature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        feature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        attribute(factory, XMLConstants.ACCESS_EXTERNAL_DTD);
        attribute(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA);
        control("validation", () -> {
            factory.setValidating(false);
            require(!factory.isValidating());
        });
        control("schema", () -> {
            factory.setSchema(null);
            require(factory.getSchema() == null);
        });
        control("XInclude", () -> {
            factory.setXIncludeAware(false);
            require(!factory.isXIncludeAware());
        });
        control("entity-reference expansion", () -> {
            factory.setExpandEntityReferences(false);
            require(!factory.isExpandEntityReferences());
        });

        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            if (builder == null) throw new ParserConfigurationException("Provider returned no builder");
        } catch (Exception e) {
            throw new ThemeParseException("XML security configuration failed at builder creation", e);
        }
        control("entity resolver installation", () -> builder.setEntityResolver(new RejectingResolver()));
        control("error handler installation", () -> builder.setErrorHandler(new ThrowingErrorHandler()));
        control("builder validation", () -> require(!builder.isValidating()));
        control("builder XInclude", () -> require(!builder.isXIncludeAware()));
        return builder;
    }

    private static void feature(DocumentBuilderFactory factory, String name, boolean value)
            throws ThemeParseException {
        control(name, () -> {
            factory.setFeature(name, value);
            require(factory.getFeature(name) == value);
        });
    }

    private static void attribute(DocumentBuilderFactory factory, String name) throws ThemeParseException {
        control(name, () -> {
            factory.setAttribute(name, "");
            require("".equals(factory.getAttribute(name)));
        });
    }

    private static void require(boolean verified) throws ParserConfigurationException {
        if (!verified) throw new ParserConfigurationException("Required secure value was not reported by XML provider");
    }

    private static void control(String name, Configuration action) throws ThemeParseException {
        try {
            action.run();
        } catch (Exception e) {
            throw new ThemeParseException("XML security configuration failed at " + name, e);
        }
    }

    @FunctionalInterface private interface Configuration {
        void run() throws Exception;
    }

    private static final class RejectingResolver implements EntityResolver2 {
        private SAXException blocked() {
            return new SAXException("External XML resolution is prohibited for theme XML (DTD/DOCTYPE policy)");
        }
        @Override public InputSource getExternalSubset(String name, String baseURI) throws SAXException {
            throw blocked();
        }
        @Override public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId)
                throws SAXException {
            throw blocked();
        }
        @Override public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
            throw blocked();
        }
    }

    private static final class ThrowingErrorHandler extends DefaultHandler {
        @Override public void error(SAXParseException e) throws SAXException { throw e; }
        @Override public void fatalError(SAXParseException e) throws SAXException { throw e; }
    }
}
