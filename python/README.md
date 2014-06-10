Organization
-------------

The tools are arranged roughly by data-type.
The core system (drives and interface definitions) is in core.py.
Tools for creating or working with categorical aggregates are found in categories.py.
Tools for working with numerical aggregates are found in numeric.py.

NOTE:  We are experimenting with names...so what is know as 'transfer' in other
  implementations is known as 'shader' in the python implementation.  Verb forms
  change accordingly.

Build and Dependencies
==========

The python distribution includes a setup file.  To install (including c++ extensions)
simply run 'python setup.py install'. Building of the extensions requires c++11 support. 

Examples Applications
---------------------

GUI: python.app arDemo.py
text checkerboard: python ar.py ../data/checkerboard.csv 2 0 1 3 1
text circlepoints: python ar.py ../data/circlepoints.csv 1 2 3 4 .1


