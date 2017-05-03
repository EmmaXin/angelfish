package CoreDataService;

import CoreDataService.impl.FilePersistentStore;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit test for simple App.
 */
public class FilePersistentStoreTest
    extends TestCase
{
    final static String cwd = System.getProperty("user.dir");

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public FilePersistentStoreTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( FilePersistentStoreTest.class );
    }

    /**
     * Rigourous Test :-)
     */
//    public void testCheckValidPath() {
//        assertTrue(FilePersistentStore.isValidPath("x:\\") == true);
//        assertTrue(FilePersistentStore.isValidPath("/x") == true);
//        assertTrue(FilePersistentStore.isValidPath("X:\\abc\\def\\123") == true);
//        assertTrue(FilePersistentStore.isValidPath("X:\\abc\\def\\123.xml") == true);
//        assertTrue(FilePersistentStore.isValidPath("X:\\abc\\def\\123.json") == true);
//        assertTrue(FilePersistentStore.isValidPath("/x/abc/def/123") == true);
//        assertTrue(FilePersistentStore.isValidPath("/x/abc/def/123.xml") == true);
//        assertTrue(FilePersistentStore.isValidPath("/x/abc/def/123.json") == true);
//        assertTrue(FilePersistentStore.isValidPath("/x/abc/def/123$meta.json") == true);
//
//        assertTrue(FilePersistentStore.isValidPath("//") == false);
//        assertTrue(FilePersistentStore.isValidPath("/x/abc/*") == false);
//    }

    // TODO: ensure the location of output file, remove it for each test

    public void testSaveFile() {
        FilePersistentStore service = new FilePersistentStore(this.cwd);

        byte[] value = "{\"hello\": \"world\"}".getBytes();
        Map<String, String> meta = new HashMap<String, String>();

        meta.put("fileExtension", ".json");

        int ret = service.save("network.current", value, meta);

        assertTrue(ret == 0);

        Map.Entry<byte[], Map<String, String>> result = service.load("network.current");

        assertTrue(Arrays.equals(result.getKey(), value));
        assertTrue(result.getValue().get("size").equals(String.valueOf(value.length)));
    }

    public void testLoadNonExistFile() {
        FilePersistentStore service = new FilePersistentStore(this.cwd);

        Map.Entry<byte[], Map<String, String>> result = service.load("network.nonExist");

        assertTrue(result.getKey() == null);
        assertTrue(result.getValue().isEmpty());
    }

}
