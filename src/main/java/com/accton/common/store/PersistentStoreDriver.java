package com.accton.common.store;

import java.util.Map;

public interface PersistentStoreDriver {
    int save(String key, byte[] value, Map<String, String> meta);

    // TODO: change the data type of return value
    Map.Entry<byte[], Map<String, String>> load(String key);

    // TODO: add delete function
}
