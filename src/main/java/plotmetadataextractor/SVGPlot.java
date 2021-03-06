/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.gvt.CompositeGraphicsNode;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.ShapeNode;
import org.apache.batik.gvt.TextNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 *
 * @author piotr
 */
public class SVGPlot {

    public Rectangle2D.Double boundary;
    public Rectangle2D declaredBoundary; // the boundary declared in the source of the SVG file
    public List<Point2D> points;
    public List<ExtLine2D> lineSegments;
    public HashMap<ExtLine2D, List<ExtLine2D>> orthogonalIntervals;
    public HashMap<Shape, String> textElements;
    public HashMap<Shape, String> splitTextElements; /// text elements consisting of separate words
    public SpatialArray<String> splitTextIndex; /// index used to search for text elements
    public static long searchDivision = 100; // divide into 10000 tiles 
    public File sourceFile;
    // The members describing the extracted data
    public List<CoordinateSystem> coordinateSystems;

    public SVGPlot() {
        this.lineSegments = new LinkedList<ExtLine2D>();
        this.points = new LinkedList<Point2D>();
        this.textElements = new HashMap<Shape, String>();
        this.splitTextElements = new HashMap<Shape, String>();
        this.boundary = new Rectangle2D.Double(0, 0, 0, 0);

        this.coordinateSystems = new LinkedList<CoordinateSystem>();
    }

    /**
     * Calculates the spatial index of text elements, which we use to match axis
     * ticks with text fragments
     */
    public final void calculateTextIndex() {
        this.splitTextIndex = new SpatialArray<String>(this.boundary, SVGPlot.searchDivision);
        for (Shape s : this.splitTextElements.keySet()) {
            this.splitTextIndex.put(s, this.splitTextElements.get(s));
        }
    }

    public SVGPlot(File plotFile) {
        this();
        try {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            String uri = "file://" + plotFile.getAbsolutePath();
            InputStream is = new FileInputStream(plotFile);
            Document doc = f.createDocument(uri, is);

            NamedNodeMap attributes = doc.getAttributes();


            NodeList e1 = doc.getElementsByTagName("path");
            NodeList e2 = doc.getElementsByTagName("line");

            GVTBuilder b = new GVTBuilder();
            UserAgentAdapter ua = new UserAgentAdapter();
            BridgeContext cx = new BridgeContext(ua);
            GraphicsNode i = b.build(cx, doc);

            this.declaredBoundary = i.getBounds();
            addGraphicsNode(i);
            this.sourceFile = plotFile;
            this.removeDuplicateLines(0.01);
            this.calculateOrthogonalIntervals();

            this.calculateTextIndex();

        } catch (IOException ex) {
            System.out.println("failed : exception " + ex.toString());
            // ...
        }
    }

    public SVGPlot(String fName) {
        this(new File(fName));
    }

    public final void calculateOrthogonalIntervals() {
        //System.out.println("Processing line intervals");
        HashMap<Integer, List<ExtLine2D>> linesByAngle = new HashMap<Integer, List<ExtLine2D>>();
        HashMap<ExtLine2D, List<ExtLine2D>> intersecting = new HashMap<ExtLine2D, List<ExtLine2D>>();

        // first divide lines into buckets by angle
        for (ExtLine2D interval : this.lineSegments) {
            int alpha = Math.round(interval.getAngle());
            if (!linesByAngle.containsKey(alpha)) {
                linesByAngle.put(alpha, new LinkedList<ExtLine2D>());
            }
            linesByAngle.get(alpha).add(interval);
            //System.out.println("Detected that line segment " + ExtractorGeometryTools.lineToString(interval) + " is inclinde by the angle " + alpha);
        }

        // now we consider every line and search for lines potentially being orthogonal to it

        for (ExtLine2D interval : this.lineSegments) {
            int searchRadius = 3; // we search 3 angles around the exact orthogonality
            int alpha = Math.round(interval.getAngle());
            LinkedList<ExtLine2D> curIntersecting = new LinkedList<ExtLine2D>();

            // using our representation, there is only one possible orthogonal direction !
            int testAngle = alpha + 90 - searchRadius;
            if (testAngle > 90) {
                testAngle = alpha - 90 - searchRadius;
            }

            for (int i = 0; i < 2 * searchRadius; i++) {
                List<ExtLine2D> ortLines = linesByAngle.get(testAngle);
                if (ortLines != null) {
                    for (ExtLine2D line : ortLines) {
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

        } catch (IOException ex) {
            Logger.getLogger(PlotMetadataExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.orthogonalIntervals = intersecting;
    }

    /**
     * Retrieves all line segments described by a given tree. All segments are
     * described in the same coordinates system.
     *
     * @param node
     * @return
     */
    private void addGraphicsNode(GraphicsNode node) {

        LinkedList<Pair<GraphicsNode, AffineTransform>> searchStack = new LinkedList<Pair<GraphicsNode, AffineTransform>>();
        searchStack.push(new ImmutablePair<GraphicsNode, AffineTransform>(node, AffineTransform.getRotateInstance(0)));

        while (!searchStack.isEmpty()) {
            Pair<GraphicsNode, AffineTransform> currentEl = searchStack.pop();
            AffineTransform curTransform = new AffineTransform(currentEl.getRight());
            if (currentEl.getLeft().getTransform() != null) {
                curTransform.concatenate(currentEl.getLeft().getTransform());
            }
            GraphicsNode curNode = currentEl.getLeft();

            if (curNode instanceof CompositeGraphicsNode) {
                CompositeGraphicsNode cNode = (CompositeGraphicsNode) curNode;
                for (Object chNode : cNode.getChildren()) {
                    if (chNode instanceof GraphicsNode) {
                        searchStack.push(new ImmutablePair<GraphicsNode, AffineTransform>((GraphicsNode) chNode, curTransform));
                    } else {
                        System.out.println("Incorrect tree at the input");
                    }
                }
            } else {
                if (curNode instanceof ShapeNode) {

                    this.includeShape(((ShapeNode) curNode).getShape(), curTransform);
                    // we are in a specialised node - we have to directly extract the 
                } else {
                    if (curNode instanceof TextNode) {
                        this.addTextNode((TextNode) curNode, curTransform);
                    } else {
                        System.err.println("Encountered Unknown type of a node !!");

                    }
                }
            }
        }
    }

    /**
     * Includes a path in the plot
     */
    public void includeShape(Shape shape, AffineTransform currentTransform) {

        // Description of the path construction can be found at http://docs.oracle.com/javase/1.5.0/docs/api/java/awt/geom/PathIterator.html#currentSegment(double[])

        PathIterator shapeIt = shape.getPathIterator(currentTransform);
        DebugGraphicalOutput debug = DebugGraphicalOutput.getInstance();

        Point2D firstPoint = null;
        Point2D prevPoint = null;
        Point2D curPoint;

        HashMap<Integer, String> types = new HashMap<Integer, String>();
        types.put(PathIterator.SEG_CLOSE, "SEG_CLOSE");
        types.put(PathIterator.SEG_CUBICTO, "SEG_CUBICTO");
        types.put(PathIterator.SEG_LINETO, "SEG_LINETO");
        types.put(PathIterator.SEG_MOVETO, "SEG_MOVETO");
        types.put(PathIterator.SEG_QUADTO, "SEG_QUADTO");

        double coords[] = new double[6];

        while (!shapeIt.isDone()) {
            int segType = shapeIt.currentSegment(coords);

            curPoint = new Point2D.Double(coords[0], coords[1]);

            if (prevPoint == null) {
                firstPoint = curPoint;
            }
            if (segType == PathIterator.SEG_CLOSE) {
                curPoint = firstPoint;
            }
            if (segType == PathIterator.SEG_QUADTO) {
                curPoint = new Point2D.Double(coords[2], coords[3]);
            }
            if (segType == PathIterator.SEG_CUBICTO) {
                curPoint = new Point2D.Double(coords[4], coords[5]);
                //TODO: Now we should fetect the situation when a CUBIC is really a line 
            }
            if (segType != PathIterator.SEG_MOVETO) {
                debug.graphics.drawLine((int) Math.round(prevPoint.getX()), (int) Math.round(prevPoint.getY()), (int) Math.round(curPoint.getX()), (int) Math.round(curPoint.getY()));
            }

            if (segType == PathIterator.SEG_CLOSE || segType == PathIterator.SEG_LINETO) {
                ExtLine2D segment = buildLineFromPoints(prevPoint, curPoint);
                if (segment.intersects(this.declaredBoundary)) {
                    this.lineSegments.add(segment);
                }
            }

            shapeIt.next();

            this.points.add(curPoint);
            Rectangle2D.Double.union(new Rectangle2D.Double(curPoint.getX(), curPoint.getY(), 0, 0), this.boundary, this.boundary);
            prevPoint = curPoint;
        }
    }

    /**
     * Builds a line interval with ends in two given points and a correct
     * orientation
     *
     * @param p1
     * @param p2
     * @return
     */
    public static ExtLine2D buildLineFromPoints(Point2D p1, Point2D p2) {
        // we have a linear segment created... now we only have to put order on the 
        // points - we want to have the point starting from the left-most point ... if equal,
        // at the bottom
        if (p2.getX() < p1.getX()) {
            Point2D tmp = p1;
            p1 = p2;
            p2 = tmp;
        }

        if (p1.getX() == p2.getX() && p1.getY() > p2.getY()) {
            Point2D tmp = p1;
            p1 = p2;
            p2 = tmp;
        }

        return new ExtLine2D(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Include a text portion with its geometrical boundary
     */
    public final void includeTextBlock(Shape bounds, String text) {
        this.textElements.put(bounds, text);
        // we also include split elements
        List<Pair<Rectangle2D.Double, String>> split = SVGPlot.splitText(bounds, text);
        for (Pair<Rectangle2D.Double, String> p : split) {
            this.splitTextElements.put(p.getKey(), p.getValue());
        }
    }

    public final void addTextNode(TextNode tn, AffineTransform curTransform) {
        Rectangle2D bounds = tn.getBounds();
        if (bounds == null) {
            return;
        }
        String text = tn.getText();
        Shape effectiveBoundary = curTransform.createTransformedShape(bounds);
        this.includeTextBlock(effectiveBoundary, text);
    }

    /**
     * This method splits all strings consisting of more than one word into
     * parts. It assumes that the font inside of a box has fixed character width
     * ... and splits it proportionally to the substring length
     *
     * @return
     */
    public Map<Rectangle2D.Double, String> getSplitText() {
        // we assume rectangles and horizontal layout of the text ... a future improvement could include extension to arbitrary text layouts
        HashMap<Rectangle2D.Double, String> results = new HashMap<Rectangle2D.Double, String>();

        for (Shape shape : this.textElements.keySet()) {
            String text = this.textElements.get(shape);
            List<Pair<Rectangle2D.Double, String>> split = SVGPlot.splitText(shape, text);
            for (Pair<Rectangle2D.Double, String> r : split) {
                results.put(r.getKey(), r.getValue());
            }
        }
        return results;
    }

    public static List<Pair<Rectangle2D.Double, String>> splitText(Shape shape, String text) {
        List<Pair<Rectangle2D.Double, String>> res = new LinkedList<Pair<Rectangle2D.Double, String>>();
        Rectangle2D bounds = shape.getBounds2D();

        double charWidth = bounds.getWidth() / text.length();

        int wordBeginning = 0;
        for (int ind = 0; ind <= text.length(); ind++) {
            if (ind == text.length() || Character.isSpaceChar(text.charAt(ind))) {
                if (wordBeginning != ind) {
                    // there really was a word
                    String fragment = text.substring(wordBeginning, ind);
                    res.add(
                            new ImmutablePair<Rectangle2D.Double, String>(
                            new Rectangle2D.Double(
                            bounds.getMinX() + (wordBeginning * charWidth),
                            bounds.getMinY(),
                            (ind - wordBeginning) * charWidth,
                            bounds.getHeight()),
                            fragment));

                }
                wordBeginning = ind + 1;
            }
        }
        return res;
    }

    public static class ApproximateLinesContainer {

        DoubleTreeMap<DoubleTreeMap<DoubleTreeMap<DoubleTreeMap<ExtLine2D>>>> lines;
        private double precission;

        public ApproximateLinesContainer(double precission) {
            this.lines = new DoubleTreeMap<DoubleTreeMap<DoubleTreeMap<DoubleTreeMap<ExtLine2D>>>>(precission);
            this.precission = precission;
        }

        public void addLine(ExtLine2D line) {

            if (!this.lines.containsKey(line.getX1())) {
                this.lines.put(line.getX1(), new DoubleTreeMap<DoubleTreeMap<DoubleTreeMap<ExtLine2D>>>(this.precission));

            }
            DoubleTreeMap<DoubleTreeMap<DoubleTreeMap<ExtLine2D>>> m1 = this.lines.get(line.getX1());
            if (!m1.containsKey(line.getY1())) {
                m1.put(line.getY1(), new DoubleTreeMap<DoubleTreeMap<ExtLine2D>>(this.precission));
            }
            DoubleTreeMap<DoubleTreeMap<ExtLine2D>> m2 = m1.get(line.getY1());
            if (!m2.containsKey(line.getX2())) {
                m2.put(line.getX2(), new DoubleTreeMap<ExtLine2D>(this.precission));
            }
            DoubleTreeMap<ExtLine2D> m3 = m2.get(line.getX2());

            m3.put(line.getY2(), line);
        }

        public boolean hasLine(ExtLine2D line) {
            DoubleTreeMap<DoubleTreeMap<DoubleTreeMap<ExtLine2D>>> m1 = this.lines.get(line.getX1());
            if (m1 == null) {
                return false;
            }
            DoubleTreeMap<DoubleTreeMap<ExtLine2D>> m2 = m1.get(line.getY1());
            if (m2 == null) {
                return false;
            }
            DoubleTreeMap<ExtLine2D> m3 = m2.get(line.getX2());
            if (m3 == null) {
                return false;
            }
            ExtLine2D m4 = m3.get(line.getY2());
            if (m4 == null) {
                return false;
            }
            return true;
        }
    }

    /**
     * Removes all lines which can be considered redundant (using the precision
     * argument)
     *
     * @param precission
     */
    public final void removeDuplicateLines(double precission) {
        List<ExtLine2D> result = new LinkedList<ExtLine2D>();
        int numRemoved = 0;
        // we will be indexing with a given precission using coordinates of the beginning and the end
        ApproximateLinesContainer alc = new ApproximateLinesContainer(precission);
        for (ExtLine2D line : this.lineSegments) {
            if (!alc.hasLine(line)) {
                alc.addLine(line);
                result.add(line);
            } else {
                numRemoved++;
            }
        }
        this.lineSegments = result;
        //System.out.println("Removed " + String.valueOf(numRemoved) + " line segments which have been detected as redundant");
    }
}
