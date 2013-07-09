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

def _projectRects(viewxform, inputs, outputs):
    if (inputs.flags.f_contiguous): 
      inputs = inputs.T
      outputs = outputs.T

    assert(len(inputs.shape) == 2 and inputs.shape[0] == 4)
    assert(inputs.shape == outputs.shape)

    if inputs.dtype == np.float64:
        inputtype = ctypes.c_double
        func = _lib.transform_d
    elif inputs.dtype == np.float32:
        inputtype = ctypes.c_float
        func = _lib.transform_f
    else:
        raise TypeError("ProjectRects only works for np.float32 and np.float64 inputs")

    assert(outputs.dtype == np.int32)

    t = ctypes.POINTER(inputtype)
    cast = ctypes.cast
    c_xforms = (inputtype * 4)(*viewxform)
    c_inputs = (t * 4)(*(cast(inputs[i].ctypes.data, t) 
                         for i in range(0, 4)))
    c_outputs = (t* 4)(*(cast(outputs[i].ctypes.data, t)
                         for i in range(0,4)))
    func(ctypes.byref(c_xforms),
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


def simple_test():
    from time import time

    use_shape = (4, 10**6)
    mock_in = np.random.random(use_shape)
    xform = [3.0, 4.0, 2.0, 2.0]

    t = time()
    out = np.empty(use_shape, dtype=np.int32)
    _projectRects(xform, mock_in, out)
    t = time() - t
    print("c version (double) took %f ms" % (t*1000))
    t = time()
    out2 = np.empty(use_shape, dtype=np.int32)
    _project_element(xform, mock_in, out2)
    t = time() - t
    print("numpy version (double) took %f ms" % (t*1000))

    mock_in = mock_in.astype(np.float32)

    t = time()
    out3 = np.empty(use_shape, dtype=np.int32)
    _projectRects(xform, mock_in, out3)
    t = time() - t
    print("c version (single) took %f ms" % (t*1000))
    t = time()
    out4 = np.empty(use_shape, dtype=np.int32)
    _project_element(xform, mock_in, out4)
    t = time() - t
    print("numpy version (single) took %f ms" % (t*1000))


    if not np.allclose(out, out2):
        for i in xrange(1, use_shape[1]):
            if not np.allclose(out[:,i], out2[:,i]):
                print('%d fails <double>\nc:\n%snp:%s\n' % 
                      (i, str(out[:,i]), str(out2[:,i])))

    if not np.allclose(out3, out4):
        for i in xrange(1, use_shape[1]):
            if not np.allclose(out3[:,i], out4[:,i]):
                print('%d fails <single>\nc:\n%snp:%s\n' % 
                      (i, str(out3[:,i]), str(out4[:,i])))
        

if __name__ == '__main__':
    simple_test()
