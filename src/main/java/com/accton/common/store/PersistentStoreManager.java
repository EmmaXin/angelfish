package com.accton.common.store;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
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

    public void init() throws IOException {
        Map.Entry<byte[], DocumentMeta> data = this.persistentStoreDriver.load(this.packageName + ".versionHistoryCache");
        byte[] bytes =  data.getKey();
        String jsonString = new String(bytes, StandardCharsets.UTF_8);

        try {
            JSONArray array = new JSONArray(jsonString);

            this.versionHistoryCache = new VersionHistoryCache();
            for (Object item : array.toList()) {
                if (item instanceof Map) {
                    DocumentMeta documentMeta = DocumentMeta.create((Map<String, Object>) item);
                    this.versionHistoryCache.add(documentMeta);
                }
            }
        } catch (JSONException e) {
            ;
        }
    }

    // TODO: using async interface
    public void dataChanged(String key) throws IOException {
        Map.Entry<byte[], DocumentMeta> data = persistentStoreDriver.load(key);

        String[] parts = key.split("\\.");

        parts[parts.length - 1] = "backups";
        String backupId = String.join(File.separator, parts) + "." + data.getValue().getId();

        try {
            persistentStoreDriver.save(backupId, data.getKey(), data.getValue());
        } catch (IllegalArgumentException e) {
            throw new IOException("failed to backup file for key(" + key + "): ", e);
        }

        {
            DocumentMeta documentMeta = data.getValue();

            this.versionHistoryCache.add(documentMeta);

            String versionHistoryJson = this.versionHistoryCache.toJsonString();

            byte[] v = versionHistoryJson.getBytes();

            Map<String, Object> versionHistoryCacheMeta = new HashMap<>();

            Date now = Calendar.getInstance().getTime();
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(now);

            versionHistoryCacheMeta.put("modified", timeStamp);
            versionHistoryCacheMeta.put("fileExtension", ".json");

            try {
                this.persistentStoreDriver.save(this.packageName + ".versionHistoryCache", v, versionHistoryCacheMeta);
            } catch (IllegalArgumentException e) {
                throw new IOException("failed to save versionHistoryCache file: ", e);
            }
        }
    }

    // TODO: using async interface
    public Map.Entry<byte[], DocumentMeta> find(String versionId)
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

                    if (doc.getDescription() != null) {
                        ArrayList<String> descriptions = new ArrayList<String>();
                        descriptions.add(doc.getDescription());
                        l.get(0).put("descriptions", descriptions);
                    }
                } else {
                    long diffMillis = current.getTime() - lastSaveDate.get(doc.getKey()).getTime();
                    if (this.autoSaveIntervalMilliSeconds <= diffMillis) {
                        //allVersions.add(0, doc);
                        ArrayList<DocumentMeta> l = allVersions.get(doc.getKey());
                        l.add(0, doc);
                        lastSaveDate.put(doc.getKey(), current);

                        if (doc.getDescription() != null) {
                            ArrayList<String> descriptions = new ArrayList<String>();
                            descriptions.add(doc.getDescription());
                            l.get(0).put("descriptions", descriptions);
                        }
                    } else {
                        ArrayList<DocumentMeta> l = allVersions.get(doc.getKey());
                        DocumentMeta old = l.remove(0);

                        l.add(0, doc);
                        //allVersions.remove(0);
                        //allVersions.add(0, doc);

                        Object obj = old.get("descriptions");
                        if (obj instanceof ArrayList) {
                            ArrayList<String> descriptions = (ArrayList<String>)obj;
                            if (doc.getDescription() != null) {
                                descriptions.add(0, doc.getDescription());
                            }
                            doc.put("descriptions", descriptions);
                        }
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
        Map<String, DocumentMeta[]> allVersions = getAllVersions();
        return allVersions.getOrDefault(key, new DocumentMeta[0]);
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

        calendarInstance = null;
        this.backupService = new BackupService(packageName, persistentStoreDriver);
    }

    public void init() throws IllegalArgumentException, IOException {
        Map.Entry<byte[], DocumentMeta> result = this.persistentStoreDriver.load(this.packageName + ".current");
        this.currentDoc = result.getValue();

        this.backupService.init();
    }

    // TODO: accept string format content
    // TODO: accept json format content
    public void save(byte[] content, String description) throws IOException {
        Calendar calendar = this.calendarInstance;
        if (this.calendarInstance == null) {
            calendar = Calendar.getInstance();
        }

        Date now = calendar.getTime();

        Map<String, Object> options = new HashMap<>();
        if (description != null) {
            options.put("description", description);
        }

        this.currentDoc = save(this.packageName + ".current", content, options, now, this.persistentStoreDriver);
        this.backupService.dataChanged(this.packageName + ".current");
    }

    public byte[] restore(String versionId)
            throws IllegalArgumentException, IOException {
        try {
            Map.Entry<byte[], DocumentMeta> result = this.backupService.find(versionId);
            this.currentDoc = this.persistentStoreDriver.save(this.packageName + ".current", result.getKey(), result.getValue());
            return result.getKey();
        } catch (IllegalArgumentException e) {
            throw new IOException("failed to restore file for id(" + versionId + "): ", e);
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
            Map.Entry<byte[], DocumentMeta> result = this.persistentStoreDriver.load(this.packageName + ".current");
            return result.getKey();
        } catch (IllegalArgumentException | IOException e) {
            return null;
        }
    }

    protected void setCalendarInstance(Calendar calendarInstance) {
        this.calendarInstance = calendarInstance;
    }

    public static DocumentMeta save(String key, byte[] content, Date now, PersistentStoreDriver persistentStoreDriver)
            throws IOException {
        return save(key, content, new HashMap<String, Object>(), now, persistentStoreDriver);
    }

    public static DocumentMeta save(String key, byte[] content, Map<String, Object> meta, Date now, PersistentStoreDriver persistentStoreDriver)
            throws IOException {
        Map<String, Object> metaClone = new HashMap<>(meta);

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(now);
        String id = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(now);

        // TODO: meta file should have version control

        metaClone.putIfAbsent("version", "1.0");
        metaClone.putIfAbsent("id", id);
        metaClone.putIfAbsent("key", key);
        metaClone.putIfAbsent("modified", timeStamp);
        metaClone.putIfAbsent("modifiedFormat", "yyyy-MM-dd HH:mm:ss.SSS");
        metaClone.putIfAbsent("fileExtension", ".json");

        return persistentStoreDriver.save(key, content, metaClone);
    }
}
