package com.accton.common.store.impl;

import com.accton.common.store.PersistentStoreDriver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

public class FilePersistentStoreDriver implements PersistentStoreDriver {
    private String baseuri;
    private static String extension;

    static {
        extension = ".xml";
    }

    public FilePersistentStoreDriver(String baseuri) {
        this.baseuri = baseuri;
    }

    private void serialize(Document dom, Map<String, String> options) {

        //
        // <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        // <!DOCTYPE roles SYSTEM "roles.dtd">
        // <doc>
        //   <modified>role1-data</modified>
        //   <modifiedBy>role2-data</modifiedBy>
        //   <size>role3-data</size>
        //   <content>role4-data</content>
        //   <description>role4-data</description>
        // </doc>
        //

        // create the root element
        Element rootEle = dom.createElement("doc");
        dom.appendChild(rootEle);

        for (Map.Entry<String, String> entry : options.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            Element e = dom.createElement(key);
            e.appendChild(dom.createTextNode(value));
            rootEle.appendChild(e);
        }
    }

    private Map<String, String> deserialize(Document dom) {
        Map<String, String> result = new HashMap<String, String>();

        Element rootEle = dom.getDocumentElement();

        NodeList nodeList = rootEle.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                String key = currentNode.getNodeName();
                String value = currentNode.getTextContent();

                result.put(key, value);
            }
        }

        return result;
    }

    private Path keyToFilePath(String key)
            throws IllegalArgumentException, FileSystemNotFoundException, SecurityException {
        String[] parts = key.split("\\.");

        String path = String.join(File.separator, parts);

        return Paths.get(this.baseuri, path);
    }

    // TODO: write 2 file, one is original file, another is meta file
    private void writeXmlFile(String path, Document dom) throws IOException {
        try {
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            // TODO: it should be able to support dtd file
            // tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "roles.dtd");
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            tr.transform(new DOMSource(dom),
                    new StreamResult(new FileOutputStream(path)));
        } catch (TransformerException e) {
            throw new IOException(e.getMessage(), e.getCause());
        }
    }

    private void writeFile(String path, byte[] value) throws IOException {
        OutputStream outputStream = new FileOutputStream(path);
        outputStream.write(value);
    }

    private byte[] readFile(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    private void writeMetaFile(String path, Map<String, String> meta) throws IOException {
        DocumentBuilderFactory dbf;
        DocumentBuilder documentBuilder;

        try {
            dbf = DocumentBuilderFactory.newInstance();
            documentBuilder = dbf.newDocumentBuilder();
        } catch (FactoryConfigurationError | ParserConfigurationException e) {
            throw new IOException(e.getMessage(), e.getCause());
        }

        Document dom = documentBuilder.newDocument();

        serialize(dom, meta);
        writeXmlFile(path, dom);
    }

    public Map<String, String> loadXmlFile(String path) throws IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            Document dom = documentBuilder.parse(path);

            return deserialize(dom);
        } catch (FactoryConfigurationError | ParserConfigurationException | SAXException e) {
            throw new IOException(e.getMessage(), e.getCause());
        }
    }

    // TODO: change the signature to `int save(key, value, meta)`
    public void save(String key, byte[] value, Map<String, String> meta) throws IllegalArgumentException, IOException {
        Path path;

        try {
            path = keyToFilePath(key);

            if (path.getNameCount() < 2) {
                throw new IllegalArgumentException("path.getNameCount() < 2");
            }

            File parent = path.getParent().toFile();
            parent.mkdirs();
        } catch (FileSystemNotFoundException | SecurityException e) {
            throw new IllegalArgumentException(e.getMessage(), e.getCause());
        }

        String fileExtension = meta.getOrDefault("fileExtension", "");
        writeFile(path.toString() + fileExtension, value);

        // TODO: Save content in external location not in `meta` file. Rename current file as <name>$meta.json
        // TODO: put encode field as 'hex'
        meta.put("fileUrl", path.toString() + fileExtension);
        meta.put("size", String.valueOf(value.length));

        writeMetaFile(path.toString() + ".meta" + FilePersistentStoreDriver.extension, meta);
    }

    public Map.Entry<byte[], Map<String, String>> load(String key) throws IllegalArgumentException, IOException {
        byte[] value = null;

        Path path = keyToFilePath(key);

        Map<String, String> meta = loadXmlFile(path.toString() + ".meta" + FilePersistentStoreDriver.extension);

        String s = meta.get("fileUrl");
        if (s != null) {
            value = readFile(s);
        }

        return new AbstractMap.SimpleEntry<>(value, meta);
    }
}
