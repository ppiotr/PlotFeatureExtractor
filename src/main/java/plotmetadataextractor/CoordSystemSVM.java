/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import plotmetadataextractor.CoordinateSystem.TickLabel;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * a SVM classifier distinguishing axis from false candidates
 *
 * The workflow for training the classifier:
 *
 * 1) We extract a number of axis candidates and convert them to the feature
 * vectors.
 *
 * 2) We produce annotated PNG files describing those coordinate system
 * candidates. The file name encodes the vector coordinates.
 *
 * 3) We manually split files into 2 directories: coordinate systems and false
 * candidates
 *
 * 4) We read the directories and decode feature vectors from the file names ...
 * preparing the training and the testing sets.
 *
 * 5) WE use the data to train the classifier, which we further use directly on
 * the samples.
 *
 * @author piotr
 */
public class CoordSystemSVM {

    /**
     * Features that can be extracted for the SVM classification
     */
    public static class CSFeatureVector {

        List<Double> features;

        /**
         * Create parameters of a coordinate system encoded in a file name.
         *
         * This method is used for example during the training procedure.
         *
         * @param fileName
         */
        public CSFeatureVector(String fileName) throws Exception {
            this.features = new LinkedList<Double>();
            File f = new File(fileName);
            String fname = f.getName();
            String[] principalParts = fname.split("\\.");
            if (principalParts.length <= 0) {
                throw new Exception("Incorrect filename");
            }
            String toProcess =principalParts[0];
            if (principalParts[0].contains("__")){
                toProcess = toProcess.split("__")[1];
            }
            String[] vectorCoeff = toProcess.split("_");
            for (String coeff : vectorCoeff) {
                if (coeff.startsWith("dup")) {
                    break;
                }
                this.features.add(Double.parseDouble(coeff));
            }
        }

        /**
         * Creates a feature vector directly from a coordinateSystm instance
         *
         * @param cs The coordinate system to create the feature vector from
         *
         */
        public CSFeatureVector(CoordinateSystem cs, SVGPlot plot) {
            this.features = new LinkedList<Double>();

            this.features.add(cs.lengthsRatio);
            this.features.add(cs.linesOutside);
            this.features.add((double) cs.getTicksNum());
            this.features.add(cs.getTicksWithLabelFraction());
            this.features.add(CSFeatureVector.widthLengthFraction(plot, cs));
            this.features.add(CSFeatureVector.calculateMinimalRelativeLabelsVariace(plot, cs));
        }

        /**
         * Calculates the relative variance of locations of ticks in the minimal
         * direction. (Relative to the width/height of the entire plot)
         *
         * @return
         */
        private static double calculateMinimalRelativeLabelsVariace(SVGPlot plot, CoordinateSystem cs) {
            if (cs.getTicksNum() > 0 && cs.getTicksWithLabelsNum() > 0) {
                /**
                 * we calculate , how far are the labels from average(choosing
                 * the minimal value and scaling to the width of the plotS)
                 */
                List<TickLabel> labelsX = CoordinateSystem.extractTickLabels(cs.axesTicks.getKey().ticks.values());
                List<TickLabel> labelsY = CoordinateSystem.extractTickLabels(cs.axesTicks.getValue().ticks.values());
                double minVar = 1.0;
                List<Double> vars = new LinkedList<Double>();
                if (labelsX.size() > 0) {
                    Pair<Double, Double> v = CoordinateSystem.calculateLabelsLocationVariance(labelsX);
                    vars.add(v.getKey() / plot.boundary.getWidth());
                    vars.add(v.getValue() / plot.boundary.getHeight());
                }
                if (labelsY.size() > 0) {
                    Pair<Double, Double> v = CoordinateSystem.calculateLabelsLocationVariance(labelsY);
                    vars.add(v.getKey() / plot.boundary.getWidth());
                    vars.add(v.getValue() / plot.boundary.getHeight());
                }
                for (Double v : vars) {
                    if (v < minVar) {
                        minVar = v;
                    }
                }
                return minVar;

            } else {
                return 1.0;
            }
        }

        private static double widthLengthFraction(SVGPlot plot, CoordinateSystem cs) {
            return (cs.axes.getKey().len() + cs.axes.getValue().len()) / (plot.boundary.getHeight() + plot.boundary.getWidth());
        }

        /**
         * Returns a file name prefix which encodes the coordinate system
         *
         * @return
         */
        public String toFileNamePrefix(String customData) {
            StringBuilder sb = new StringBuilder();
            boolean isFirst = true;
            
            if (customData.length() >0 ){
                sb.append(customData);
                sb.append("__");
            }
            for (Double feature : this.features) {
                if (!isFirst) {
                    sb.append("_");
                } else {
                    isFirst = false;
                }
                sb.append(feature.toString());
            }
            return sb.toString();
        }
    }

    /**
     * Generates a directory containing coordinate system candidates ready for
     * manual classification.
     *
     * @param plotsDirName
     * @param samplesDirName Name of the directory to write the samples
     */
    public static void writeSamplesToDirectory(String plotsDirName, String samplesDirName) throws Exception {
        File inputDir = new File(plotsDirName);
        File outputDir = new File(samplesDirName);
        if (!inputDir.exists()) {
            throw new Exception("The input directory does not exist");
        }
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        String[] svgFileNames = inputDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".svg");
            }
        });

        for (String svgFileName : svgFileNames) {
            System.out.println("processing " + svgFileName);
            try {
                SVGPlot svgPlot = new SVGPlot(new File(plotsDirName, svgFileName));
                List<CoordinateSystem> candidates = CoordinateSystem.extractCSCandidates(svgPlot);
                for (CoordinateSystem csCandidate : candidates) {
                    CSFeatureVector vec = new CSFeatureVector(csCandidate, svgPlot);
                    String fname;
                    File outputFile;
                    int dupInd = 0;
                    do {
                        /**
                         * There might already exists a feature vector having
                         * exactly the same parameters ... better it has similar
                         * properties, otherwise the feature selection has not
                         * been performed correctly
                         */
                        fname = vec.toFileNamePrefix(svgFileName.replace(".svg", "")) + (dupInd == 0 ? "" : "_dup" + dupInd) + ".png";
                        outputFile = new File(samplesDirName, fname);
                        dupInd++;
                    } while (outputFile.exists());
                    DebugGraphicalOutput.dumpCoordinateSystem(svgPlot, csCandidate, outputFile.getAbsolutePath());
                    vec = new CSFeatureVector(csCandidate, svgPlot);

                }
            } catch (Exception e) {
                System.err.println("Failed when processing " + svgFileName);
            }
        }

        // just creating empty directories for true and false candidates
        (new File(samplesDirName, "true")).mkdir();
        (new File(samplesDirName, "false")).mkdir();
    }

    /**
     * A method reading a manually classified directory and creating two lists
     * of feature vectors.
     *
     * The first list contains feature vectors corresponding to real coordinate
     * systems. The second list consists of false candidates.
     *
     * @param dirName The name of the directory under which the classification
     * took place. This directory is supposed to contain two subdirectories:
     * true and false
     *
     * @return
     */
    public static Pair<List<CSFeatureVector>, List<CSFeatureVector>> readClassifiedDirectory(String dirName) {
        throw new NotImplementedException();
    }

    /**
     * Creates a list of feature vectors encoded in file names from the given
     * directory
     *
     * @param dirName
     * @return
     */
    private static List<CSFeatureVector> getFeaturesFromDirectory(String dirName) {
        throw new NotImplementedException();
    }
}
