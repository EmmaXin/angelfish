package com.accton.common.store;

import java.io.IOException;
import java.util.Map;

public interface PersistentStoreDriver {
    void save(String key, byte[] value, Map<String, String> meta) throws IllegalArgumentException, IOException;

    // TODO: change the data type of return value
    Map.Entry<byte[], Map<String, String>> load(String key) throws IllegalArgumentException, IOException;

    // TODO: add delete function
}
