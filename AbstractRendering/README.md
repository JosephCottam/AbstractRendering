Abstract Rendering: Java
======================

Build:
* With demo app: ant build
* Just core: ant core
* With extensions: ant ext

Demo Application(s)
-----------------

Build with the demo app (default build) and execute "java -jar ARDemo.jar"
A number of preset datasets/treatments are available in the drop-down box.
Shifting between datasets results in a full rendering, but only shifting
between a treatments results in partial re-rendering (just 
re-executing the transfer function).


Pan is done with Drag.  Zoom is done with Shift+Drag.
Triple-click on the main plot area to zoom-extents.

A JSON encoding of int-aggregates can be saved with the 
"Export Aggregates" button. If saved as "aggregates.json" in 
the TransferJS directory, this file will be used as the 
dataset for the pixel-shader implementation.


For a more full-featured application execute "java -jar ARdemo.jar -ext"
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

