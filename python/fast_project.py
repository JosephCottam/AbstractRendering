"""
This module provides alternative methods of performing the 2d
projection needed, they will use the same interface and should be
interchangeable.

all functions will take as inputs a transform and an array of AABB
using floating point numbers. The result will be quantized into
integers.

"""
from __future__ import print_function
from math import floor
import ctypes
import numpy as np

_lib = ctypes.CDLL('libtransform.dylib')

def _project_many(viewxform, inputs, outputs):
    assert(len(inputs.shape) == 2 and inputs.shape[0] == 4)
    assert(inputs.shape == outputs.shape)
    t = ctypes.POINTER(ctypes.c_double)
    cast = ctypes.cast
    c_xforms = (ctypes.c_double * 4)(*viewxform)
    c_inputs = (t * 4)(*(cast(inputs[i].ctypes.data, t) 
                         for i in range(0, 4)))
    c_outputs = (t* 4)(*(cast(outputs[i].ctypes.data, t)
                         for i in range(0,4)))
    _lib.transform_d(ctypes.byref(c_xforms),
                     ctypes.byref(c_inputs),
                     ctypes.byref(c_outputs),
                     inputs.shape[1])


def _project_element(viewxform, inputs, output):
    tx, ty, sx, sy = viewxform
    x, y, w, h = inputs
    x2 = x + w
    y2 = y + h
    np.floor(sx * x + tx, out=output[0,:])
    np.floor(sy * y + ty, out=output[1,:])
    np.floor(sx * x2 + tx, out=output[2,:])
    np.floor(sy * y2 + ty, out=output[3,:])


def fast_project_1(viewxform, boundbox_array):
    pass


def simple_test():
    from time import time

    use_shape = (4, 10**6)
    mock_in = np.random.random(use_shape)
    xform = [3.0, 4.0, 2.0, 2.0]

    t = time()
    out = np.empty(use_shape, dtype=np.int32)
    _project_many(xform, mock_in, out)
    t = time() - t
    print("c version took %f ms" % (t*1000))
    t = time()
    out2 = np.empty(use_shape, dtype=np.int32)
    _project_element(xform, mock_in, out2)
    t = time() - t
    print("numpy version took %f ms" % (t*1000))

    for i in xrange(1, use_shape[1]):
        if not np.allclose(out[:,i], out2[:,i]):
            print('%d fails\nc:\n%snp:%s\n' % (i, str(out[:,i]), str(out2[:,i])))
    

if __name__ == '__main__':
    simple_test()
