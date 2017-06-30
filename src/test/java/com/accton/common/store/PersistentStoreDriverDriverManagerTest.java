package com.accton.common.store;

import com.accton.common.store.impl.FilePersistentStoreDriver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.NoSuchFileException;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Unit test for simple App.
 */
public class PersistentStoreDriverDriverManagerTest
    extends TestCase
{
    final static String cwd = System.getProperty("user.dir");

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public PersistentStoreDriverDriverManagerTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( PersistentStoreDriverDriverManagerTest.class );
    }

    static Object createObject(String className, Object... args) {
        Object object = null;
        try {
            Class classDefinition = Class.forName(className);

            Class[] cArg = new Class[args.length];
            for (Integer i = 0; i < args.length; ++i) {
                cArg[i] = args[i].getClass();
            }

            Constructor constructor = classDefinition.getDeclaredConstructor(cArg);
            object = constructor.newInstance(args);
        } catch (InstantiationException e) {
            System.out.println(e);
        } catch (IllegalAccessException e) {
            System.out.println(e);
        } catch (ReflectiveOperationException e) {
            System.out.println(e);
        //} catch (ClassNotFoundException e) {
        //    System.out.println(e);
        //} catch (NoSuchMethodException e) {
        //    System.out.println(e);
        }
        return object;
    }

    /**
     * Rigourous Test :-)
     */
    public void testAutoSaveEvery10Min() {
        PersistentStoreDriver persistentStoreDriver = new FilePersistentStoreDriver("abc");
        BackupService backupService = new BackupService("abc", persistentStoreDriver);

        String key = "test.current";
        DocumentMeta[] allVersions;

        try {
            PersistentStoreManager.save(key,
                    "version 1".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:00:00.000"),
                    persistentStoreDriver);
            backupService.dataChanged(key);

            allVersions = backupService.getAllVersions(key);
            assertTrue(allVersions.length == 1);
            assertTrue(allVersions[0].getModified().equals("2017-05-01 01:00:00.000"));
            assertTrue(allVersions[0].getKey().equals(key));

            PersistentStoreManager.save(key,
                    "version 2".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:10:00.000"),
                    persistentStoreDriver);
            backupService.dataChanged(key);

            allVersions = backupService.getAllVersions(key);
            assertTrue(allVersions.length == 2);
            assertTrue(allVersions[0].getModified().equals("2017-05-01 01:10:00.000"));
            assertTrue(allVersions[0].getKey().equals(key));
            assertTrue(allVersions[1].getModified().equals("2017-05-01 01:00:00.000"));
            assertTrue(allVersions[1].getKey().equals(key));

            PersistentStoreManager.save(key,
                    "internal version 1".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:20:00.000"),
                    persistentStoreDriver);
            backupService.dataChanged("test.current");

            allVersions = backupService.getAllVersions(key);
            assertTrue(allVersions.length == 3);
            assertTrue(allVersions[0].getModified().equals("2017-05-01 01:20:00.000"));
            assertTrue(allVersions[0].getKey().equals(key));
            assertTrue(allVersions[1].getModified().equals("2017-05-01 01:10:00.000"));
            assertTrue(allVersions[1].getKey().equals(key));
            assertTrue(allVersions[2].getModified().equals("2017-05-01 01:00:00.000"));
            assertTrue(allVersions[2].getKey().equals(key));

            //

            PersistentStoreManager.save(key,
                    "internal version 2".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:21:00.000"),
                    persistentStoreDriver);
            backupService.dataChanged(key);

            allVersions = backupService.getAllVersions(key);
            assertTrue(allVersions.length == 3);
            assertTrue(allVersions[0].getModified().equals("2017-05-01 01:21:00.000")); // Last version
            assertTrue(allVersions[0].getKey().equals(key));
            assertTrue(allVersions[1].getModified().equals("2017-05-01 01:10:00.000"));
            assertTrue(allVersions[1].getKey().equals(key));
            assertTrue(allVersions[2].getModified().equals("2017-05-01 01:00:00.000"));
            assertTrue(allVersions[2].getKey().equals(key));

            //

            PersistentStoreManager.save(key,
                    "internal version 3".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:29:59.000"),
                    persistentStoreDriver);
            backupService.dataChanged(key);

            allVersions = backupService.getAllVersions(key);
            assertTrue(allVersions.length == 3);
            assertTrue(allVersions[0].getModified().equals("2017-05-01 01:29:59.000")); // Last version
            assertTrue(allVersions[0].getKey().equals(key));
            assertTrue(allVersions[1].getModified().equals("2017-05-01 01:10:00.000"));
            assertTrue(allVersions[1].getKey().equals(key));
            assertTrue(allVersions[2].getModified().equals("2017-05-01 01:00:00.000"));
            assertTrue(allVersions[2].getKey().equals(key));

            //

            PersistentStoreManager.save(key,
                    "new version".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:30:00.000"),
                    persistentStoreDriver);
            backupService.dataChanged(key);

            allVersions = backupService.getAllVersions(key);
            assertTrue(allVersions.length == 4);
            assertTrue(allVersions[0].getModified().equals("2017-05-01 01:30:00.000")); // Last version
            assertTrue(allVersions[0].getKey().equals(key));
            assertTrue(allVersions[1].getModified().equals("2017-05-01 01:29:59.000"));
            assertTrue(allVersions[1].getKey().equals(key));
            assertTrue(allVersions[2].getModified().equals("2017-05-01 01:10:00.000"));
            assertTrue(allVersions[2].getKey().equals(key));
            assertTrue(allVersions[3].getModified().equals("2017-05-01 01:00:00.000"));
            assertTrue(allVersions[3].getKey().equals(key));

            //

            PersistentStoreManager.save(key,
                    "new version".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:40:00.000"),
                    persistentStoreDriver);
            backupService.dataChanged(key);

            allVersions = backupService.getAllVersions(key);
            assertTrue(allVersions.length == 5);
            assertTrue(allVersions[0].getModified().equals("2017-05-01 01:40:00.000")); // Last version
            assertTrue(allVersions[0].getKey().equals(key));
            assertTrue(allVersions[1].getModified().equals("2017-05-01 01:30:00.000")); // Last version
            assertTrue(allVersions[1].getKey().equals(key));
            assertTrue(allVersions[2].getModified().equals("2017-05-01 01:29:59.000"));
            assertTrue(allVersions[2].getKey().equals(key));
            assertTrue(allVersions[3].getModified().equals("2017-05-01 01:10:00.000"));
            assertTrue(allVersions[3].getKey().equals(key));
            assertTrue(allVersions[4].getModified().equals("2017-05-01 01:00:00.000"));
            assertTrue(allVersions[4].getKey().equals(key));
        } catch (ParseException | IOException e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

//    public void testReturnDescriptions() {
//        PersistentStoreDriver persistentStoreDriver = new FilePersistentStoreDriver("abc");
//        BackupService backupService = new BackupService("abc", persistentStoreDriver);
//
//        String key = "test.current";
//        DocumentMeta[] allVersions;
//
//        try {
//            Map<String, Object> meta = new HashMap<>();
//            meta.put("description", "version 1");
//            PersistentStoreManager.save(key, "version 1".getBytes(),
//                    meta,
//                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:00:00.000"),
//                    persistentStoreDriver);
//            backupService.dataChanged(key);
//
//            allVersions = backupService.getAllVersions(key);
//            assertTrue(allVersions.length == 1);
//            assertTrue(allVersions[0].getModified().equals("2017-05-01 01:00:00.000"));
//            assertTrue(allVersions[0].getKey().equals(key));
//
//            Object obj;
//
//            obj = allVersions[0].get("descriptions");
//            assertTrue(obj instanceof ArrayNode);
//            assertTrue(((ArrayNode) obj).size() == 1);
//            assertTrue(((ArrayNode) obj).get(0).textValue().equals("version 1"));
//
//            //String jsonString = allVersions[0].toJsonString();
//
//            meta.put("description", "version 2");
//            PersistentStoreManager.save(key, "version 2".getBytes(),
//                    meta,
//                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:10:00.000"),
//                    persistentStoreDriver);
//            backupService.dataChanged(key);
//
//            allVersions = backupService.getAllVersions(key);
//            assertTrue(allVersions.length == 2);
//            assertTrue(allVersions[0].getModified().equals("2017-05-01 01:10:00.000"));
//            assertTrue(allVersions[0].getKey().equals(key));
//
//            obj = allVersions[0].get("descriptions");
//            assertTrue(obj instanceof ArrayNode);
//            assertTrue(((ArrayNode) obj).size() == 1);
//            assertTrue(((ArrayNode) obj).get(0).textValue().equals("version 2"));
//
//            assertTrue(allVersions[1].getModified().equals("2017-05-01 01:00:00.000"));
//            assertTrue(allVersions[1].getKey().equals(key));
//
//            //
//            meta.put("description", "internal version 2");
//            PersistentStoreManager.save(key, "internal version 2".getBytes(),
//                    meta,
//                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:11:00.000"),
//                    persistentStoreDriver);
//            backupService.dataChanged(key);
//
//            allVersions = backupService.getAllVersions(key);
//            assertTrue(allVersions.length == 2);
//            assertTrue(allVersions[0].getModified().equals("2017-05-01 01:11:00.000")); // Last version
//            assertTrue(allVersions[0].getKey().equals(key));
//
//            obj = allVersions[0].get("descriptions");
//            assertTrue(obj instanceof ArrayNode);
//            assertTrue(((ArrayNode) obj).size() == 2);
//            assertTrue(((ArrayNode) obj).get(0).textValue().equals("internal version 2"));
//            assertTrue(((ArrayNode) obj).get(1).textValue().equals("version 2"));
//
//            assertTrue(allVersions[1].getModified().equals("2017-05-01 01:00:00.000"));
//            assertTrue(allVersions[1].getKey().equals(key));
//        } catch (ParseException | IOException e) {
//            System.out.println(e.getMessage());
//            fail();
//        }
//    }

    public void testGetAllKeysVersion() {
        PersistentStoreDriver persistentStoreDriver = new FilePersistentStoreDriver("abc");
        BackupService backupService = new BackupService("abc", persistentStoreDriver);

        String key1 = "feature1.current";
        String key2 = "feature2.current";
        DocumentMeta[] key1Versions;

        Map<String, DocumentMeta[]> allVersions;

        try {
            PersistentStoreManager.save(key1, "feature1: version 1".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:00:00.000"),
                    persistentStoreDriver);
            backupService.dataChanged(key1);

            key1Versions = backupService.getAllVersions(key1);
            assertTrue(key1Versions.length == 1);
            assertTrue(key1Versions[0].getModified().equals("2017-05-01 01:00:00.000"));
            assertTrue(key1Versions[0].getKey().equals(key1));

            allVersions = backupService.getAllVersions();
            assertTrue(allVersions.size() == 1);
            assertTrue(allVersions.get(key1) != null);

            PersistentStoreManager.save(key2, "feature2: version 1".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:00:00.000"),
                    persistentStoreDriver);
            backupService.dataChanged(key2);

            allVersions = backupService.getAllVersions();
            assertTrue(allVersions.size() == 2);
            assertTrue(allVersions.get(key1) != null);
            assertTrue(allVersions.get(key1).length == 1);
            assertTrue(allVersions.get(key1)[0].getModified().equals("2017-05-01 01:00:00.000"));

            assertTrue(allVersions.get(key2) != null);
            assertTrue(allVersions.get(key2).length == 1);
            assertTrue(allVersions.get(key2)[0].getModified().equals("2017-05-01 01:00:00.000"));

            PersistentStoreManager.save(key1, "feature1: internal".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:01:00.000"),
                    persistentStoreDriver);
            backupService.dataChanged(key1);

            PersistentStoreManager.save(key2, "feature2: internal".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:01:01.000"),
                    persistentStoreDriver);
            backupService.dataChanged(key2);

            allVersions = backupService.getAllVersions();
            assertTrue(allVersions.size() == 2);
            assertTrue(allVersions.get(key1) != null);
            assertTrue(allVersions.get(key1).length == 1);
            assertTrue(allVersions.get(key1)[0].getModified().equals("2017-05-01 01:01:00.000"));

            assertTrue(allVersions.get(key2) != null);
            assertTrue(allVersions.get(key2).length == 1);
            assertTrue(allVersions.get(key2)[0].getModified().equals("2017-05-01 01:01:01.000"));

            PersistentStoreManager.save(key1, "feature1: version 2".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:10:00.000"),
                    persistentStoreDriver);
            backupService.dataChanged(key1);

            PersistentStoreManager.save(key2, "feature2: version 2".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:10:01.000"),
                    persistentStoreDriver);
            backupService.dataChanged(key2);

            allVersions = backupService.getAllVersions();
            assertTrue(allVersions.size() == 2);
            assertTrue(allVersions.get(key1) != null);
            assertTrue(allVersions.get(key1).length == 2);
            assertTrue(allVersions.get(key1)[0].getModified().equals("2017-05-01 01:10:00.000"));
            assertTrue(allVersions.get(key1)[1].getModified().equals("2017-05-01 01:01:00.000"));

            assertTrue(allVersions.get(key2) != null);
            assertTrue(allVersions.get(key2).length == 2);
            assertTrue(allVersions.get(key2)[0].getModified().equals("2017-05-01 01:10:01.000"));
            assertTrue(allVersions.get(key2)[1].getModified().equals("2017-05-01 01:01:01.000"));

            PersistentStoreManager.save(key1, "feature1: version 2.1".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:15:00.000"),
                    persistentStoreDriver);
            backupService.dataChanged(key1);

            PersistentStoreManager.save(key2, "feature2: version 2.1".getBytes(),
                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).parse("2017-05-01 01:15:01.000"),
                    persistentStoreDriver);
            backupService.dataChanged(key2);

            allVersions = backupService.getAllVersions();
            assertTrue(allVersions.size() == 2);
            assertTrue(allVersions.get(key1) != null);
            assertTrue(allVersions.get(key1).length == 2);
            assertTrue(allVersions.get(key1)[0].getModified().equals("2017-05-01 01:15:00.000"));
            assertTrue(allVersions.get(key1)[1].getModified().equals("2017-05-01 01:01:00.000"));

            assertTrue(allVersions.get(key2) != null);
            assertTrue(allVersions.get(key2).length == 2);
            assertTrue(allVersions.get(key2)[0].getModified().equals("2017-05-01 01:15:01.000"));
            assertTrue(allVersions.get(key2)[1].getModified().equals("2017-05-01 01:01:01.000"));
        } catch (ParseException | IOException e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    public void testSave() {
        PersistentStoreDriver filePersistentStoreDriver = (FilePersistentStoreDriver)createObject("com.accton.common.store.impl.FilePersistentStoreDriver", this.cwd);
        if (filePersistentStoreDriver == null) {
            fail();
        }

        PersistentStoreManager mgr = new PersistentStoreManager("foo", filePersistentStoreDriver);

        byte[] value = "{\"hello\": \"world\"}".getBytes();

        try {
            mgr.save(value);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            fail();
        }

        DocumentMeta[] versionHistory = mgr.getAllVersions("foo.current");

        assertTrue(versionHistory.length == 1);
        assertTrue(versionHistory[0].getSize() == value.length);

        String currentVerId = mgr.getCurrentVersionId();
        assertTrue(currentVerId.equals(versionHistory[0].getId()));

        byte[] currentOuput = mgr.getCurrentVersionContent();
        assertTrue(Arrays.equals(currentOuput, value));
    }

    public void testRestore() {
        PersistentStoreDriver filePersistentStoreDriver = (FilePersistentStoreDriver)createObject("com.accton.common.store.impl.FilePersistentStoreDriver", this.cwd);
        if (filePersistentStoreDriver == null) {
            fail();
        }

        PersistentStoreManager mgr = new PersistentStoreManager("foo", filePersistentStoreDriver);

        ArrayList<byte[]> files = new ArrayList<byte[]>() {{
            add("{\"version\": \"v1\"}".getBytes());
            add("{\"version\": \"last\"}".getBytes());
        }};

        Calendar calendar = Calendar.getInstance();

        int minute = 0;

        try {
            for (byte[] file : files) {
                calendar.set(2017, 5, 1, 0, minute += 10, 0);
                mgr.setCalendarInstance(calendar);
                mgr.save(file);
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            fail();
        }

        DocumentMeta[] versionHistory = mgr.getAllVersions("foo.current");

        assert(versionHistory.length == 2);
        assert(versionHistory[0].getSize() == files.get(1).length);

        try {
            byte[] output = mgr.restore(versionHistory[1].getId());
            assertTrue(Arrays.equals(output, files.get(0)));
        } catch (IOException e) {
            System.out.println(e);
            fail();
        }

        String currentVerId = mgr.getCurrentVersionId();
        assertTrue(currentVerId.equals(versionHistory[1].getId()));

        byte[] currentOuput = mgr.getCurrentVersionContent();
        assertTrue(Arrays.equals(currentOuput, files.get(0)));

        byte[] output = mgr.getVersionContent(versionHistory[0].getId());
        assertTrue(Arrays.equals(output, files.get(1)));

        output = mgr.getVersionContent(versionHistory[1].getId());
        assertTrue(Arrays.equals(output, files.get(0)));
    }

    public void testInit() {
        PersistentStoreDriver filePersistentStoreDriver = (FilePersistentStoreDriver)createObject("com.accton.common.store.impl.FilePersistentStoreDriver", this.cwd);
        if (filePersistentStoreDriver == null) {
            fail();
        }

        PersistentStoreManager mgr = new PersistentStoreManager("foo", filePersistentStoreDriver);

        byte[] value = "{\"hello\": \"world\"}".getBytes();

        try {
            mgr.save(value);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            fail();
        }

        PersistentStoreManager mgr2 = new PersistentStoreManager("foo", filePersistentStoreDriver);

        try {
            mgr2.init();
        } catch (IllegalArgumentException | IOException e) {
            System.out.println(e.getMessage());
            fail();
        }

        DocumentMeta[] versionHistory = mgr2.getAllVersions("foo.current");

        assertTrue(versionHistory.length == 1);
        assertTrue(versionHistory[0].getSize() == value.length);

        String currentVerId = mgr2.getCurrentVersionId();
        assertTrue(currentVerId.equals(versionHistory[0].getId()));

        byte[] currentOuput = mgr2.getCurrentVersionContent();
        assertTrue(Arrays.equals(currentOuput, value));
    }

    public JsonNode getNetworkConfig(PersistentStoreManager persistentStoreManager, String id) {
        ObjectMapper mapper = new ObjectMapper();
        DocumentMeta[] documentMetas = persistentStoreManager.getAllVersions("network.current");

        try {
            for (DocumentMeta meta : documentMetas) {
                if (meta.getId().equals(id)) {
                    ObjectNode el = JsonNodeFactory.instance.objectNode();
                    el.put("id", meta.getId());
                    el.put("modifiedBy", meta.getModifiedBy());
                    el.put("modified", meta.getModified());
//                    el.put("description", meta.getDescription())

                    byte[] output = persistentStoreManager.getVersionContent(meta.getId());
                    JsonNode networkCfg = mapper.readTree(output);
                    el.put("networkCfg", networkCfg);

                    return el;
                }
            }
        } catch (IOException e) {
            return null;
        }

        return null;
    }

    public void testResp() {
        PersistentStoreDriver filePersistentStoreDriver = (FilePersistentStoreDriver)createObject("com.accton.common.store.impl.FilePersistentStoreDriver", this.cwd);
        if (filePersistentStoreDriver == null) {
            fail();
        }

        PersistentStoreManager persistentStoreManager = new PersistentStoreManager("network", filePersistentStoreDriver);

        byte[] value = "{\"hello\": \"world\"}".getBytes();

        try {
            persistentStoreManager.save(value);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            fail();
        }

        ObjectNode payload =JsonNodeFactory.instance.objectNode();
        payload.put("id", persistentStoreManager.getCurrentVersionId());

        JsonNode id = payload.get("id");
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        ObjectMapper mapper = new ObjectMapper();

        JsonNode networkConfig = getNetworkConfig(persistentStoreManager, id.asText());

        if (networkConfig != null) {
            data.put(id.asText(), networkConfig);
        }
    }
}
