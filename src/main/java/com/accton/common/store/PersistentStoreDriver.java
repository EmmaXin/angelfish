package com.accton.common.store;

import java.io.IOException;
import java.util.Map;

public interface PersistentStoreDriver {
    DocumentMeta save(String key, byte[] value, Map<String, Object> meta) throws IllegalArgumentException, IOException;
    DocumentMeta save(String key, byte[] value, DocumentMeta meta) throws IllegalArgumentException, IOException;

    // TODO: change the data type of return value
    Map.Entry<byte[], DocumentMeta> load(String key) throws IllegalArgumentException, IOException;

    // TODO: add delete function
}
