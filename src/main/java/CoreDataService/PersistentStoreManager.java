package CoreDataService;

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
            if (docMeta.id.equals(docId)) {
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

            s += entry.toJSONString() + "\n";
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

public class PersistentStoreManager {
    private PersistentStore persistentStore;
    private String packageName;

    private VersionHistoryCache versionHistoryCache;
    private DocumentMeta currentDoc;

    public PersistentStoreManager(String packageName, PersistentStore persistentStore) {
        this.persistentStore = persistentStore;
        this.packageName = packageName;

        this.versionHistoryCache = new VersionHistoryCache();
        this.currentDoc = null;
        // TODO: load veersionHistoryCache from persistent storage
        // TODO: rebuild versionHistoryCache from persistent storage
    }

    /**
     * Call to save data
     * @param content
     * @param observer, null means sync-call
     */
    // TODO: accept string format content
    // TODO: accept json format content
    public int dataChanged(byte[] content /*, BackupObserver observer*/) {

        int ret;

        // TODO: get current time and put into options
        Map<String, String> meta = new HashMap<>();

        Date now = Calendar.getInstance().getTime();

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(now);
        String id = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(now);

        meta.put("id", id);
        meta.put("modified", timeStamp);
        meta.put("fileExtension", ".json");

        {
            ret = this.persistentStore.save(this.packageName + ".backups." + id, content, meta);
            // TODO: check return value
        }

        {
            ret = this.persistentStore.save(this.packageName + ".current", content, meta);
            if (ret == 0) {
                this.currentDoc = DocumentMeta.create(meta);
            }

            // TODO: check return value, if error case
        }

        {
            Integer size = Integer.parseInt(meta.getOrDefault("size", "-1"));
            String fileUri = meta.get("fileUri");

            // TODO: the document meta should be obtained from options
            DocumentMeta record = new DocumentMeta(id, timeStamp, size, fileUri);

            this.versionHistoryCache.add(record);

            String versionHistoryJSON = this.versionHistoryCache.toString();

            byte[] v = versionHistoryJSON.getBytes();

            Map<String, String> versionHistoryCacheMeta = new HashMap<>();
            versionHistoryCacheMeta.put("modified", timeStamp);
            versionHistoryCacheMeta.put("fileExtension", ".json");

            ret = this.persistentStore.save(this.packageName + ".versionHistoryCache", v, versionHistoryCacheMeta);
        }


        // backService.save(db = <packageName>, col = 'backup',  doc = <versionId>,      content)
        // backService.save(db = <packageName>, col = 'current', doc = 'current',        content)
        // backService.save(db = <packageName>, col = 'current', doc = 'versionHistory', meta)
        return ret;
    }

    /**
     * Call to restore
     * @param versionId
     * @param observer, null means sync-call
     * @return
     */
    public byte[] requestRestore(String versionId /*, BackupObserver observer*/ ) {
        DocumentMeta docMeta = this.versionHistoryCache.find(versionId);

        if (docMeta == null) {
            return null;
        }

        Map.Entry<byte[], Map<String, String>> result = this.persistentStore.load(this.packageName + ".backups." + versionId);

        if (result.getValue() != null) {
            Integer ret = this.persistentStore.save(this.packageName + ".current", result.getKey(), result.getValue());
            this.currentDoc = DocumentMeta.create(result.getValue());
        }

        return result.getKey();
    }

    // TODO: return versionHistoryCache plus self information, like current used config, packetname ... etc
    public ArrayList<DocumentMeta> getVersionHistory() {
        return this.versionHistoryCache.getHistoryList();
    }

    public String getCurrentVersionId() {
        if (this.currentDoc == null) {
            return null;
        }

        return this.currentDoc.id;
    }

    public byte[] getCurrentVersionContent() {
        if (this.currentDoc == null) {
            return null;
        }

        Map.Entry<byte[], Map<String, String>> result = this.persistentStore.load(this.packageName + ".current");
        return result.getKey();
    }
}
