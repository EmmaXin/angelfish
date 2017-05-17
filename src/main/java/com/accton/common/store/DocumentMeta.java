package com.accton.common.store;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class DocumentMeta {
    private JSONObject object;

//    public static DocumentMeta create(Map<String, String> attrs) {
//        DocumentMeta docMeta = new DocumentMeta("", "", -1, "");
//
//        for (Map.Entry<String, String> entry : attrs.entrySet()) {
//            docMeta.put(entry.getKey(), entry.getValue());
//        }
//
//        return docMeta;
//    }

    public static DocumentMeta create(Map<String, Object> map) {
        DocumentMeta docMeta = new DocumentMeta("", "", -1, "");

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            docMeta.put(entry.getKey(), entry.getValue());
        }

        return docMeta;
    }

    public static DocumentMeta create(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return new DocumentMeta(jsonObject);
        } catch (JSONException e) {
            return null;
        }
    }

//    public static DocumentMeta create(Object object) {
//        try {
//            JSONObject jsonObject = new JSONObject(object);
//            return new DocumentMeta(jsonObject);
//        } catch (JSONException e) {
//            return null;
//        }
//    }

    private DocumentMeta(JSONObject jsonObject) {
        object = jsonObject;
    }

    public DocumentMeta(String id, String modified, Integer size, String fileUrl) {
        object = new JSONObject();

        put("id", id);
        put("modified", modified);
        put("modifiedFormat", "yyyy-MM-dd HH:mm:ss.SSS");
        put("size", size);
        put("fileUrl", fileUrl);
    }

    public String getId() {
        return object.optString("id", null);
    }

    public String getKey() {
        return object.optString("key", null);
    }

    public Integer getSize() {
        return object.optInt("size", -1);
    }

    public String getModified() {
        return object.optString("modified", null);
    }

    public String getModifiedFormat() {
        return object.optString("modifiedFormat", null);
    }

    public String getModifiedBy() {
        return object.optString("modifiedBy", null);
    }

    public String getDescription() {
        return object.optString("description", null);
    }

    public String getFileUrl() {
        return object.optString("fileUrl", null);
    }

    public DocumentMeta put(String key, Object value) {
        try {
            object.put(key, value);
        } catch (JSONException e) {
            ;
        }

        return this;
    }

    public String[] keys() {
        return object.keySet().toArray(new String[0]);
    }

    public Object get(String key) {
        return object.opt(key);
    }

    public String getString(String key) {
        return object.optString(key);
    }

    public String getString(String key, String defaultValue) {
        return object.optString(key, defaultValue);
    }

    public String toJsonString() {
        return object.toString(4);
    }
}