/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.io.FileNotFoundException;
import java.util.List;

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
