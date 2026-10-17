package com.github.eclipsethemes.core.parser;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.function.Supplier;
import javax.xml.parsers.*;
import org.osgi.framework.Bundle;

/** Compiled with the maintained test-only spy/canary sources, never product classes. */
public final class RuntimeXmlChecks {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        System.out.println("PASS " + message);
    }
    public static void run(Bundle product, Path scratch) throws Exception {
        Class<?> parserClass = product.loadClass("com.github.eclipsethemes.core.parser.XmlThemeParser");
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Path local = null;
        try {
            Thread.currentThread().setContextClassLoader(parserClass.getClassLoader());
            local = Files.createTempFile(scratch, "harmless-", ".xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Class<?> helper = product.loadClass("com.github.eclipsethemes.core.parser.SecureXmlBuilder");
            Method create = helper.getDeclaredMethod("create", Supplier.class);
            create.setAccessible(true);
            DocumentBuilder secured = (DocumentBuilder) create.invoke(null, (Supplier<DocumentBuilderFactory>) () -> factory);
            System.out.println("JAXP provider=" + factory.getClass().getName() + " builder=" + secured.getClass().getName()
                    + " Java=" + System.getProperty("java.runtime.version"));
            XmlProviderSpy spy = new XmlProviderSpy();
            Constructor<?> seam = parserClass.getDeclaredConstructor(Supplier.class);
            seam.setAccessible(true);
            Object monitored = seam.newInstance((Supplier<DocumentBuilderFactory>) () -> spy);
            Object discovered = parserClass.getConstructor().newInstance();
            Method parse = parserClass.getMethod("parse", InputStream.class);
            Files.writeString(local, "<background color='#13579b'/>");
            try (LoopbackCanary monitor = new LoopbackCanary()) {
                for (String dtd : List.of("<!DOCTYPE colorTheme [<!ENTITY x SYSTEM '" + local.toUri() + "'>]>",
                        "<!DOCTYPE colorTheme SYSTEM '" + monitor.url() + "'>", "<!DOCTYPE colorTheme>")) {
                    byte[] input = (dtd + "<colorTheme>" + (dtd.contains("<!ENTITY") ? "&x;" : "") + "</colorTheme>")
                            .getBytes(StandardCharsets.UTF_8);
                    for (Object parser : List.of(monitored, discovered)) {
                        try { parse.invoke(parser, new ByteArrayInputStream(input)); throw new AssertionError("DTD returned a theme"); }
                        catch (InvocationTargetException expected) {
                            Throwable cause = expected.getCause();
                            check(cause.getClass().getName().endsWith("ThemeParseException") && cause.getCause() != null
                                    && cause.getMessage().contains("DOCTYPE"), "packaged parser rejects harmless DTD");
                        }
                    }
                }
                monitor.finish();
                check(spy.resolutions == 0 && monitor.connections.get() == 0, "zero resolver callbacks and loopback connections");
            }
            try (LoopbackCanary calibration = new LoopbackCanary()) {
                URI address = URI.create(calibration.url());
                try (Socket socket = new Socket(address.getHost(), address.getPort())) {
                    socket.setSoTimeout(1000);
                    check(socket.getInputStream().readAllBytes().length > 0, "canary answers bounded response");
                }
                calibration.finish();
                check(calibration.connections.get() == 1, "runtime network canary positive calibration");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
            if (local != null) Files.delete(local);
        }
    }
}
