ARServer
===========

The AR Server is a self-contained HTTP server that can return images or JSON.
Using an ARExt.jar file built using "ant fetch-ext ext" to get the extensions, the server can be executed with
"java -jar ARExt.jar ar.ext.server.ARServer".  

Optional configuration parameters are -host and -port.

The server uses the ARLang extension for configuration.  
The server home-page includes details on the many URL-defined runtime options. 

###Server-side caching

The Server-side caching follows a "lazy" tiling strategy for aggregates.
Tiles are created from the source data on first request and served from cache thereafter.
For larger data sets, this can save time as the system only needs to read the tiles from disk (typically a few megabytes per tile).

Even though tiles are created lazily, there are a few things to be aware of.
A tiles is addressed by their zoom level an a position in a global coordinate space.
The zoom level is determined by the view transform.  
Zoom level is the concatenation of the x-scale and y-scale factors.
To prevent floating-point rounding from causing new zoom-levels during normal variation, 
	the scale factor is truncated at four digits after the decimal.

Tile file names go as such: <dataset-name>/<aggregator-name>/<scale-x>/<scale-y>/<top-left-x>__<top-left-y>__<width>__<height>.avsc.
The width and height are not *strictly* required but they help for some sanity checks.
There are potential issues with this naming scheme  (e.g., dataset or aggregator names may clash) but it works for controlled environments. 

Server side caching is controlled at startup with three parameters:
* -cache <directory> : Sets the cache directory (default is "./cache")
* -clearCache <True/False>: Delete the contents of the cache directory at startup, default is false.
* -tile <size>: How large should the cached tiles be made (default is 1000).


### Server language function
The AR server provides some additional operators in ARL. 

* *(dynSpread <targetPercent>)* Transfer function that spreads based on the observed percent of non-empty bins.
The spreading is circular with a radius that if all points were evenly distributed, the total
 percent would be near the target percent.  Its impossible to guarantee the target percent given
 that the radius must always be an integer. Additionally, the actual data distribution may be clumpy,
 resulting in a slightly lower overall bin coverage.

* *(dynScale <characteristic-zoom> <damp-factor>)* Expand-time function that computes the ratio between the characteristic
zoom and the actual zoom then applies the damping factor.  This can be used to do dynamic spreading per (spread(circle(dynScale,1))).
The result is similar to dynSpread, but more difficult to get right (you need to know something about the dataset).
dynScale is more flexible and faster than dynSpread.  One way to discover the 'characteristic zoom' is to render the dataset
at a zoom that looks good with a spread of 0, and include '(print(vt,sx))' in the transfer sequence.

* *(vt <field>)* Return part of the view transform. 'field is one of sx,sy,tx,ty.
* *(print <msg>)* Transfer thatt prints out the message at execution time, returns the aggregates passed in.
