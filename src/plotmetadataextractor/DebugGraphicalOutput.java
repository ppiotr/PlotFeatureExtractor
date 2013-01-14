/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import invenio.common.Images;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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
        this.image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
        this.graphics = (Graphics2D) this.image.getGraphics();
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
     * @param outputFile 
     */
    public void flush(File outputFile) throws IOException {
        Images.writeImageToFile(this.image, outputFile);
    }

    /**
     * Resets the content of the image
     */
    public final void reset() {
        this.graphics.fillRect(0, 0, this.width, this.height);
        this.graphics.setBackground(Color.WHITE);
        this.graphics.setColor(Color.BLACK);
    }
}
