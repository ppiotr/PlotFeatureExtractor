/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
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

    private static List<Pair<Line2D, Line2D>> getPlotAxis(SVGPlot plot) {
        HashSet<Line2D> consideredLines = new HashSet<Line2D>();
        for (Line2D interval : plot.orthogonalIntervals.keySet()) {
            consideredLines.add(interval);
            for (Line2D ortInt : plot.orthogonalIntervals.get(interval)) {
                if (!consideredLines.contains(ortInt)) {
                    // we haven't considered this pair yet
                    Pair<Line2D, Line2D> axisCandidate = new ImmutablePair<Line2D, Line2D>(interval, ortInt);
                    int linesOutside = getNumberOfPointsOutside(axisCandidate, plot.points);
                }
            }
        }
        return null;
    }

    /**
     * Calculates the number of points present in the graph, which are outside
     * of the scope of the axis candidate The scope of axis candidate is defined
     * as a rectangle spanned by its lines, extended by a small margin (Which is
     * the parameter of the algorithm.
     *
     * @param segments List of all line segments present in the graph.
     * @return
     */
    private static int getNumberOfPointsOutside(Pair<Line2D, Line2D> axisCandidate, List<Point2D> lineSegments) {

        Line2D line1 = axisCandidate.getKey();
        Line2D line2 = axisCandidate.getValue();


        Rectangle axisArea = line1.getBounds().union(line2.getBounds());

        return 0;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException {
        SVGPlot plot = new SVGPlot(args[0]);
        List<Pair<Line2D, Line2D>> plotAxis = getPlotAxis(plot);

        System.out.println("Detected the following number of axis: " + plotAxis.size());

    }
}
