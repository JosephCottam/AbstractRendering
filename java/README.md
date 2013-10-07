Abstract Rendering: Java
======================
Build:
* With demo app: ./ant
* Just core: ./ant core
* With extensions: ./ant ext
* Java 1.6 subset: ./ant onesix
* Extended dependencies: ./ant depends-dev  (useful for development environments; not required for basic build)

Requires ant version 1.8 or higher.
Dependencies for all extension are downloaded, regardless of the build configuration.

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

For a more full-featured application execute "java -jar ARApp.jar -ext".
This application allows exploration of your own data and various treatments.
However, there are combinations that will not work (for example, glyph-parallel 
rendering requires a list-based glyph container type).  No effort is made to 
guide the selection of appropriate values in the more complex application.
Plot area navigation and aggregate exports work as in the simpler demo application.

A JSON encoding of int-aggregates can be saved with the 
"Export Aggregates" button. If saved as "aggregates.json" in 
the TransferJS directory, this file will be used as the 
dataset for the pixel-shader implementation.


Extensions
-----------

Extensions are experimental or task-specific tools being co-developed with the core
Abstract Rendering system. Extensions are all compiled together (no cherry-picking 
extensions at this time).  To compile extensions, invoke "ant ext".

### Avro
The avro extensions provide tools for working with [apache avro](avro.apache.org) serialization.
Support for saving/restoring aggregate sets and raw datasets (through implicit geometry) are implemented.
Serialization is based on schemas that are included as JAR resources (current count, RLE and color
are supported as aggregate types). Avro can be used to serialize to binary or to JSON files.

### Sever
The ARServer is a self-contained HTTP server that responds to post messages that describe
a dataset and treatment.  It returns json-encoded aggregates that result 
If the AR.jar file was built with extensions, the server can be executed with
"java -jar AR.jar ar.ext.server.ARServer" paramters are -host and -port 
(which default to "localhost" and "8080," respectively).  The sever uses the Avro extensions
to format the return result.

### Spark
Abstract Rendering implementation to run in the [AMP Spark](http://spark-project.org/) framework.
This uses many of the standard tools, but different driver (e.g., not true "Renderer" class)
to do aggregation in a distributed memory environment.  Transfer is still done locally.

Unlike other extensions, the Spark extension requires the Spark jar to be provided "by hand."
If the a jar named spark-core-assembly-0.7.2.jar is not found in the lib directory, the spark
extensions will not be included in the resulting jar file.  This is a restriction is because
the spark jar file needs to be compiled differently depending on the runtime environment.
 
### Tiles
Tools for manipulating aggregates for use with a tile-server.
Can create tiles from aggregates or combine multiple tiles into an aggregate set.
RElies on the Avro extension.

### RHIPE Tools
A set of tools for working with R-integration. Targeted at the [RHIPE](http://www.datadr.org/).


