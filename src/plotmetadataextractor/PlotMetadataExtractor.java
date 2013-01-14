/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import invenio.common.ExtractorGeometryTools;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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
import org.apache.batik.ext.awt.geom.ExtendedGeneralPath;
import org.apache.batik.gvt.CompositeGraphicsNode;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.ShapeNode;
import org.apache.batik.parser.DefaultPathHandler;
import org.apache.batik.parser.DefaultPointsHandler;
import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PointsHandler;
import org.apache.batik.parser.PointsParser;

import org.apache.batik.parser.PathParser;
import org.apache.batik.parser.PathHandler;

import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 *
 * @author piotr
 */
public class PlotMetadataExtractor {
    /// The debugging code ! (drawing segments of the svg)

    public static List extractPoints(Reader r) throws ParseException {
        final LinkedList points = new LinkedList();
        PointsParser pp = new PointsParser();
        PointsHandler ph = new DefaultPointsHandler() {

            @Override
            public void point(float x, float y) throws ParseException {
                Point2D p = new Point2D.Float(x, y);
                points.add(p);
            }
        };
        pp.setPointsHandler(ph);
        pp.parse(r);

        PathParser pathp = new PathParser();
        PathHandler pathh = new DefaultPathHandler() {
        };


        return points;
    }

    /**
     * Retrieves all line segments described by a given tree.
     * All segments are described in the same coordinates system.
     * @param node
     * @return 
     */
    public static List<Line2D> retrieveLineSegments(GraphicsNode node) {
        LinkedList<Line2D> segments = new LinkedList<Line2D>();
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
                    extractSegmentsFromSimpleNode((ShapeNode) curNode, segments, curTransform);
                    // we are in a specialised node - we have to directly extract the 
                } else {
                    System.out.println("Encountered Unknown type of a node !!");
                }
            }
        }
        DebugGraphicalOutput debug = DebugGraphicalOutput.getInstance();
        try {
            debug.flush(new File("/tmp/out.png"));
        } catch (IOException ex) {
            Logger.getLogger(PlotMetadataExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return segments;
    }

    /**
     * 
     * @param pathIter
     * @param segments
     * @param currentTransform 
     */
    public static void extractSegmentsFromPath(PathIterator shapeIt, LinkedList<Line2D> segments) {

        // Description of the path construction can be found at http://docs.oracle.com/javase/1.5.0/docs/api/java/awt/geom/PathIterator.html#currentSegment(double[])

        DebugGraphicalOutput debug = DebugGraphicalOutput.getInstance();

        Point2D firstPoint = null;
        Point2D prevPoint = null;
        Point2D curPoint = null;

        HashMap<Integer, String> types = new HashMap<Integer, String>();
        types.put(PathIterator.SEG_CLOSE, "SEG_CLOSE");
        types.put(PathIterator.SEG_CUBICTO, "SEG_CUBICTO");
        types.put(PathIterator.SEG_LINETO, "SEG_LINETO");
        types.put(PathIterator.SEG_MOVETO, "SEG_MOVETO");
        types.put(PathIterator.SEG_QUADTO, "SEG_QUADTO");
        double coords[] = new double[6];

        while (!shapeIt.isDone()) {
            System.out.println();
            int segType = shapeIt.currentSegment(coords);

            System.out.println("Segment type: " + types.get(segType) + "  points: ");
            // printing the points:
            for (int i = 0; i < 3; ++i) {
                System.out.println(" (" + coords[2 * i] + ", " + coords[2 * i + 1] + ")");
            }

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
                segments.add(buildLineFromPoints(prevPoint, curPoint));
            }

            shapeIt.next();
            prevPoint = curPoint;
        }
    }

    /**
     * Builds a line interval with ends in two given points and a correct orientation
     * @param p1
     * @param p2
     * @return 
     */
    public static Line2D buildLineFromPoints(Point2D p1, Point2D p2) {
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

        return new Line2D.Double(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public static void extractSegmentsFromSimpleNode(ShapeNode node, LinkedList<Line2D> segments, AffineTransform currentTransform) {

        extractSegmentsFromPath(node.getShape().getPathIterator(currentTransform), segments);
        if (node.getShape() instanceof Rectangle2D) {
            Rectangle2D rectangle2D = (Rectangle2D) node.getShape();
            System.out.println("Encountered the rectangle");

            return;
        }

        if (node.getShape() instanceof ExtendedGeneralPath) {
            ExtendedGeneralPath path = (ExtendedGeneralPath) node.getShape();
            System.out.println("Encountered a general path");
            return;
        }
        if (node.getShape() instanceof Ellipse2D) {
            Ellipse2D ellipse = (Ellipse2D) node.getShape();
            return;
        }
        System.out.println("Encountered a different type of node");
    }

    /**
     * We can try to process the document using the SVG DOM
     * @param fname 
     */
    public static void trySVGDOMProcessing(String fname) {
        try {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            String uri = "file://" + fname; //"http://www.example.org/diagram.svg";
            Document doc = f.createDocument(uri);
            System.out.println("Finished building the DOM structure");
            NodeList e1 = doc.getElementsByTagName("path");
            NodeList e2 = doc.getElementsByTagName("line");

            GVTBuilder b = new GVTBuilder();
            UserAgentAdapter ua = new UserAgentAdapter();
            BridgeContext cx = new BridgeContext(ua);
            GraphicsNode i = b.build(cx, doc);
            CompositeGraphicsNode n;
            AffineTransform t;
            Point.Float p = new Point.Float();
            List<Line2D> lineSegments = retrieveLineSegments(i);
            getOrthogonalIntervals(lineSegments);



            System.out.println("Finished building the DOM structure");

        } catch (IOException ex) {
            System.out.println("failed : exception " + ex.toString());
            // ...
        }
    }

    public static double getIntervalAngle(Line2D interval) {
        double xLen = Math.abs(interval.getX2() - interval.getX1());
        double alpha; // we use grades for debugging purposes ... detection of PI/2 etc.. is difficult when seeing a number

        if (xLen == 0) {
            alpha = 90;
        } else {
            double sinAlpha = xLen / interval.getP1().distance(interval.getP2());
            alpha = Math.asin(sinAlpha) * 360 / (2 * Math.PI);
        }
        if (interval.getY2() < interval.getY1()) {
            alpha = -alpha;
        }
        return alpha;

    }

    public static Map<Line2D, List<Line2D>> getOrthogonalIntervals(List<Line2D> intervals) {
        System.out.println("Processing line intervals");
        for (Line2D interval : intervals) {
            double alpha = getIntervalAngle(interval);

            System.out.println("Detected that line segment " + ExtractorGeometryTools.lineToString(interval) + " is inclinde by the angle " + alpha);
        }
        return null;
    }

    /** Collect all lines being children of the given node */
    public static LinkedList<String> getLines(GraphicsNode root) {
        LinkedList<String> result = new LinkedList<String>();
        return result;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("Executing with parameters: ");
        for (String s : args) {
            System.out.print(s + " ");
        }
        System.out.println("Starting the processing using SVG DOM");
        trySVGDOMProcessing(args[0]);
        System.out.println("Starting the processing by the Batik parser");
        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        //    extractPoints(reader);
    }
}
