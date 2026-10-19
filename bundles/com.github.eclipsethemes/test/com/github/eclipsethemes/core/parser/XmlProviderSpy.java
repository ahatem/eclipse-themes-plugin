package com.github.eclipsethemes.core.parser;

import java.io.IOException;
import javax.xml.parsers.*;
import javax.xml.validation.Schema;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.*;
import org.xml.sax.ext.EntityResolver2;

/** Delegates parsing to the real provider; faults never change JVM-wide settings. */
class XmlProviderSpy extends DocumentBuilderFactory {
    enum Fault { SET, GET, UNSAFE }
    final DocumentBuilderFactory delegate = DocumentBuilderFactory.newInstance();
    final String failingControl;
    final Fault fault;
    final RuntimeException failure = new UnsupportedOperationException("injected provider failure");
    int builders;
    int parses;
    int resolutions;
    EntityResolver2 resolver;
    ErrorHandler handler;
    String callback;
    SAXParseException callbackFailure;

    XmlProviderSpy() { this(null, null); }
    XmlProviderSpy(String control, Fault fault) { this.failingControl = control; this.fault = fault; }

    private void set(String control) {
        if (control.equals(failingControl) && fault == Fault.SET) throw failure;
    }
    private Object get(String control, Object safe) {
        if (!control.equals(failingControl)) return safe;
        if (fault == Fault.GET) throw failure;
        if (fault == Fault.UNSAFE) {
            if (safe instanceof Boolean value) return !value;
            if (safe instanceof String) return "file,http";
            return new Schema() {
                public javax.xml.validation.Validator newValidator() { return null; }
                public javax.xml.validation.ValidatorHandler newValidatorHandler() { return null; }
            };
        }
        return safe;
    }

    @Override public void setFeature(String n, boolean v) throws ParserConfigurationException { set(n); delegate.setFeature(n, v); }
    @Override public boolean getFeature(String n) throws ParserConfigurationException { return (boolean) get(n, delegate.getFeature(n)); }
    @Override public void setAttribute(String n, Object v) { set(n); delegate.setAttribute(n, v); }
    @Override public Object getAttribute(String n) { return get(n, delegate.getAttribute(n)); }
    @Override public void setValidating(boolean v) { set("validation"); delegate.setValidating(v); }
    @Override public boolean isValidating() { return (boolean) get("validation", delegate.isValidating()); }
    @Override public void setSchema(Schema v) { set("schema"); delegate.setSchema(v); }
    @Override public Schema getSchema() { return (Schema) get("schema", delegate.getSchema()); }
    @Override public void setXIncludeAware(boolean v) { set("XInclude"); delegate.setXIncludeAware(v); }
    @Override public boolean isXIncludeAware() { return (boolean) get("XInclude", delegate.isXIncludeAware()); }
    @Override public void setExpandEntityReferences(boolean v) { set("entity-reference expansion"); delegate.setExpandEntityReferences(v); }
    @Override public boolean isExpandEntityReferences() { return (boolean) get("entity-reference expansion", delegate.isExpandEntityReferences()); }

    @Override public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        builders++;
        set("builder creation");
        DocumentBuilder real = delegate.newDocumentBuilder();
        return new DocumentBuilder() {
            @Override public Document parse(InputSource source) throws SAXException, IOException {
                parses++;
                if (callback != null) {
                    switch (callback) {
                        case "subset": resolver.getExternalSubset("theme", "file:/unused"); break;
                        case "entity2": resolver.resolveEntity("entity", null, "file:/unused", "unused"); break;
                        case "entity": resolver.resolveEntity(null, "file:/unused"); break;
                        case "error":
                            callbackFailure = new SAXParseException("ordinary error", null);
                            handler.error(callbackFailure); break;
                        case "fatal":
                            callbackFailure = new SAXParseException("ordinary fatal", null);
                            handler.fatalError(callbackFailure); break;
                        default: throw new AssertionError(callback);
                    }
                    throw new AssertionError("Guard/handler returned content or swallowed failure");
                }
                return real.parse(source);
            }
            @Override public boolean isNamespaceAware() { return real.isNamespaceAware(); }
            @Override public boolean isValidating() { return (boolean) get("builder validation", real.isValidating()); }
            @Override public boolean isXIncludeAware() { return (boolean) get("builder XInclude", real.isXIncludeAware()); }
            @Override public void setEntityResolver(EntityResolver r) {
                set("entity resolver installation");
                resolver = (EntityResolver2) r;
                real.setEntityResolver(new EntityResolver2() {
                    public InputSource getExternalSubset(String n, String b) throws SAXException, IOException {
                        resolutions++; return resolver.getExternalSubset(n, b);
                    }
                    public InputSource resolveEntity(String n, String p, String b, String s) throws SAXException, IOException {
                        resolutions++; return resolver.resolveEntity(n, p, b, s);
                    }
                    public InputSource resolveEntity(String p, String s) throws SAXException, IOException {
                        resolutions++; return resolver.resolveEntity(p, s);
                    }
                });
            }
            @Override public void setErrorHandler(ErrorHandler h) {
                set("error handler installation"); handler = h; real.setErrorHandler(h);
            }
            @Override public Document newDocument() { return real.newDocument(); }
            @Override public DOMImplementation getDOMImplementation() { return real.getDOMImplementation(); }
        };
    }
}
