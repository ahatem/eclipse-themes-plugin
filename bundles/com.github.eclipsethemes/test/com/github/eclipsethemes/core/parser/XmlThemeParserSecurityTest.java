package com.github.eclipsethemes.core.parser;

import static org.junit.Assert.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.net.Socket;
import java.net.URI;
import java.util.List;
import org.junit.Test;
import org.xml.sax.SAXException;
import com.github.eclipsethemes.core.exceptions.ThemeParseException;

public class XmlThemeParserSecurityTest {
    @Test public void localEntityInjectionIsRejected() throws Exception {
        var canary = Files.createTempFile("xxe-harmless-", ".xml");
        try {
            Files.writeString(canary, "<background color='#13579b'/>");
            String input = "<!DOCTYPE colorTheme [<!ENTITY token SYSTEM '" + canary.toUri()
                    + "'>]><colorTheme name='Canary'>&token;</colorTheme>";
            XmlProviderSpy provider = new XmlProviderSpy();
            assertDtdFailure(() -> new XmlThemeParser(() -> provider).parse(XmlThemeParserCompatibilityTest.xml(input)));
            assertEquals(0, provider.resolutions);
        } finally { Files.delete(canary); }
    }

    @FunctionalInterface private interface Parse { Object run() throws Exception; }

    private static void assertDtdFailure(Parse parse) {
        ThemeParseException result = assertThrows(ThemeParseException.class, () -> parse.run());
        Throwable cause = result;
        boolean policyDiagnostic = false;
        while (cause != null) {
            policyDiagnostic |= cause.getMessage() != null && cause.getMessage().contains("DOCTYPE");
            if (cause.getCause() == null) assertTrue(cause instanceof SAXException);
            cause = cause.getCause();
        }
        assertTrue(result.getMessage(), policyDiagnostic);
    }

    @Test public void everyDtdFixtureAndEntryPointRejectsBeforeResolution() throws Exception {
        var canary = Files.createTempFile("xxe-local-", ".xml");
        var source = Files.createTempFile("xxe-input-", ".xml");
        try (LoopbackCanary network = new LoopbackCanary()) {
            Files.writeString(canary, "<background color='#13579b'/>");
            String local = canary.toUri().toString();
            String remote = network.url();
            List<String> declarations = List.of(
                    "<!DOCTYPE colorTheme>",
                    "<!DOCTYPE colorTheme []>",
                    "<!DOCTYPE colorTheme [<!ENTITY x 'internal'>]>",
                    "<!DOCTYPE colorTheme [<!ATTLIST colorTheme name CDATA 'DTD default'>]>",
                    "<!DOCTYPE colorTheme [<!ENTITY x 'x'><!ENTITY y '&x;&x;'><!ENTITY z '&y;&y;'>]>",
                    "<!DOCTYPE colorTheme SYSTEM '" + local + "'>",
                    "<!DOCTYPE colorTheme SYSTEM '" + remote + "'>",
                    "<!DOCTYPE colorTheme PUBLIC 'canary' '" + remote + "'>",
                    "<!DOCTYPE colorTheme [<!ENTITY x SYSTEM '" + local + "'>]>",
                    "<!DOCTYPE colorTheme [<!ENTITY x SYSTEM '" + remote + "'>]>",
                    "<!DOCTYPE colorTheme [<!ENTITY % x SYSTEM '" + local + "'>%x;]>",
                    "<!DOCTYPE colorTheme [<!ENTITY % x SYSTEM '" + remote + "'>%x;]>");
            for (String declaration : declarations) {
                String input = declaration + "<colorTheme name='Rejected'>&#65;</colorTheme>";
                // Include actual uses of general/nested entities, not declarations alone.
                if (declaration.contains("<!ENTITY x")) input = declaration + "<colorTheme>&x;</colorTheme>";
                if (declaration.contains("<!ENTITY z")) input = declaration + "<colorTheme>&z;</colorTheme>";
                byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
                Files.write(source, bytes);
                XmlProviderSpy provider = new XmlProviderSpy();
                XmlThemeParser parser = new XmlThemeParser(() -> provider);
                assertDtdFailure(() -> parser.parse(new java.io.ByteArrayInputStream(bytes)));
                assertDtdFailure(() -> parser.parse(new java.io.ByteArrayInputStream(bytes), source.toFile()));
                assertDtdFailure(() -> parser.parse(source.toFile()));
                assertEquals(declaration, 0, provider.resolutions);
                assertEquals(3, provider.parses);
                // Public construction still exercises the discovered provider on every route.
                XmlThemeParser discovered = new XmlThemeParser();
                assertDtdFailure(() -> discovered.parse(new java.io.ByteArrayInputStream(bytes)));
                assertDtdFailure(() -> discovered.parse(new java.io.ByteArrayInputStream(bytes), source.toFile()));
                assertDtdFailure(() -> discovered.parse(source.toFile()));
            }
            byte[] utf16 = ("<?xml version='1.0' encoding='UTF-16'?><!DOCTYPE colorTheme><colorTheme/>")
                    .getBytes(StandardCharsets.UTF_16);
            assertDtdFailure(() -> new XmlThemeParser().parse(new java.io.ByteArrayInputStream(utf16)));
            network.finish();
            assertEquals("No XML resource connections", 0, network.connections.get());
        } finally { Files.delete(canary); Files.delete(source); }
    }

    @Test public void xincludeAndSchemaHintsNeitherRetrieveNorIncorporate() throws Exception {
        var canary = Files.createTempFile("xml-inclusion-", ".xml");
        try (LoopbackCanary network = new LoopbackCanary()) {
            Files.writeString(canary, "<background color='#13579b'/>");
            for (String target : List.of(canary.toUri().toString(), network.url())) {
                XmlProviderSpy provider = new XmlProviderSpy();
                var builder = SecureXmlBuilder.create(() -> provider);
                var document = builder.parse(XmlThemeParserCompatibilityTest.xml(
                        "<colorTheme xmlns:xi='http://www.w3.org/2001/XInclude'"
                        + " xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'"
                        + " xsi:noNamespaceSchemaLocation='" + target + "' xsi:schemaLocation='urn:canary " + target + "'>"
                        + "<xi:include href='" + target + "' parse='xml'/><xi:include href='" + target + "' parse='text'/>"
                        + "</colorTheme>"));
                assertEquals(0, document.getElementsByTagName("background").getLength());
                assertEquals(2, document.getElementsByTagName("xi:include").getLength());
                assertFalse(builder.isValidating());
                assertFalse(builder.isXIncludeAware());
                assertNull(provider.delegate.getSchema());
                assertEquals(0, provider.resolutions);
            }
            network.finish();
            assertEquals(0, network.connections.get());
        } finally { Files.delete(canary); }
    }

    @Test public void networkMonitorDetectsAnActualConnection() throws Exception {
        try (LoopbackCanary network = new LoopbackCanary()) {
            URI address = URI.create(network.url());
            try (Socket socket = new Socket(address.getHost(), address.getPort())) {
                socket.setSoTimeout(1000);
                assertTrue(socket.getInputStream().readAllBytes().length > 0);
            }
            network.finish();
            assertEquals(1, network.connections.get());
        }
    }
}
