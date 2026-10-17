package com.github.eclipsethemes.core.parser;

import static org.junit.Assert.*;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.xml.sax.SAXException;
import com.github.eclipsethemes.core.exceptions.ThemeParseException;

public class SecureXmlBuilderTest {
    private static final List<String> FACTORY_CONTROLS = List.of(
            XMLConstants.FEATURE_SECURE_PROCESSING,
            "http://apache.org/xml/features/disallow-doctype-decl",
            "http://xml.org/sax/features/external-general-entities",
            "http://xml.org/sax/features/external-parameter-entities",
            "http://apache.org/xml/features/nonvalidating/load-external-dtd",
            XMLConstants.ACCESS_EXTERNAL_DTD, XMLConstants.ACCESS_EXTERNAL_SCHEMA,
            "validation", "schema", "XInclude", "entity-reference expansion");

    static final class CountingInput extends InputStream {
        int reads;
        @Override public int read() { reads++; return -1; }
    }

    @Test public void everyRequiredControlFailsClosedOnRefusalReadbackOrMismatch() throws Exception {
        for (String control : FACTORY_CONTROLS) {
            for (XmlProviderSpy.Fault fault : XmlProviderSpy.Fault.values()) {
                assertConfigurationFailure(control, fault, 0);
            }
        }
        for (String control : List.of("builder validation", "builder XInclude")) {
            assertConfigurationFailure(control, XmlProviderSpy.Fault.GET, 1);
            assertConfigurationFailure(control, XmlProviderSpy.Fault.UNSAFE, 1);
        }
    }

    @Test public void creationAndGuardInstallationFailuresConsumeNoInput() throws Exception {
        for (String stage : List.of("builder creation", "entity resolver installation", "error handler installation")) {
            assertConfigurationFailure(stage, XmlProviderSpy.Fault.SET, 1);
        }
        Exception cause = new Exception("missing provider");
        FactoryConfigurationError failure = new FactoryConfigurationError(cause, "injected discovery failure");
        AtomicInteger attempts = new AtomicInteger();
        CountingInput input = new CountingInput();
        ThemeParseException result = assertThrows(ThemeParseException.class, () -> new XmlThemeParser(() -> {
            attempts.incrementAndGet(); throw failure;
        }).parse(input));
        assertSame(failure, result.getCause());
        assertTrue(result.getMessage().contains("factory discovery"));
        assertEquals(0, input.reads);
        assertEquals(1, attempts.get());
    }

    @Test public void missingProcessorsAndCheckedConfigurationFailuresRetainCause() throws Exception {
        ParserConfigurationException checked = new ParserConfigurationException("injected builder failure");
        IllegalArgumentException rejected = new IllegalArgumentException("injected attribute refusal");
        List<XmlProviderSpy> providers = List.of(
                new XmlProviderSpy() {
                    @Override public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
                        builders++; throw checked;
                    }
                },
                new XmlProviderSpy() {
                    @Override public DocumentBuilder newDocumentBuilder() { builders++; return null; }
                },
                new XmlProviderSpy() {
                    @Override public void setAttribute(String name, Object value) { throw rejected; }
                });
        for (int i = 0; i < providers.size(); i++) {
            XmlProviderSpy provider = providers.get(i);
            CountingInput input = new CountingInput();
            ThemeParseException result = assertThrows(ThemeParseException.class,
                    () -> new XmlThemeParser(() -> provider).parse(input));
            assertNotNull(result.getCause());
            if (provider.builders == 0) {
                assertSame(rejected, result.getCause());
                assertTrue(result.getMessage().contains(XMLConstants.ACCESS_EXTERNAL_DTD));
            } else {
                assertTrue(result.getMessage().contains("builder creation"));
                if (i == 0) assertSame(checked, result.getCause());
                else assertTrue(result.getCause() instanceof ParserConfigurationException);
            }
            assertEquals(0, input.reads);
            assertEquals(0, provider.parses);
            assertEquals(i == 2 ? 0 : 1, provider.builders);
        }
        for (boolean returnNull : List.of(false, true)) {
            CountingInput input = new CountingInput();
            AtomicInteger attempts = new AtomicInteger();
            ThemeParseException result = assertThrows(ThemeParseException.class,
                    () -> new XmlThemeParser(() -> {
                        attempts.incrementAndGet();
                        if (returnNull) return null;
                        throw rejected;
                    }).parse(input));
            assertTrue(result.getMessage().contains("factory discovery"));
            assertNotNull(result.getCause());
            if (!returnNull) assertSame(rejected, result.getCause());
            assertEquals(0, input.reads);
            assertEquals(1, attempts.get());
        }
    }

    private static void assertConfigurationFailure(String control, XmlProviderSpy.Fault fault, int builders)
            throws Exception {
        XmlProviderSpy provider = new XmlProviderSpy(control, fault);
        CountingInput input = new CountingInput();
        AtomicInteger attempts = new AtomicInteger();
        ThemeParseException result = assertThrows(control + " " + fault, ThemeParseException.class,
                () -> new XmlThemeParser(() -> { attempts.incrementAndGet(); return provider; }).parse(input));
        assertTrue(result.getMessage(), result.getMessage().contains(control));
        assertNotNull(result.getCause());
        if (fault != XmlProviderSpy.Fault.UNSAFE) assertSame(provider.failure, result.getCause());
        assertEquals(control, 0, input.reads);
        assertEquals(control, 0, provider.parses);
        assertEquals(control, builders, provider.builders);
        assertEquals(control, 1, attempts.get());
    }

    @Test public void everyResolverCallbackAndErrorHandlerPropagatesCause() throws Exception {
        for (String callback : List.of("subset", "entity2", "entity", "error", "fatal")) {
            XmlProviderSpy provider = new XmlProviderSpy();
            provider.callback = callback;
            CountingInput input = new CountingInput();
            ThemeParseException result = assertThrows(ThemeParseException.class,
                    () -> new XmlThemeParser(() -> provider).parse(input));
            assertTrue(result.getCause() instanceof SAXException);
            assertEquals(0, input.reads);
            assertEquals(1, provider.parses);
            if (callback.startsWith("entity") || callback.equals("subset")) {
                assertTrue(result.getMessage(), result.getMessage().contains("External XML resolution is prohibited"));
            } else {
                assertSame(provider.callbackFailure, result.getCause());
                assertTrue(result.getMessage().contains("ordinary"));
                assertFalse(result.getMessage().contains("DOCTYPE"));
            }
        }
    }

    @Test public void realProviderAndFreshBuildersPreserveDiscovery() throws Exception {
        List<XmlProviderSpy> providers = new ArrayList<>();
        String property = System.getProperty("javax.xml.parsers.DocumentBuilderFactory");
        XmlThemeParser parser = new XmlThemeParser(() -> {
            XmlProviderSpy provider = new XmlProviderSpy(); providers.add(provider); return provider;
        });
        for (int i = 0; i < 3; i++) parser.parse(ParserTestSupport.xml("<colorTheme name='Safe'/>"));
        assertEquals(3, providers.size());
        for (XmlProviderSpy p : providers) { assertEquals(1, p.builders); assertEquals(1, p.parses); }
        assertEquals(property, System.getProperty("javax.xml.parsers.DocumentBuilderFactory"));
        DocumentBuilderFactory actual = DocumentBuilderFactory.newInstance();
        SecureXmlBuilder.create(() -> actual);
        System.out.println("XML provider: " + actual.getClass().getName() + "; Java " + System.getProperty("java.runtime.version"));
    }

    @Test public void ordinaryXmlAndIoFailuresAreNotMisidentifiedAsDtds() throws Exception {
        XmlThemeParser parser = new XmlThemeParser();
        ThemeParseException xml = assertThrows(ThemeParseException.class,
                () -> parser.parse(ParserTestSupport.xml("<colorTheme>")));
        assertTrue(xml.getCause() instanceof SAXException);
        assertFalse(xml.getMessage().contains("DOCTYPE"));
        IOException failure = new IOException("test input failure");
        ThemeParseException io = assertThrows(ThemeParseException.class, () -> parser.parse(new InputStream() {
            public int read() throws IOException { throw failure; }
        }));
        assertSame(failure, io.getCause());
        assertFalse(io.getMessage().contains("DOCTYPE"));
        AssertionError vmError = new AssertionError("must propagate");
        assertSame(vmError, assertThrows(AssertionError.class,
                () -> new XmlThemeParser(() -> { throw vmError; }).parse(new CountingInput())));
    }
}
