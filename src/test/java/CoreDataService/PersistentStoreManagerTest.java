package CoreDataService;

import CoreDataService.impl.FilePersistentStore;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Unit test for simple App.
 */
public class PersistentStoreManagerTest
    extends TestCase
{
    final static String cwd = System.getProperty("user.dir");

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public PersistentStoreManagerTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( PersistentStoreManagerTest.class );
    }

//    static Object createObject(String className) {
//        Object object = null;
//        try {
//            Class classDefinition = Class.forName(className);
//            object = classDefinition.newInstance();
//        } catch (InstantiationException e) {
//            System.out.println(e);
//        } catch (IllegalAccessException e) {
//            System.out.println(e);
//        } catch (ClassNotFoundException e) {
//            System.out.println(e);
//        }
//        return object;
//    }

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
    public void testSave() {
        // TODO: associate the writer class by config file.
//        PersistentStore writer0 = (FilePersistentStore)createObject("CoreDataService.impl.FilePersistentStore");

        PersistentStore writer = (FilePersistentStore)createObject("CoreDataService.impl.FilePersistentStore", this.cwd);
//        PersistentStore writer2 = (FilePersistentStore)createObject("CoreDataService.impl.FilePersistentStore", this.cwd, this.cwd);
        if (writer == null) {
            fail();
        }

        PersistentStoreManager mgr = new PersistentStoreManager("foo", /*new FilePersistentStore(this.cwd)*/ writer);

        byte[] value = "{\"hello\": \"world\"}".getBytes();
        int ret = mgr.dataChanged(value);

        assertTrue( ret == 0);

        ArrayList<DocumentMeta> versionHistory = mgr.getVersionHistory();

        assertTrue(versionHistory.size() == 1);
        assertTrue(versionHistory.get(0).size == value.length);

        String currentVerId = mgr.getCurrentVersionId();
        assertTrue(currentVerId.equals(versionHistory.get(0).id));

        byte[] currentOuput = mgr.getCurrentVersionContent();
        assertTrue(Arrays.equals(currentOuput, value));
    }

    public void testRestore() {
        // TODO: associate the writer class by config file.
        PersistentStore writer = (FilePersistentStore)createObject("CoreDataService.impl.FilePersistentStore", this.cwd);
        if (writer == null) {
            fail();
        }

        PersistentStoreManager mgr = new PersistentStoreManager("foo", writer);

        ArrayList<byte[]> files = new ArrayList<byte[]>() {{
            add("{\"version\": \"v1\"}".getBytes());
            add("{\"version\": \"last\"}".getBytes());
        }};

        //byte[] file1 = "{\"version\": 1}".getBytes();

        for (byte[] file : files) {
            Integer ret = mgr.dataChanged(file);
            assertTrue( ret == 0);
        }

//        ret = mgr.dataChanged(file1);
//        assertTrue( ret == 0);

        ArrayList<DocumentMeta> versionHistory = mgr.getVersionHistory();

        assert(versionHistory.size() == 2);
        assert(versionHistory.get(0).size == files.get(1).length);

        byte[] output = mgr.requestRestore(versionHistory.get(1).id);
        assertTrue(Arrays.equals(output, files.get(0)));

        String currentVerId = mgr.getCurrentVersionId();
        assertTrue(currentVerId.equals(versionHistory.get(1).id));

        byte[] currentOuput = mgr.getCurrentVersionContent();
        assertTrue(Arrays.equals(currentOuput, files.get(0)));
    }
}
