package CoreDataService;

import java.util.HashMap;
import java.util.Map;

class DocumentMeta {
    // TODO: move optional attribute into attributes

    // TODO: set priavte level for all data member
    // TODO: create a `getAttribute(name)` function to access all data member
    public String id;
    public String modified;
    public String modifiedBy;
    public Integer size;
    public String description;
    public String fileUrl;

//    Map<String, String> attributes;

//    public DocumentMeta(String id, String modified, String modifiedBy, Integer size, String description) {
//        this.id = id;
//        this.modified = modified;
//        this.modifiedBy = modifiedBy;
//        this.size = size;
//        this.description = description;
//    }

    public static DocumentMeta create(Map<String, String> attrs) {
        DocumentMeta docMeta = new DocumentMeta("", "", -1, "");

        docMeta.id = attrs.getOrDefault("id", "");
        docMeta.modified = attrs.getOrDefault("modified", "");
        docMeta.modifiedBy = attrs.getOrDefault("modifiedBy", "");
        docMeta.size = Integer.parseInt(attrs.getOrDefault("size", "-1"));
        docMeta.description = attrs.getOrDefault("description", "");
        docMeta.fileUrl = attrs.getOrDefault("fileUrl", "");

        return docMeta;
    }

    public DocumentMeta(String id, String modified, Integer size, String fileUrl) {
        this.id = id;
        this.modified = modified;
        //this.modifiedBy = "";
        this.size = size;
        //this.description = "";
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

    public String toJSONString() {
        return "{\n" +
                "  \"id\":" + "\"" + this.id + "\",\n" +
                "  \"modified\":" + "\"" + this.modified + "\",\n" +
                "  \"modifiedBy\":" + "\"" + this.modifiedBy + "\",\n" +
                "  \"size\":" + String.valueOf(this.size) + ",\n" +
                "  \"description\":" + "\"" + this.description + "\"\n" +
                "}";
    }
}