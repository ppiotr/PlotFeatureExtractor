/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import com.hp.hpl.jena.mem.BunchMap;
import invenio.common.Images;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

/**
 *
 * @author piotr
 */
public class DebugGraphicalOutput {

    private static DebugGraphicalOutput instance = null;
    public BufferedImage image;
    public Graphics2D graphics;
    public int width;
    public int height;
    private HashMap<String, BufferedImage> plotImages;

    private DebugGraphicalOutput() {
        this.width = 1000;
        this.height = 1000;

        this.plotImages = new HashMap<String, BufferedImage>();
        //this.image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
        //this.graphics = (Graphics2D) this.image.getGraphics();
        this.reset();
    }

    public static DebugGraphicalOutput getInstance() {
        if (instance == null) {
            instance = new DebugGraphicalOutput();
        }
        return instance;
    }

    /**
     * Flushes the content of the graphical debug message into a file
     *
     * @param outputFile
     */
    public void flush(File outputFile) throws IOException {
        Images.writeImageToFile(this.image, outputFile);
    }

    /**
     * Resets the content of the image
     */
    public final void reset() {
        this.image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
        this.graphics = (Graphics2D) this.image.getGraphics();
        this.graphics.fillRect(0, 0, this.width, this.height);
        this.graphics.setBackground(Color.WHITE);
        this.graphics.setColor(Color.BLACK);
    }

    public final void reset(int width, int height) {
        this.width = width;
        this.height = height;
        this.reset();
    }

    /**
     * Dumps the coordinates system into a file. Draws all the lines from the
     * plot as less visible than lines of the coordinate system
     *
     * @param plot
     * @param cs
     * @param fname
     */
    public static void dumpCoordinateSystem(SVGPlot plot, CoordinateSystem cs, String fname) throws IOException {
        DebugGraphicalOutput dgo = DebugGraphicalOutput.getInstance();
        dgo.reset((int) Math.round(plot.declaredBoundary.getWidth()) + 10, (int) Math.round(plot.declaredBoundary.getHeight()) + 10);

        dgo.graphics.setTransform(AffineTransform.getTranslateInstance(-plot.declaredBoundary.getMinX() + 5, -plot.declaredBoundary.getMinY() + 5));
        dgo.graphics.drawImage(dgo.getPlotImage(plot), AffineTransform.getRotateInstance(0), null);

//        dgo.graphics.setColor(Color.PINK);
//        for (Shape sh : plot.splitTextElements.keySet()) {
//            dgo.graphics.draw(sh);
//        }
//
//        dgo.graphics.setColor(new Color(127, 127, 127, 80));
//        for (ExtLine2D line : plot.lineSegments) {
//            dgo.graphics.draw(line);
//        }

        dgo.graphics.setColor(new Color(255, 255, 255, 190));
        dgo.graphics.fill(new Rectangle(0, 0, (int) plot.declaredBoundary.getWidth(), (int) plot.declaredBoundary.getHeight()));
        dgo.graphics.setColor(new Color(0, 0, 0));
        dgo.graphics.setStroke(new BasicStroke(4));

        dgo.graphics.setColor(Color.red);
        // drawing the first axis

        dgo.graphics.draw(cs.axes.getKey());

        dgo.graphics.setStroke(new BasicStroke(2));

        for (CoordinateSystem.Tick tick : cs.axesTicks.getKey().ticks.values()) {
            if (tick.line != cs.axes.getValue()) {
                dgo.graphics.setColor(Color.red);
                dgo.graphics.draw(tick.line);
            }
            if (tick.label != null) {
                dgo.graphics.setColor(Color.BLACK);
                dgo.graphics.draw(tick.label.boundary);
                Rectangle2D bounds = tick.label.boundary.getBounds2D();
                int mx = (int) Math.round(bounds.getMinX() + (bounds.getWidth() / 2));
                int my = (int) Math.round(bounds.getMinY() + (bounds.getHeight() / 2));
                dgo.graphics.drawLine((int) Math.round(tick.intersection.getX()), (int) Math.round(tick.intersection.getY()), mx, my);
            }
        }
        dgo.graphics.setStroke(new BasicStroke(4));

        dgo.graphics.setColor(Color.blue);
        dgo.graphics.draw(cs.axes.getValue());
        dgo.graphics.setStroke(new BasicStroke(2));

        for (CoordinateSystem.Tick tick : cs.axesTicks.getValue().ticks.values()) {
            if (tick.line != cs.axes.getKey()) {
                dgo.graphics.setColor(Color.blue);
                dgo.graphics.draw(tick.line);
            }
            if (tick.label != null) {
                dgo.graphics.setColor(Color.BLACK);
                dgo.graphics.draw(tick.label.boundary);
                Rectangle2D bounds = tick.label.boundary.getBounds2D();
                int mx = (int) Math.round(bounds.getCenterX());
                int my = (int) Math.round(bounds.getCenterY());
                dgo.graphics.drawLine((int) Math.round(tick.intersection.getX()), (int) Math.round(tick.intersection.getY()), mx, my);
            }
        }

        dgo.graphics.setColor(new Color(0, 255, 0, 40));
        dgo.graphics.draw(cs.getBoundary());
        dgo.graphics.fill(cs.getBoundary());

        dgo.flush(new File(fname));
    }

    private BufferedImage getPlotImage(SVGPlot plot) {
        if (this.plotImages.containsKey(plot.sourceFile.getAbsolutePath())) {
            return this.plotImages.get(plot.sourceFile.getAbsolutePath());
        }
        BufferedImage result;

        PNGTranscoder t = new PNGTranscoder();
        String svgURI = plot.sourceFile.toURI().toString();
        TranscoderInput input = new TranscoderInput(svgURI);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TranscoderOutput output = new TranscoderOutput(bos);
        try {
            t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) plot.declaredBoundary.getWidth());
            t.transcode(input, output);
            bos.flush();
            bos.close();
        } catch (IOException ex) {
            Logger.getLogger(DebugGraphicalOutput.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (TranscoderException ex) {
            Logger.getLogger(DebugGraphicalOutput.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        // reading the stream into a buffered image

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        try {
            result = ImageIO.read(bis);
        } catch (IOException ex) {
            Logger.getLogger(DebugGraphicalOutput.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        this.plotImages.put(plot.sourceFile.getAbsolutePath(), result);
        return result;
    }

    /**
     * Creates an image file with outline of the plot and with a predefined
     * rectangle drawn over it
     *
     * @param plot
     * @param rec
     * @param fname
     */
    public static void drawFileWithRectangle(SVGPlot plot, Rectangle rec, String fname) throws IOException {
        DebugGraphicalOutput dgo = DebugGraphicalOutput.getInstance();
        dgo.reset();

        dgo.graphics.setColor(Color.PINK);

        for (Shape sh : plot.splitTextElements.keySet()) {
            dgo.graphics.draw(sh);
        }
        dgo.graphics.setColor(new Color(0, 255, 0, 127));
        dgo.graphics.draw(rec);
        dgo.graphics.fill(rec);

        dgo.flush(new File(fname));
    }
}
