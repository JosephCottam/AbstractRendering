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
