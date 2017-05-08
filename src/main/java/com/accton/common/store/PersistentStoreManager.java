package com.accton.common.store;

import javax.print.Doc;
import java.io.File;
import java.nio.file.NoSuchFileException;
import java.text.DateFormat;
import java.text.ParseException;
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

    private long autoSaveIntervalMilliSeconds;

    BackupService(String packageName, PersistentStoreDriver persistentStoreDriver, long autoSaveIntervalMilliSeconds) {
        this.packageName = packageName;
        this.persistentStoreDriver = persistentStoreDriver;
        this.versionHistoryCache = new VersionHistoryCache();

        this.autoSaveIntervalMilliSeconds = autoSaveIntervalMilliSeconds;
    }

    BackupService(String packageName, PersistentStoreDriver persistentStoreDriver) {
        this(packageName, persistentStoreDriver, 10 * 60 * 1000);
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
            DocumentMeta documentMeta = DocumentMeta.create(data.getValue());

            this.versionHistoryCache.add(documentMeta);

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

    // TODO: Change the return value to Map<String, DocumentMeta[]>
    public ArrayList<DocumentMeta> getAllVersions() {
        return this.versionHistoryCache.getHistoryList();
    }

    // TODO: Add getKeys() : string[]

    public DocumentMeta[] getAllVersions(String key) {
        ArrayList<DocumentMeta> allVersions  = this.versionHistoryCache.getHistoryList();
        ArrayList<DocumentMeta> list = new ArrayList<>();

        Date lastSaveDate = null;

        for (ListIterator iterator = allVersions.listIterator(allVersions.size()); iterator.hasPrevious();) {
            DocumentMeta doc = (DocumentMeta) iterator.previous();

            if (key.equals(doc.getKey())) {
                try {
                    Date current = BackupService.getDocumentModifiedDate(doc);

                    if (lastSaveDate == null) {
                        list.add(0, doc);
                        lastSaveDate = current;
                    } else {
                        long diffMillis = current.getTime() - lastSaveDate.getTime();
                        if (this.autoSaveIntervalMilliSeconds <= diffMillis) {
                            list.add(0, doc);
                            lastSaveDate = current;
                        } else {
                            list.remove(0);
                            list.add(0, doc);
                        }
                    }
                } catch (ParseException e) {
                    // TODO: log bad record
                }
            }
        }

        return list.toArray(new DocumentMeta[0]);
    }

    static Date getDocumentModifiedDate(DocumentMeta documentMeta) throws ParseException {
        String format = documentMeta.getModifiedFormat();
        String modified = documentMeta.getModified();

        DateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.parse(modified);
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
     * Call to save data.
     * @param content
     */
    // TODO: accept string format content
    // TODO: accept json format content
    public int save(byte[] content) {
        int ret = 0;

        Date now = Calendar.getInstance().getTime();

        Map<String, String> meta = save(this.packageName + ".current", content, now, this.persistentStoreDriver);
        this.currentDoc = DocumentMeta.create(meta);

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
    public ArrayList<DocumentMeta> getAllVersions() {
        return this.backupService.getAllVersions();
    }

    public DocumentMeta[] getAllVersions(String key) {
        return this.backupService.getAllVersions(key);
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

    public static Map<String, String> save(String key, byte[] content, Date now, PersistentStoreDriver persistentStoreDriver) {
        Map<String, String> meta = new HashMap<>();

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(now);
        String id = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(now);

        meta.put("id", id); // TOD: id is timeStamp, it will change each saving operation. NOT id.
        meta.put("key", key);
        meta.put("modified", timeStamp);
        meta.put("fileExtension", ".json");

        persistentStoreDriver.save(/*"test" + ".current"*/ key, content, meta);
        // TODO: check return value, if error case

        return meta;
    }
}
