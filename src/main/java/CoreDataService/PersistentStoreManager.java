package CoreDataService;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * sql:     database -- table      -- row -- column
 * mongodb: database -- collection -- doc -- field   (v)
 * es:      index    -- type       -- doc -- field
 *
 * +-- appHome
 *     +-- config
 *         +-- <packageName>                <--     database
 *             +-- current                  <--     collection
 *                  +-- current.json        <--     document, `current` is document ID
 *                  +-- versionHistory.json <--     document
 *             +-- backup                   <--     collection
 *                 +-- <versionId>.json
 *                 +-- <versionId>.json
 *                 +-- ...
 *
 * document schema:
 *     modified:    dateTime        (required)
 *   modifiedBy:    string
 *         size:    number          (auto)
 *      content:    binary content  (required)
 *  description:    string
 *
 * backup collection
 *    versionId:    string          (milliseconds since 1970)
 *
 */


class VersionHistoryCache {
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
    private ArrayList<DocumentMeta> versionHistory;

    public VersionHistoryCache() {
        this.versionHistory = new ArrayList<DocumentMeta>();
    }

    public boolean add(DocumentMeta record) {
        this.versionHistory.add(0, record);
        return true;
    }

    public DocumentMeta find(String docId) {
        for (DocumentMeta docMeta : this.versionHistory) {
            if (docMeta.getId().equals(docId)) {
                return docMeta;
            }
        }

        return null;
    }

    public ArrayList<DocumentMeta> getHistoryList() {
        return this.versionHistory;
    }

    public String toString() {
        String s = "[\n";

        for (DocumentMeta entry : this.versionHistory) {
            if (!s.equals("[\n")) {
                s += ",";
            }

            s += entry.toJsonString() + "\n";
        }

        s += "]";

        return s;
    }
}

/*
interface BackupObserver {
    void onComplete(int error);
}
*/

class NoSuchVersionException extends RuntimeException {
    public NoSuchVersionException(String s) {
        super(s);
    }
}

class BackupService {
    private String packageName;
    private PersistentStoreDriver persistentStoreDriver;
    private VersionHistoryCache versionHistoryCache;

    BackupService(String packageName, PersistentStoreDriver persistentStoreDriver) {
        this.packageName = packageName;
        this.persistentStoreDriver = persistentStoreDriver;
        this.versionHistoryCache = new VersionHistoryCache();
    }

    // TODO: using async interface
    public void dataChanged(String key) {
        Map.Entry<byte[], Map<String, String>> data = persistentStoreDriver.load(key);

        String[] parts = key.split("\\.");

        parts[parts.length - 1] = "backups";
        String backupId = String.join(File.separator, parts) + "." + data.getValue().get("id");

        int ret = persistentStoreDriver.save(backupId, data.getKey(), data.getValue());
        assert(ret == 0); // TODO: re-throw except if error

        {
            Integer size = Integer.parseInt(data.getValue().getOrDefault("size", "-1"));
            String fileUri = data.getValue().get("fileUri");

            // TODO: the document meta should be obtained from options
            DocumentMeta record = new DocumentMeta(data.getValue().get("id"), data.getValue().get("modified"), size, fileUri);

            this.versionHistoryCache.add(record);

            String versionHistoryJson = this.versionHistoryCache.toString();

            byte[] v = versionHistoryJson.getBytes();

            Map<String, String> versionHistoryCacheMeta = new HashMap<>();

            Date now = Calendar.getInstance().getTime();
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(now);

            versionHistoryCacheMeta.put("modified", timeStamp);
            versionHistoryCacheMeta.put("fileExtension", ".json");

            ret = this.persistentStoreDriver.save(this.packageName + ".versionHistoryCache", v, versionHistoryCacheMeta);
            assert(ret == 0); // TODO: re-throw except if error
        }
    }

    // TODO: using async interface
    public Map.Entry<byte[], Map<String, String>> find(String versionId)
            throws NoSuchVersionException, NoSuchFileException {
        DocumentMeta docMeta = this.versionHistoryCache.find(versionId);

        if (docMeta == null) {
            throw new NoSuchVersionException(versionId);
        }

        Map.Entry<byte[], Map<String, String>> result = this.persistentStoreDriver.load(this.packageName + ".backups." + versionId);
        if (result.getKey() == null) {
            throw new NoSuchFileException("this.packageName + \".backups.\" + versionId");
        }

        return result;
    }

    public ArrayList<DocumentMeta> getAllVersion() {
        return this.versionHistoryCache.getHistoryList();
    }
}

public class PersistentStoreManager {
    private PersistentStoreDriver persistentStoreDriver;
    private String packageName;

    private BackupService backupService;

    private DocumentMeta currentDoc;

    public PersistentStoreManager(String packageName, PersistentStoreDriver persistentStoreDriver) {
        this.persistentStoreDriver = persistentStoreDriver;
        this.packageName = packageName;

        this.currentDoc = null;

        // TODO: load veersionHistoryCache from persistent storage
        // TODO: rebuild versionHistoryCache from persistent storage

        this.backupService = new BackupService(packageName, persistentStoreDriver);
    }

    public void init() {
        // TODO: load currentDoc
        // TODO: load versionHistory
    }

    /**
     * Call to save data
     * @param content
     * @param observer, null means sync-call
     */
    // TODO: accept string format content
    // TODO: accept json format content
    public int save(byte[] content /*, BackupObserver observer*/) {
        int ret;

        Map<String, String> meta = new HashMap<>();

        Date now = Calendar.getInstance().getTime();

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(now);
        String id = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(now);

        meta.put("id", id); // TOD: id is timeStamp, it will change each saving operation. NOT id.
        meta.put("modified", timeStamp);
        meta.put("fileExtension", ".json");

        {
            ret = this.persistentStoreDriver.save(this.packageName + ".current", content, meta);
            if (ret == 0) {
                this.currentDoc = DocumentMeta.create(meta);
            }

            // TODO: check return value, if error case
        }

        this.backupService.dataChanged(this.packageName + ".current");

        return ret;
    }

    /**
     * Call to restore
     * @param versionId
     * @param observer, null means sync-call
     * @return
     */
    public byte[] restore(String versionId /*, BackupObserver observer*/)
            throws NoSuchVersionException, NoSuchFileException {
        Map.Entry<byte[], Map<String, String>> result = this.backupService.find(versionId);

        Integer ret = this.persistentStoreDriver.save(this.packageName + ".current", result.getKey(), result.getValue());
        this.currentDoc = DocumentMeta.create(result.getValue());

        return result.getKey();
    }

    // TODO: return versionHistoryCache plus self information, like current used config, packetname ... etc
    public ArrayList<DocumentMeta> getVersionHistory() {
        return this.backupService.getAllVersion();
    }

    public String getCurrentVersionId() {
        if (this.currentDoc == null) {
            return null;
        }

        return this.currentDoc.getId();
    }

    public byte[] getCurrentVersionContent() {
        if (this.currentDoc == null) {
            return null;
        }

        Map.Entry<byte[], Map<String, String>> result = this.persistentStoreDriver.load(this.packageName + ".current");
        return result.getKey();
    }
}
