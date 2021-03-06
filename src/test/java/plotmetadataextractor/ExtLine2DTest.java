package plotmetadataextractor;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import junit.framework.TestCase;

/**
 *
 * @author piotr
 */
public class ExtLine2DTest extends TestCase{

    public ExtLine2DTest() {
        super();
    }

    @Override
    public void setUp() throws Exception{
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception{
        super.tearDown();
    }
    
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

    public void testLineDistances() {
        ExtLine2D line = new ExtLine2D(0, 0, 1, -1);
        double dst = line.distance(new Point2D.Double(0, 0));
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

    public void testGetIntersection() {
        ExtLine2D line1 = new ExtLine2D(0, 10, 10, 10);
        ExtLine2D line2 = new ExtLine2D(0, 0, 0, 10);
        Point2D.Double inter = line1.getIntersection(line2);
        //TODO: Write assertions
    }

    public void testDistanceSegment() {
        ExtLine2D l = new ExtLine2D(0, 0, 2, 0);
        assertEquals(l.distanceSegment(new Point2D.Double(0, 0)), 0.0, 0.001);
        assertEquals(l.distanceSegment(new Point2D.Double(0.5, 0)), 0.0, 0.001);
        assertEquals(l.distanceSegment(new Point2D.Double(1, 0)), 0.0, 0.001);
        assertEquals(l.distanceSegment(new Point2D.Double(-1, 0)), 1.0, 0.001);
        assertEquals(l.distanceSegment(new Point2D.Double(3, 0)), 1.0, 0.001);                      
    }
    
    public void testDistanceRectangle(){
        Rectangle2D.Double rec = new Rectangle2D.Double(130.05, 464.999999, 17.38, 10.999999);
        double d1 = ExtLine2D.distanceRectangle(new Point2D.Double(147.962, 470.8), rec);
        double d2 = ExtLine2D.distanceRectangle(new Point2D.Double(147.962, 462.64), rec);
        
        assertTrue(true);
    }
    
    
    
}
