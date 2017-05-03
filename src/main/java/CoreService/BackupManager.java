package CoreService;

/**
 * sql:     database -- table      -- row -- column
 * mongodb: database -- collection -- doc -- field   (v)
 * es:      index    -- type       -- doc -- field
 *
 * +-- appHome
 *     +-- config
 *         +-- packageName
 *             +-- current.json
 *             +-- versionHistory.json
 *             +-- backup
 *                 +-- <id>.json
 *                 +-- <id>.json
 *                 +-- <id>.json
 *
 * version history (fields)
 *           id:    string (milliseconds since 1970): key
 *     modified:    dateTime
 *   modifiedBy:    string
 *         size:    number
 *      content:    binary context
 *  description:    string
 *
 */

interface BackupService {
    /**
     * config format:
     *     modified:    dateTime
     *   modifiedBy:    string
     *         size:    number
     *      content:    binary context
     *  description:    string
     */

    // save(key, value, options={modified / modifiedBy / description})
    // load()
}

interface VersionHistory {
    /**
     * version history format:
     *  packageName:    string: <packageName>
     *      current:    string: <version id>
     *       backup:    array
     *
     *       backup element:
     *           id:    string (milliseconds since 1970): key
     *     modified:    dateTime
     *   modifiedBy:    string
     *         size:    number
     *  description:    string
     */
}

public class BackupManager {

    public BackupManager(String packageName, BackupService backupService) {
    }

    public void dataChanged() {
        // backService.save(<appName>.<packageName>.config.backup.<id>, content)
        // backService.save(<appName>.<packageName>.current, content)

        // backService.save(<appName>.<packageName>.versionHistory, meta)
    }

    public int requestRestore(String versionId) {
        // backSerice.load(...)
        return 0;
    }

    public VersionHistory getVersionHistory() {
        return null;
    }
}
