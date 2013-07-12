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

try:
    _lib_dispatch = ctypes.CDLL('libtransform_libdispatch.dylib')
except OSError:
    print ("no libdispatch version found")
    _lib_dispatch = _lib

def _projectRects(viewxform, inputs, outputs, use_dispatch = False):
    if (inputs.flags.f_contiguous): 
      inputs = inputs.T
      outputs = outputs.T

    assert(len(inputs.shape) == 2 and inputs.shape[0] == 4)
    assert(inputs.shape == outputs.shape)

    use_lib = _lib_dispatch if use_dispatch else _lib

    if inputs.dtype == np.float64:
        inputtype = ctypes.c_double
        func = use_lib.transform_d
    elif inputs.dtype == np.float32:
        inputtype = ctypes.c_float
        func = use_lib.transform_f
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


def report_diffs(a, b, name):
    last_dim = a.shape[1]
    if not np.allclose(a, b):
        for i in xrange(1, last_dim):
            if not np.allclose(a[:,i], b[:,i]):
                print('%s::%d fails \nc:\n%s != %s\n' % 
                      (name, i, str(a[:,i]), str(b[:,i])))

def simple_test():
    from time import time

    use_shape = (4, 10**6)
    mock_in = np.random.random(use_shape)
    xform = [3.0, 4.0, 2.0, 2.0]

    out = np.zeros(use_shape, dtype=np.int32)

    t = time()
    _projectRects(xform, mock_in, out, use_dispatch=True)
    t = time() - t
    out0 = np.copy(out)
    print("c version - libdispatch (double) took %f ms" % (t*1000))

    t = time()
    _projectRects(xform, mock_in, out)
    t = time() - t
    out1 = np.copy(out)
    print("c version (double) took %f ms" % (t*1000))

    t = time()
    _project_element(xform, mock_in, out)
    t = time() - t
    out2 = np.copy(out)
    print("numpy version (double) took %f ms" % (t*1000))

    mock_in = mock_in.astype(np.float32)

    t = time()
    _projectRects(xform, mock_in, out, use_dispatch=True)
    t = time() - t
    out3 = np.copy(out)
    print("c version - libdispatch (single) took %f ms" % (t*1000))

    t = time()
    _projectRects(xform, mock_in, out)
    t = time() - t
    out4 = np.copy(out)
    print("c version (single) took %f ms" % (t*1000))

    t = time()
    _project_element(xform, mock_in, out)
    t = time() - t
    out5 = np.copy(out)
    print("numpy version (single) took %f ms" % (t*1000))

    report_diffs(out0, out2, "libdispatch (double)")
    report_diffs(out1, out2, "plain C (double)")
    report_diffs(out3, out5, "libdispatch (single)")
    report_diffs(out4, out5, "plain C (single)")
         

if __name__ == '__main__':
    simple_test()
