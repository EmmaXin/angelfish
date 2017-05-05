package CoreDataService;

import java.util.Map;

// TODO: rename to BackupService
public interface PersistentStore {
    /**
     * config format:
     *     modified:    dateTime
     *   modifiedBy:    string
     *         size:    number
     *      content:    binary context
     *  description:    string
     */

    int save(String key, byte[] value, Map<String, String> meta);

    // TODO: change the data type of return value
    Map.Entry<byte[], Map<String, String>> load(String key);
}
