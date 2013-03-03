/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import invenio.common.Images;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private DebugGraphicalOutput() {
        this.width = 1000;
        this.height = 1000;
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
        dgo.reset();

        dgo.graphics.setColor(Color.PINK);

        for (Shape sh : plot.splitTextElements.keySet()) {
            dgo.graphics.draw(sh);
        }

        dgo.graphics.setColor(Color.red);
        // drawing the first axis

        dgo.graphics.draw(cs.axes.getKey());
        for (CoordinateSystem.Tick tick : cs.axesTicks.getKey().ticks.values()) {
            dgo.graphics.setColor(Color.red);
            dgo.graphics.draw(tick.line);

            if (tick.label != null) {
                dgo.graphics.setColor(Color.BLACK);
                dgo.graphics.draw(tick.label.boundary);
                Rectangle2D bounds = tick.label.boundary.getBounds2D();
                int mx = (int) Math.round(bounds.getMinX() + (bounds.getWidth() / 2));
                int my = (int) Math.round(bounds.getMinY() + (bounds.getHeight() / 2));
                dgo.graphics.drawLine((int) Math.round(tick.intersection.getX()), (int) Math.round(tick.intersection.getY()), mx, my);
            }
        }

        dgo.graphics.setColor(Color.blue);
        dgo.graphics.draw(cs.axes.getValue());

        for (CoordinateSystem.Tick tick : cs.axesTicks.getValue().ticks.values()) {
            dgo.graphics.setColor(Color.blue);
            dgo.graphics.draw(tick.line);
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
