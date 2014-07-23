Abstract Rendering
======

Status: [![Build Status](https://travis-ci.org/JosephCottam/AbstractRendering.svg?branch=master)](https://travis-ci.org/JosephCottam/AbstractRendering)


Information visualization rests on the idea that a meaningful relationship
can be drawn between pixels and data.  This is most often mediated by
geometric entities (such as circles, squares and text) but always involves
pixels eventually to display.  In most systems, the pixels are tucked away
under levels of abstraction in the rendering system.  Abstract Rendering
takes the opposite approach: expose the pixels and gain powerful pixel-level
control.  This pixel-level power is a complement many existing visualization
techniques.  It is an elaboration on rendering, not an analytic or projection step,
so it can be used as epilogue to many existing techniques.


In standard rendering, geometric objects are projected to an image and 
represented on that image's discrete pixels.  The source space is a
canvas that contains logically continuous geometric primitives 
and the target space is an image that contains discrete colors.
Abstract Rendering fits between these two states.  It introduces
a discretization of the data at the pixel-level, but not necessarily all
the way to colors.  This enables many pixel-level concerns to be efficiently 
and concisely captured.

Sample images can be found in [the wiki](https://github.com/JosephCottam/AbstractRendering/wiki).

NOTE: The python version of abstract rendering has moved to 
[its own repository](https://github.com/ContinuumIO/abstract_rendering).


Pixel-Level Effects (an example)
-----------

Consider a scatterplot with many overlapping items.
The classic technique is to use alpha composition to ensure that some
of the density information is conveyed in the overplot regions.
However, alpha composition (as it typically implemented) can silently saturate,
then further overplotting is lost.  (Most systems saturate their alpha channels
around 255 elements when the minimum alpha level is used, significantly fewer
for more common alpha levels).

Taking a step back, the alpha composition is essentially trying to communicate
how many items touch each pixel.  Instead of directly converting to colors
(and composing them), why not count the items that touch each pixel and then transform those counts into
colors directly?  Over-saturation can be completely avoided because the whole
data range can be known and interpolated over.  Additionally, the minimum
value can also be set to ensure visibility of even a single element.

Abstract rendering provides the tools to define the intermediate stage
of "counting the items that touch each pixel" and for "transforming those counts into colors."
These two stages are encapsulated as "aggregation" and "transfer" respectively.

The Framework
---------

An abstract rendering program looks like a collection of four functions combined into two equations.
These equations operate on 'glyphs.'
Glyphs are geometric descriptions with accompanying data annotations.  
The data annotations may be a color or category label or other source-data values.  
Conceptually, the framework creates a bin for each pixel, aggregates the values from a
 glyph-set into that bin and then transforms the bin values into pixels.

The first equation is the reducer.
The reducer determines a value for a single bin (corresponding to a single pixel).
Its component functions are:
* Selector: Determines which glyphs to consider for a particular bin.  The most common
  selector is 'contains the target pixel' (though 'neighbors the target pixel' is also useful).
* Info: Given a single glyph from the selected set, return a data value.
  This is often the color or category that accompanies the geometry in the glyph, 
  but for counts it is simply a function that returns the number 1.
* Aggregator: Given a set of information values, produce a single value to place into the aggregates set.


The second equation in abstract rendering converts the aggregate set into
an image by transforming the bin-values produced durring reductino into colors.
This equation is a direct derivation of the 'transfer function' seen in 
more traditional scientific visualization, and is also called a 'transfer function.'
In many cases, there is only one stage to transfer (directly converting from the
 aggregate values to colors).  However, in other cases it is useful to do several
 smaller transformations.  The transformations after aggregation are collectively 
 called 'the transfer function' regardless of the number of functions actually applied. 
Much of the power of the transfer function is derived from the fact that it has all of the 
aggregates already available.  Therefore, it can perform operations
with knowledge of the global distribution of values, of a neighborhood of values
or just a single value.


Extensions
--------

The basic system described above can be extended in a variety of ways.
This repository represents investigation into efficient and
convenient ways to work with such pixel-level intermediates.
It explores multiple ways to store and access data, including
parallelization strategies, out-of-core data access, distributed rendering
and interfacing with preexisting data structures.  In all, the variety of options
leads to many efficient (and some inefficient) ways of realizing the
system described above.


----------
#### Organizational Note

This repository is divided into sections based on implementation.  The "java" directory
contains a Java implementation of the system described above.  README.md in that directory
describes building the system and execution of an example application.  JavaDocs
can also be generated to describe the API.

The "TransferJS" directory demonstrates a JavaScript and WebGL implementation of 
the transfer function.  It is a proof-of-concept for one form of distributed
rendering supported by the Abstract Rendering system.  The readme in that directory
also describes how to use this demonstration application.

The python version of abstract rendering has moved to 
[its own repository](https://github.com/ContinuumIO/abstract_rendering).

