/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.parser.DefaultPathHandler;
import org.apache.batik.parser.DefaultPointsHandler;
import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PathHandler;
import org.apache.batik.parser.PathParser;
import org.apache.batik.parser.PointsHandler;
import org.apache.batik.parser.PointsParser;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author piotr
 */
public class PlotMetadataExtractor {
  
    public static float getIntervalAngle(Line2D interval) {

        float xLen = Math.abs((float) (interval.getX2() - interval.getX1()));
        float yLen = Math.abs((float) (interval.getY2() - interval.getY1()));

        float alpha; // we use grades for debugging purposes ... detection of PI/2 etc.. is difficult when seeing a number

        if (xLen == 0) {
            alpha = 90;
        } else {
            float den = (float) interval.getP1().distance(interval.getP2());
            float sinAlpha = yLen / den;

            alpha = (float) Math.asin(sinAlpha) * 360 / (2 * (float) Math.PI);
            System.out.print("");
        }
        if (interval.getY2() < interval.getY1()) {
            alpha = -alpha;
        }
        return alpha;
    }

    public static Map<Line2D, List<Line2D>> getOrthogonalIntervals(List<Line2D> intervals) {
        System.out.println("Processing line intervals");
        HashMap<Integer, List<Line2D>> linesByAngle = new HashMap<Integer, List<Line2D>>();
        HashMap<Line2D, List<Line2D>> intersecting = new HashMap<Line2D, List<Line2D>>();

        // first divide lines into buckets by angle
        for (Line2D interval : intervals) {
            int alpha = Math.round(getIntervalAngle(interval));
            if (!linesByAngle.containsKey(alpha)) {
                linesByAngle.put(alpha, new LinkedList<Line2D>());
            }
            linesByAngle.get(alpha).add(interval);
            //System.out.println("Detected that line segment " + ExtractorGeometryTools.lineToString(interval) + " is inclinde by the angle " + alpha);
        }

        // now we consider every line and search for lines potentially being orthogonal to it

        for (Line2D interval : intervals) {
            int searchRadius = 3; // we search 3 angles around the exact orthogonality
            int alpha = Math.round(getIntervalAngle(interval));
            LinkedList<Line2D> curIntersecting = new LinkedList<Line2D>();

            // using our representation, there is only one possible orthogonal direction !
            int testAngle = alpha + 90 - searchRadius;
            if (testAngle > 90) {
                testAngle = alpha - 90 - searchRadius;
            }

            for (int i = 0; i < 2 * searchRadius; i++) {
                List<Line2D> ortLines = linesByAngle.get(testAngle);
                if (ortLines != null) {
                    for (Line2D line : ortLines) {
                        if (line.intersectsLine(interval)) {
                            curIntersecting.add(line);
                        }
                    }
                }
                if (testAngle == 90) {
                    testAngle = -90;
                }
                testAngle++;
            }
            if (curIntersecting.size() > 0) {
                intersecting.put(interval, curIntersecting);
            }

        }


        //render orthogonal

        try {
            DebugGraphicalOutput dout = DebugGraphicalOutput.getInstance();
            dout.flush(new File("/tmp/out2.png"));


            Random r = new Random();
            for (Line2D line : intersecting.keySet()) {
                if (r.nextInt(300) == 0) {
                    //we search for the longest intersecting and draw intersecting with this one

                    Line2D winner = line;

                    for (Line2D inter : intersecting.get(line)) {
                        if (lineLen(inter) > lineLen(winner)) {
                            winner = inter;
                        }
                    }

                    // now drawing the winner
                    dout.graphics.setColor(new Color(r.nextInt(256), r.nextInt(256), r.nextInt(256)));
                    Rectangle bounds = winner.getBounds();
                    dout.graphics.draw(bounds);

                    for (Line2D inter : intersecting.get(winner)) {
                        dout.graphics.draw(inter.getBounds());
                    }

                }
            }
            dout.flush(new File("/tmp/out3.png"));
        } catch (IOException ex) {
            Logger.getLogger(PlotMetadataExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }

        return intersecting;


    }

    private static List<Pair<Line2D, Line2D>> getPlotAxis(LinkedList<Line2D> lineSegments1, LinkedList<Point2D> points, Map<Line2D, List<Line2D>> orthogonalIntervals) {
        HashSet<Line2D> consideredLines = new HashSet<Line2D>();
        for (Line2D interval : orthogonalIntervals.keySet()) {
            consideredLines.add(interval);
            for (Line2D ortInt : orthogonalIntervals.get(interval)) {
                if (!consideredLines.contains(ortInt)) {
                    // we haven't considered this pair yet
                    Pair<Line2D, Line2D> axisCandidate = new ImmutablePair<Line2D, Line2D>(interval, ortInt);
                    int linesOutside = getNumberOfPointsOutside(axisCandidate, points);
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

    private static double lineLen(Line2D l) {
        return Math.sqrt((l.getX2() - l.getX1()) * (l.getX2() - l.getX1()) + (l.getY2() - l.getY1()) * (l.getY2() - l.getY1()));
    }

    /**
     * Collect all lines being children of the given node
     */
    public static LinkedList<String> getLines(GraphicsNode root) {
        LinkedList<String> result = new LinkedList<String>();
        return result;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException {
        SVGPlot plot = new SVGPlot(args[0]);
        trySVGDOMProcessing(args[0]);

        //System.out.println("Starting the processing by the Batik parser");
        //BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        //    extractPoints(reader);
    }
}
