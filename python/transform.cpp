/*
  This has been tested on mac using the following compile line:
  clang -O3 -march=native -std=c++11 -fPIC -dynamiclib transform.cpp -o libtransform.dylib

 */


#include <stddef.h>
#include <stdint.h>

#define RESTRICT __restrict__

namespace {

template <typename IT, typename OT>
inline void transf_single(IT tx, IT ty, IT sx, IT sy,
                          IT x, IT y, IT w, IT h,
                          OT& x0, OT& y0, OT& x1, OT& y1)
{
        x0 = x*sx + tx;
        y0 = y*sx + ty;
        x1 = (x + w)*sx + tx;
        y1 = (y + h)*sy + ty;
} 

template <typename INPUT_TYPE, typename OUTPUT_TYPE>
inline void transform(INPUT_TYPE* xtransform, 
                      INPUT_TYPE** in_arrays, 
                      OUTPUT_TYPE * RESTRICT * out_arrays,
                      size_t count)
{
    INPUT_TYPE tx = xtransform[0];
    INPUT_TYPE ty = xtransform[1];
    INPUT_TYPE sx = xtransform[2];
    INPUT_TYPE sy = xtransform[3];

    INPUT_TYPE *x = in_arrays[0];
    INPUT_TYPE *y = in_arrays[1];
    INPUT_TYPE *w = in_arrays[2];
    INPUT_TYPE *h = in_arrays[3];

    OUTPUT_TYPE * RESTRICT x0 = out_arrays[0];
    OUTPUT_TYPE * RESTRICT y0 = out_arrays[1];
    OUTPUT_TYPE * RESTRICT x1 = out_arrays[2];
    OUTPUT_TYPE * RESTRICT y1 = out_arrays[3];

    for (size_t i = 0; i < count; i++)
    {
        transf_single(tx, ty, sx, sy,
                      x[i], y[i], w[i], h[i],
                      x0[i], y0[i], x1[i], y1[i]);
    }
}
}

extern "C" void 
transform_f(float* xtr, float** in, int32_t *RESTRICT* out, size_t count)
{
    transform<float, int32_t>(xtr, in, out, count);
}

extern "C" void 
transform_d(double* xtr, double** in, int32_t * RESTRICT * out, size_t count)
{
    transform<double, int32_t>(xtr, in, out, count);
}
