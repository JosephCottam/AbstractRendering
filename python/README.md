Organization
-------------

The tools are arranged roughly by data-type.
The core system (drives and interface definitions) is in ar.py.
Tools for creating or working with categorical aggregates are found in categories.py.
Tools for working with numerical aggregates are found in numeric.py.

Build and Dependencies
==========

Abstract Rendering relies on a c-module that can be built as:
clang -O3 -march=native -fPIC -dynamiclib transform.cpp -o libtransform.dylib

Additionally, the system works best if Numba is also installed (though it is optional).  


Examples Applications
---------------------

GUI: python.app arDemo.py
text checkerboard: python ar.py ../data/checkerboard.csv 2 0 1 3 1
text circlepoints: python ar.py ../data/circlepoints.csv 1 2 3 4 .1



Future Work
------------

Implicit geometry using ColumnDataSource and a few functions
* One functions for location
* One function for color
* Works better with the aggregate-reducers and glyph-based iteration (as opposed to pixel-based)

