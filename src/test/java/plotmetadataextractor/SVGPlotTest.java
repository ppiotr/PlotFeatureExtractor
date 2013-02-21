/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.Map;
import junit.framework.TestCase;

/**
 *
 * @author piotr
 */
public class SVGPlotTest extends TestCase {

    public SVGPlotTest() {
    }

    @Override
    public void setUp() throws Exception{
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception{
        super.tearDown();
    }

    /**
     * Tests the correctness of splitting boundaries of blocks of text
     */
    public void testTextSplitting() {
        SVGPlot plot = new SVGPlot();
        plot.includeTextBlock(new Rectangle2D.Double(5.5, 6.6, 11.34, 12.24), "Hello world");
        plot.includeTextBlock(new Rectangle2D.Double(90.5, 6.6, 11.34, 12.24), " Ala    ma       kota  ");
        Map<Double, String> splittedText = plot.getSplitText();
        assertEquals(5, splittedText.size());
    }
}
