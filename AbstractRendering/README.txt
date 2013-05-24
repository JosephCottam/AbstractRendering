To build: ant build
To build just core: ant core 


Simple demo application: 
java -jar ARDemo.jar

Select a preset data configuration.
Shifting between treatments, notice the difference in render time 
  when shifting datasets (thus incurring aggregates creation)
  and when changing transfer functions (reusing the aggregates).


Triple-click on the main plot area to zoom-extents.

A JSON encoding of int-aggregates can be saved with the 
"Export Aggregates" button. If saved as "aggregates.json" in 
the TransferJS directory, this file will be used as the 
dataset for the pixel-shader implementation.



For a more full-featured application:
java -jar ARdemo.jar -ext

This application allows exploration of your own data and various treatments.
However, there are combinations that will not work (for example, glyph-parallel 
rendering requires a list-based glyph container type).  No effort is made to 
guide the selection of appropriate values in the more complex application.
Plot area navigation and aggregate exports work as in the simpler demo application.


