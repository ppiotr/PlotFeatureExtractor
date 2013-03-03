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