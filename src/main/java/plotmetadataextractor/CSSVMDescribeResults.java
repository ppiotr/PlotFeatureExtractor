/* This program reads th emanually classified diles and generates the annotation in .train file
 */
package plotmetadataextractor;

import invenio.common.FileUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author piotr
 */
public class CSSVMDescribeResults {

    /**
     * Read the provided directory and prepare a list of classified entried from
     * them Entries are aggregated by the source file name
     *
     * @param inDir
     * @return
     */
    public static HashMap<String, List<String>> generateClassifiedList(File inDir) throws Exception {
        HashMap<String, List<String>> result = new HashMap<String, List<String>>();
        List<String> relevantFiles = FileUtils.getRelevantFiles(inDir.getAbsolutePath(), ".png");
        for (String fname : relevantFiles) {
            Pair<String, String> res = CoordSystemSVM.splitEncodedFileName(fname);
            if (!result.containsKey(res.getKey())) {
                result.put(res.getKey(), new LinkedList<String>());
            }
            result.get(res.getKey()).add(res.getValue());
        }

        return result;
    }

    public static void main(String args[]) throws Exception {
        // TODO: This should be replaced by nice argument parsing
        String inputDirName = "toprepare";
        String classifiedDirName = "out";


        // the main code
        File trueDir = new File(classifiedDirName, "true");
        File falseDir = new File(classifiedDirName, "false");
        HashMap<String, List<String>> trueMap = generateClassifiedList(trueDir);
        HashMap<String, List<String>> falseMap = generateClassifiedList(falseDir);


        List<String> svgNames = FileUtils.getRelevantFiles(inputDirName, ".svg");
        for (String svgFname : svgNames) {
            String basename = FileUtils.stripFileExt(svgFname);
            File outputFile = new File(inputDirName, basename + ".train");
            writeSelectedCSDescriptor(basename, outputFile, trueMap, falseMap);
        }

    }

    private static void writeResultsToFile(String basename, FileWriter writer, HashMap<String, List<String>> map, String additionalInfo) throws IOException {
        if (map.containsKey(basename)) {
            for (String label: map.get(basename)){
                writer.write(label.replaceAll("_", " "));
                writer.write(" ");
                writer.write(additionalInfo);
                writer.write("\n");
            }
        }
    }

    private static void writeSelectedCSDescriptor(String basename, File outputFile, HashMap<String, List<String>> trueMap, HashMap<String, List<String>> falseMap) throws IOException {
        FileWriter oWriter = new FileWriter(outputFile);
        writeResultsToFile(basename, oWriter, trueMap, "Y");
        writeResultsToFile(basename, oWriter, falseMap, "N");

        oWriter.close();
    }
}
