/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;

/**
 *
 * @author piotr
 */
public class CoordinateSystem {
    Line2D axe1;
    Line2D axe2;
    Map<Double, Line2D> ticks1; // ticks on the 1st axis
    Map<Double, Line2D> ticks2; // ticks on the 2nd axis ... mapping the point with the 
    
    
    /**
     * Returns the same coordinate axis with the axis meaning inverted
     * @return 
     */
    public CoordinateSystem transpose(){
        return null;
    }
    
    /**
     * Detects all the coordinate systems encoded in a graph
     * @param plot
     * @return 
     */
    public static List<CoordinateSystem> extractCoordinateSystems(SVGPlot plot){
        Line2D l1;
        Line2D l2;
        
        return null;
    }
    
    
    
    
}
