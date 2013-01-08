/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

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
                if (curNode instanceof ShapeNode){
                    extractSegmentsFromSimpleNode((ShapeNode) curNode, segments, curTransform);
                    // we are in a specialised node - we have to directly extract the 
                } else {
                    System.out.println("Encountered Unknown type of a node !!");
                }
            }
        }
        return segments;
    }

    public static void extractSegmentsFromSimpleNode(ShapeNode node, LinkedList<Line2D> segments, AffineTransform currentTransform) {
        if (node.getShape() instanceof Rectangle2D) {
            Rectangle2D rectangle2D = (Rectangle2D) node.getShape();
            System.out.println("Encountered the rectangle");
            return;
        }
        if (node.getShape() instanceof ExtendedGeneralPath){
            ExtendedGeneralPath path = (ExtendedGeneralPath) node.getShape();
            System.out.println("Encountered a general path");
            return;
        }
        if (node.getShape() instanceof Ellipse2D){
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
            retrieveLineSegments(i);
            System.out.println("Finished building the DOM structure");

        } catch (IOException ex) {
            System.out.println("failed : exception " + ex.toString());
            // ...
        }
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
