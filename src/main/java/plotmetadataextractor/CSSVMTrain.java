/* Reads the input directory and the corresponding .train files.
 * Describes all the coordinate system candidates from .train files and creates a SVM training file from them
 */
package plotmetadataextractor;

import invenio.common.FileUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import plotmetadataextractor.CoordSystemSVM.CSFeatureVector;

/**
 * The implementation of a trainer of the SVM recognising coordinate systems
 *
 * @author piotr
 */
public class CSSVMTrain {

    public static void usage() {
        System.out.println("This program processes a number of input SVG files together with annotation of sample coordinate systems (.train files) ");
        System.out.println("For every annotated coordinate system candidate, a vector of features is created and written together with the classification to the output file");
        System.out.println("The produced output file can be passed to the LibSVM training executable to generate model which in turn can be used by the coordinate system detection algorithm");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("   CSSVMTrain input_dir output_file");
        System.out.println("Where:");
        System.out.println("   input_dir     -    path to the directory containing .svg and .train files");
        System.out.println("   output_file   -    path to the file in which the output will be written");

    }

    public static void main(String[] args) throws Exception {
        String inDirName = "toprepare";
        String outFileName = "training.t";
        String inputDir = "CoordinateSystemSVMTraining";

//        FileWriter outWriter = new FileWriter(outFileName);
        LinkedList<LinkedList<svm_node>> training_nodes = new LinkedList<LinkedList<svm_node>>();
        LinkedList<Double> training_classes = new LinkedList<Double>();
        for (String svgFileName : FileUtils.getRelevantFiles(inDirName, ".svg")) {
            System.out.println("processing " + svgFileName);
            String basename = FileUtils.stripFileExt(svgFileName);
            File trainFile = new File(inDirName, basename + ".train");

            if (trainFile.exists()) {
                try {
                    SamplesSet annotatedSamples = new SamplesSet(trainFile);
                    SVGPlot svgPlot = new SVGPlot(new File(inDirName, svgFileName));

                    List<CoordinateSystem> candidates = CoordinateSystem.extractCSCandidates(svgPlot);
                    for (CoordinateSystem csCandidate : candidates) {
                        Boolean classification = annotatedSamples.getCSClassification(csCandidate);
                        if (classification != null) {
                            // if the sample is assigned to one of the classes
                            LinkedList<svm_node> nodes = new LinkedList<svm_node>();
                            CoordSystemSVM.CSFeatureVector featureVec = new CSFeatureVector(csCandidate, svgPlot);
                            int pos = 1;
                            for (Double feature : featureVec.features) {
                                svm_node node = new svm_node();
                                node.index = pos;
                                node.value = feature;
                                nodes.add(node);
                                pos++;
                            }

                            training_nodes.add(nodes);
                            training_classes.add(classification ? 1. : 2.);
                            //                           outWriter.write(featureVec.toTrainingRepresentation(classification ? "1" : "0"));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed when processing " + svgFileName);
                }
            }
        }

//        outWriter.close();



// constructing the problem for the training
        svm_problem problem = new svm_problem();
        problem.l = training_nodes.size();

        problem.y = new double[training_classes.size()];
        for (int i = 0; i < training_classes.size(); ++i) {
            problem.y[i] = training_classes.get(i);
        }

        problem.x = new svm_node[training_nodes.size()][];
        for (int i = 0; i < training_nodes.size(); ++i) {
            LinkedList<svm_node> nodes = training_nodes.get(i);
            problem.x[i] = new svm_node[nodes.size()];
            for (int j = 0; j < nodes.size(); ++j) {
                problem.x[i][j] = nodes.get(j);
            }
        }

        svm_parameter param = new svm_parameter();

        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.degree = 3;
        param.gamma = 0.2;	// 1/num_features
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 100;
        param.C = 1;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        param.probability = 0;
        param.nr_weight = 0;
        param.weight_label = new int[0];
        param.weight = new double[0];

	

        String svm_check_parameter = svm.svm_check_parameter(problem, param);

        System.out.println("Arrived to the training\n");
        System.out.flush();
        svm_model model = svm.svm_train(problem, param);
        System.out.println("Passed the training\n");
        System.out.flush();

        svm.svm_save_model("model.mod", model);
        /// now the training 

//                public static svm_model svm_train(svm_problem prob, svm_parameter param);

    }
}
