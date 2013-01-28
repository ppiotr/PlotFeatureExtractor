/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.awt.geom.Point2D;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author piotr
 */
public class CoordinateSystemTest {

    public CoordinateSystemTest() {
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

    @Test
    public void testAxisExtraction() {
        SVGPlot plot;
        List<CoordinateSystem> extractCoordinateSystems;


        //plot = getSamplePlot0();
        //extractCoordinateSystems = CoordinateSystem.extractCoordinateSystems(plot);


//        plot = getSamplePlot1();
//        extractCoordinateSystems = CoordinateSystem.extractCoordinateSystems(plot);
//
        
        plot = getSamplePlot2();
        extractCoordinateSystems = CoordinateSystem.extractCoordinateSystems(plot);

        plot = getSamplePlot3();
        extractCoordinateSystems = CoordinateSystem.extractCoordinateSystems(plot);

        System.out.println("Hello test");
    }

    /**
     * Returns an empty sample plot
     *
     * @return
     */
    private SVGPlot getSamplePlot0() {
        SVGPlot result = new SVGPlot();
        result.calculateOrthogonalIntervals();
        return result;
    }

    /**
     * returns a sample SVG plot containnig only 2 orthogonal lines
     */
    private SVGPlot getSamplePlot1() {
        SVGPlot result = new SVGPlot();
        result.lineSegments.add(SVGPlot.buildLineFromPoints(new Point2D.Double(0, 0),
                new Point2D.Double(0, 10)));

        result.lineSegments.add(SVGPlot.buildLineFromPoints(new Point2D.Double(0, 10),
                new Point2D.Double(10, 10)));
        result.calculateOrthogonalIntervals();
        return result;
    }

    /**
     * Returns a sample SVG plot containnig only 2 orthogonal lines and some
     * equally distributed ticks on them
     */
    private SVGPlot getSamplePlot2() {
        SVGPlot result = new SVGPlot();
        result.lineSegments.add(SVGPlot.buildLineFromPoints(new Point2D.Double(0, 0),
                new Point2D.Double(0, 10)));

        result.lineSegments.add(SVGPlot.buildLineFromPoints(new Point2D.Double(0, 10),
                new Point2D.Double(10, 10)));


        for (int i = 1; i < 10; i++) {
            result.lineSegments.add(SVGPlot.buildLineFromPoints(new Point2D.Double(i, 9.5),
                    new Point2D.Double(i, 10.5)));
            result.lineSegments.add(SVGPlot.buildLineFromPoints(new Point2D.Double(-0.5, i),
                    new Point2D.Double(0.5, i)));
        }

        result.calculateOrthogonalIntervals();
        return result;
    }

    /**
     * Returns a sample SVG plot containnig only 2 orthogonal lines and some
     * equally distributed small ticks plus larger ticks
     */
    private SVGPlot getSamplePlot3() {
        SVGPlot result = new SVGPlot();
        result.lineSegments.add(SVGPlot.buildLineFromPoints(new Point2D.Double(0, 0),
                new Point2D.Double(0, 10)));

        result.lineSegments.add(SVGPlot.buildLineFromPoints(new Point2D.Double(0, 10),
                new Point2D.Double(10, 10)));


        for (int i = 1; i < 10; i++) {
            result.lineSegments.add(SVGPlot.buildLineFromPoints(new Point2D.Double(i, 9.5),
                    new Point2D.Double(i, 10.5)));
            result.lineSegments.add(SVGPlot.buildLineFromPoints(new Point2D.Double(-0.5, i),
                    new Point2D.Double(0.5, i)));
        }

        for (int i = 3; i < 10; i += 3) {
            result.lineSegments.add(SVGPlot.buildLineFromPoints(new Point2D.Double(i, 9),
                    new Point2D.Double(i, 11)));
            result.lineSegments.add(SVGPlot.buildLineFromPoints(new Point2D.Double(1, i),
                    new Point2D.Double(1, i)));
        }

        result.calculateOrthogonalIntervals();
        return result;
    }
}
