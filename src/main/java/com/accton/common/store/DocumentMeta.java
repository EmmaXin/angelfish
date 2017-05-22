package com.accton.common.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.json.JSONException;
//import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class DocumentMeta {
//    private JSONObject object;
    private ObjectNode object2;

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
//        try {
//            JSONObject jsonObject = new JSONObject(jsonString);
//            return new DocumentMeta(jsonObject);
//        } catch (JSONException e) {
//            return null;
//        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualObj = mapper.readTree(jsonString);
            if (!actualObj.isObject()) {
                return null;
            }

            return new DocumentMeta((ObjectNode) actualObj);
        } catch (IOException e) {
            return null;
        }

    }

    public static DocumentMeta create(ObjectNode objectNode) {
        return new DocumentMeta(objectNode);
    }

//    public static DocumentMeta create(Object object) {
//        try {
//            JSONObject jsonObject = new JSONObject(object);
//            return new DocumentMeta(jsonObject);
//        } catch (JSONException e) {
//            return null;
//        }
//    }

//    private DocumentMeta(JSONObject jsonObject) {
//        object = jsonObject;
//    }

    private DocumentMeta(ObjectNode objectNode) {
//        object = jsonObject;
        object2 = objectNode;
    }

    public DocumentMeta(String id, String modified, Integer size, String fileUrl) {
//        object = new JSONObject();
        object2 = JsonNodeFactory.instance.objectNode();

        put("id", id);
        put("modified", modified);
        put("modifiedFormat", "yyyy-MM-dd HH:mm:ss.SSS");
        put("size", size);
        put("fileUrl", fileUrl);
    }

    public String getId() {
//        return object.optString("id", null);
        return getString("id");
    }

    public String getKey() {
//        return object.optString("key", null);
        return getString("key");
    }

    public Integer getSize() {
//        return object.optInt("size", -1);
        return getInt("size", -1);
    }

    public String getModified() {
//        return object.optString("modified", null);
        return getString("modified");
    }

    public String getModifiedFormat() {
//        return object.optString("modifiedFormat", null);
        return getString("modifiedFormat");
    }

    public String getModifiedBy() {
//        return object.optString("modifiedBy", null);
        return getString("modifiedBy");
    }

    public String getDescription() {
//        return object.optString("description", null);
        return getString("description");
    }

    public String getFileUrl() {
//        return object.optString("fileUrl", null);
        return getString("fileUrl");
    }

    public DocumentMeta put(String key, Object value) {
//        try {
//            object.put(key, value);
//        } catch (JSONException e) {
//            ;
//        }

        ObjectMapper mapper = new ObjectMapper();

        JsonNode jsonNode = mapper.convertValue(value, JsonNode.class);
        object2.put(key, jsonNode);

        return this;
    }

    public String[] keys() {
        //return object.keySet().toArray(new String[0]);

        ArrayList<String> arrayList = new ArrayList<>();

        for (Iterator<String> iterator = object2.fieldNames(); iterator.hasNext();) {
            String key = iterator.next();
            arrayList.add(key);
        }

        return arrayList.toArray(new String[0]);
    }

    public Object get(String key) {
//        return object.opt(key);
        return object2.get(key);
    }

    public String getString(String key) {
        //return object.optString(key);

//        JsonNode value = object2.get(key);
//
//        if (value == null) {
//            return "";
//        }
//
//        return value.asText();
        return getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        //return object.optString(key, defaultValue);
        JsonNode value = object2.get(key);

        if (value == null) {
            return defaultValue;
        }

        return value.asText();
    }

    public Integer getInt(String key) {
        return getInt(key, null);
    }

    public Integer getInt(String key, Integer defaultValue) {
        JsonNode value = object2.get(key);

        if (value == null) {
            return defaultValue;
        }

        return value.asInt();
    }

    public String toJsonString() {
        //return object.toString(4);
        return object2.toString();
    }
}