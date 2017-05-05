package CoreDataService.impl;

import CoreDataService.PersistentStore;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.xml.sax.*;
import org.w3c.dom.*;

public class FilePersistentStore implements PersistentStore {
    private static String extension = ".xml";

    private String baseuri;

    public FilePersistentStore(String baseuri) {
        this.baseuri = baseuri;
    }

    Document serialize(Document dom, Map<String, String> options) {

        /**
         * <?xml version="1.0" encoding="UTF-8" standalone="no"?>
         * <!DOCTYPE roles SYSTEM "roles.dtd">
         * <doc>
         *   <modified>role1-data</modified>
         *   <modifiedBy>role2-data</modifiedBy>
         *   <size>role3-data</size>
         *   <content>role4-data</content>
         *   <description>role4-data</description>
         * </doc>
         */

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

        return dom;
    }

    Map<String, String> deserialize(Document dom) {
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

    // TODO: write 2 file, one is original file, another is meta file
    int writeXmlFile(String path, Document dom) {
        try {
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            // TODO: it should be able to support dtd file
//            tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "roles.dtd");
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            tr.transform(new DOMSource(dom),
                    new StreamResult(new FileOutputStream(path)));

        } catch (TransformerException te) {
            System.out.println(te.getMessage());
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }

        return 0;
    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    static Path getPath(String db, String col, String doc) {
        return Paths.get(db, col, doc + ".xml");
    }

//    public static boolean isValidPath(String uri) {
//        try {
//            Path path = Paths.get(uri);
//        } catch (IllegalArgumentException e) {
//            return false;
//        } catch (FileSystemNotFoundException e) {
//            return false;
//        } catch (SecurityException e) {
//            return false;
//        }
//
//        return true;
//    }

    Path keyToFilePath(String key) {
        String[] parts = key.split("\\.");

        String path = String.join(File.separator, parts);

        return Paths.get(this.baseuri, path);
    }

    int writeFile(String path, byte[] value) {
        try{
            OutputStream outputStream = new FileOutputStream(path);
            outputStream.write(value);
        } catch (IOException ex) {
            return -1; // TODO: instead hard code by enum value
        }

        return 0;
    }

    static byte[] readFile(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException ex) {
            return null;
        }
    }

    int writeMetaFile(String path, Map<String, String> meta) {
        DocumentBuilderFactory dbf;
        try {
            dbf = DocumentBuilderFactory.newInstance();
        } catch (FactoryConfigurationError e) {
            return -1; // TODO: instead hard code by enum value
        }

        DocumentBuilder _db;
        try {
            _db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            return -2; // TODO: instead hard code by enum value
        }

        Document dom = _db.newDocument();

        if (dom == null) {
            return -3; // TODO: instead hard code by enum value
        }

        serialize(dom, meta);

        int ret = writeXmlFile(path, dom);

        if (ret != 0) {
            return -6;
        }

        return 0;
    }

    public Map<String, String> loadXmlFile(String path) {
        Document dom;
        // Make an  instance of the DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // use the factory to take an instance of the document builder
            DocumentBuilder _db = dbf.newDocumentBuilder();
            // parse using the builder to get the DOM mapping of the
            // XML file
            dom = _db.parse(path);

            return deserialize(dom);

            // TODO: which operation will throw this exception, or miss others
        } catch (ParserConfigurationException pce) {

            // TODO: use return code instead print out to console.
            System.out.println(pce.getMessage());
        } catch (SAXException se) {
            System.out.println(se.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }

        return new HashMap<String, String>();
    }

    // TODO: change the signature to `int save(key, value, meta)`
    public int save(String key, byte[] value, Map<String, String> meta) {
        Path path = keyToFilePath(key);

        if (path.getNameCount() < 2) {
            return -1; // TODO: instead hard code by enum value, and save log if something wrong
        }

        try {
            File parent = path.getParent().toFile();
            parent.mkdirs();
        } catch (SecurityException e) {
            return -2; // TODO: instead hard code by enum value, and save log if something wrong
        }

        String fileExtension = meta.getOrDefault("fileExtension", "");

        // write file ...
        if (writeFile(path.toString() + fileExtension, value) != 0) {
            return -5; // TODO: instead hard code by enum value, and save log if something wrong
        }

        // TODO: Save content in external location not in `meta` file. Rename current file as <name>$meta.json
        // TODO: put encode field as 'hex'
        meta.put("fileUrl", path.toString() + fileExtension);
        meta.put("size", String.valueOf(value.length));

        if (writeMetaFile(path.toString() + ".meta" + FilePersistentStore.extension, meta) != 0) {
            return -6; // TODO: instead hard code by enum value, and save log if something wrong
        }

        return 0;
    }

    public Map.Entry<byte[], Map<String, String>> load(String key) {
        byte[] value = null;

        Path path = keyToFilePath(key);

        Map<String, String> meta = loadXmlFile(path.toString() + ".meta" + FilePersistentStore.extension);

        String s = meta.get("fileUrl");
        if (s != null) {
            value = FilePersistentStore.readFile(s);
        }

        return new AbstractMap.SimpleEntry<byte[], Map<String, String>>(value, meta);
    }

    // TODO: provide async interface
    public int save(String db, String col, String doc, byte[] value, Map<String, String> options) {
        Document dom;

        Path path = getPath(db, col, doc);
        // TODO: ensure the directories had created before access it.

        // instance of a DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // use factory to get an instance of document builder
            DocumentBuilder _db = dbf.newDocumentBuilder();

            // create instance of DOM
            dom = _db.newDocument();

            // TODO: Save content in external location not in `meta` file. Rename current file as <name>$meta.json
            options.put("content", bytesToHex(value));
            options.put("size", String.valueOf(value.length));
            // TODO: put encode field as 'hex'

            serialize(dom, options);

            writeXmlFile(path.toString(), dom);

            // TODO: which operation will throw this exception, or miss others
        } catch (ParserConfigurationException pce) {
            // TODO: use return code instead print out to console.
            System.out.println("UsersXML: Error trying to instantiate DocumentBuilder " + pce);
        }

        return 0;
    }
}
