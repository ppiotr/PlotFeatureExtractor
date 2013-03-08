/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.inveniosoftware.inveniosemantics.InvenioOntologyAccessor;
import plotmetadataextractor.CoordinateSystem.Tick;

/**
 * This class is responsible for writing all the extracted data into the
 * semantic repository of Invenio
 *
 *
 * Currently, the class supports only writing new resources (it uses an internal
 * algorithm to assign URIs ... this algorithm assuemes that no related objects
 * exist
 *
 * @author piotr
 */
public class SemanticsOutputWriter {

    private final InvenioOntologyAccessor invenio;

    /**
     * Write the extracted plot
     *
     * @param plot The object representing the plot
     * @param uri the URI of the plot (We assume that the plot object has been
     * created before)
     */
    public SemanticsOutputWriter() {
        this.invenio = new InvenioOntologyAccessor("../InvenioSemantics/files/inveniomodel.owl");
    }

    public void flush() {
        try {
            this.invenio.saveModel();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SemanticsOutputWriter.class.getName()).log(Level.SEVERE, "Unable to save statements into the repository", ex);
        }
    }

    public void writePlotData(SVGPlot plot, String uri) {
        Model model = invenio.getModel();
        Resource plotR = model.createResource(uri, invenio.plot);


        int curNum = 1;
        for (CoordinateSystem cs : plot.coordinateSystems) {
            String csURI = uri + "/CoordinateSystem" + curNum;
            this.writeCoordinateSystem(cs, plotR, csURI);
            curNum++;
        }


    }

    public Resource pointToAnonymousResource(Point2D.Double point) {
        Model model = this.invenio.getModel();
        return this.invenio.getModel().createResource().addLiteral(this.invenio.hasXCoordinate, point.x).addLiteral(this.invenio.hasYCoordinate, point.y);
    }

    public Resource rectangleToAnonymousResource(Rectangle2D.Double rectangle) {
        Model model = this.invenio.getModel();
        return this.invenio.getModel().createResource().
                addLiteral(this.invenio.hasXCoordinate, rectangle.getMinX()).
                addLiteral(this.invenio.hasYCoordinate, rectangle.getMinY()).
                addLiteral(this.invenio.hasWidth, rectangle.getWidth()).
                addLiteral(this.invenio.hasHeight, rectangle.getHeight());
    }

    public void writeCoordinateSystem(CoordinateSystem cs, Resource plot, String csURI) {
        Model model = invenio.getModel();
        Resource coordinateSystemR = model.createResource(csURI, invenio.coordinateSystem);
        Resource originR = model.createResource(csURI, invenio.coordinateSystemOrigin);

        model.createStatement(plot, invenio.contains, coordinateSystemR);
        model.createStatement(originR, invenio.isLocatedAt, this.pointToAnonymousResource(cs.origin));

        this.writeCSAxis(cs.axes.getKey(), cs.axesTicks.getKey(), coordinateSystemR, csURI + "/axis1");
        this.writeCSAxis(cs.axes.getValue(), cs.axesTicks.getValue(), coordinateSystemR, csURI + "/axis2");

    }

    public void writeCSAxis(ExtLine2D axisLine, CoordinateSystem.AxisTick ticks, Resource cs, String axisURI) {
        Model model = invenio.getModel();
        Resource axisR = model.createResource(axisURI, invenio.axis);
        model.createStatement(cs, invenio.contains, axisR);
        // now writing the axis ticks
        int tickNum = 1;
        for (Tick tick : ticks.ticks.values()) {
            String tickURI = axisURI + "/tick" + tickNum;
            Resource tickR = model.createResource(tickURI, this.invenio.axisTick);
            model.createStatement(axisR, this.invenio.contains, tickR);
            model.createStatement(tickR, this.invenio.isLocatedAt, this.pointToAnonymousResource(tick.intersection));
            if (tick.label != null) {
                Resource labelR = model.createResource(tickURI + "/label", this.invenio.axisTickLabel);
                labelR.addProperty(DC.description, tick.label.text);
                labelR.addProperty(this.invenio.hasBoundary, this.rectangleToAnonymousResource((Rectangle2D.Double) tick.label.boundary.getBounds2D()));
            }
            tickNum++;
        }
    }
}
