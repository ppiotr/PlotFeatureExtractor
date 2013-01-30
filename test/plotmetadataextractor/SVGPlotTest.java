/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author piotr
 */
public class SVGPlotTest {

    public SVGPlotTest() {
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
     * Tests the correctness of splitting boundaries of blocks of text
     */
    @Test
    public void testTextSplitting() {
        SVGPlot plot = new SVGPlot();
        plot.includeTextBlock(new Rectangle2D.Double(5.5, 6.6, 11.34, 12.24), "Hello world");
        plot.includeTextBlock(new Rectangle2D.Double(90.5, 6.6, 11.34, 12.24), " Ala    ma       kota  ");
        Map<Double, String> splittedText = plot.getSplitText();        
        assertEquals(5, splittedText.size());        
    }
}
