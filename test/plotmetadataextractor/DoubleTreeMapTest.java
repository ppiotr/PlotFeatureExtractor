/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author piotr
 */
public class DoubleTreeMapTest {

    public DoubleTreeMapTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * We test on a map which should be exact ...
     */
    @Test
    public void testExactRetrieval() {
        DoubleTreeMap<Integer> tm = new DoubleTreeMap<Integer>(0);
        tm.put(10.56, 5);
        assertEquals((int) tm.get(10.56), 5);
        Integer val = tm.get(10.567);
        assertEquals((Object) val, null);

        tm = new DoubleTreeMap<Integer>(0.01);
        tm.put(10.56, 5);
        assertEquals((int) tm.get(10.56), 5);
        val = tm.get(10.567);
        assertEquals((int) val, 5);
        val = tm.get(10.555);
        assertEquals((int) val, 5);
        val = tm.get(10.55);
        assertEquals((int) val, 5);
        val = tm.get(10.4999);
        assertEquals((Object) val, null);
        val = tm.get(10.57001);
        assertEquals((Object) val, null);

        Integer oldVal = tm.put(10.5634, 78);
        assertEquals((int) oldVal, 5);
        assertEquals((int) tm.get(10.56), 78);
        val = tm.get(10.567);
        assertEquals((int) val, 78);
        val = tm.get(10.555);
        assertEquals((int) val, 78);
        val = tm.get(10.55);
        assertEquals((int) val, 78);
        val = tm.get(10.4999);
        assertEquals((Object) val, null);
        val = tm.get(10.57001);
        assertEquals((Object) val, null);


        tm.put(5.5, 543);
        assertEquals((int) tm.get(10.56), 78);
        val = tm.get(10.567);
        assertEquals((int) val, 78);
        val = tm.get(10.555);
        assertEquals((int) val, 78);
        val = tm.get(10.55);
        assertEquals((int) val, 78);
        val = tm.get(10.4999);
        assertEquals((Object) val, null);
        val = tm.get(10.57001);
        assertEquals((Object) val, null);

        val = tm.get(5.5);
        assertEquals((int) val, 543);
        val = tm.get(5.51);
        assertEquals((int) val, 543);
        val = tm.get(5.49);
        assertEquals((int) val, 543);
        val = tm.get(5.48999);
        assertEquals((Object) val, null);
        val = tm.get(5.51111);
        assertEquals((Object) val, null);

        /**
         * we can test the proximity retrieval ... we should always get the
         * value associated with the closest key
         */
        tm.put(5.5101, 11);
        assertEquals((int) tm.get(5.5101), 11);
        assertEquals((int) tm.get(5.51), 11);
        assertEquals((int) tm.get(5.5055), 11);
        assertEquals((int) tm.get(5.505), 543);
        


    }
}
