/*
  This has been tested on mac using the following compile line:
  clang -O3 -march=native -std=c++11 -fPIC -dynamiclib transform_libdispatch.cpp -o libtransform_libdispatch.dylib

 */


#include <stddef.h>
#include <stdint.h>
#include <alloca.h>
#include <algorithm>
#include <dispatch/dispatch.h>

#define RESTRICT __restrict__

#define STRIDE (1<<4)

namespace {

typedef struct _TRANSFORM_PARAMS_T
{
    void *xtr;
    void **in;
    void ** RESTRICT out;
    size_t count;
} transform_params_t;

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
void transform_task(void* arg, size_t idx)
{
    const transform_params_t* t = reinterpret_cast<const transform_params_t*>(arg);

    INPUT_TYPE const * const xtr = reinterpret_cast<INPUT_TYPE* const>(t->xtr);
    INPUT_TYPE const * const * const in = reinterpret_cast<INPUT_TYPE const * const * const>(t->in);
    OUTPUT_TYPE * RESTRICT const * const out = reinterpret_cast<OUTPUT_TYPE * RESTRICT const * const>(t->out);

    size_t count = t->count;

    count = (idx != count/STRIDE) ? STRIDE : count % STRIDE;
    size_t off = idx*STRIDE;
    const INPUT_TYPE tx = xtr[0];
    const INPUT_TYPE ty = xtr[1];
    const INPUT_TYPE sx = xtr[2];
    const INPUT_TYPE sy = xtr[3];

    INPUT_TYPE const *x = in[0] + off;
    INPUT_TYPE const *y = in[1] + off;
    INPUT_TYPE const *w = in[2] + off;
    INPUT_TYPE const *h = in[3] + off;

    OUTPUT_TYPE * RESTRICT x0 = out[0] + off;
    OUTPUT_TYPE * RESTRICT y0 = out[1] + off;
    OUTPUT_TYPE * RESTRICT x1 = out[2] + off;
    OUTPUT_TYPE * RESTRICT y1 = out[3] + off;

    for (size_t i = 0; i < count; i++)
    {
        transf_single(tx, ty, sx, sy,
                      x[i], y[i], w[i], h[i],
                      x0[i], y0[i], x1[i], y1[i]);
    }
}


template <typename IN_T, typename OUT_T>
inline void transform_dispatch(IN_T *xtr,
                               IN_T **in,
                               OUT_T **RESTRICT out,
                               size_t offset,
                               size_t count,
                               size_t task_size)
{
    size_t task_count = (count + task_size - 1) / task_size;
    transform_params_t params;

    IN_T* in_buffs[4];

    for (size_t i = 0; i < 4; i++)
    {
        in_buffs[i] = in[i] + offset;
    }


    dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0);

    params.xtr = reinterpret_cast<void*>(xtr);
    params.in = reinterpret_cast<void**>(&in[0]);
    params.out = reinterpret_cast<void**>(out);
    params.count = count;

    dispatch_apply_f(count/STRIDE, queue, &params, transform_task<IN_T, OUT_T>);
}                                                                 

}

extern "C" void 
transform_f(float* xtr,
            float** in,
            int32_t** RESTRICT out,
            size_t offset,
            size_t count)
{
    transform_dispatch<float, int32_t>(xtr, in, out, offset, count, 2<<13);
}

extern "C" void 
transform_d(double* xtr,
            double** in,
            int32_t** RESTRICT out,
            size_t offset,
            size_t count)
{
    transform_dispatch<double, int32_t>(xtr, in, out, offset, count, 2<<13);
}
