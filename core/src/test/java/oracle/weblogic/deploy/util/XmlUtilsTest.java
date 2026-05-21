/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */
package oracle.weblogic.deploy.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class XmlUtilsTest {
    private static final String DOCUMENT_BUILDER_FACTORY_PROPERTY = DocumentBuilderFactory.class.getName();

    @TempDir
    Path tempDirectory;

    @Test
    void testGetPsuUsesDefaultDocumentBuilderFactoryWhenJaxpProviderIsOverridden() throws Exception {
        assumeTrue(hasDefaultDocumentBuilderFactory());

        Path patchesDirectory = tempDirectory.resolve("inventory").resolve("patches");
        Files.createDirectories(patchesDirectory);
        Files.write(patchesDirectory.resolve("patch.xml"),
            Collections.singletonList("<patch description=\"WLS PATCH SET UPDATE 12.2.1.4.220329\"/>"),
            StandardCharsets.UTF_8);

        String previousFactory = System.getProperty(DOCUMENT_BUILDER_FACTORY_PROPERTY);
        System.setProperty(DOCUMENT_BUILDER_FACTORY_PROPERTY, FailingDocumentBuilderFactory.class.getName());
        try {
            assertEquals("220329", new XPathUtil(tempDirectory.toString()).getPSU());
        } finally {
            if (previousFactory == null) {
                System.clearProperty(DOCUMENT_BUILDER_FACTORY_PROPERTY);
            } else {
                System.setProperty(DOCUMENT_BUILDER_FACTORY_PROPERTY, previousFactory);
            }
        }
    }

    private static boolean hasDefaultDocumentBuilderFactory() {
        try {
            DocumentBuilderFactory.class.getMethod("newDefaultInstance");
            return true;
        } catch (NoSuchMethodException nsme) {
            return false;
        }
    }

    public static final class FailingDocumentBuilderFactory extends DocumentBuilderFactory {
        @Override
        public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
            throw new ParserConfigurationException("The overridden JAXP provider should not be used");
        }

        @Override
        public void setAttribute(String name, Object value) {
            throw new IllegalAccessError("The overridden JAXP provider should not be used");
        }

        @Override
        public Object getAttribute(String name) {
            throw new IllegalArgumentException(name);
        }

        @Override
        public void setFeature(String name, boolean value) throws ParserConfigurationException {
            throw new ParserConfigurationException("The overridden JAXP provider should not be used");
        }

        @Override
        public boolean getFeature(String name) throws ParserConfigurationException {
            throw new ParserConfigurationException("The overridden JAXP provider should not be used");
        }
    }
}
