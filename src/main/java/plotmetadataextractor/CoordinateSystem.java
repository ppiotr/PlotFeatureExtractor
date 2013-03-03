/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import invenio.common.ExtractorGeometryTools;
import invenio.common.IntervalTree;
import invenio.common.IterablesUtils;
import invenio.common.SpatialClusterManager;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
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
    // parameters for the coordinate system detection

    private static double precission = 4;
    private static double descriptionDistance = 1.7; // the maximal distance of tick description from the tick... expressed as minimal tick
    Pair<ExtLine2D, ExtLine2D> axes;
    public double linesOutside;
    public double lengthsRatio;
    public Pair<AxisTick, AxisTick> axesTicks;
    public Point2D.Double origin;

    /**
     * Returns the same coordinate axis with the axis meaning being inverted
     *
     * @return
     */
    public CoordinateSystem transpose() {
        return null;
    }

    /**
     * Returns the rectangle defined by axis of the coordinate system
     *
     * @return
     */
    public Rectangle2D.Double getBoundary() {
        return (Rectangle2D.Double) this.axes.getKey().toRectangle().createUnion(this.axes.getValue().toRectangle());
    }

    public static class TickLabel {

        /**
         * A representation of a label assigned to a tick
         */
        String text;
        Shape boundary;

        /**
         * Creates a new label consisting of a boundary and the describing text
         *
         * @param b
         * @param t
         */
        public TickLabel(Shape b, String t) {
            this.text = t;
            this.boundary = b;
        }
    }

    public static class Tick {

        public ExtLine2D line;
        public Point2D.Double intersection; // intersection with the axis
        public double length;
        public double distanceFromTheOrigin;
        public boolean isMajor;
        public TickLabel label;

        public Tick(ExtLine2D line, Point2D.Double intersect, double distance) {
            this.line = line;
            this.intersection = intersect;
            this.distanceFromTheOrigin = distance;
            this.length = line.getP1().distance(line.getP2());
            this.isMajor = false;
            this.label = null;
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
     * Matches ticks with captions ... for
     */
    private static void matchTicksWithCaptionsAxis(SVGPlot plot, AxisTick ticks) {
        double majorTolerance = 4; // how much father can the label be from a major tick to match it instead of a minor tick
        double dist = ticks.minorTick * CoordinateSystem.descriptionDistance;
        HashMap<Shape, Pair<Double, Tick>> mins = new HashMap<Shape, Pair<Double, Tick>>(); // boundary -> <minimal dist, tick>

        for (Tick tick : ticks.ticks.values()) {
            String minString = "";
            Shape closestSegment = null;
            double minDst = Double.MAX_VALUE;
            Map<Shape, String> nearby = plot.splitTextIndex.getByDistance(tick.intersection, dist);
            for (Shape s : nearby.keySet()) {
                double dst = ExtLine2D.distanceRectangle(tick.intersection, (Rectangle2D.Double) s.getBounds2D());
                if (!tick.isMajor) {
                    dst *= majorTolerance;
                    dst += Double.MIN_VALUE;
                }
                if (minDst > dst) {
                    closestSegment = s;
                    minString = nearby.get(s);
                    minDst = dst;
                }
            }

            Tick prevMin = null;
            double prevDist = Double.MAX_VALUE;
            if (mins.containsKey(closestSegment)) {
                prevDist = mins.get(closestSegment).getKey();
                prevMin = mins.get(closestSegment).getValue();
            }

            if (minDst < prevDist) {
                if (prevMin != null) {
                    prevMin.label = null;
                }
                tick.label = new TickLabel(closestSegment, minString);

                mins.put(closestSegment, new ImmutablePair<Double, Tick>(minDst, tick));
            }
        }
        /**
         * After the initial matching we make an attempt to rematch -> giving
         * more priority to the major ticks. First we check if the number of
         * major ticks is comparable to the number of assigned labels... if so,
         * we should
         */
        int numAssigned = Iterables.size(Iterables.filter(ticks.ticks.values(), new Predicate<Tick>() {
            @Override
            public boolean apply(Tick t) {
                return t.label != null;
            }
        }));

        int numMajor = Iterables.size(Iterables.filter(ticks.ticks.values(), new Predicate<Tick>() {
            @Override
            public boolean apply(Tick t) {
                return t.isMajor;
            }
        }));

        if (numAssigned == 0) {
            return;
        }

        double coeff = (double) numMajor / (double) numAssigned;
        double assignmentTolerance = 1.5;


        if (coeff > 1 / assignmentTolerance && coeff < assignmentTolerance) {
            TickLabel[] labels = new TickLabel[numAssigned];
            int labelsInd = 0;

            for (Tick t : ticks.ticks.values()) {
                if (t.label != null) {
                    labels[labelsInd++] = t.label;

                    t.label = null; // detach the label from the previously-attached tick
                }
            }
            TickLabel[] matchableLabels = selectAppropriateLabels(labels, numMajor);
            // We can sort the selected labels using the distance from the smallest major tick (in order to assure the distance
            Iterable<Tick> major = Iterables.filter(ticks.ticks.values(), new Predicate<Tick>() {
                @Override
                public boolean apply(Tick t) {
                    return t.isMajor;
                }
            });
            Tick first = major.iterator().next(); // there does exist at least one, which we already know;          
            Arrays.sort(matchableLabels, new TickLabelsDistanceFromFirstTickComparator(first.intersection));



            /**
             * we might still have more ticks than labels... we consider all the
             * possible assignments preserving the order of distances from the
             * first tick
             */
            Iterable<Tick> minSubsequence = null;
            double minSum = Double.MAX_VALUE;
            for (Iterable<Tick> curSubsequence : IterablesUtils.skipN(major, numMajor - numAssigned)) {
                double curSum = 0;
                Iterator<Tick> majorIter = curSubsequence.iterator();
                for (int i = 0; i < matchableLabels.length; i++) {
                    Tick tick = majorIter.next();
                    curSum += ExtLine2D.distanceRectangle(tick.intersection, (Rectangle2D.Double) (matchableLabels[i].boundary.getBounds2D()));
                }

                if (curSum < minSum) {
                    minSum = curSum;
                    minSubsequence = curSubsequence;
                }
            }


            Iterator<Tick> majorIter = minSubsequence.iterator();
            for (int i = 0; i < matchableLabels.length; i++) {
                // we do not check the existence of "next" because we assure it in the loop condition
                Tick tick = majorIter.next();
                tick.label = matchableLabels[i];
            }
        }
    }

    /**
     * This method is useful in a situation, when there are more labels than can
     * be assigned (more preliminarily selected labels than ticks on the axis.
     * In such a situation we have to select the number of labels equal to the
     * number of major ticks. In order to do so, we observe that in the most
     * common case, the labels of axis are aligned in a line (vertical or
     * horizontal) We first try to guess, which direction is in due this time
     * (by measuring the variance of centres of the labels in both directions).
     * Later, we select labels with centres lying as close as possible to the
     * average centre.
     *
     * @param labels the previously assigned labels
     * @param num number of labels that should be elected
     * @return
     */
    private static TickLabel[] selectAppropriateLabels(TickLabel[] labels, int num) {
        double avgx = 0;
        double avgy = 0;

        for (TickLabel l : labels) {
            avgx += l.boundary.getBounds2D().getCenterX();
            avgy += l.boundary.getBounds2D().getCenterY();
        }
        avgx /= labels.length;
        avgy /= labels.length;

        // let's calculate which direction is farther from the center 

        double varx = 0;
        double vary = 0;

        for (TickLabel l : labels) {
            varx = Math.abs(avgx - l.boundary.getBounds2D().getCenterX());
            vary = Math.abs(avgy - l.boundary.getBounds2D().getCenterY());
        } // Both vars are multiplied by the same number ... we can compare them without scaling

        if (varx < vary) {
            Arrays.sort(labels, new XTickLabelsComparator(avgx));
        } else {
            Arrays.sort(labels, new YTickLabelsComparator(avgy));
        }

        return Arrays.copyOfRange(labels, 0, Math.min(num, labels.length));
    }

    /**
     * This method embarks on a remarkable quest of matching ticks of axis
     * candidates with possible labels, which might indicate numerical values
     *
     * @param plot
     */
    private static void matchTicksWithCaptions(SVGPlot plot, CoordinateSystem coord) {
        matchTicksWithCaptionsAxis(plot, coord.axesTicks.getKey());
        matchTicksWithCaptionsAxis(plot, coord.axesTicks.getValue());
    }

    public static class TickLabelsComparator implements Comparator<TickLabel> {

        private final HashMap<TickLabel, PriorityQueue<Tick>> kb;

        /**
         * Creates a comparator for labels based on which one has the closest
         * tick ATTENTION: Only some modifications to kb will be safe if we use
         * this comparator in a PriorityQueue or some structure of a similar
         * type
         *
         * @param o The reference point for comparison
         */
        public TickLabelsComparator(HashMap<TickLabel, PriorityQueue<Tick>> kb) {
            this.kb = kb;

        }

        @Override
        public int compare(TickLabel o1, TickLabel o2) {

            Tick t1 = this.kb.get(o1).peek();
            Tick t2 = this.kb.get(o1).peek();
            if (t1 == null) {
                return -1;
            }
            if (t2 == null) {
                return 1;
            }
            double d1 = ExtLine2D.distanceRectangle(t1.intersection, (Rectangle2D.Double) o1.boundary.getBounds2D());
            double d2 = ExtLine2D.distanceRectangle(t2.intersection, (Rectangle2D.Double) o2.boundary.getBounds2D());

            return d1 == d2 ? 0 : d1 > d2 ? 1 : -1;
        }
    }

    public static class TicksComparator implements Comparator<Tick> {

        private final Rectangle2D boundary;

        /**
         * Creates a comparator for ticks based on the distance to a given
         * rectangle
         *
         * @param o The reference point for comparison
         */
        public TicksComparator(Rectangle2D o) {
            this.boundary = o;
        }

        @Override
        public int compare(Tick o1, Tick o2) {
            double d1 = ExtLine2D.distanceRectangle(o1.intersection, (Rectangle2D.Double) this.boundary);
            double d2 = ExtLine2D.distanceRectangle(o2.intersection, (Rectangle2D.Double) this.boundary);
            return d1 == d2 ? 0 : d1 > d2 ? 1 : -1;
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

        LinkedList<CoordinateSystem> coordParams = new LinkedList<CoordinateSystem>();

        HashSet<ExtLine2D> alreadyConsideredL = new HashSet<ExtLine2D>();
        for (ExtLine2D interval : plot.orthogonalIntervals.keySet()) {
            alreadyConsideredL.add(interval);
            for (ExtLine2D ortInt : plot.orthogonalIntervals.get(interval)) {
                if (!alreadyConsideredL.contains(ortInt)) {
                    // we haven't considered this pair yet
                    CoordinateSystem params = new CoordinateSystem();

                    params.axes = new ImmutablePair<ExtLine2D, ExtLine2D>(interval, ortInt);
                    params.linesOutside = getRatioOfPointsOutside(params.axes, plot.points);
                    // calculations based on the line proportions
                    params.lengthsRatio = getAxisLengthRatio(params.axes);
                    params.axesTicks = retrieveAxisTicks(plot, params.axes);
                    params.origin = params.axes.getKey().getIntersection(params.axes.getValue());
                    if (initialFilter(params)) {
                        coordParams.add(params);
                    }
                }
            }
        }


        /// we have extracted information about all possible candidates, now we need to determine which ones are the real axes ...

        LinkedList<CoordinateSystem> coordinateSystems = CoordinateSystem.filterCandidates(coordParams, plot);
        /// now we need to transform the selected axes into coordinate systems
        //        for (CoordinateSystem params : coordinateSystems) {
        //            result.add(params.toCoordinateSystem());
        //        }

        plot.coordinateSystems = coordinateSystems;

        return coordinateSystems;

    }

    /**
     * An initial heuristic allowing to asses if axis candidate has a chance of
     * describing axis ... very rough
     *
     * @param coordParams
     * @return
     */
    private static boolean initialFilter(CoordinateSystem params) {
        //return true;
        return (params.lengthsRatio > 0.7) && (params.lengthsRatio < 1.3);

    }

    /**
     * Analyse a list of coordinate system candidates and return those which
     * really describe coordinate systems
     *
     * @param coordParams
     * @return
     */
    private static LinkedList<CoordinateSystem> filterCandidates(LinkedList<CoordinateSystem> coordParams, SVGPlot plot) {
        // here we plug a SVM to determine which candidates really describe a coordinates system and which do not
        LinkedList<CoordinateSystem> results = new LinkedList<CoordinateSystem>();

        Object[] array = coordParams.toArray();
        Arrays.sort(array, new Comparator<Object>() {
            @Override
            public int compare(Object os1, Object os2) {
                CoordinateSystem o1 = (CoordinateSystem) os1;
                CoordinateSystem o2 = (CoordinateSystem) os2;
                int s1 = o1.axesTicks.getKey().ticks.size() + o1.axesTicks.getValue().ticks.size();
                int s2 = o2.axesTicks.getKey().ticks.size() + o2.axesTicks.getValue().ticks.size();

                return s2 - s1;
            }
        });

        // let's draw best 10 !

        DebugGraphicalOutput dgo = DebugGraphicalOutput.getInstance();

        for (int i = 0; i < Math.min(500, array.length); i++) {
            CoordinateSystem par = (CoordinateSystem) array[i];
            results.add(par);

            matchTicksWithCaptions(plot, par);
            try {
                DebugGraphicalOutput.dumpCoordinateSystem(plot, par, "/tmp/detected_" + String.valueOf(i) + ".png");
            } catch (IOException ex) {
                Logger.getLogger(CoordinateSystem.class.getName()).log(Level.SEVERE, "Saving a debug output file failed", ex);
            }

            System.out.println("Matched ticks with labels");
        }
        //let's sort by the number of detected ticks
        //throw new UnsupportedOperationException("Not yet implemented");
        List<CoordinateSystem> nonIntersecting = CoordinateSystem.chooseBestAmongIntersecting(plot, results);

        return results;
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
                retrieveAxisTick(a1, plot, origin, a2),
                retrieveAxisTick(a2, plot, origin, a1));
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
        if (!plot.orthogonalIntervals.containsKey(axis)) {
            return intersections;
        }
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

    public static AxisTick retrieveAxisTick(ExtLine2D axis, SVGPlot plot, Point2D.Double origin, ExtLine2D adjAxis) {
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
        double unitLen = adjAxis.len() * eqFraction;
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
        Tick originTick = new Tick(adjAxis, origin, 0.0);
        originTick.isMajor = true;
        res.ticks.put(0.0, originTick);
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

    /**
     * An ugly class... should be replaced with a closure expression (which is
     * currently not available in Java
     */
    private static class XTickLabelsComparator implements Comparator<TickLabel> {

        private final double avgx;

        public XTickLabelsComparator(double avgx) {
            this.avgx = avgx;
        }

        @Override
        public int compare(TickLabel o1, TickLabel o2) {
            double d1 = Math.abs(o1.boundary.getBounds2D().getCenterX() - avgx);
            double d2 = Math.abs(o2.boundary.getBounds2D().getCenterX() - avgx);
            if (d1 == d2) {
                return 0;
            }
            return d1 < d2 ? -1 : 1;
        }
    }

    /**
     * In some cases, there are more coordinate system candidates defining
     * intersecting areas. (By the defined area we understand the rectangle
     * spanned by both axis).
     *
     * This method detects all classes of coordinate system candidates which
     * intersect each other and elects the best candidate, which is further
     * considered.
     *
     * Sometimes a plot deliberately contains several redundant axis, but in
     * such a case, we can without loosing generality, consider only one pair.
     *
     * The criteria used to select the best candidate include: - The size of the
     * defined area - The number of detected ticks on both axis - The number of
     * ticks matched with labels - The alignment of labels (they should lie in a
     * row) - The distance of axis label from the end of the axis
     *
     * @param plot The plot from which we extract the axis
     * @param candidates previously detected coordinate system candidates
     * @return a non-intersecting list of candidates
     */
    private static List<CoordinateSystem> chooseBestAmongIntersecting(SVGPlot plot, List<CoordinateSystem> candidates) {
        List<CoordinateSystem> result = new LinkedList<CoordinateSystem>();
        // 1) we need to scale our coordinate systems so taht after roundign to integer, no frangments collapse
        DoubleTreeMap<Boolean> xs = new DoubleTreeMap<Boolean>(0.001);
        DoubleTreeMap<Boolean> ys = new DoubleTreeMap<Boolean>(0.001);

        for (CoordinateSystem candidate : candidates) {
            xs.put(candidate.axes.getKey().x1, true);
            xs.put(candidate.axes.getKey().x2, true);
            ys.put(candidate.axes.getKey().y2, true);
            ys.put(candidate.axes.getKey().y1, true);
        }

        Double prevX = null;
        double minDifX = Double.MAX_VALUE;

        for (Double x : xs.keySet()) {
            if (prevX != null) {
                if (x - prevX < minDifX) {
                    minDifX = x - prevX;
                }
            }
            prevX = x;
        }

        Double prevY = null;
        double minDifY = Double.MAX_VALUE;

        for (Double y : ys.keySet()) {
            if (prevY != null) {
                if (y - prevY < minDifY) {
                    minDifY = y - prevY;
                }
            }
            prevY = y;
        }

        double scaleCoeff = 1 / Math.min(minDifX, minDifY); // coeffs cannot be 0 because we assured that we have only one copy of each point !

        // 2) We have to detect intersecting clusters .. we use the same intervals tree technique as in the plots exractor

        IntervalTree<CoordinateSystem> intX = new IntervalTree<CoordinateSystem>((int) Math.round(plot.boundary.getMinX() * scaleCoeff), (int) Math.round(plot.boundary.getMaxX() * scaleCoeff));
        IntervalTree<CoordinateSystem> intY = new IntervalTree<CoordinateSystem>((int) Math.round(plot.boundary.getMinY() * scaleCoeff), (int) Math.round(plot.boundary.getMaxY() * scaleCoeff));

        for (CoordinateSystem candidate : candidates) {
            Rectangle2D.Double boundary = candidate.getBoundary();
            intX.addInterval((int) Math.round(boundary.getMinX() * scaleCoeff), (int) Math.round((boundary.getMaxX()) * scaleCoeff), candidate);
            intY.addInterval((int) Math.round(boundary.getMinY() * scaleCoeff), (int) Math.round((boundary.getMaxY()) * scaleCoeff), candidate);
        }

        SpatialClusterManager<CoordinateSystem> scm = new SpatialClusterManager<CoordinateSystem>(ExtractorGeometryTools.extendRectangle(ExtractorGeometryTools.roundScaleRectangle(plot.boundary, scaleCoeff), 10, 10), 1, 1);
        try {
            int stepNum = 0;
            for (CoordinateSystem candidate : candidates) {
                Rectangle roundScaleRectangle = ExtractorGeometryTools.roundScaleRectangle(candidate.getBoundary(), scaleCoeff);
                scm.addRectangle(ExtractorGeometryTools.roundScaleRectangle(candidate.getBoundary(), scaleCoeff), candidate);
                Map<Rectangle, List<CoordinateSystem>> finalBoundaries = scm.getFinalBoundaries();
                // DEBUG: Let's wirte information about all the clsters now ! (images by cluster)
                int clusterNum = 0;
                String stepDirName = "/tmp/clusteringstep_" + stepNum;
                File stepDir = new File(stepDirName);
                stepDir.mkdir();

                for (Rectangle clusterBoundary : finalBoundaries.keySet()) {
                    String clusterDirName = stepDirName + "/cluster_" + clusterNum;
                    File clusterDir = new File(clusterDirName);
                    clusterDir.mkdir();

                    DebugGraphicalOutput.drawFileWithRectangle(plot, ExtractorGeometryTools.roundScaleRectangle(clusterBoundary, 1/scaleCoeff), clusterDirName + "/cluster_overview.png");
                    int csNum = 0;
                    for (CoordinateSystem cs : finalBoundaries.get(clusterBoundary)) {
                        DebugGraphicalOutput.dumpCoordinateSystem(plot, cs, clusterDirName + "/cs_" + csNum + ".png");
                        csNum++;
                    }
                    clusterNum++;
                }
                stepNum++;
            }

        } catch (Exception ex) {
            Logger.getLogger(CoordinateSystem.class.getName()).log(Level.SEVERE, "Something got wrong when clustering the objects", ex);
        }

        Collection<List<CoordinateSystem>> values = scm.getFinalBoundaries().values();

        // 3) now for every cluster, we have to determine, which candidate is the best

        return result;
    }

    /**
     * A comparator of two coordinate systems. The comparison is performed on a
     * basis of criteria maximising the chance of a coordinate system (fully
     * extracted) to truly be a coordinate system of the original plot. We take
     * into account the following measures: - the size of the covered area. -
     * the number of extracted ticks (on both axis) - the number of ticks
     * matched with labels - the colinearity of labels - graphical primitives
     * located outside of the boundary but close to it
     *
     */
    private static class CoordinateSystemComparator implements Comparator<CoordinateSystem> {

        @Override
        public int compare(CoordinateSystem o1, CoordinateSystem o2) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    /**
     * An ugly class... should be replaced with a closure expression (which is
     * currently not available in Java
     */
    private static class YTickLabelsComparator implements Comparator<TickLabel> {

        private final double avgy;

        public YTickLabelsComparator(double avgy) {
            this.avgy = avgy;
        }

        @Override
        public int compare(TickLabel o1, TickLabel o2) {
            double d1 = Math.abs(o1.boundary.getBounds2D().getCenterY() - avgy);
            double d2 = Math.abs(o2.boundary.getBounds2D().getCenterY() - avgy);
            if (d1 == d2) {
                return 0;
            }
            return d1 < d2 ? -1 : 1;
        }
    }

    private static class TickLabelsDistanceFromFirstTickComparator implements Comparator<TickLabel> {

        private final Point2D.Double referencePoint;

        public TickLabelsDistanceFromFirstTickComparator(Point2D.Double referencePoint) {
            this.referencePoint = referencePoint;
        }

        @Override
        public int compare(TickLabel o1, TickLabel o2) {
            double d1 = ExtLine2D.distanceRectangle(referencePoint, (Rectangle2D.Double) o1.boundary.getBounds2D());
            double d2 = ExtLine2D.distanceRectangle(referencePoint, (Rectangle2D.Double) o2.boundary.getBounds2D());
            if (d1 == d2) {
                return 0;
            }

            return d1 > d2 ? 1 : -1;
        }
    }
}
