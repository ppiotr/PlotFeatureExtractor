/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plotmetadataextractor;

/**
 * The executable allowing to prepare coordinate system candidates for manual
 * selection
 *
 * @author piotr
 */
public class CSSVMTrainingSetPreparation {
    /**
     * Prints the usage parameters of the program
     */
    public static void usage(){
        System.out.println("CSSVMTrainingSetPreparation is a program dumping all preselected coordinate system candidates form files stored in a given directory");
        System.out.println("  Later, the dumped coordinate systems can be used to train the Support Vector Machine classifier deciding");
        System.out.println("  which candudates should be further considered and which should not");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("   CSSVMTrainingSetPreparation inputDir outputDir ");
        System.out.println("");
        System.out.println("Where:");
        System.out.println("   inputDir:  The directory from which the SVG files for interpretation should be taken");
        System.out.println("   outputDir: The directiry in which the output images should be written");
    }
    public static void main(String[] args) throws Exception {
//        if (args.length != 2) {
//            usage();
//            return;
//        }
//        
//        CoordSystemSVM.writeSamplesToDirectory(args[0], args[1]);
        CoordSystemSVM.writeSamplesToDirectory("toprepare", "out", 4000);
        
    }
}
