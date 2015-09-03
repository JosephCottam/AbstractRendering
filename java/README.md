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
"java -cp ARApp.jar ar.SimpleApp".  This will produce a simple scatterplot visualization.
The source code (found in app/ar/SimpleApp.jar) provides documented example of how
to use Abstract Rendering through the provided swing panel or to produce images directly.

For a more interactive demo, run 'java -cp "ARApp.jar:AR.jar:lib/*" ar.app.ARApp'.
This application presents a number of preset datasets/treatments in the drop-down box.
Shifting between datasets results in a full rendering, but only shifting
between a treatments results in partial re-rendering (just 
re-executing the transfer function).

For a more full-featured application execute 'java -cp "ARApp.jar:AR.jar:lib/*" ar.app.ARApp -ext'.
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
extensions at this time).  To compile extensions, invoke "ant fetch-ext ext".

### Avro
The avro extensions provide tools for working with [apache avro](avro.apache.org) serialization.
Support for saving/restoring aggregate sets and raw datasets (through implicit geometry) are implemented.
Serialization is based on schemas that are included as JAR resources (current count, RLE and color
are supported as aggregate types). Avro can be used to serialize to binary or to JSON files.

### Seam Carving
Image retargeting based on [seam-carving](http://en.wikipedia.org/wiki/Seam_carving).

### Server
The ARServer is a self-contained HTTP server that responds to post messages that describe
a dataset and treatment.  Details on the server are in the [server documentation](./java/ext/ar/ext/server/Readme.md).

### Spark
Abstract Rendering implementation to run in the [AMP Spark](http://spark-project.org/) framework.
This uses many of the standard tools, but different driver (e.g., not true "Renderer" class)
to do aggregation in a distributed memory environment.  Transfer is still done locally.

Because there are many Spark-specific dependencies, they are acquired with "ant fetch-spark".
Run with 'java -cp "./*:lib/*" ar.ext.spark.SimpleSparkApp' for a basic demo.  For access to
other demos, use 'java -cp "./*:lib/*" ar.ext.spark.SparkDemoApp' and related options.
 
### Tiles
Tools for manipulating aggregates for use with a tile-server.
Can create tiles from aggregates or combine multiple tiles into an aggregate set.
Relies on the Avro extension.


