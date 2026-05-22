/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */
package oracle.weblogic.deploy.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * XML parser utility methods.
 */
public final class XmlUtils {
    private static final String DISALLOW_DOCTYPE_DECL_FEATURE =
        "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String NEW_DEFAULT_INSTANCE_METHOD = "newDefaultInstance";

    private XmlUtils() {
        // utility class
    }

    /**
     * Get a secure DocumentBuilderFactory for WDT-owned XML parsing.
     *
     * @return the secure DocumentBuilderFactory
     * @throws ParserConfigurationException if the parser factory does not support the required secure settings
     */
    public static DocumentBuilderFactory getSecureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = newDocumentBuilderFactory();
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        dbf.setFeature(DISALLOW_DOCTYPE_DECL_FEATURE, true);
        setAttribute(dbf, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setAttribute(dbf, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return dbf;
    }

    static DocumentBuilderFactory newDocumentBuilderFactory() {
        try {
            Method method = DocumentBuilderFactory.class.getMethod(NEW_DEFAULT_INSTANCE_METHOD);
            return (DocumentBuilderFactory) method.invoke(null);
        } catch (NoSuchMethodException nsme) {
            return DocumentBuilderFactory.newInstance();
        } catch (IllegalAccessException iae) {
            return DocumentBuilderFactory.newInstance();
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            return DocumentBuilderFactory.newInstance();
        }
    }

    private static void setAttribute(DocumentBuilderFactory dbf, String name, Object value)
        throws ParserConfigurationException {
        try {
            dbf.setAttribute(name, value);
        } catch (IllegalArgumentException iae) {
            ParserConfigurationException pce = new ParserConfigurationException(iae.getLocalizedMessage());
            pce.initCause(iae);
            throw pce;
        }
    }
}
