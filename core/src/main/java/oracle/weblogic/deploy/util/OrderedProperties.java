/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * Licensed under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl.
 */
package oracle.weblogic.deploy.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyTuple;

/**
 * A Properties implementation that preserves the order in which local properties are first added.
 * <p>
 * The exact ordered contract covers the normal Properties mutation paths and the Java 8+ mutators whose
 * signatures use only Object. Function-based Java 8+ mutators inherited from Properties are reconciled before
 * ordered reads and store operations so entries do not disappear or remain stale, but their exact insertion order
 * and inherited forEach iteration order are outside this Java 7-compatible class's ordered contract.
 */
public class OrderedProperties extends Properties {
    private static final long serialVersionUID = 1L;

    private LinkedHashSet<Object> orderedKeys = new LinkedHashSet<Object>();

    /**
     * The no-args constructor.
     */
    public OrderedProperties() {
        super();
    }

    /**
     * Creates an empty property list with the specified defaults.
     *
     * @param defaults the default properties
     */
    public OrderedProperties(Properties defaults) {
        super(defaults);
    }

    /**
     * Creates an ordered property list from a PyDictionary, using the dictionary's item iteration order.
     *
     * @param dictionary the dictionary
     */
    public OrderedProperties(PyDictionary dictionary) {
        this();
        putAll(dictionary);
    }

    /**
     * Creates an ordered property list from a PyOrderedDict.
     *
     * @param dictionary the ordered dictionary
     */
    public OrderedProperties(PyOrderedDict dictionary) {
        this();
        putAll(dictionary);
    }

    /**
     * Creates an ordered property list from a PyDictionary, using the dictionary's item iteration order.
     *
     * @param dictionary the dictionary
     * @return the ordered properties object
     */
    public static OrderedProperties fromPyDictionary(PyDictionary dictionary) {
        return new OrderedProperties(dictionary);
    }

    /**
     * Creates an ordered property list from a PyOrderedDict.
     *
     * @param dictionary the ordered dictionary
     * @return the ordered properties object
     */
    public static OrderedProperties fromPyOrderedDict(PyOrderedDict dictionary) {
        return new OrderedProperties(dictionary);
    }

    /**
     * Loads an ordered properties object from the specified file name.
     *
     * @param fileName the properties file name
     * @return the ordered properties object
     * @throws IOException if an error occurs loading the file
     */
    public static OrderedProperties load(String fileName) throws IOException {
        return load(new File(fileName));
    }

    /**
     * Loads an ordered properties object from the specified file.
     *
     * @param file the properties file
     * @return the ordered properties object
     * @throws IOException if an error occurs loading the file
     */
    public static OrderedProperties load(File file) throws IOException {
        OrderedProperties result = new OrderedProperties();
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            result.load(inputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return result;
    }

    /**
     * Loads a PyOrderedDict from the specified properties file name.
     *
     * @param fileName the properties file name
     * @return the ordered dictionary
     * @throws IOException if an error occurs loading the file
     */
    public static PyOrderedDict loadPyOrderedDict(String fileName) throws IOException {
        return load(fileName).toPyOrderedDict();
    }

    /**
     * Loads a PyOrderedDict from the specified properties file.
     *
     * @param file the properties file
     * @return the ordered dictionary
     * @throws IOException if an error occurs loading the file
     */
    public static PyOrderedDict loadPyOrderedDict(File file) throws IOException {
        return load(file).toPyOrderedDict();
    }

    /**
     * Stores the PyOrderedDict to the specified properties file name, replacing any existing file content.
     * This method uses the standard JDK Properties.store() behavior, including escaping, comments, and timestamp.
     *
     * @param dictionary the ordered dictionary
     * @param fileName the properties file name
     * @param comments optional comments
     * @throws IOException if an error occurs storing the file
     */
    public static void store(PyOrderedDict dictionary, String fileName, String comments) throws IOException {
        store(dictionary, new File(fileName), comments);
    }

    /**
     * Stores the PyDictionary to the specified properties file name, replacing any existing file content.
     * This method uses the standard JDK Properties.store() behavior, including escaping, comments, and timestamp.
     *
     * @param dictionary the dictionary
     * @param fileName the properties file name
     * @param comments optional comments
     * @throws IOException if an error occurs storing the file
     */
    public static void store(PyDictionary dictionary, String fileName, String comments) throws IOException {
        store(dictionary, new File(fileName), comments);
    }

    /**
     * Stores the PyOrderedDict to the specified properties file, replacing any existing file content.
     * This method uses the standard JDK Properties.store() behavior, including escaping, comments, and timestamp.
     *
     * @param dictionary the ordered dictionary
     * @param file the properties file
     * @param comments optional comments
     * @throws IOException if an error occurs storing the file
     */
    public static void store(PyOrderedDict dictionary, File file, String comments) throws IOException {
        OrderedProperties properties = fromPyOrderedDict(dictionary);
        store(properties, file, comments);
    }

    /**
     * Stores the PyDictionary to the specified properties file, replacing any existing file content.
     * This method uses the standard JDK Properties.store() behavior, including escaping, comments, and timestamp.
     *
     * @param dictionary the dictionary
     * @param file the properties file
     * @param comments optional comments
     * @throws IOException if an error occurs storing the file
     */
    public static void store(PyDictionary dictionary, File file, String comments) throws IOException {
        OrderedProperties properties = fromPyDictionary(dictionary);
        store(properties, file, comments);
    }

    private static void store(OrderedProperties properties, File file, String comments) throws IOException {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            properties.store(outputStream, comments);
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Object setProperty(String key, String value) {
        return put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        super.load(inStream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void load(Reader reader) throws IOException {
        super.load(reader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void store(OutputStream out, String comments) throws IOException {
        reconcileOrderedKeys();
        super.store(out, comments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void store(Writer writer, String comments) throws IOException {
        reconcileOrderedKeys();
        super.store(writer, comments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Object put(Object key, Object value) {
        if (key == null || value == null) {
            return super.put(key, value);
        }
        reconcileOrderedKeys();
        return putInOrder(key, value);
    }

    /*
     * These Object-signature methods intentionally omit @Override so this source can still compile against a
     * Java 7 API while overriding the Java 8+ Properties methods when they exist at runtime.
     */
    public synchronized Object putIfAbsent(Object key, Object value) {
        if (key == null || value == null) {
            return super.put(key, value);
        }

        reconcileOrderedKeys();
        Object previousValue = super.get(key);
        if (previousValue == null) {
            super.put(key, value);
            orderedKeys.add(key);
        }
        return previousValue;
    }

    public synchronized boolean remove(Object key, Object value) {
        if (key == null) {
            super.remove(key);
            return false;
        }
        if (value == null) {
            throw new NullPointerException();
        }

        reconcileOrderedKeys();
        Object currentValue = super.get(key);
        boolean removed = value.equals(currentValue);
        if (removed) {
            super.remove(key);
            orderedKeys.remove(key);
        }
        return removed;
    }

    public synchronized Object replace(Object key, Object value) {
        if (key == null || value == null) {
            return super.put(key, value);
        }

        reconcileOrderedKeys();
        Object previousValue = null;
        if (super.containsKey(key)) {
            previousValue = super.put(key, value);
        }
        return previousValue;
    }

    public synchronized boolean replace(Object key, Object oldValue, Object newValue) {
        if (oldValue == null || newValue == null) {
            throw new NullPointerException();
        }
        if (key == null) {
            super.containsKey(key);
            return false;
        }

        reconcileOrderedKeys();
        boolean replaced = oldValue.equals(super.get(key));
        if (replaced) {
            super.put(key, newValue);
        }
        return replaced;
    }

    private Object putInOrder(Object key, Object value) {
        boolean containedKey = super.containsKey(key);
        Object previousValue = super.put(key, value);
        if (!containedKey) {
            orderedKeys.add(key);
        }
        return previousValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void putAll(Map<?, ?> map) {
        if (map == null) {
            super.putAll(map);
            return;
        }

        reconcileOrderedKeys();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            putInOrder(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds all entries from the PyDictionary to this property list, using the dictionary's item iteration order.
     *
     * @param dictionary the dictionary
     */
    public synchronized void putAll(PyDictionary dictionary) {
        if (dictionary == null) {
            throw new NullPointerException();
        }

        reconcileOrderedKeys();
        PyList items = dictionary.items();
        for (int i = 0; i < items.size(); i++) {
            PyTuple tuple = (PyTuple) items.get(i);
            setProperty(toJavaString(tuple.get(0)), toJavaString(tuple.get(1)));
        }
    }

    /**
     * Adds all entries from the PyOrderedDict to this property list.
     *
     * @param dictionary the ordered dictionary
     */
    public synchronized void putAll(PyOrderedDict dictionary) {
        putAll((PyDictionary) dictionary);
    }

    /**
     * Converts this ordered property list to a PyOrderedDict.
     *
     * @return the ordered dictionary
     */
    public synchronized PyOrderedDict toPyOrderedDict() {
        reconcileOrderedKeys();
        PyOrderedDict result = new PyOrderedDict();
        for (Map.Entry<Object, Object> entry : entrySet()) {
            result.__setitem__(Py.java2py((String) entry.getKey()), Py.java2py((String) entry.getValue()));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Object remove(Object key) {
        if (key == null) {
            return super.remove(key);
        }

        reconcileOrderedKeys();
        Object previousValue = super.remove(key);
        if (previousValue != null) {
            orderedKeys.remove(key);
        }
        return previousValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void clear() {
        super.clear();
        orderedKeys.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Enumeration<Object> keys() {
        reconcileOrderedKeys();
        return Collections.enumeration(new LinkedHashSet<Object>(orderedKeys));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Enumeration<Object> elements() {
        reconcileOrderedKeys();
        return Collections.enumeration(orderedValues());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Set<Object> keySet() {
        reconcileOrderedKeys();
        return Collections.unmodifiableSet(new LinkedHashSet<Object>(orderedKeys));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Collection<Object> values() {
        reconcileOrderedKeys();
        return Collections.unmodifiableList(orderedValues());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Set<Map.Entry<Object, Object>> entrySet() {
        reconcileOrderedKeys();
        LinkedHashSet<Map.Entry<Object, Object>> result = new LinkedHashSet<Map.Entry<Object, Object>>();
        for (Object key : orderedKeys) {
            if (super.containsKey(key)) {
                result.add(new AbstractMap.SimpleImmutableEntry<Object, Object>(key, super.get(key)));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Enumeration<?> propertyNames() {
        reconcileOrderedKeys();
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        addLocalPropertyNames(result);
        addDefaultPropertyNames(result);
        return Collections.enumeration(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Set<String> stringPropertyNames() {
        reconcileOrderedKeys();
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        addLocalStringPropertyNames(result);
        addDefaultStringPropertyNames(result);
        return Collections.unmodifiableSet(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Object clone() {
        reconcileOrderedKeys();
        OrderedProperties clone = (OrderedProperties) super.clone();
        clone.orderedKeys = new LinkedHashSet<Object>(orderedKeys);
        return clone;
    }

    private void reconcileOrderedKeys() {
        LinkedHashSet<Object> reconciledKeys = new LinkedHashSet<Object>();
        if (orderedKeys != null) {
            for (Object key : orderedKeys) {
                if (key != null && super.containsKey(key)) {
                    reconciledKeys.add(key);
                }
            }
        }
        for (Object key : super.keySet()) {
            reconciledKeys.add(key);
        }
        orderedKeys = reconciledKeys;
    }

    private ArrayList<Object> orderedValues() {
        ArrayList<Object> result = new ArrayList<Object>();
        for (Object key : orderedKeys) {
            if (super.containsKey(key)) {
                result.add(super.get(key));
            }
        }
        return result;
    }

    private void addLocalPropertyNames(LinkedHashSet<String> result) {
        for (Object key : orderedKeys) {
            result.add((String) key);
        }
    }

    private void addDefaultPropertyNames(LinkedHashSet<String> result) {
        if (defaults != null) {
            Enumeration<?> names = defaults.propertyNames();
            while (names.hasMoreElements()) {
                result.add((String) names.nextElement());
            }
        }
    }

    private void addLocalStringPropertyNames(LinkedHashSet<String> result) {
        for (Object key : orderedKeys) {
            Object value = super.get(key);
            if (key instanceof String && value instanceof String) {
                result.add((String) key);
            }
        }
    }

    private void addDefaultStringPropertyNames(LinkedHashSet<String> result) {
        if (defaults != null) {
            result.addAll(defaults.stringPropertyNames());
        }
    }

    private static String toJavaString(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }
        if (value instanceof PyObject) {
            Object stringValue = ((PyObject) value).__tojava__(String.class);
            if (stringValue != Py.NoConversion) {
                return (String) stringValue;
            }
        }
        return value.toString();
    }

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        reconcileOrderedKeys();
    }
}
