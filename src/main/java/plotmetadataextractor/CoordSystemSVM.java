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
import java.util.Random;
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

        /**
         * Creates the representation as a string suitable for the SVM training
         * file
         *
         * @param string
         */
        public String toTrainingRepresentation(String svmClass) {
            StringBuilder resB = new StringBuilder();
            resB.append(svmClass);
            int featureInd = 0;
            for (Double feature : this.features) {
                resB.append(" ");
                resB.append(featureInd);
                resB.append(":");
                resB.append(feature);
                featureInd++;
            }
            resB.append("\n");
            return resB.toString();
        }
    }

    /**
     * Creates a file name which encodes the given coordinate system
     *
     * @param cs The object storing the coorindate system
     * @param fname The name of hte file from which this coordinate system has
     * been extracted
     * @return
     */
    public static String csToFileNameRoot(CoordinateSystem cs, String fname) {
        return String.format("%s_%s_%s", fname, lineToString(cs.axes.getKey()), lineToString(cs.axes.getValue()));
    }

    public static String lineToString(ExtLine2D line) {
        return String.format("%f_%f_%f_%f", line.x1, line.y1, line.x2, line.y2);

    }

    /**
     * Analyses the file name of a classified coordinate system and splits it
     * into the coordinates part and the source filename path
     *
     * @param fname
     * @return
     */
    public static Pair<String, String> splitEncodedFileName(String fname) {
        // find the 8th '_' char from the end and use the entire remaining prefix
        String fnamep = FileUtils.stripFileExt(fname);
        int pos = fnamep.length() - 1;
        for (int i = 0; i < 8; i++) {
            while (pos >= 0 && fnamep.charAt(pos) != '_') {
                pos--;
            }
            pos--;
        }

        if (pos <= 0) {
            return new ImmutablePair<String, String>("", fnamep);

        } else {
            return new ImmutablePair<String, String>(fnamep.substring(0, pos + 1), fnamep.substring(pos + 2));
        }
    }

    /**
     * Generates a directory containing coordinate system candidates ready for
     * manual classification.
     *
     * @param plotsDirName
     * @param samplesDirName Name of the directory to write the samples
     * @param numSamples the number of samples to be selected from the
     * candidates
     */
    public static void writeSamplesToDirectory(String plotsDirName, String samplesDirName, int numSamples) throws Exception {
        File outputDir = new File(samplesDirName);

        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        LinkedList<Pair<CoordinateSystem, SVGPlot>> csCandidates = new LinkedList<Pair<CoordinateSystem, SVGPlot>>(); // cscandidate, source fname
        for (String svgFileName : FileUtils.getRelevantFiles(plotsDirName, ".svg")) {
            System.out.println("processing " + svgFileName);
            try {
                SVGPlot svgPlot = new SVGPlot(new File(plotsDirName, svgFileName));

                List<CoordinateSystem> candidates = CoordinateSystem.extractCSCandidates(svgPlot);
                for (CoordinateSystem csCandidate : candidates) {
                    csCandidates.add(new ImmutablePair<CoordinateSystem, SVGPlot>(csCandidate, svgPlot));

                    System.out.println(csToFileNameRoot(csCandidate, svgFileName));
                }
            } catch (Exception e) {
                System.err.println("Failed when processing " + svgFileName);
            }
        }

        // now selecting the random samples ! we do not want to write all the extracted samples as they will be very similar within every file
        Random random = new Random();
        for (int i = 0; i < numSamples && !csCandidates.isEmpty(); ++i) {
            int selPos = random.nextInt(csCandidates.size());

            writeCsCandidateDebugImage(csCandidates.get(selPos).getKey(), csCandidates.get(selPos).getValue(), outputDir);
            csCandidates.remove(selPos);
        }

        // just creating empty directories for true and false candidates
        (new File(samplesDirName, "true")).mkdir();
        (new File(samplesDirName, "false")).mkdir();
    }

    /**
     * Process a single coordinate system candidate ... write its annotated
     * image into a graphical file
     *
     * @param cs
     * @param sourceFname
     * @param outputDir
     */
    private static void writeCsCandidateDebugImage(CoordinateSystem cs, SVGPlot plot, File outputDir) throws IOException {
        File outputFile = new File(outputDir, csToFileNameRoot(cs, FileUtils.stripFileExt(plot.sourceFile.getName())) + ".png");
        DebugGraphicalOutput.dumpCoordinateSystem(plot, cs, outputFile.getAbsolutePath());
    }


    /**
     * Writes a list of samples belonging to the same class to a stream
     *
     * @param classIdentifier
     * @param samples
     * @param os
     */

    /// the prediction - related stuff
    public svm_model model;

    public static CoordSystemSVM getStandardModel() throws IOException {
        return new CoordSystemSVM("misc/coordinate_system_SVM.model");
    }

    public CoordSystemSVM(String modelFileName) throws IOException {
        this.model = libsvm.svm.svm_load_model(modelFileName);

    }

    private boolean isCoordinateSystem(CSFeatureVector csFeatureVector) {
        svm_node[] featureVec = new svm_node[csFeatureVector.features.size()];
        int index = 1;
        for (Double feature : csFeatureVector.features) {
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
