/*
 * This class represents the content of a .train file and provides facilities allowing to detect if a given coordnate system candidate 
 * has been described in the given file
 */
package plotmetadataextractor;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author piotr
 */
public class SamplesSet {

    /**
     * a class identifying a particular Coordinate System
     */
    private class CSID {

        ExtLine2D axis1;
        ExtLine2D axis2;
        boolean isCS;

        /**
         * Check if this descriptor matches with a given coordinate system
         *
         * @param cs the coordinate system to check
         * @return
         */
        public boolean isTheSame(CoordinateSystem cs) {
            // we need to introduce some order among the points before we can start comparing !
            double totalTolerance = 0.001;

            double dist1 = this.axis1.endsDistance(cs.axes.getKey()) + this.axis2.endsDistance(cs.axes.getValue());
            double dist2 = this.axis1.endsDistance(cs.axes.getValue()) + this.axis2.endsDistance(cs.axes.getKey());

            return (dist1 < totalTolerance || dist2 < totalTolerance);
        }
    }
    private List<CSID> csDescriptors;

    public SamplesSet() {
        this.csDescriptors = new LinkedList<CSID>();
    }

    public SamplesSet(File tFile) throws FileNotFoundException, IOException {
        this.csDescriptors = new LinkedList<CSID>();
        this.load(tFile);
    }

    
    /**
     * Uses the loaded samples classification to determine if the given coordinate 
     * system candidate is a coordinate system or not.
     * returns true for a coordinate system, false for a false candidate and null 
     * in the case of the CS candidate not being described by the loaded samples set
     * @param csCandidate
     * @return 
     */
    public Boolean getCSClassification(CoordinateSystem csCandidate) {
        for (CSID id : this.csDescriptors){
            if (id.isTheSame(csCandidate)){
                return id.isCS;
            }
        }
        return null;
    }

    /**
     * Load the train file into the memory
     *
     * @param tFile
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void load(File tFile) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(tFile));
        String line;


        while ((line = reader.readLine()) != null) {
            line = line.replaceAll("\\s+", " ");
            String[] elements = line.split(" ");

            CSID desc = new CSID();
            // normalised lines are easier to compare as it is well defined, which point will be the 1st
            desc.axis1 = (ExtLine2D) ExtLine2D.normaliseLine(new ExtLine2D(Double.parseDouble(elements[0]),
                    Double.parseDouble(elements[1]),
                    Double.parseDouble(elements[2]),
                    Double.parseDouble(elements[3])));
            desc.axis2 = (ExtLine2D) ExtLine2D.normaliseLine(new ExtLine2D(Double.parseDouble(elements[4]),
                    Double.parseDouble(elements[5]),
                    Double.parseDouble(elements[6]),
                    Double.parseDouble(elements[7])));
            desc.isCS = elements[8].equals("Y");

            this.csDescriptors.add(desc);
        }
    }
}
