package com.accton.common.store;

import com.accton.common.store.impl.FilePersistentStoreDriver;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.imageio.IIOException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit test for simple App.
 */
public class FilePersistentStoreDriverDriverTest
    extends TestCase
{
    final static String cwd = System.getProperty("user.dir");

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public FilePersistentStoreDriverDriverTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( FilePersistentStoreDriverDriverTest.class );
    }

    /**
     * Rigourous Test :-)
     */
//    public void testCheckValidPath() {
//        assertTrue(FilePersistentStoreDriver.isValidPath("x:\\") == true);
//        assertTrue(FilePersistentStoreDriver.isValidPath("/x") == true);
//        assertTrue(FilePersistentStoreDriver.isValidPath("X:\\abc\\def\\123") == true);
//        assertTrue(FilePersistentStoreDriver.isValidPath("X:\\abc\\def\\123.xml") == true);
//        assertTrue(FilePersistentStoreDriver.isValidPath("X:\\abc\\def\\123.json") == true);
//        assertTrue(FilePersistentStoreDriver.isValidPath("/x/abc/def/123") == true);
//        assertTrue(FilePersistentStoreDriver.isValidPath("/x/abc/def/123.xml") == true);
//        assertTrue(FilePersistentStoreDriver.isValidPath("/x/abc/def/123.json") == true);
//        assertTrue(FilePersistentStoreDriver.isValidPath("/x/abc/def/123$meta.json") == true);
//
//        assertTrue(FilePersistentStoreDriver.isValidPath("//") == false);
//        assertTrue(FilePersistentStoreDriver.isValidPath("/x/abc/*") == false);
//    }

    // TODO: ensure the location of output file, remove it for each test

    public void testSaveFile() {
        FilePersistentStoreDriver service = new FilePersistentStoreDriver(this.cwd);

        byte[] value = "{\"hello\": \"world\"}".getBytes();
        Map<String, String> meta = new HashMap<>();

        meta.put("fileExtension", ".json");
        meta.put("sayHello", "hello");

        try {
            service.save("network.current", value, meta);

            File file = new File(meta.get("fileUrl"));
            assertTrue(file.isFile());

            Map.Entry<byte[], Map<String, String>> result = service.load("network.current");

            assertTrue(Arrays.equals(result.getKey(), value));
            assertTrue(result.getValue().get("size").equals(String.valueOf(value.length)));
            assertTrue(result.getValue().get("sayHello").equals("hello"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    public void testSaveFileError() {
        FilePersistentStoreDriver service = new FilePersistentStoreDriver("a:\\qwertyuiop\\asdfghjkl");

        byte[] value = "{\"hello\": \"world\"}".getBytes();
        Map<String, String> meta = new HashMap<>();

        meta.put("fileExtension", ".json");
        meta.put("sayHello", "hello");

        try {
            service.save("network.current", value, meta);
            fail();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void testSaveFileIllegalArgumentError() {
        FilePersistentStoreDriver service = new FilePersistentStoreDriver("/qwertyuiop??/asdfghjkl");

        byte[] value = "{\"hello\": \"world\"}".getBytes();
        Map<String, String> meta = new HashMap<>();

        meta.put("fileExtension", ".json");
        meta.put("sayHello", "hello");

        try {
            service.save("network.current", value, meta);
            fail();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void testSaveFileIllegalKeyError() {
        FilePersistentStoreDriver service = new FilePersistentStoreDriver("");

        byte[] value = "{\"hello\": \"world\"}".getBytes();
        Map<String, String> meta = new HashMap<>();

        meta.put("fileExtension", ".json");
        meta.put("sayHello", "hello");

        try {
            service.save("network", value, meta);
            fail();
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    public void testLoadNonExistFile() {
        FilePersistentStoreDriver service = new FilePersistentStoreDriver(this.cwd);

        try {
            Map.Entry<byte[], Map<String, String>> result = service.load("network.noExist");
            fail();
        } catch (IOException e) {
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail();
        }
//        assertTrue(result.getKey() == null);
//        assertTrue(result.getValue().isEmpty());
    }

}
