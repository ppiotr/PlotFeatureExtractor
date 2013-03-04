/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;

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
        // Now writing the semantic data
        OutputWriter ow = new OutputWriter();
        Random random = new Random();

        
        ow.writePlotData(plot, "http://plots.com/" + random.nextInt(100000000));
        ow.flush();
    }
}
