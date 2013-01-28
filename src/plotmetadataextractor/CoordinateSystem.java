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
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author piotr
 */
public class CoordinateSystem {

    private static double precission = 4;
    Line2D axe1;
    Line2D axe2;
    DoubleTreeMap<Line2D> ticks1; // ticks on the 1st axis
    DoubleTreeMap<Line2D> ticks2; // ticks on the 2nd axis ... mapping the point with the 

    /**
     * Returns the same coordinate axis with the axis meaning inverted
     *
     * @return
     */
    public CoordinateSystem transpose() {
        return null;
    }

    /**
     * Describes parameters of a coordinate system candidate a set of such
     * objects is then passed through a filter
     */
    public static class CoordCandidateParams {

        Pair<ExtLine2D, ExtLine2D> axes;
        public double linesOutside;
        public double lengthsRatio;
        public Pair<AxisTick, AxisTick> axesTicks;

        /**
         * Transform this minimal description into a full coordinates system
         */
        public CoordinateSystem toCoordinateSystem() {
            return null;
        }
    }

    /**
     * Detects all the coordinate systems encoded in a graph
     *
     * @param plot
     * @return
     */
    public static List<CoordinateSystem> extractCoordinateSystems(SVGPlot plot) {
        LinkedList<CoordinateSystem> result = new LinkedList<CoordinateSystem>();

        LinkedList<CoordCandidateParams> coordParams = new LinkedList<CoordCandidateParams>();

        HashSet<ExtLine2D> alreadyConsideredL = new HashSet<ExtLine2D>();
        for (ExtLine2D interval : plot.orthogonalIntervals.keySet()) {
            alreadyConsideredL.add(interval);
            for (ExtLine2D ortInt : plot.orthogonalIntervals.get(interval)) {
                if (!alreadyConsideredL.contains(ortInt)) {
                    // we haven't considered this pair yet
                    CoordCandidateParams params = new CoordCandidateParams();

                    params.axes = new ImmutablePair<ExtLine2D, ExtLine2D>(interval, ortInt);
                    params.linesOutside = getRatioOfPointsOutside(params.axes, plot.points);
                    // calculations based on the line proportions
                    params.lengthsRatio = getAxisLengthRatio(params.axes);
                    params.axesTicks = retrieveAxisTicks(plot, params.axes);
                    if (initialFilter(params)) {
                        coordParams.add(params);
                    }
                }
            }
        }


        /// we have extracted information about all possible candidates, now we need to determine which ones are the real axes ... 

        LinkedList<CoordCandidateParams> coordinateSystems = CoordinateSystem.filterCandidates(coordParams);

        /// now we need to transform the selected axes into coordinate systems

        for (CoordCandidateParams params : coordinateSystems) {
            result.add(params.toCoordinateSystem());
        }

        return result;

    }

    /**
     * An initial heuristic allowing to asses if axis candidate has a chance of
     * describing axis ... very rough
     *
     * @param coordParams
     * @return
     */
    private static boolean initialFilter(CoordCandidateParams params) {
        return (params.axesTicks.getKey().ticks.size() > 4) && (params.lengthsRatio > 0.7) && (params.lengthsRatio < 1.3);

    }

    /**
     * Analyse a list of coordinate system candidates and return those which
     * really describe coordinate systems
     *
     * @param coordParams
     * @return
     */
    private static LinkedList<CoordCandidateParams> filterCandidates(LinkedList<CoordCandidateParams> coordParams) {
        // here we plug a SVM to determine which candidates really describe a coordinates system and which do not
        Object[] array = coordParams.toArray();
        Arrays.sort(array, new Comparator<Object>() {
            @Override
            public int compare(Object os1, Object os2) {
                CoordCandidateParams o1 = (CoordCandidateParams) os1;
                CoordCandidateParams o2 = (CoordCandidateParams) os2;
                int s1 = o1.axesTicks.getKey().ticks.size() + o1.axesTicks.getValue().ticks.size();
                int s2 = o2.axesTicks.getKey().ticks.size() + o2.axesTicks.getValue().ticks.size();

                return s2 - s1;
            }
        });

        // let's draw best 10 !

        DebugGraphicalOutput dgo = DebugGraphicalOutput.getInstance();
        for (int i = 0; i < 500; i++) {
            dgo.reset();
            dgo.graphics.setColor(Color.red);

            // drawing the first axis
            CoordCandidateParams par = (CoordCandidateParams) array[i];
            dgo.graphics.draw(par.axes.getKey());
            for (ExtLine2D line : par.axesTicks.getKey().ticks.values()) {
                dgo.graphics.draw(line);
            }

            dgo.graphics.setColor(Color.blue);
            dgo.graphics.draw(par.axes.getValue());
            for (ExtLine2D line : par.axesTicks.getValue().ticks.values()) {
                dgo.graphics.draw(line);
            }
            try {
                dgo.flush(new File("/tmp/detected_" + String.valueOf(i) + ".png"));
            } catch (IOException ex) {
                Logger.getLogger(CoordinateSystem.class.getName()).log(Level.SEVERE, null, ex);
            }
        }


        //let's sort by the number of detected ticks

        throw new UnsupportedOperationException("Not yet implemented");

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
        public DoubleTreeMap<ExtLine2D> ticks;
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

    private static DoubleTreeMap<List<Pair<ExtLine2D, Double>>> ticksCalculateOriginDist(ExtLine2D axis, SVGPlot plot, Point2D.Double origin) {
        DoubleTreeMap<List<Pair<ExtLine2D, Double>>> intersections = new DoubleTreeMap<List<Pair<ExtLine2D, Double>>>(CoordinateSystem.precission);

        for (ExtLine2D intLine : plot.orthogonalIntervals.get(axis)) {
            Point2D.Double tickInt = axis.getIntersection(intLine);
            double origDist = tickInt.distance(origin);
            // we have to figure out the sign ( we want to be able to distinguish points on both sides of the origin
            // we take a unit vector of the acis as reference
            Pair<Double, Double> unitVector = axis.getUnitVector();
            origDist *= Math.signum(ExtLine2D.vecScalarProd(unitVector, ExtLine2D.getVector(origin, tickInt)));

            if (Math.abs(origDist - 0.01) > 0) {
                if (!intersections.containsKey(Math.abs(origDist))) {
                    intersections.put(Math.abs(origDist), new LinkedList<Pair<ExtLine2D, Double>>());
                }
                intersections.get(Math.abs(origDist)).add(new ImmutablePair<ExtLine2D, Double>(intLine, origDist));
            }
        }
        return intersections;
    }

    /**
     * Puts ticks in buckets - aggregating by every distance which multiplied by
     * an integer can give the given distance (approximately, with a given
     * tolerance expressed as a fraction of the basic tick)
     *
     * @param intersections
     * @param toleranceLimit
     * @return
     */
    
    private static DoubleTreeMap<List<Pair<ExtLine2D, Double>>> ticksAggregateByMinimalDistance(
            TreeMap<Double, List<Pair<ExtLine2D, Double>>> intersections,
            double toleranceLimit) {
        DoubleTreeMap<List<Pair<ExtLine2D, Double>>> byTick = new DoubleTreeMap<List<Pair<ExtLine2D, Double>>>(CoordinateSystem.precission);

        for (Double origDst : intersections.keySet()) {
            if (!byTick.containsKey(origDst)) {
                byTick.put(origDst, new LinkedList<Pair<ExtLine2D, Double>>());
            }

            // let's check the already added ticks (in any order as they will be <= to this one

            for (Double exDst : byTick.keySet()) {
                double divided = origDst / exDst;
                if (Math.abs(Math.round(divided) - divided) < toleranceLimit) {
                    // this is a multiplication of the tick
                    byTick.get(exDst).addAll(intersections.get(origDst));
                }
            }
        }
        return byTick;
    }

    public static AxisTick retrieveAxisTick(ExtLine2D axis, SVGPlot plot, Point2D.Double origin, double adjAxisLen) {
        double toleranceLimit = 2;
    
        DoubleTreeMap<List<Pair<ExtLine2D, Double>>> intersections = CoordinateSystem.ticksCalculateOriginDist(axis, plot, origin);

        DoubleTreeMap<List<Pair<ExtLine2D, Double>>> byTick = CoordinateSystem.ticksAggregateByMinimalDistance(intersections, toleranceLimit);

        /**
         * Now we have to make sure that the ticks are more or less uniform. For
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
        DoubleTreeMap<List<Pair<ExtLine2D, Double>>> uniformByTick = new DoubleTreeMap<List<Pair<ExtLine2D, Double>>>(CoordinateSystem.precission);
        DoubleTreeMap<Pair<Double, Double>> uniformByTickLengths = new DoubleTreeMap<Pair<Double, Double>>(CoordinateSystem.precission);



        for (Double dst : byTick.keySet()) {
            //let's create a histogram of ticks
            TreeMap<Long, List<Pair<ExtLine2D, Double>>> histo =
                    new TreeMap<Long, List<Pair<ExtLine2D, Double>>>();

            // describes the minimum distance from the origin for a given tick length
            HashMap<Long, Double> minDistance = new HashMap<Long, Double>();

            for (Pair<ExtLine2D, Double> tickPair : byTick.get(dst)) {
                ExtLine2D tick = tickPair.getKey();




                double length = tick.getP1().distance(tick.getP2());
                long bucketNum = Math.round(length / unitLen);



                Double dist = Math.abs(tickPair.getValue());
                if (!minDistance.containsKey(bucketNum) || dst < minDistance.get(bucketNum)) {
                    minDistance.put(bucketNum, dist);
                }

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
            double firstMin = 0; // the smallest distance from the origin to the small tick
            double secondMin = 0;  // the smallest distance from the origin to the large tick
            uniformByTick.put(dst, new LinkedList<Pair<ExtLine2D, Double>>());
            Long maxBucketSize;
            try {
                maxBucketSize = maximums.lastKey();
            } catch (Exception e) {
                maxBucketSize = null;
            }

            if (maxBucketSize != null) {
                Long maxBucket = maximums.get(maxBucketSize);
                firstMin = maxBucket * unitLen;
                uniformByTick.get(dst).addAll(histo.get(maxBucket));
                if (histo.containsKey(maxBucket - 1)) {
                    uniformByTick.get(dst).addAll(histo.get(maxBucket - 1));
                }
                if (histo.containsKey(maxBucket + 1)) {
                    uniformByTick.get(dst).addAll(histo.get(maxBucket + 1));
                }




                Long secondMaxSize;
                try {
                    secondMaxSize = maximums.floorKey(maxBucketSize - 1);
                } catch (Exception e) {
                    secondMaxSize = null;
                }

                if (secondMaxSize != null) {
                    Long secondMaxBucket = maximums.get(secondMaxSize);
                    secondMin = secondMaxBucket * unitLen;
                    uniformByTick.get(dst).addAll(histo.get(secondMaxBucket));
                    if (histo.containsKey(secondMaxBucket - 1)) {
                        uniformByTick.get(dst).addAll(histo.get(secondMaxBucket - 1));
                    }
                    if (histo.containsKey(secondMaxBucket + 1)) {
                        uniformByTick.get(dst).addAll(histo.get(secondMaxBucket + 1));
                    }

                    secondMin = minDistance.get(secondMaxBucket);
                }
            }

            uniformByTickLengths.put(dst, new ImmutablePair<Double, Double>(firstMin, secondMin));
        }

        // now select the distance having the most ticks
        long maxNum = 0;
        double maxTick = 0;

        for (Double tick : uniformByTick.keySet()) {
            if (uniformByTick.get(tick).size() > maxNum) {
                maxNum = uniformByTick.get(tick).size();
                maxTick = tick;
            }
        }
        Pair<java.lang.Double, java.lang.Double> minDists;

        minDists = uniformByTickLengths.get(maxTick);
        if (minDists == null) {
            System.out.println("ups - there is no minimals distance for this axis candidate");
            minDists = new ImmutablePair<java.lang.Double, java.lang.Double>(0.0, 0.0);
        }


        AxisTick res = new AxisTick();
        res.minorTick = maxTick;
        res.ticks = new DoubleTreeMap<ExtLine2D>(CoordinateSystem.precission);
        res.majorTick = minDists.getValue();

        if (uniformByTick.get(maxTick) != null) {
            for (Pair<ExtLine2D, Double> tick : uniformByTick.get(maxTick)) {
                res.ticks.put(tick.getValue(), tick.getKey());
            }
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
