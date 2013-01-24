/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author piotr
 */
public class PlotMetadataExtractor {



    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException {
        SVGPlot plot = new SVGPlot(args[0]);
        List<CoordinateSystem> plotAxis = CoordinateSystem.extractCoordinateSystems(plot);

        System.out.println("Detected the following number of axis: " + plotAxis.size());

    }
}
