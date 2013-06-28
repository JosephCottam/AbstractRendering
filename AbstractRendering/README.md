Abstract Rendering: Java
======================

Build:
* With demo app: ./build.sh build
* Just core: ./build.sh core
* With extensions: ./build.sh ext

The java implementation is built via a bash script that auto-fetches dependencies
and an apache ant script. We don't fetch ant or java 1.7, so you'll need those on your system
already.  Also, make sure that java 1.7 is the selected version.  If you have already
the dependencies satisified, you can skip the bash-script (called "build.sh") and
just use ant directly.  If not, the bash script will attempt to acquire them,
put them in a directory called "lib" and then invoke ant.  The bash script 
takes as a parameter the ant task to execute.



Demo Application(s)
-----------------

All demo applications are available through the demo-application build (i.e, the default build).
If there is no ARApp.jar, the demo app has not been built.


The simplest application is titled "SimpleApp." It is executed with
"java -cp ARapp.jar ar.SimpleApp".  This will produce a simple scatterplot visualization.
The source code (found in app/ar/SimpleApp.jar) provides documented example of how
to use Abstract Rendering through the provided swing panel or to produce images directly.

For a more interactive demo, run "java -jar ARApp.jar".
This application presents a number of preset datasets/treatments in the drop-down box.
Shifting between datasets results in a full rendering, but only shifting
between a treatments results in partial re-rendering (just 
re-executing the transfer function).

Pan is done with Drag.  Zoom is done with Shift+Drag.
Triple-click on the main plot area to zoom-extents.

A JSON encoding of int-aggregates can be saved with the 
"Export Aggregates" button. If saved as "aggregates.json" in 
the TransferJS directory, this file will be used as the 
dataset for the pixel-shader implementation.

For a more full-featured application execute "java -jar ARApp.jar -ext".
This application allows exploration of your own data and various treatments.
However, there are combinations that will not work (for example, glyph-parallel 
rendering requires a list-based glyph container type).  No effort is made to 
guide the selection of appropriate values in the more complex application.
Plot area navigation and aggregate exports work as in the simpler demo application.


Extensions
-----------

There are two extensions: Avro serialization and Server.
These can be compiled into the main jar by invoking "ant ext".
Avro extension requires the 
[Avro library (1.7.4)](http://mirror.metrocast.net/apache/avro/avro-1.7.4/java/avro-1.7.4.jar)
and Jackson 1.9 
([part 1](http://repo1.maven.org/maven2/org/codehaus/jackson/jackson-core-asl/1.9.12/jackson-core-asl-1.9.12.jar);
[part 2](http://repo1.maven.org/maven2/org/codehaus/jackson/jackson-mapper-asl/1.9.12/jackson-mapper-asl-1.9.12.jar) 
to be in the ./lib directory.

### Sever
The ARServer is a self-contained HTTP server that responds to post messages that describe
a dataset and treatment.  It returns json-encoded aggregates that result 
If the AR.jar file was built with extensions, the server can be executed with
"java -jar AR.jar ar.ext.server.ARServer" paramters are -host and -port 
(which default to "localhost" and "8080," respectively).  The sever uses the Avro extensions
to format the return result.

### Avro
The Avro extensions provide serialization support for aggregates and
tools for working with input datasets encoded as avro files (through the implicit geometry system).
Serialization is based on schemas that are included as JAR resources (current count, RLE and color
are supported as aggregate types). Avro can be used to serialize to binary or to JSON files.

