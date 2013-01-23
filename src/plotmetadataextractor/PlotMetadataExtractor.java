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

    private static List<Pair<ExtLine2D, ExtLine2D>> getPlotAxis(SVGPlot plot) {
        HashSet<ExtLine2D> consideredLines = new HashSet<ExtLine2D>();
        for (ExtLine2D interval : plot.orthogonalIntervals.keySet()) {
            consideredLines.add(interval);
            for (ExtLine2D ortInt : plot.orthogonalIntervals.get(interval)) {
                if (!consideredLines.contains(ortInt)) {
                    // we haven't considered this pair yet
                    Pair<ExtLine2D, ExtLine2D> axisCandidate = new ImmutablePair<ExtLine2D, ExtLine2D>(interval, ortInt);

                    double linesOutside = getRatioOfPointsOutside(axisCandidate, plot.points);
                    // calculations based on the line proportions
                    double lengthsRatio = getAxisLengthRatio(axisCandidate);
                    
                    //double 
                }
            }
        }
        return null;
    }

    /**
     * Returns a normalised ratio of length between two axis candidates
     * @param axisCandidate
     * @return 
     */
    
    private static double getAxisLengthRatio(Pair<ExtLine2D, ExtLine2D> axisCandidate) {
        double res = 0;
        double l2 = axisCandidate.getValue().len();

        if (l2 != 0) {
            res = axisCandidate.getKey(). len() / l2;
            if (res > 1) {
                res = 1 / res; // we always normalise the ratio
            }
        }

        return res;
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
    private static double getRatioOfPointsOutside(Pair<ExtLine2D, ExtLine2D> axisCandidate, List<Point2D> points) {
        int numPoints = 0;
        ExtLine2D line1 = axisCandidate.getKey();
        ExtLine2D line2 = axisCandidate.getValue();

        Rectangle axisArea = line1.getBounds().union(line2.getBounds());
        for (Point2D point : points) {
            if (axisArea.contains(point)) {
                numPoints++;
            }
        }

        if (points.size() > 0) {
            return ((double) numPoints) / ((double) (points.size()));
        } else {
            return 0;
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException {
        SVGPlot plot = new SVGPlot(args[0]);
        List<Pair<ExtLine2D, ExtLine2D>> plotAxis = getPlotAxis(plot);

        System.out.println("Detected the following number of axis: " + plotAxis.size());

    }
}
