package com.accton.common.store;

import java.io.File;
import java.io.IOException;
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

    public String toJsonString() {
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
    static final long DEFAULT_AUTO_SAVE_INTERVAL_MILLI_SECONDS = 10 * 60 * 1000;

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
        this(packageName, persistentStoreDriver, DEFAULT_AUTO_SAVE_INTERVAL_MILLI_SECONDS);
    }

    // TODO: using async interface
    public void dataChanged(String key) throws IOException {
        Map.Entry<byte[], Map<String, String>> data = persistentStoreDriver.load(key);

        String[] parts = key.split("\\.");

        parts[parts.length - 1] = "backups";
        String backupId = String.join(File.separator, parts) + "." + data.getValue().get("id");

        try {
            persistentStoreDriver.save(backupId, data.getKey(), data.getValue());
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e.getCause());
        }

        {
            Integer size = Integer.parseInt(data.getValue().getOrDefault("size", "-1"));
            String fileUri = data.getValue().get("fileUri");

            // TODO: the document meta should be obtained from options
            DocumentMeta documentMeta = DocumentMeta.create(data.getValue());

            this.versionHistoryCache.add(documentMeta);

            String versionHistoryJson = this.versionHistoryCache.toJsonString();

            byte[] v = versionHistoryJson.getBytes();

            Map<String, String> versionHistoryCacheMeta = new HashMap<>();

            Date now = Calendar.getInstance().getTime();
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(now);

            versionHistoryCacheMeta.put("modified", timeStamp);
            versionHistoryCacheMeta.put("fileExtension", ".json");

            try {
                this.persistentStoreDriver.save(this.packageName + ".versionHistoryCache", v, versionHistoryCacheMeta);
            } catch (IllegalArgumentException e) {
                throw new IOException(e.getMessage(), e.getCause());
            }
        }
    }

    // TODO: using async interface
    public Map.Entry<byte[], Map<String, String>> find(String versionId)
            throws IllegalArgumentException, IOException {
        DocumentMeta docMeta = this.versionHistoryCache.find(versionId);

        if (docMeta == null) {
            throw new IllegalArgumentException("Can't find this version " + versionId);
        }

        return this.persistentStoreDriver.load(this.packageName + ".backups." + versionId);
    }

    public Map<String, DocumentMeta[]> getAllVersions() {
        ArrayList<DocumentMeta> historyList  = this.versionHistoryCache.getHistoryList();
        Map<String, ArrayList<DocumentMeta>> allVersions = new HashMap<>();
        Map<String, Date> lastSaveDate = new HashMap<>();

        for (ListIterator iterator = historyList.listIterator(historyList.size()); iterator.hasPrevious();) {
            DocumentMeta doc = (DocumentMeta) iterator.previous();

            if (allVersions.get(doc.getKey()) == null) {
                allVersions.put(doc.getKey(), new ArrayList<>());
            }

            //if (key.equals(doc.getKey())) {
            try {
                Date current = BackupService.getDocumentModifiedDate(doc);

                if (lastSaveDate.get(doc.getKey()) == null) {
                    ArrayList<DocumentMeta> l = allVersions.get(doc.getKey());
                    l.add(0, doc);
                    lastSaveDate.put(doc.getKey(), current);
                } else {
                    long diffMillis = current.getTime() - lastSaveDate.get(doc.getKey()).getTime();
                    if (this.autoSaveIntervalMilliSeconds <= diffMillis) {
                        //allVersions.add(0, doc);
                        ArrayList<DocumentMeta> l = allVersions.get(doc.getKey());
                        l.add(0, doc);
                        lastSaveDate.put(doc.getKey(), current);
                    } else {
                        ArrayList<DocumentMeta> l = allVersions.get(doc.getKey());
                        l.remove(0);
                        l.add(0, doc);
                        //allVersions.remove(0);
                        //allVersions.add(0, doc);
                    }
                }
            } catch (ParseException e) {
                // TODO: log bad record
            }
            //}
        }

        Map<String, DocumentMeta[]> ret = new HashMap<>();
        for (Map.Entry<String, ArrayList<DocumentMeta>> entry : allVersions.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                ret.put(entry.getKey(), entry.getValue().toArray(new DocumentMeta[0]));
            }
        }

        return ret;
    }

    // TODO: Add getKeys() : string[]

    public DocumentMeta[] getAllVersions(String key) {
        ArrayList<DocumentMeta> historyList  = this.versionHistoryCache.getHistoryList();
        ArrayList<DocumentMeta> allVersions = new ArrayList<>();
        Date lastSaveDate = null;

        for (ListIterator iterator = historyList.listIterator(historyList.size()); iterator.hasPrevious();) {
            DocumentMeta doc = (DocumentMeta) iterator.previous();

            if (key.equals(doc.getKey())) {
                try {
                    Date current = BackupService.getDocumentModifiedDate(doc);

                    if (lastSaveDate == null) {
                        allVersions.add(0, doc);
                        lastSaveDate = current;
                    } else {
                        long diffMillis = current.getTime() - lastSaveDate.getTime();
                        if (this.autoSaveIntervalMilliSeconds <= diffMillis) {
                            allVersions.add(0, doc);
                            lastSaveDate = current;
                        } else {
                            allVersions.remove(0);
                            allVersions.add(0, doc);
                        }
                    }
                } catch (ParseException e) {
                    // TODO: log bad record
                }
            }
        }

        return allVersions.toArray(new DocumentMeta[0]);
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

    private Calendar calendarInstance;  // for internal testing
    private BackupService backupService;

    private DocumentMeta currentDoc;

    public PersistentStoreManager(String packageName, PersistentStoreDriver persistentStoreDriver) {
        this.persistentStoreDriver = persistentStoreDriver;
        this.packageName = packageName;

        this.currentDoc = null;

        // TODO: load veersionHistoryCache from persistent storage
        // TODO: rebuild versionHistoryCache from persistent storage

        calendarInstance = null;
        this.backupService = new BackupService(packageName, persistentStoreDriver);
    }

    public void init() {
        // TODO: load currentDoc
        // TODO: load versionHistory
    }

    // TODO: accept string format content
    // TODO: accept json format content
    public int save(byte[] content, String description /*, BackupObserver observer*/) throws IOException {
        int ret = 0;

        Calendar calendar = this.calendarInstance;
        if (this.calendarInstance == null) {
            calendar = Calendar.getInstance();
        }

        Date now = calendar.getTime();

        Map<String, String> meta = save(this.packageName + ".current", content, now, this.persistentStoreDriver);
        this.currentDoc = DocumentMeta.create(meta);

        this.backupService.dataChanged(this.packageName + ".current");

        return ret;
    }

    public byte[] restore(String versionId /*, BackupObserver observer*/)
            throws IllegalArgumentException, IOException {
        try {
            Map.Entry<byte[], Map<String, String>> result = this.backupService.find(versionId);
            this.persistentStoreDriver.save(this.packageName + ".current", result.getKey(), result.getValue());
            this.currentDoc = DocumentMeta.create(result.getValue());
            return result.getKey();
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e.getCause());
        }
    }

    // TODO: return versionHistoryCache plus self information, like current used config, packetname ... etc
    public Map<String, DocumentMeta[]> getAllVersions() {
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

        try {
            Map.Entry<byte[], Map<String, String>> result = this.persistentStoreDriver.load(this.packageName + ".current");
            return result.getKey();
        } catch (IllegalArgumentException | IOException e) {
            return null;
        }
    }

    protected void setCalendarInstance(Calendar calendarInstance) {
        this.calendarInstance = calendarInstance;
    }

    public static Map<String, String> save(String key, byte[] content, Date now, PersistentStoreDriver persistentStoreDriver) throws IOException {
        Map<String, String> meta = new HashMap<>();

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(now);
        String id = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(now);

        // TODO: id is timeStamp, it will change each saving operation. NOT id.
        meta.put("id", id);
        meta.put("key", key);
        meta.put("modified", timeStamp);
        meta.put("modifiedFormat", "yyyy-MM-dd HH:mm:ss.SSS");
        meta.put("fileExtension", ".json");

        persistentStoreDriver.save(key, content, meta);
        // TODO: check return value, if error case

        return meta;
    }
}
