PlotFeatureExtractor
====================

This project aims at building the set of tools allowing to extract basic semantics from figures encoded as SVG files. Main focus lies on the figures from High Energy Physics


The following packages do not automatically resolve in Maven. You have to download the following files and add them manually:

http://repo.opengeo.org/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar
http://repo.opengeo.org/javax/media/jai_codec/1.1.3/jai_codec-1.1.3.jar


The commands allowing to install the downloaded files in the local Maven repository:

mvn install:install-file -DgroupId=com.sun.media -DartifactId=jai-codec -Dversion=1.1.3 -Dpackaging=jar -Dfile=jai_codec-1.1.3.jar
mvn install:install-file -DgroupId=javax.media -DartifactId=jai-core -Dversion=1.1.3 -Dpackaging=jar -Dfile=jai_core-1.1.3.jar
-*




** Preparing the training set for SVM classifiers and using them **


SVM classification is used to distinguish coordinate system candidates which represent coordinate systems from those which do not.
Using SVM requires the dataset to be represented in a certain multi-dimensional space.
In the case of coordinate systems, these dimensions are synthetised from the description of coordinate system candidates.
Example dimensions include the ratio between length of axes, the number of long axis ticks and the number of thicks.
Those dimensions can change in future versions of the extractor.

The training of SVG classifeirs proceeds in 2 steps:

1) The training set of SVG documetns has to be constructed.
   * Every of the input files of this directory is processed using the first steps of the extraction algorithm and coordinate system candidates are identified
   * A number of random samples is selected out of the entire set of candidates (using all the candidates would generate too much data to process manually ... we tend to concentrate on having a larger number of source documents ... candidates from the same document tend to be similar)
   * Every selected sample is written in a graphical file which can be manually inspected and classified as coordinate system or a false candidate.
     The names of the generated files encode candidates in non-ambiguous way (file name + coordinates of both axes)
     The way of creating the file names:
     source_x1_y2_x2_y2_x3_y3_x4_y4.png
   * The user needs to manually classify the extracted candidates into two categories -> false and true candidates.
     The selection can be performed by moving the generated files into two directories names "false" and "true" inside of the output directory
   * The training set preparation script reads the manually generated annotations and generates training input files (*.train).
     One file is created for every input SVG file. Each file consists of a number of lines corresponding to one coordinate system candidate
     The example line looks like follows:
     (x,y,x1,y1) (x2,y2,x3,y3) Y
     First, two pairs of coordinates are privuded. They encode the position of both coordinate system axes candidates.
     The first line is longer (or having larger x ... or y coordinate in the case of x being the same).
     The coordinates of the second point of each line have to have greater x .... or y in the case of having the same x value
     The last entry in the line consists of a character Y or N. Y for candidates encoding a coordinate system. N for false candidates.
  * The training input set can be passed (together with the corresponding SVG files) to the SVM trainer which generates a global classifer file (which is used by the main algorithm)

In the case of changing the used dimensiosn, only the last step needs to bre repeated

Example (commands needed to prepare the SVM training


extractCSCandidates /home/piotr/input_dir /tmp/training_output_dir -n 10000    # extracts up to 10000 coordinate system candidates from the files


cd /tmp/training_output_dir

mv 1.png false
mv 2.png true
(...)                 # this part can be done using graphical interface much faster .... Nautilus provides a preview mode which allows to immediately see true/false candidates without opening files


createTrainingInput  /tmp/training_output_dir  /home/piotr/input_dir   # creates the .train files
trainSVM /home/piotr/input_dir  /tmp/classifier_file    # trains the SVM using the input directory together with previously generated .train files stored there


The next step is cross-validation of the training.
This can be done using additional directory containing .train files


validateSVM /home/piotr/validation /tmp/classifier_file