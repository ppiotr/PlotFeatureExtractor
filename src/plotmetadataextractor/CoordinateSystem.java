/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author piotr
 */
public class CoordinateSystem {

    Line2D axe1;
    Line2D axe2;
    Map<Double, Line2D> ticks1; // ticks on the 1st axis
    Map<Double, Line2D> ticks2; // ticks on the 2nd axis ... mapping the point with the 

    /**
     * Returns the same coordinate axis with the axis meaning inverted
     *
     * @return
     */
    public CoordinateSystem transpose() {
        return null;
    }

    /**
     * Detects all the coordinate systems encoded in a graph
     *
     * @param plot
     * @return
     */
    public static List<CoordinateSystem> extractCoordinateSystems(SVGPlot plot) {
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


                }
            }
        }
        return null;
    }

    /**
     * A class representing ticks on an axis
     */
    public static class AxisTick {

        public double minorTick;
        /**
         * sometimes there are ticks which are longer, btu stll repeat regularly
         * every N smaller ticks. These usually are marked with labels which we
         * should detect
         */
        public double majorTick;
        public Map<Double, ExtLine2D> ticks;
    }

    /**
     * Retrieves the most likely tick marked on the axis. By tick we understand
     * an orthogonal line intersecting the axis, which is repeated in equal
     * distances at least a certain number of times.
     *
     * TODO: constraints could be extended with the necessity of ticks covering
     * the entire axis, but this is about risky as could yield higher rate of
     * false negatives
     *
     * Another possible extension would bind the number of ticks on the Y axis
     * with those on the X axis.
     *
     * @param plot
     * @param axisCandidate
     * @return
     */
    public static Pair<AxisTick, AxisTick> retrieveAxisTicks(SVGPlot plot, Pair<ExtLine2D, ExtLine2D> axisCandidate) {
        Point2D.Double origin = axisCandidate.getKey().getIntersection(axisCandidate.getValue());
        ExtLine2D a1 = axisCandidate.getKey();
        ExtLine2D a2 = axisCandidate.getValue();



        return new ImmutablePair<AxisTick, AxisTick>(
                retrieveAxisTick(a1, plot, origin, a2.len()),
                retrieveAxisTick(a2, plot, origin, a1.len()));
    }

    public static AxisTick retrieveAxisTick(ExtLine2D axis, SVGPlot plot, Point2D.Double origin, double adjAxisLen) {
        double toleranceLimit = 0.05;
        TreeMap<Double, List<Pair<ExtLine2D, Double>>> intersections = new TreeMap<Double, List<Pair<ExtLine2D, Double>>>();
        // first we process all the intersecting lines and calculate their distance from the origin
        for (ExtLine2D intLine : plot.orthogonalIntervals.get(axis)) {
            Point2D.Double tickInt = axis.getIntersection(intLine);
            double origDist = tickInt.distance(origin);

            if (Math.abs(origDist - 0.01) > 0) {
                if (!intersections.containsKey(Math.abs(origDist))) {
                    intersections.put(Math.abs(origDist), new LinkedList<Pair<ExtLine2D, Double>>());
                }
                intersections.get(Math.abs(origDist)).add(new ImmutablePair<ExtLine2D, Double>(intLine, origDist));
            }
        }

        /*
         * Now let's put ticks in buckets - aggregating by every distance which
         * multiplied by an integer can give the given distance (approximately,
         * with a given tolerance expressed as a fraction of the basic tick)
         */


        TreeMap<Double, List<Pair<ExtLine2D, Double>>> byTick = new TreeMap<Double, List<Pair<ExtLine2D, Double>>>();

        for (Double origDst : intersections.keySet()) {
            if (!byTick.containsKey(origDst)) {
                byTick.put(origDst, new LinkedList<Pair<ExtLine2D, Double>>());
            }

            // let's check the already added ticks (in any order as they will be <= to this one

            for (Double exDst : byTick.keySet()) {
                double divided = origDst / exDst;
                if (Math.abs(Math.round(divided) - divided) < toleranceLimit) {
                    // this is a multiplication of the tick
                    byTick.get(exDst).addAll(intersections.get(exDst));
                }
            }
        }

        /**
         * now we have to make sure that the ticks are more or less uniform. For
         * every tick we select two most common line lengths and filter out the
         * rest
         *
         * Comparison of lengths is not exact (as we deal with graphics which
         * means that the image is designed to be human-understandable ... and
         * in addition, everything is encoded using floating point numbers). The
         * margins of tolerance allowing to consider differently-lenghted
         * segments to be considered the same are expressed as a fraction of the
         * intersecting axis candidate. The second axis is always classified as
         * a tick, regardless its length )
         */
        double eqFraction = 0.01; // difference of 1% of the adjacent axis counts like being equal
        double unitLen = adjAxisLen * eqFraction;
        TreeMap<Double, List<Pair<ExtLine2D, Double>>> uniformByTick = new TreeMap<Double, List<Pair<ExtLine2D, Double>>>();
        TreeMap<Double, Pair<Double, Double>> uniformByTickLengths = new TreeMap<Double, Pair<Double, Double>>();

        for (Double dst : byTick.keySet()) {
            //let's create a histogram of ticks
            TreeMap<Long, List<Pair<ExtLine2D, Double>>> histo =
                    new TreeMap<Long, List<Pair<ExtLine2D, Double>>>();

            for (Pair<ExtLine2D, Double> tickPair : byTick.get(dst)) {
                ExtLine2D tick = tickPair.getKey();
                double length = tick.getP1().distance(tick.getP2());
                long bucketNum = Math.round(length / unitLen);

                if (!histo.containsKey(bucketNum)) {
                    histo.put(bucketNum, new LinkedList<Pair<ExtLine2D, Double>>());
                }
                histo.get(bucketNum).add(tickPair);
            }

            /**
             * Now we read the histogram and select two buckets containing the
             * most lines. We consider 3 adjacent buckets to be of single
             * length. ... we also assume that different lengths of ticks will
             * be distant enough
             */
            TreeMap<Long, Long> maximums = new TreeMap<Long, Long>(); // a queue number of elements -> centre

            for (Long bucket : histo.keySet()) {
                long numEl = histo.get(bucket).size();
                if (histo.containsKey(bucket - 1)) {
                    numEl += histo.get(bucket - 1).size();
                }
                if (histo.containsKey(bucket + 1)) {
                    numEl += histo.get(bucket + 1).size();
                }
                maximums.put(numEl, bucket);
            }

            // now read two maximal values and copy the pairs into the final structure

            uniformByTick.put(dst, new LinkedList<Pair<ExtLine2D, Double>>());

            Long maxBucketSize = maximums.lastKey();
            Long maxBucket = maximums.get(maxBucketSize);
            
            Long secondMaxSize = maximums.floorKey(maxBucket - 1);
            Long secondMaxBucket = maximums.get(secondMaxSize);

            uniformByTick.get(dst).addAll(histo.get(maxBucket));
            if (histo.containsKey(maxBucket - 1)) {
                uniformByTick.get(dst).addAll(histo.get(maxBucket - 1));
            }
            if (histo.containsKey(maxBucket + 1)) {
                uniformByTick.get(dst).addAll(histo.get(maxBucket + 1));
            }

            uniformByTick.get(dst).addAll(histo.get(secondMaxBucket));
            if (histo.containsKey(secondMaxBucket - 1)) {
                uniformByTick.get(dst).addAll(histo.get(maxBucket - 1));
            }
            if (histo.containsKey(secondMaxBucket + 1)) {
                uniformByTick.get(dst).addAll(histo.get(secondMaxBucket + 1));
            }
        }

        // now select two distances having the maximal number of ticks assigned to them
        long maxNum = 0;
        double maxTick = 0;

        for (Double tick : uniformByTick.keySet()) {
            if (uniformByTick.get(tick).size() > maxNum) {
                maxNum = uniformByTick.get(tick).size();
                maxTick = tick;
            }
        }

        AxisTick res = new AxisTick();
        res.minorTick = maxTick;
        res.ticks = new TreeMap<Double, ExtLine2D>();
        
        for (Pair<ExtLine2D, Double> tick : uniformByTick.get(maxTick)) {
            res.ticks.put(tick.getValue(), tick.getKey());
        }
        
        /**
         * We haven't taken into account any type of requirement that the basic
         * tick distance should be covered with an orthogonal line ... this
         * might be an improvement
         */
        
        return res;
    }

    /**
     * Returns a normalised ratio of length between two axis candidates
     *
     * @param axisCandidate
     * @return
     */
    private static double getAxisLengthRatio(Pair<ExtLine2D, ExtLine2D> axisCandidate) {
        double res = 0;
        double l2 = axisCandidate.getValue().len();

        if (l2 != 0) {
            res = axisCandidate.getKey().len() / l2;
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
}
