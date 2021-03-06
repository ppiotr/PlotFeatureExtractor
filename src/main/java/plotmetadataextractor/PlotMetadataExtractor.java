/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.io.FileNotFoundException;
import java.io.IOException;
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
    public static void main(String[] args) throws FileNotFoundException, IOException {


        SVGPlot plot = new SVGPlot(args[0]);
        List<CoordinateSystem> plotAxis = CoordinateSystem.extractCSCandidates(plot);
        CoordSystemSVM svm = CoordSystemSVM.getStandardModel();
       // for (CoordinateSystem cs : plotAxis) {
       //     svm.isCoordinateSystem(cs, plot);
       // }

        System.out.println("Detected the following number of axis: " + plotAxis.size());
        // Now writing the semantic data
        SemanticsOutputWriter ow = new SemanticsOutputWriter();
        Random random = new Random();


        ow.writePlotData(plot, "http://plots.com/" + random.nextInt(100000000));
        ow.flush();
    }
}
