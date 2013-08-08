/*
 * This class represents the content of a .train file and provides facilities allowing to detect if a given coordnate system candidate 
 * has been described in the given file
 */
package plotmetadataextractor;

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
         * @param cs the coordinate system to check
         * @return 
         */
        public boolean isTheSame(CoordinateSystem cs){
            // we need to introduce some order among the points before we can start comparing !
            return false;
        }
    }

    
    private List<CSID> csDescriptors;
    
    public SamplesSet(){
        this.csDescriptors = new LinkedList<CSID>();
    }
    
    
    public SamplesSet(File tFile) throws FileNotFoundException, IOException{
        this.csDescriptors = new LinkedList<CSID>();
        this.load(tFile);
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
            desc.axis1 = new ExtLine2D(Double.parseDouble(elements[0]),
                    Double.parseDouble(elements[1]),
                    Double.parseDouble(elements[2]),
                    Double.parseDouble(elements[3]));
            desc.axis2 = new ExtLine2D(Double.parseDouble(elements[4]),
                    Double.parseDouble(elements[5]),
                    Double.parseDouble(elements[6]),
                    Double.parseDouble(elements[7]));
            desc.isCS = elements[8].equals("Y");
            
            this.csDescriptors.add(desc);
        }
    }
}
