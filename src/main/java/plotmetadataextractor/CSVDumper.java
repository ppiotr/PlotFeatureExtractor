/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Dumping the CSV representation of a directory containing figures
 *
 * @author piotr
 */
public class CSVDumper {

    /**
     * Dumps a plot to a CSV file
     *
     * @param plot
     * @param fname
     */
    public static void writeCSV(SVGPlot plot, String fname) throws FileNotFoundException, IOException {
        FileOutputStream fos = new FileOutputStream(fname);
        PrintStream ps = new PrintStream(fos);


        for (ExtLine2D line : plot.lineSegments) {
            ps.print(line.x1);
            ps.print(",");
            ps.print(line.y1);
            ps.print(",");
            ps.print(line.x2);
            ps.print(",");
            ps.println(line.y2);
        }

        ps.flush();
        fos.close();
    }

    public static void dumpDirCSV(String dirName) {
        File f = new File(dirName);
        for (String fname : f.list()) {
            if (fname.endsWith(".svg")) {
                try {
                    System.out.println("The file being processed: " + fname);
                    String ofname = dirName + "/" + fname.replace(".svg", ".csv");
                    SVGPlot plot = new SVGPlot(dirName + "/" + fname);
                    writeCSV(plot, ofname);
                } catch (FileNotFoundException e) {
                    System.out.println("Failed processing a SVG document: "  + fname);
                } catch (IOException e){
                    System.out.println("Failed processing a SVG document: "  + fname);
                } catch (Exception e){
                    System.out.println("Another error !");
                }
            }
        }
    }

    public static void main(String[] args) {
        dumpDirCSV(args[0]);
    }
}
