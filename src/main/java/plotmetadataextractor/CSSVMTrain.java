/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import plotmetadataextractor.CoordSystemSVM.CSFeatureVector;

/**
 * The implementation of a trainer of the SVM recognising coordinate systems
 *
 * @author piotr
 */
public class CSSVMTrain {

    public static void main(String[] args) throws Exception {
        String inputDir = "CoordinateSystemSVMTraining";
        //String inputDir = args[0];
        Pair<List<CSFeatureVector>, List<CSFeatureVector>> samples = CoordSystemSVM.
                readClassifiedDirectory(inputDir);
        File outputFile = new File(inputDir, "dataset.t");
        PrintStream ps = new PrintStream(outputFile);
        CoordSystemSVM.writeTrainingFile(samples, ps);
        ps.flush();
        ps.close();
    }
}
