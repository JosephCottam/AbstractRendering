To build: ant jar
To build documentation: ant doc  
To execute demo application: java -jar AR.jar

To Use:
Select a reduction function from the top-left drop-down.
Select a transfer function from the top-right drop-down.
The transfer and reduction must have the same value in parenthesis 
  (this indicates the type of the aggregates produced).
Datasets can be selected from the bottom-left drop-down.

A JSON encoding of int-aggregates can be saved with the 
bottom-right button. If saved as "aggregates.json" in 
the TransferJS directory, this file will be used as the 
dataset for the pixel-shader implementation.
