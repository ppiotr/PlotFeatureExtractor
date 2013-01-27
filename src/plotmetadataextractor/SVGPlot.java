/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.gvt.CompositeGraphicsNode;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.ShapeNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 *
 * @author piotr
 */
public class SVGPlot {

    public Rectangle boundary;
    public List<Point2D> points;
    public List<ExtLine2D> lineSegments;
    public HashMap<ExtLine2D, List<ExtLine2D>> orthogonalIntervals;

    public SVGPlot() {
        this.lineSegments = new LinkedList<ExtLine2D>();
        this.points = new LinkedList<Point2D>();
    }

    public SVGPlot(String fName) {
        this();
        try {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            String uri = "file://" + fName;
            Document doc = f.createDocument(uri);

            NodeList e1 = doc.getElementsByTagName("path");
            NodeList e2 = doc.getElementsByTagName("line");

            GVTBuilder b = new GVTBuilder();
            UserAgentAdapter ua = new UserAgentAdapter();
            BridgeContext cx = new BridgeContext(ua);
            GraphicsNode i = b.build(cx, doc);
            CompositeGraphicsNode n;
            AffineTransform t;
            Point.Float p = new Point.Float();

            addGraphicsNode(i);
            this.calculateOrthogonalIntervals();
        } catch (IOException ex) {
            System.out.println("failed : exception " + ex.toString());
            // ...
        }
    }


    public void calculateOrthogonalIntervals() {
        System.out.println("Processing line intervals");
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


            Random r = new Random();
            for (ExtLine2D line : intersecting.keySet()) {
                if (r.nextInt(300) == 0) {
                    //we search for the longest intersecting and draw intersecting with this one

                    ExtLine2D winner = line;

                    for (ExtLine2D inter : intersecting.get(line)) {
                        if (inter.len() > winner.len()) {
                            winner = inter;
                        }
                    }

                    // now drawing the winner
                    dout.graphics.setColor(new Color(r.nextInt(256), r.nextInt(256), r.nextInt(256)));
                    Rectangle bounds = winner.getBounds();
                    dout.graphics.draw(bounds);

                    for (ExtLine2D inter : intersecting.get(winner)) {
                        dout.graphics.draw(inter.getBounds());
                    }

                }
            }
            dout.flush(new File("/tmp/out3.png"));
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
                    System.out.println("Encountered Unknown type of a node !!");
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
                this.lineSegments.add(buildLineFromPoints(prevPoint, curPoint));
            }

            shapeIt.next();

            this.points.add(curPoint);
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
}
