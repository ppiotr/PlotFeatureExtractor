/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import invenio.common.FileUtils;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import plotmetadataextractor.CoordinateSystem.TickLabel;

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
            String[] principalParts = fname.split("\\.png");
            if (principalParts.length <= 0) {
                throw new Exception("Incorrect filename");
            }

            String toProcess = principalParts[0];
            if (principalParts[0].contains("__")) {
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

            if (customData.length() > 0) {
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
     * @param numSamples  the number of samples to be selected from the candidates
     */
    public static void writeSamplesToDirectory(String plotsDirName, String samplesDirName, int numSamples) throws Exception {
        File outputDir = new File(samplesDirName);
        
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }
        
        LinkedList<Pair<CoordinateSystem, String>> csCandidates = new LinkedList<Pair<CoordinateSystem, String>>(); // cscandidate, source fname
        for (String svgFileName : FileUtils.getRelevantFiles(plotsDirName, ".png")) {
            System.out.println("processing " + svgFileName);
            try {
                SVGPlot svgPlot = new SVGPlot(new File(plotsDirName, svgFileName));
                List<CoordinateSystem> candidates = CoordinateSystem.extractCSCandidates(svgPlot);
                for (CoordinateSystem csCandidate : candidates) {
                    csCandidates.add(new ImmutablePair<CoordinateSystem, String>(csCandidate, svgFileName));
                    
/*                    CSFeatureVector vec = new CSFeatureVector(csCandidate, svgPlot);
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
  /*                      fname = vec.toFileNamePrefix(svgFileName.replace(".svg", "")) + (dupInd == 0 ? "" : "_dup" + dupInd) + ".png";
                        outputFile = new File(samplesDirName, fname);
                        dupInd++;
                    } while (outputFile.exists());
                    DebugGraphicalOutput.dumpCoordinateSystem(svgPlot, csCandidate, outputFile.getAbsolutePath());
                    vec = new CSFeatureVector(csCandidate, svgPlot);
*/
                    
                }
            } catch (Exception e) {
                System.err.println("Failed when processing " + svgFileName);
            }
        }
        
        // now selecting the random samples ! we do not want to write all the extracted samples as they will be very similar within every file
        
        

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
    public static Pair<List<CSFeatureVector>, List<CSFeatureVector>> readClassifiedDirectory(String dirName) throws Exception {
        File trueDir = new File(dirName, "true");
        File falseDir = new File(dirName, "false");
        return new ImmutablePair<List<CSFeatureVector>, List<CSFeatureVector>>(
                getFeaturesFromDirectory(trueDir.getAbsolutePath()),
                getFeaturesFromDirectory(falseDir.getAbsolutePath()));
    }

    /**
     * Creates a list of feature vectors encoded in file names from the given
     * directory
     *
     * @param dirName
     * @return
     */
    private static List<CSFeatureVector> getFeaturesFromDirectory(String dirName) throws Exception {
        File inputDir = new File(dirName);
        if (!inputDir.exists() || !inputDir.isDirectory() || !inputDir.canRead()) {
            throw new Exception("Error when reading the input directory");
        }
        List<CSFeatureVector> result = new LinkedList<CSFeatureVector>();
        String[] files = inputDir.list();
        for (String fname : files) {
            if (fname.endsWith(".png")) {
                result.add(new CSFeatureVector(fname));
            }
        }
        return result;
    }

    /**
     * Writes a training set into a stream, using the format of LibSVM true
     * candidates are encoded with the label +1, while false candidates with -1
     *
     * @param samples
     * @param os
     */
    public static void writeTrainingFile(
            Pair<List<CSFeatureVector>, List<CSFeatureVector>> samples,
            PrintStream os) {
        writeSamples("+1", samples.getKey(), os);
        writeSamples("-1", samples.getValue(), os);
    }

    /**
     * Writes a list of samples belonging to the same class to a stream
     *
     * @param classIdentifier
     * @param samples
     * @param os
     */
    public static void writeSamples(String classIdentifier,
            List<CSFeatureVector> samples, PrintStream os) {
        for (CSFeatureVector vec : samples) {
            os.print(classIdentifier);
            os.print(" ");
            int pos = 1;
            for (Double feature : vec.features) {
                os.print(pos);
                os.print(":");
                os.print(feature);
                os.print(" ");
                pos++;
            }
            os.println();
        }
    }
    /// the prediction - related stuff
    public svm_model model;

    public static CoordSystemSVM getStandardModel() throws IOException{
        return new CoordSystemSVM("misc/coordinate_system_SVM.model");
    }
    
    public CoordSystemSVM(String modelFileName) throws IOException {
        this.model = libsvm.svm.svm_load_model(modelFileName);
        
    }

    private boolean isCoordinateSystem(CSFeatureVector csFeatureVector) {
        svm_node[] featureVec = new svm_node[csFeatureVector.features.size()];
        int index=1;
        for (Double feature: csFeatureVector.features){
            featureVec[index] = new svm_node();
            featureVec[index].index = index;
            featureVec[index].value = feature;
        }
        
//        double predicted_val = libsvm.svm.svm_predict(this.model, featureVec);
        double[] prob_estimates = new double[csFeatureVector.features.size()];
        double predicted_val = libsvm.svm.svm_predict_probability(model, featureVec, prob_estimates);
        return predicted_val < 0;
    }

    public boolean isCoordinateSystem(CoordinateSystem cs, SVGPlot plot) {
        return this.isCoordinateSystem(new CSFeatureVector(cs, plot));
    }
}
