"""
This module provides alternative methods of performing the 2d
projection needed, they will use the same interface and should be
interchangeable.

all functions will take as inputs a transform and an array of AABB
using floating point numbers. The result will be quantized into
integers.

"""
from __future__ import print_function
import ctypes
import numpy as np
import os

def _type_lib(lib):
    from ctypes import c_void_p, c_size_t

    lib.transform_f.argtypes = [c_void_p, c_void_p, c_void_p, c_size_t, c_size_t]
    lib.transform_d.argtypes = [c_void_p, c_void_p, c_void_p, c_size_t, c_size_t]

    if hasattr(lib, "async_transform_f_start"):
        #assume all the async interface
        lib.async_transform_f_start.argtypes = [c_void_p, c_void_p, 
                                                c_size_t, c_size_t]
        lib.async_transform_f_start.restype = c_void_p
        lib.async_transform_f_end.argtypes = [c_void_p]
        lib.async_transform_f_next.argtypes = [c_void_p, c_void_p, c_void_p]
        lib.async_transform_d_start.argtypes = [c_void_p, c_void_p, 
                                                c_size_t, c_size_t]
        lib.async_transform_d_start.restype = c_void_p
        lib.async_transform_d_end.argtypes = [c_void_p]
        lib.async_transform_d_next.argtypes = [c_void_p, c_void_p, c_void_p]

_lib = ctypes.CDLL(os.path.join(os.path.dirname(__file__), 'transform.so'))
_type_lib(_lib)

try:
    _lib_dispatch = ctypes.CDLL(os.path.join(os.path.dirname(__file__), 'transform_libdispatch.so'))
    _type_lib(_lib_dispatch)
except OSError:
    print ("no libdispatch version found")
    _lib_dispatch = _lib

mk_buff = ctypes.pythonapi.PyBuffer_FromMemory
mk_buff.restype = ctypes.py_object

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
         0,
         inputs.shape[1])
        

def _projectRectsGenerator(viewxform,
                           inputs,
                           chunk_size,
                           use_dispatch = False):
    if (inputs.flags.f_contiguous):
        inputs = inputs.T
        outputs = outputs.T

    assert(len(inputs.shape) == 2 and inputs.shape[0] == 4)
    result_size = inputs.shape[1]
    chunk_size = min(result_size, chunk_size)

    use_lib = _lib_dispatch if use_dispatch else _lib
    if inputs.dtype == np.float64:
        inputtype = ctypes.c_double
        func = use_lib.transform_d
        has_async = hasattr(use_lib, "async_transform_d_start")
    elif inputs.dtype == np.float32:
        inputtype = ctypes.c_float
        func = use_lib.transform_f
        has_async = hasattr(use_lib, "async_transform_f_start")
    else:
        raise TypeError("ProjectRects only works for np.float32 and np.float64 inputs")

    t = ctypes.POINTER(inputtype)
    cast = ctypes.cast
    c_xforms = (inputtype * 4)(*viewxform)
    c_inputs = (t*4)(*(cast(inputs[i].ctypes.data, t)
                       for i in range(0,4)))

    if not has_async:
        print("no async")
        # simple case: no need to pipeline
        outputs = np.empty((4, chunk_size), dtype=np.int32)
        c_outputs = (t*4)(*(cast(outputs[i].ctypes.data, t)
                            for i in range(0, 4)))
        offset = 0
        while offset != result_size:
            curr_size = min(result_size - offset, chunk_size)
            func(ctypes.byref(c_xforms),
                 ctypes.byref(c_inputs),
                 ctypes.byref(c_outputs),
                 offset,
                 curr_size)
            offset += curr_size
            yield outputs[0:curr_size]
    else:
        print("async")
        if inputs.dtype == np.float64:
            start_function = use_lib.async_transform_d_start
            end_function  = use_lib.async_transform_d_end
            next_function  = use_lib.async_transform_d_next
        elif inputs.dtype == np.float32:
            start_function = use_lib.async_transform_f_start
            end_function  = use_lib.async_transform_f_end
            next_function  = use_lib.async_transform_f_next
        else:
            raise TypeError("ProjectRects only works for np.float32 and np.float64 inputs")

        buff = ctypes.c_void_p(0)
        count = ctypes.c_size_t(0)
        total_size = chunk_size * 4 * 4
        token = start_function(ctypes.byref(c_xforms), ctypes.byref(c_inputs),
                               ctypes.c_size_t(result_size),
                               ctypes.c_size_t(chunk_size))

        while True:
            next_function(token, ctypes.byref(buff), ctypes.byref(count))
            if count.value == 0:
                break

            yield np.frombuffer(mk_buff(buff, total_size),
                                dtype='i4').reshape((4,chunk_size))[:,0:count.value]

        end_function(token)

# testing code starts here

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

    use_shape = (4, 10**8)
    chunksize = 10**4
    mock_in = np.random.random(use_shape)
    xform = [3.0, 4.0, 2.0, 2.0]

    t = time()
    check_sum = 0
    for arr in _projectRectsGenerator(xform, mock_in, chunksize):
        check_sum += arr.shape[1]
        np.random.random(1000)
    t = time() - t
    print("checksum (_projectRectsGenerator) took %f ms" % (t*1000))
    chk1 = check_sum

    t = time()
    check_sum = 0
    for arr in _projectRectsGenerator(xform, mock_in, chunksize, use_dispatch = True):
        check_sum += arr.shape[1]
        np.random.random(1000)

    t = time() - t
    print("checksum (_projectRectsGenerator libdispatch) took %f ms" % (t*1000))
    chk2 = check_sum

    t = time()
    out = np.empty(use_shape, dtype=np.int32)
    res = _projectRects(xform, mock_in, out)
    t = time() - t
    print("checksum (_projectRects) took %f ms" % (t*1000))

    if not (chk1 == chk2 == out.shape[1]):
        print('checksums diverge')
        print('%s == %s' % ('chk1', chk1)) 
        print('%s == %s' % ('chk2', chk2))

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
