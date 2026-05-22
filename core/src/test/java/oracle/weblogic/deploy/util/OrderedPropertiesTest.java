/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * Licensed under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl.
 */
package oracle.weblogic.deploy.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.python.core.PyDictionary;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyString;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderedPropertiesTest {
    @Test
    void insertionOrderAppliesToIterationSurfaces() {
        OrderedProperties properties = new OrderedProperties();
        properties.setProperty("first", "1");
        properties.put("second", "2");
        properties.setProperty("third", "3");

        properties.setProperty("first", "one");
        properties.put("second", "two");

        List<String> expectedKeys = Arrays.asList("first", "second", "third");
        assertEquals(expectedKeys, enumerationToList(properties.keys()));
        assertEquals(expectedKeys, new ArrayList<Object>(properties.keySet()));
        assertEquals(expectedKeys, entryKeys(properties));
        assertEquals(expectedKeys, enumerationToList(properties.propertyNames()));
        assertEquals(expectedKeys, new ArrayList<String>(properties.stringPropertyNames()));

        assertEquals("one", properties.getProperty("first"));
        assertEquals("two", properties.getProperty("second"));
    }

    @Test
    void removeClearAndReaddMaintainOrder() {
        OrderedProperties properties = new OrderedProperties();
        properties.setProperty("first", "1");
        properties.setProperty("second", "2");
        properties.setProperty("third", "3");

        properties.remove("second");
        assertEquals(Arrays.asList("first", "third"), enumerationToList(properties.keys()));

        properties.setProperty("second", "2");
        assertEquals(Arrays.asList("first", "third", "second"), enumerationToList(properties.keys()));

        properties.clear();
        assertTrue(Collections.list(properties.keys()).isEmpty());
        assertTrue(properties.keySet().isEmpty());
        assertTrue(properties.entrySet().isEmpty());
        assertTrue(Collections.list(properties.propertyNames()).isEmpty());
        assertTrue(properties.stringPropertyNames().isEmpty());

        properties.setProperty("after-clear", "value");
        assertEquals(Arrays.asList("after-clear"), enumerationToList(properties.keys()));
    }

    @Test
    void putAllAppendsNewKeysInSourceOrderAndUpdatesExistingKeysInPlace() {
        OrderedProperties properties = new OrderedProperties();
        properties.setProperty("first", "1");
        properties.setProperty("second", "2");

        Map<String, String> source = new LinkedHashMap<String, String>();
        source.put("second", "updated");
        source.put("third", "3");
        source.put("fourth", "4");

        properties.putAll(source);

        assertEquals(Arrays.asList("first", "second", "third", "fourth"), enumerationToList(properties.keys()));
        assertEquals("updated", properties.getProperty("second"));
    }

    @Test
    void fromPyOrderedDictPreservesOrderAndConvertsToProperties() {
        PyOrderedDict dictionary = new PyOrderedDict();
        dictionary.__setitem__(new PyString("first"), new PyString("1"));
        dictionary.__setitem__(new PyString("space key"), new PyString("value with spaces"));
        dictionary.__setitem__(new PyInteger(3), new PyInteger(3));

        OrderedProperties properties = new OrderedProperties(dictionary);
        assertEquals(Arrays.asList("first", "space key", "3"), enumerationToList(properties.keys()));
        assertEquals("value with spaces", properties.getProperty("space key"));
        assertEquals("3", properties.getProperty("3"));

        OrderedProperties fromStaticMethod = OrderedProperties.fromPyOrderedDict(dictionary);
        assertEquals(Arrays.asList("first", "space key", "3"), enumerationToList(fromStaticMethod.keys()));
    }

    @Test
    void fromPyDictionaryConvertsNonStringKeysAndValuesToProperties() {
        PyDictionary dictionary = new PyDictionary();
        dictionary.__setitem__(new PyInteger(7001), new PyInteger(7002));
        dictionary.__setitem__(new PyString("space key"), new PyString("value with spaces"));

        OrderedProperties properties = OrderedProperties.fromPyDictionary(dictionary);
        assertEquals("7002", properties.getProperty("7001"));
        assertEquals("value with spaces", properties.getProperty("space key"));
    }

    @Test
    void toPyOrderedDictPreservesPropertyOrder() {
        OrderedProperties properties = new OrderedProperties();
        properties.setProperty("first", "1");
        properties.setProperty("second", "2");
        properties.setProperty("third", "3");

        PyOrderedDict dictionary = properties.toPyOrderedDict();
        assertEquals(Arrays.asList("first", "second", "third"), pyDictionaryKeys(dictionary));
        assertEquals("2", dictionary.get(new PyString("second")).toString());
    }

    @Test
    void pyOrderedDictFileHelpersUseJdkLoadAndStoreSemantics() throws Exception {
        File propertiesFile = File.createTempFile("ordered-properties", ".properties");
        try {
            writeFile(propertiesFile, "old=value\n");

            PyOrderedDict dictionary = new PyOrderedDict();
            dictionary.__setitem__(new PyString("space key"), new PyString("value with spaces"));
            dictionary.__setitem__(new PyString("colon:key"), new PyString("a=b:c"));
            dictionary.__setitem__(new PyString("unicode"), new PyString("ol\u00e9"));

            OrderedProperties.store(dictionary, propertiesFile, "comments");

            String output = new String(Files.readAllBytes(propertiesFile.toPath()), "ISO-8859-1");
            assertEquals(Arrays.asList("space\\ key=value with spaces", "colon\\:key=a\\=b\\:c",
                    "unicode=ol\\u00E9"), propertyLines(output));

            PyOrderedDict loadedDictionary = OrderedProperties.loadPyOrderedDict(propertiesFile.getAbsolutePath());
            assertEquals(Arrays.asList("space key", "colon:key", "unicode"), pyDictionaryKeys(loadedDictionary));
            assertEquals("a=b:c", loadedDictionary.get(new PyString("colon:key")).toString());
            assertEquals("ol\u00e9", loadedDictionary.get(new PyString("unicode")).toString());

            OrderedProperties loadedProperties = OrderedProperties.load(propertiesFile);
            assertEquals(Arrays.asList("space key", "colon:key", "unicode"), enumerationToList(loadedProperties.keys()));
        } finally {
            propertiesFile.delete();
        }
    }

    @Test
    void pyDictionaryStoreConvertsNonStringEntriesAndUsesJdkEscaping() throws Exception {
        File propertiesFile = File.createTempFile("ordered-properties", ".properties");
        try {
            PyDictionary dictionary = new PyDictionary();
            dictionary.__setitem__(new PyInteger(7001), new PyInteger(7002));
            dictionary.__setitem__(new PyString("space key"), new PyString("value with spaces"));

            OrderedProperties.store(dictionary, propertiesFile.getAbsolutePath(), null);

            String output = new String(Files.readAllBytes(propertiesFile.toPath()), "ISO-8859-1");
            List<String> lines = propertyLines(output);
            assertEquals(2, lines.size());
            assertTrue(lines.contains("7001=7002"));
            assertTrue(lines.contains("space\\ key=value with spaces"));

            Properties standardProperties = new Properties();
            standardProperties.load(new ByteArrayInputStream(output.getBytes("ISO-8859-1")));
            assertEquals("7002", standardProperties.getProperty("7001"));
            assertEquals("value with spaces", standardProperties.getProperty("space key"));
        } finally {
            propertiesFile.delete();
        }
    }

    @Test
    void objectSignatureModernMutatorsKeepOrderedStateExact() throws Exception {
        OrderedProperties properties = new OrderedProperties();
        properties.setProperty("first", "1");

        assertNull(properties.putIfAbsent("second", "2"));
        assertEquals("1", properties.putIfAbsent("first", "ignored"));
        assertEquals("1", properties.getProperty("first"));
        assertEquals(Arrays.asList("first", "second"), enumerationToList(properties.keys()));

        assertEquals("2", properties.replace("second", "two"));
        assertNull(properties.replace("missing", "ignored"));
        assertTrue(properties.replace("second", "two", "2"));
        assertFalse(properties.replace("second", "missing", "ignored"));
        assertFalse(properties.remove("second", "missing"));
        assertEquals(Arrays.asList("first", "second"), entryKeys(properties));

        assertTrue(properties.remove("second", "2"));
        assertEquals(Arrays.asList("first"), new ArrayList<Object>(properties.keySet()));

        properties.putIfAbsent("second", "2");
        StringWriter writer = new StringWriter();
        properties.store(writer, null);
        assertEquals(Arrays.asList("first=1", "second=2"), propertyLines(writer.toString()));
    }

    @Test
    void functionBasedInheritedMutatorsAreReconciledBeforeOrderedReadsAndStore() throws Exception {
        OrderedProperties properties = new OrderedProperties();
        properties.setProperty("first", "1");

        properties.computeIfAbsent("computed", key -> "2");
        assertEquals(Arrays.asList("first", "computed"), enumerationToList(properties.keys()));
        assertEquals(Arrays.asList("first", "computed"), entryKeys(properties));

        properties.computeIfPresent("computed", (key, value) -> null);
        assertEquals(Arrays.asList("first"), enumerationToList(properties.keys()));

        properties.merge("merged", "3", (oldValue, newValue) -> oldValue.toString() + newValue.toString());
        properties.compute("first", (key, value) -> null);
        assertEquals(Arrays.asList("merged"), new ArrayList<Object>(properties.keySet()));

        StringWriter writer = new StringWriter();
        properties.store(writer, null);
        assertEquals(Arrays.asList("merged=3"), propertyLines(writer.toString()));
    }

    @Test
    void oldAndModernStoreIterationSurfacesUseSameOrder() {
        OrderedProperties properties = new OrderedProperties();
        properties.setProperty("first", "1");
        properties.setProperty("second", "2");
        properties.setProperty("third", "3");

        assertEquals(Arrays.asList("first=1", "second=2", "third=3"), oldStoreStyleLines(properties));
        assertEquals(Arrays.asList("first=1", "second=2", "third=3"), modernStoreStyleLines(properties));
    }

    @Test
    void loadReaderUsesJdkParsingAndKeepsFirstDuplicatePosition() throws Exception {
        OrderedProperties properties = new OrderedProperties();
        String content = "# comment\n"
                + "alpha = one\n"
                + "escaped\\ key: spaced\\ value\n"
                + "separator\\:key=value:with:colon\n"
                + "continued = part1, \\\n"
                + "    part2\n"
                + "unicode\\u003Akey = value\\u003Done\n"
                + "alpha = two\n";

        properties.load(new StringReader(content));

        assertEquals(Arrays.asList("alpha", "escaped key", "separator:key", "continued", "unicode:key"),
                enumerationToList(properties.keys()));
        assertEquals("two", properties.getProperty("alpha"));
        assertEquals("spaced value", properties.getProperty("escaped key"));
        assertEquals("value:with:colon", properties.getProperty("separator:key"));
        assertEquals("part1, part2", properties.getProperty("continued"));
        assertEquals("value=one", properties.getProperty("unicode:key"));
    }

    @Test
    void loadInputStreamUsesJdkIso88591ParsingAndKeepsFirstDuplicatePosition() throws Exception {
        OrderedProperties properties = new OrderedProperties();
        String content = "latin=ol\u00e9\n"
                + "escaped\\ space = has\\ spaces\n"
                + "unicode=\\u2603\n"
                + "latin=ol\u00e9-updated\n";

        properties.load(new ByteArrayInputStream(content.getBytes("ISO-8859-1")));

        assertEquals(Arrays.asList("latin", "escaped space", "unicode"), enumerationToList(properties.keys()));
        assertEquals("ol\u00e9-updated", properties.getProperty("latin"));
        assertEquals("has spaces", properties.getProperty("escaped space"));
        assertEquals("\u2603", properties.getProperty("unicode"));
    }

    @Test
    void loadRejectsMalformedUnicodeEscapesLikeProperties() {
        OrderedProperties properties = new OrderedProperties();

        assertThrows(IllegalArgumentException.class, () -> properties.load(new StringReader("bad=\\u12xz\n")));
    }

    @Test
    void storeWriterWritesLocalPropertiesInOrderWithJdkEscaping() throws Exception {
        OrderedProperties properties = new OrderedProperties();
        properties.setProperty("first", "1");
        properties.setProperty("space key", "value with spaces");
        properties.setProperty("colon:key", "a=b:c");
        properties.setProperty("leading", " value");

        StringWriter writer = new StringWriter();
        properties.store(writer, "comments");

        assertEquals(Arrays.asList("first=1", "space\\ key=value with spaces", "colon\\:key=a\\=b\\:c",
                "leading=\\ value"), propertyLines(writer.toString()));

        Properties standardProperties = new Properties();
        standardProperties.load(new StringReader(writer.toString()));
        assertEquals("value with spaces", standardProperties.getProperty("space key"));
        assertEquals("a=b:c", standardProperties.getProperty("colon:key"));
        assertEquals(" value", standardProperties.getProperty("leading"));
    }

    @Test
    void storeOutputStreamWritesLocalPropertiesInOrderWithJdkEscaping() throws Exception {
        OrderedProperties properties = new OrderedProperties();
        properties.setProperty("ascii", "plain");
        properties.setProperty("unicode", "ol\u00e9");
        properties.setProperty("tab", "a\tb");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        properties.store(outputStream, "comments");

        String output = outputStream.toString("ISO-8859-1");
        assertEquals(Arrays.asList("ascii=plain", "unicode=ol\\u00E9", "tab=a\\tb"), propertyLines(output));

        Properties standardProperties = new Properties();
        standardProperties.load(new ByteArrayInputStream(outputStream.toByteArray()));
        assertEquals("ol\u00e9", standardProperties.getProperty("unicode"));
        assertEquals("a\tb", standardProperties.getProperty("tab"));
    }

    @Test
    void stringPropertyNamesFiltersNonStringEntriesInOrder() {
        OrderedProperties properties = new OrderedProperties();
        properties.setProperty("first", "1");
        properties.put("non-string-value", Integer.valueOf(2));
        properties.put(Integer.valueOf(3), "non-string-key");
        properties.setProperty("second", "2");

        assertEquals(Arrays.asList("first", "second"), new ArrayList<String>(properties.stringPropertyNames()));
    }

    @Test
    void propertyNamesIncludesStringKeysWithNonStringValuesLikeStandardProperties() {
        Properties standardProperties = new Properties();
        standardProperties.setProperty("first", "1");
        standardProperties.put("non-string-value", Integer.valueOf(2));
        standardProperties.setProperty("second", "2");

        OrderedProperties properties = new OrderedProperties();
        properties.setProperty("first", "1");
        properties.put("non-string-value", Integer.valueOf(2));
        properties.setProperty("second", "2");

        List<Object> standardNames = enumerationToList(standardProperties.propertyNames());
        List<Object> orderedNames = enumerationToList(properties.propertyNames());
        assertTrue(standardNames.contains("non-string-value"));
        assertEquals(new LinkedHashSet<Object>(standardNames), new LinkedHashSet<Object>(orderedNames));
        assertEquals(Arrays.asList("first", "non-string-value", "second"), orderedNames);
    }

    @Test
    void defaultsAppearAfterLocalNamesAndStoreWritesOnlyLocalProperties() throws Exception {
        OrderedProperties defaults = new OrderedProperties();
        defaults.setProperty("default-first", "1");
        defaults.setProperty("shadowed", "default");
        defaults.setProperty("default-second", "2");

        OrderedProperties properties = new OrderedProperties(defaults);
        properties.setProperty("local", "value");
        properties.setProperty("shadowed", "local");

        List<String> expectedNames = Arrays.asList("local", "shadowed", "default-first", "default-second");
        assertEquals(expectedNames, enumerationToList(properties.propertyNames()));
        assertEquals(expectedNames, new ArrayList<String>(properties.stringPropertyNames()));

        StringWriter writer = new StringWriter();
        properties.store(writer, null);
        assertEquals(Arrays.asList("local=value", "shadowed=local"), propertyLines(writer.toString()));
    }

    @Test
    void standardErrorBehaviorIsPreserved() {
        OrderedProperties properties = new OrderedProperties();

        assertThrows(NullPointerException.class, () -> properties.put(null, "value"));
        assertThrows(NullPointerException.class, () -> properties.put("key", null));
        assertTrue(properties.keySet().isEmpty());

        properties.setProperty("string-key", "value");
        properties.put("non-string-value", Integer.valueOf(1));
        assertThrows(ClassCastException.class, () -> properties.store(new StringWriter(), null));

        OrderedProperties nonStringKeyProperties = new OrderedProperties();
        nonStringKeyProperties.put(Integer.valueOf(1), "value");
        assertThrows(ClassCastException.class, nonStringKeyProperties::propertyNames);
        assertThrows(ClassCastException.class, () -> nonStringKeyProperties.store(new StringWriter(), null));
    }

    @Test
    void serializationPreservesOrder() throws Exception {
        OrderedProperties properties = new OrderedProperties();
        properties.setProperty("first", "1");
        properties.setProperty("second", "2");
        properties.setProperty("third", "3");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(bytes);
        objectOutputStream.writeObject(properties);
        objectOutputStream.close();

        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        OrderedProperties restoredProperties = (OrderedProperties) objectInputStream.readObject();
        objectInputStream.close();

        assertEquals(Arrays.asList("first", "second", "third"), enumerationToList(restoredProperties.keys()));
    }

    private List<Object> enumerationToList(Enumeration<?> enumeration) {
        List<Object> result = new ArrayList<Object>();
        while (enumeration.hasMoreElements()) {
            result.add(enumeration.nextElement());
        }
        return result;
    }

    private List<Object> entryKeys(OrderedProperties properties) {
        List<Object> result = new ArrayList<Object>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            result.add(entry.getKey());
        }
        return result;
    }

    private List<String> oldStoreStyleLines(OrderedProperties properties) {
        List<String> result = new ArrayList<String>();
        Enumeration<Object> keys = properties.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            result.add(key + "=" + properties.get(key));
        }
        return result;
    }

    private List<String> modernStoreStyleLines(OrderedProperties properties) {
        List<String> result = new ArrayList<String>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            result.add(entry.getKey() + "=" + entry.getValue());
        }
        return result;
    }

    private List<String> pyDictionaryKeys(PyOrderedDict dictionary) {
        List<String> result = new ArrayList<String>();
        PyList keys = dictionary.keys();
        for (Object key : keys) {
            result.add(key.toString());
        }
        return result;
    }

    private void writeFile(File file, String content) throws Exception {
        FileOutputStream outputStream = new FileOutputStream(file);
        try {
            outputStream.write(content.getBytes("ISO-8859-1"));
        } finally {
            outputStream.close();
        }
    }

    private List<String> propertyLines(String content) throws Exception {
        List<String> result = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new StringReader(content));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("#") && !line.startsWith("!")) {
                result.add(line);
            }
        }
        return result;
    }
}
