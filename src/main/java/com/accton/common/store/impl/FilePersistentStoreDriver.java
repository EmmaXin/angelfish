package com.accton.common.store.impl;

import com.accton.common.store.DocumentMeta;
import com.accton.common.store.PersistentStoreDriver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import java.util.AbstractMap;
import java.util.Map;

public class FilePersistentStoreDriver implements PersistentStoreDriver {
    private String baseuri;

    public FilePersistentStoreDriver(String baseuri) {
        this.baseuri = baseuri;
    }

    private Path keyToFilePath(String key)
            throws IllegalArgumentException, FileSystemNotFoundException, SecurityException {
        String[] parts = key.split("\\.");

        String path = String.join(File.separator, parts);

        return Paths.get(this.baseuri, path);
    }

    private void writeFile(String path, byte[] value) throws IOException {
        OutputStream outputStream = new FileOutputStream(path);
        outputStream.write(value);
    }

    private byte[] readFile(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    public DocumentMeta save(String key, byte[] value, Map<String, Object> meta) throws IllegalArgumentException, IOException {
        DocumentMeta documentMeta = DocumentMeta.create2(meta);
        return save(key, value, documentMeta);
    }

    public DocumentMeta save(String key, byte[] value, DocumentMeta meta) throws IllegalArgumentException, IOException {
        Path path;

        try {
            path = keyToFilePath(key);

            if (path.getNameCount() < 2) {
                throw new IllegalArgumentException("invalid path (" + path + ")");
            }

            File parent = path.getParent().toFile();
            parent.mkdirs();
        } catch (FileSystemNotFoundException | SecurityException e) {
            throw new IllegalArgumentException("invalid key (" + key + "): ", e);
        }

        String fileExtension = meta.getString("fileExtension", "");
        writeFile(path.toString() + fileExtension, value);

        meta.put("fileUrl", path.toString() + fileExtension);
        meta.put("size", value.length);

        writeFile(path.toString() + ".meta.json", meta.toJsonString().getBytes());
        return meta;
    }

    public Map.Entry<byte[], DocumentMeta> load(String key) throws IllegalArgumentException, IOException {
        byte[] value = null;
        Path path = keyToFilePath(key);

        byte[] m = readFile(path.toString() + ".meta.json");
        String str = new String(m, StandardCharsets.UTF_8);

        DocumentMeta documentMeta = DocumentMeta.create(str);
        if (documentMeta == null) {
            throw new IOException("meta file is not valid json format.");
        }

        String s = documentMeta.getFileUrl();
        if (s != null) {
            value = readFile(s);
        }

        return new AbstractMap.SimpleEntry<>(value, documentMeta);
    }
}
