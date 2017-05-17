package com.accton.common.store;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.json.*;

/**
 * Unit test for simple App.
 */
public class DocumentMetaTest
        extends TestCase {

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public DocumentMetaTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(DocumentMetaTest.class);
    }

    public void testClassName() {
        assertTrue(Character.class.getSimpleName().equals("Character"));
        assertTrue(Byte.class.getSimpleName().equals("Byte"));

        assertTrue(Short.class.getSimpleName().equals("Short"));
        assertTrue(Integer.class.getSimpleName().equals("Integer"));
        assertTrue(Long.class.getSimpleName().equals("Long"));

        assertTrue(Float.class.getSimpleName().equals("Float"));
        assertTrue(Double.class.getSimpleName().equals("Double"));

        assertTrue(Boolean.class.getSimpleName().equals("Boolean"));

        assertTrue(String.class.getSimpleName().equals("String"));
        assertTrue("".getClass().getSimpleName().equals("String"));


        assertTrue(String[].class.getSimpleName().equals("String[]"));
        assertTrue(String[].class.isArray());

        assertTrue(Object[].class.getSimpleName().equals("Object[]"));
        assertTrue(new HashMap<String, Object>().getClass().getSimpleName().equals("HashMap"));
        assertTrue(new HashMap<String, String>().getClass().getSimpleName().equals("HashMap"));

        String[] array = new HashMap<String, String>().values().toArray(new String[0]);

        System.out.println();
    }

//    public void testDeserializeFromHashMap() {
//        Map<String, String> attributes = new HashMap<>();
//        attributes.put("id", "id");
//        attributes.put("key", "key");
//        attributes.put("modified", "modified");
//        attributes.put("modifiedFormat", "yyyy-MM-dd HH:mm:ss.SSS");
//        attributes.put("modifiedBy", "who");
//        attributes.put("size", "100");
//        attributes.put("description", "test");
//        attributes.put("fileUrl", "c:\\cfg.json");
//
//        DocumentMeta meta = DocumentMeta.create(attributes);
//        assertTrue(meta.getId().equals("id"));
//        assertTrue(meta.getKey().equals("key"));
//        assertTrue(meta.getModified().equals("modified"));
//        assertTrue(meta.getModifiedFormat().equals("yyyy-MM-dd HH:mm:ss.SSS"));
//        assertTrue(meta.getModifiedBy().equals("who"));
//        assertTrue(meta.getSize() == 100);
//        assertTrue(meta.getDescription().equals("test"));
//        assertTrue(meta.getFileUrl().equals("c:\\cfg.json"));
//    }

    public void testDeserializeFromStringObjectHashMap() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "id");
        attributes.put("key", "key");
        attributes.put("modified", "modified");
        attributes.put("modifiedFormat", "yyyy-MM-dd HH:mm:ss.SSS");
        attributes.put("modifiedBy", "who");
        attributes.put("size", 100);
        attributes.put("description", "test");
        attributes.put("fileUrl", "c:\\cfg.json");

        DocumentMeta meta = DocumentMeta.create(attributes);
        assertTrue(meta.getId().equals("id"));
        assertTrue(meta.getKey().equals("key"));
        assertTrue(meta.getModified().equals("modified"));
        assertTrue(meta.getModifiedFormat().equals("yyyy-MM-dd HH:mm:ss.SSS"));
        assertTrue(meta.getModifiedBy().equals("who"));
        assertTrue(meta.getSize() == 100);
        assertTrue(meta.getDescription().equals("test"));
        assertTrue(meta.getFileUrl().equals("c:\\cfg.json"));

        assertTrue(Arrays.asList(meta.keys()).contains("id"));
        assertTrue(Arrays.asList(meta.keys()).contains("key"));
        assertTrue(Arrays.asList(meta.keys()).contains("modified"));
        assertTrue(Arrays.asList(meta.keys()).contains("modifiedFormat"));
        assertTrue(Arrays.asList(meta.keys()).contains("modifiedBy"));
        assertTrue(Arrays.asList(meta.keys()).contains("size"));
        assertTrue(Arrays.asList(meta.keys()).contains("description"));
        assertTrue(Arrays.asList(meta.keys()).contains("fileUrl"));
    }

    public void testGetKeys() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "id");
        attributes.put("key", "key");
        attributes.put("modified", "modified");
        attributes.put("modifiedFormat", "yyyy-MM-dd HH:mm:ss.SSS");
        attributes.put("modifiedBy", "who");
        attributes.put("size", 100);
        attributes.put("description", "test");
        attributes.put("fileUrl", "c:\\cfg.json");

        DocumentMeta meta = DocumentMeta.create(attributes);

        assertTrue(Arrays.asList(meta.keys()).contains("id"));
        assertTrue(Arrays.asList(meta.keys()).contains("key"));
        assertTrue(Arrays.asList(meta.keys()).contains("modified"));
        assertTrue(Arrays.asList(meta.keys()).contains("modifiedFormat"));
        assertTrue(Arrays.asList(meta.keys()).contains("modifiedBy"));
        assertTrue(Arrays.asList(meta.keys()).contains("size"));
        assertTrue(Arrays.asList(meta.keys()).contains("description"));
        assertTrue(Arrays.asList(meta.keys()).contains("fileUrl"));
    }

    public void testDocumentMetaCanDeserializeFromString() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "id");
        attributes.put("key", "key");
        attributes.put("modified", "2000-01-01 00:00:00:000");
        attributes.put("modifiedFormat", "yyyy-MM-dd HH:mm:ss.SSS");
        attributes.put("modifiedBy", "who");
        attributes.put("size", "100");
        attributes.put("description", "test");
        attributes.put("fileUrl", "C:\\cfg.json");

        DocumentMeta meta = DocumentMeta.create(attributes);
        assertTrue(meta.getId().equals("id"));
        assertTrue(meta.getKey().equals("key"));
        assertTrue(meta.getModified().equals("2000-01-01 00:00:00:000"));
        assertTrue(meta.getModifiedFormat().equals("yyyy-MM-dd HH:mm:ss.SSS"));
        assertTrue(meta.getModifiedBy().equals("who"));
        assertTrue(meta.getSize() == 100);
        assertTrue(meta.getDescription().equals("test"));
        assertTrue(meta.getFileUrl().equals("C:\\cfg.json"));

        String string = meta.toJsonString();

        JSONObject jsonObject = new JSONObject(string);
        assertTrue(jsonObject.getString("id").equals("id"));
        assertTrue(jsonObject.getString("key").equals("key"));
        assertTrue(jsonObject.getString("modified").equals("2000-01-01 00:00:00:000"));
        assertTrue(jsonObject.getString("modifiedFormat").equals("yyyy-MM-dd HH:mm:ss.SSS"));
        assertTrue(jsonObject.getString("modifiedBy").equals("who"));
        assertTrue(jsonObject.getInt("size") == 100);
        assertTrue(jsonObject.getString("description").equals("test"));
        assertTrue(jsonObject.getString("fileUrl").equals("C:\\cfg.json"));

        String abc = jsonObject.optString("abc");
        assertTrue(true);
    }
}
