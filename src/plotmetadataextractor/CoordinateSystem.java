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

    public static class Tick {

        public ExtLine2D line;
        public Point2D.Double intersection; // intersection with the axis
        public double length;
        public double distanceFromTheOrigin;
        public boolean isMajor;

        public Tick(ExtLine2D line, Point2D.Double intersect, double distance) {
            this.line = line;
            this.intersection = intersect;
            this.distanceFromTheOrigin = distance;
            this.length = line.getP1().distance(line.getP2());
            this.isMajor = false;
        }

        public Tick(ExtLine2D line, ExtLine2D axis, Point2D.Double origin) {
            this.line = line;
            this.intersection = line.getIntersection(axis);

            this.distanceFromTheOrigin = line.distance(origin);
            Pair<Double, Double> unitVector = axis.getUnitVector();
            this.distanceFromTheOrigin *= Math.signum(ExtLine2D.vecScalarProd(unitVector, ExtLine2D.getVector(origin, this.intersection)));

            this.length = line.getP1().distance(line.getP2());
            this.isMajor = false;
        }
    }

    /**
     * This method embarks on a remarkable quest of matching ticks of axis
     * candidates with possible labels, which might indicate numerical values
     *
     * @param plot
     */
    private static void matchTicksWithCaptions(SVGPlot plot, CoordCandidateParams coord) {
        for (Tick tick: coord.axesTicks.getKey().ticks.values()){
            
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

        LinkedList<CoordCandidateParams> coordinateSystems = CoordinateSystem.filterCandidates(coordParams, plot);

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
    private static LinkedList<CoordCandidateParams> filterCandidates(LinkedList<CoordCandidateParams> coordParams, SVGPlot plot) {
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
            for (Tick tick : par.axesTicks.getKey().ticks.values()) {
                dgo.graphics.draw(tick.line);
            }

            dgo.graphics.setColor(Color.blue);
            dgo.graphics.draw(par.axes.getValue());
            for (Tick tick : par.axesTicks.getValue().ticks.values()) {
                dgo.graphics.draw(tick.line);
            }
            try {
                dgo.flush(new File("/tmp/detected_" + String.valueOf(i) + ".png"));
            } catch (IOException ex) {
                Logger.getLogger(CoordinateSystem.class.getName()).log(Level.SEVERE, null, ex);
            }
            CoordinateSystem.retrieveAxisTicks(plot, par.axes);
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
         * sometimes there are ticks which are longer, but still repeat
         * regularly every N smaller ticks. These usually are marked with labels
         * which we should detect
         */
        public DoubleTreeMap<Tick> ticks;
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

    /**
     * In this case we return an exact tree map rather than approximate !
     *
     * @param axis
     * @param plot
     * @param origin
     * @return
     */
    private static TreeMap<Double, List<Tick>> ticksCalculateOriginDist(ExtLine2D axis, SVGPlot plot, Point2D.Double origin) {
        TreeMap<Double, List<Tick>> intersections = new TreeMap<Double, List<Tick>>();
        for (ExtLine2D intLine : plot.orthogonalIntervals.get(axis)) {
            Point2D.Double tickInt = axis.getIntersection(intLine);
            Tick tick = new Tick(intLine, axis, origin);
            if (Math.abs(tick.distanceFromTheOrigin - 0.01) > 0) {
                if (!intersections.containsKey(Math.abs(tick.distanceFromTheOrigin))) {
                    intersections.put(Math.abs(tick.distanceFromTheOrigin), new LinkedList<Tick>());
                }
                intersections.get(Math.abs(tick.distanceFromTheOrigin)).add(tick);
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
    private static DoubleTreeMap<List<Tick>> ticksAggregateByMinimalDistance(
            TreeMap<Double, List<Tick>> intersections,
            double toleranceLimit) {
        DoubleTreeMap<List<Tick>> byTick = new DoubleTreeMap<List<Tick>>(CoordinateSystem.precission);

        for (Double origDst : intersections.keySet()) {
            if (!byTick.containsKey(origDst)) {
                byTick.put(origDst, new LinkedList<Tick>());
            }

            // let's check the already added ticks (in any order as they will be <= to this one

            for (Double exDst : byTick.keySet()) {
                double divided = origDst / exDst;
                if (Math.abs(Math.round(divided) * exDst - origDst) < toleranceLimit) {
                    // this is a multiplication of the tick
                    byTick.get(exDst).addAll(intersections.get(origDst));
                }
            }
        }
        return byTick;
    }

    /**
     * Creates a histogram aggregating ticks by the minimal lengths they belong
     *
     *
     * @param byTick
     * @param dst
     * @param unitLen
     * @return
     */
    private static TreeMap<Long, List<Tick>> createLengthsHistogram(DoubleTreeMap<List<Tick>> byTick, double dst, double unitLen) {
        TreeMap<Long, List<Tick>> histo = new TreeMap<Long, List<Tick>>();
        // describes the minimum distance from the origin for a given tick length

        for (Tick tick : byTick.get(dst)) {
            long bucketNum = Math.round(tick.length / unitLen);

            Double dist = Math.abs(tick.distanceFromTheOrigin);

            if (!histo.containsKey(bucketNum)) {
                histo.put(bucketNum, new LinkedList<Tick>());
            }
            histo.get(bucketNum).add(tick);
        }
        return histo;
    }

    public static AxisTick retrieveAxisTick(ExtLine2D axis, SVGPlot plot, Point2D.Double origin, double adjAxisLen) {
        double toleranceLimit = 4;

        TreeMap<Double, List<Tick>> intersections = CoordinateSystem.ticksCalculateOriginDist(axis, plot, origin);
        DoubleTreeMap<List<Tick>> byTick = CoordinateSystem.ticksAggregateByMinimalDistance(intersections, toleranceLimit);
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
        double eqFraction = 0.002; // difference of 1% of the adjacent axis counts like being equal
        double unitLen = adjAxisLen * eqFraction;
        DoubleTreeMap<List<Tick>> uniformByTick = new DoubleTreeMap<List<Tick>>(CoordinateSystem.precission);

        for (Double dst : byTick.keySet()) {
            //let's create a histogram of ticks
            TreeMap<Long, List<Tick>> histo = CoordinateSystem.createLengthsHistogram(byTick, dst.doubleValue(), unitLen);


            /**
             * Now we read the histogram and select two buckets containing the
             * most lines. We consider 3 adjacent buckets to be of single
             * length. ... we also assume that different lengths of ticks will
             * be distant enough
             */
            TreeMap<Long, List<Long>> maximums = new TreeMap<Long, List<Long>>(); // a queue number of elements -> centre

            for (Long bucket : histo.keySet()) {
                long numEl = histo.get(bucket).size();
                if (!maximums.containsKey(numEl)) {
                    maximums.put(numEl, new LinkedList<Long>());
                }
                maximums.get(numEl).add(bucket);
            }

            // now read two maximal values and copy the pairs into the final structure
            uniformByTick.put(dst, new LinkedList<Tick>());
            Long maxBucketSize;
            try {
                maxBucketSize = maximums.lastKey();
            } catch (Exception e) {
                maxBucketSize = null;
            }

            if (maxBucketSize != null) {
                List<Long> maxList = maximums.get(maxBucketSize);
                Long maxBucket = maxList.get(0);
                uniformByTick.get(dst).addAll(histo.get(maxBucket));
                if (histo.containsKey(maxBucket - 1)) {
                    uniformByTick.get(dst).addAll(histo.get(maxBucket - 1));
                }
                if (histo.containsKey(maxBucket + 1)) {
                    uniformByTick.get(dst).addAll(histo.get(maxBucket + 1));
                }

                Long secondMaxSize;
                Long secondMaxBucket = null;


                if (maxList.size() > 1) {
                    secondMaxBucket = maxList.get(1);
                } else {
                    try {
                        secondMaxSize = maximums.floorKey(maxBucketSize - 1);
                    } catch (Exception e) {
                        secondMaxSize = null;
                    }

                    if (secondMaxSize != null) {
                        secondMaxBucket = maximums.get(secondMaxSize).get(0);
                    }
                }

                if (secondMaxBucket != null) {
                    for (Tick tick : histo.get(secondMaxBucket)) {
                        tick.isMajor = true;
                        uniformByTick.get(dst).add(tick);

                    }

                    if (histo.containsKey(secondMaxBucket - 1)) {
                        for (Tick tick : histo.get(secondMaxBucket - 1)) {
                            tick.isMajor = true;
                            uniformByTick.get(dst).add(tick);
                        }
                    }

                    if (histo.containsKey(secondMaxBucket + 1)) {
                        for (Tick tick : histo.get(secondMaxBucket + 1)) {
                            tick.isMajor = true;
                            uniformByTick.get(dst).add(tick);
                        }
                    }
                }
            }
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


        AxisTick res = new AxisTick();
        res.minorTick = maxTick;
        res.ticks = new DoubleTreeMap<Tick>(CoordinateSystem.precission);

        if (uniformByTick.get(maxTick) != null) {
            for (Tick tick : uniformByTick.get(maxTick)) {
                res.ticks.put(tick.distanceFromTheOrigin, tick);
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
