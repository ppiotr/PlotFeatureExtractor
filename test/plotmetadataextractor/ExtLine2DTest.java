package plotmetadataextractor;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.awt.geom.Point2D;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import plotmetadataextractor.ExtLine2D;

/**
 *
 * @author piotr
 */
public class ExtLine2DTest {

    public ExtLine2DTest() {
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
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

    @Test
    public void testIntersections() {
        ExtLine2D line1 = new ExtLine2D(0, 0, 1, 1);
        ExtLine2D line2 = new ExtLine2D(0, 0, 1, -1);
        ExtLine2D line3 = new ExtLine2D(0, 0, -1, 1);
        ExtLine2D line4 = new ExtLine2D(0, 0, -1, -1);
        ExtLine2D line5 = new ExtLine2D(0, 1, 1, 2);
        ExtLine2D line6 = new ExtLine2D(1, 1, 2, 2);
        ExtLine2D line7 = new ExtLine2D(1, -1, 2, -2);


        Point2D.Double int1 = line1.getIntersection(line5); // there is no intersection
        assertNull("An intersection detected, but should not be", int1);

        Point2D.Double int3 = line2.getIntersection(line4); // (0,0)
        assertEquals(int3.x, 0.0, 0.001);
        assertEquals(int3.y, 0.0, 0.001);

        Point2D.Double int4 = line6.getIntersection(line7); // this intersection falls in the point (0, 0) which is outside of the intervals
        assertEquals(int4.x, 0.0, 0.001);
        assertEquals(int4.y, 0.0, 0.001);

        Point2D.Double int2 = line2.getIntersection(line3); // colinear, (0,0)
        assertEquals(int3.x, -int3.y, 0.001);



        /// now we test lines parallel to axis
        ExtLine2D line8 = new ExtLine2D(4, 40, 4, 0);
        ExtLine2D line9 = new ExtLine2D(4, 40, 5, 40);
        Point2D.Double int5 = line8.getIntersection(line9);
        assertEquals(int5.getX(), 4, 0.001);
        assertEquals(int5.getY(), 40, 0.001);

        // the case of intersection outside of the axis
        line8 = new ExtLine2D(4, 40, 4, 0);
        line9 = new ExtLine2D(534, 40, 542, 40);
        int5 = line8.getIntersection(line9);
        assertEquals(int5.getX(), 4, 0.001);
        assertEquals(int5.getY(), 40, 0.001);

        // the case of colinear

        line8 = new ExtLine2D(4, 40, -10, 40);
        line9 = new ExtLine2D(534, 40, 542, 40);
        int5 = line8.getIntersection(line9);
        assertEquals(int5.getY(), 40, 0.001);


    }

    @Test
    public void testAngles() {
        ExtLine2D line = new ExtLine2D(0, 0, 1, 1);
        double angle = line.getAngle();
        assertEquals(angle, 45, 0.001);

        line = new ExtLine2D(0, 0, 1, -1);
        angle = line.getAngle();
        assertEquals(angle, -45, 0.001);

        line = new ExtLine2D(0, 0, -1, 1);
        angle = line.getAngle();
        assertEquals(angle, -45, 0.001);

        line = new ExtLine2D(0, 0, -1, -1);
        angle = line.getAngle();
        assertEquals(angle, 45, 0.001);

        line = new ExtLine2D(-1, -1, 0, 0);
        angle = line.getAngle();
        assertEquals(angle, 45, 0.001);

        line = new ExtLine2D(0, 1, 1, 2);
        angle = line.getAngle();
        assertEquals(angle, 45, 0.001);

        line = new ExtLine2D(1, 2, 0, 1);
        angle = line.getAngle();
        assertEquals(angle, 45, 0.001);


        line = new ExtLine2D(1, 1, 2, 2);
        angle = line.getAngle();
        assertEquals(angle, 45, 0.001);

        line = new ExtLine2D(1, -1, 2, -2);
        angle = line.getAngle();
        assertEquals(angle, -45, 0.001);

    }
    
    
    @Test
    public void testLineDistances(){
        ExtLine2D line = new ExtLine2D(0, 0, 1, -1);
        double dst = line.distance(new Point2D.Double(0,0));
        assertEquals(0, dst, 0.001);
        dst = line.distance(new Point2D.Double(-1000, 1000));
        assertEquals(0, dst, 0.001);
        dst = line.distance(new Point2D.Double(-1, 0));
        assertEquals(Math.sqrt(2) / 2, dst, 0.001);
        dst = line.distance(new Point2D.Double(-5, 4));
        assertEquals(Math.sqrt(2) / 2, dst, 0.001);
        dst = line.distance(new Point2D.Double(0.5, -0.5));
        assertEquals(0, dst, 0.001);
        
        // testing lines orthogonal to the axis
        line = new ExtLine2D(0, 0, 15, 0);
        dst = line.distance(new Point2D.Double(0, 0));
        assertEquals(0, dst, 0.01);
        
        dst = line.distance(new Point2D.Double(132445135, 0));
        assertEquals(0, dst, 0.01);
        
        dst = line.distance(new Point2D.Double(132445135, 0.0001));
        assertEquals(0.0001, dst, 0.01);
        
        line = new ExtLine2D(0, 0, 0, 12);
        dst = line.distance(new Point2D.Double(0, 0));
        assertEquals(0, dst, 0.01);
        
        dst = line.distance(new Point2D.Double(4.6, 0));
        assertEquals(4.6, dst, 0.01);      
        
    }
}
