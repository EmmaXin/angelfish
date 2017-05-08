package com.accton.common.store;

import java.util.Map;

class DocumentMeta {
    // TODO: move optional attribute into attributes

    // TODO: set priavte level for all data member
    // TODO: create a `getAttribute(name)` function to access all data member
    private String id;
    private String key;
    private String modified;
    private String modifiedFormat;
    private String modifiedBy;
    private Integer size;
    private String description;
    private String fileUrl;

//    Map<String, String> properties;

//    public DocumentMeta(String id, String modified, String modifiedBy, Integer size, String description) {
//        this.id = id;
//        this.modified = modified;
//        this.modifiedBy = modifiedBy;
//        this.size = size;
//        this.description = description;
//    }

    public static DocumentMeta create(Map<String, String> attrs) {
        DocumentMeta docMeta = new DocumentMeta("", "", -1, "");

        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            docMeta.set(entry.getKey(), entry.getValue());
        }

        return docMeta;
    }

    public DocumentMeta(String id, String modified, Integer size, String fileUrl) {
        this.id = id;
        this.key = "";
        this.modified = modified;
        this.modifiedFormat = "yyyy-MM-dd HH:mm:ss.SSS";
        this.modifiedBy = "";
        this.size = size;
        this.description = "";
        this.fileUrl = fileUrl;

//        this.attributes = new HashMap<>();
    }

//    public DocumentMeta setModifiedBy(String modifiedBy) {
//        this.modifiedBy = modifiedBy;
//        return this;
//    }
//
//    public DocumentMeta setDescription(String description) {
//        this.description = description;
//        return this;
//    }

    public String getId() {
        return this.id;
    }

    public String getKey() {
        return this.key;
    }

    public Integer getSize() {
        return this.size;
    }

    public String getModified() { return this.modified; }

    public String getModifiedFormat() { return this.modifiedFormat; }

    public String value(String key) {
        if (key.equals("modified")) {
            return this.modified;
        }

        return null;
    }

    public DocumentMeta set(String key, String value) {
        switch (key) {
            case "id":
                this.id = value;
                break;

            case "key":
                this.key = value;
                break;

            case "modified":
                this.modified = value;
                break;

            case "modifiedFormat":
                this.modifiedFormat = value;
                break;

            case "modifiedBy":
                this.modifiedBy = value;
                break;

            case "size":
                this.size = Integer.parseInt(value);
                break;

            case "description":
                this.description = value;
                break;

            case "fileUrl":
                this.fileUrl = value;
                break;
        }

        return this;
    }

    public String toJsonString() {
        return "{\n" +
                "  \"id\":" + "\"" + this.id + "\",\n" +
                "  \"modified\":" + "\"" + this.modified + "\",\n" +
                "  \"modifiedBy\":" + "\"" + this.modifiedBy + "\",\n" +
                "  \"size\":" + String.valueOf(this.size) + ",\n" +
                "  \"description\":" + "\"" + this.description + "\"\n" +
                "}";
    }
}