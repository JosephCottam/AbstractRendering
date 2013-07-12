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

namespace {

typedef struct _TRANSFORM_PARAMS_T
{
    void *xtr;
    void *in[4];
    void * RESTRICT out[4];
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
void transform_task(void* arg)
{
    const transform_params_t* t = reinterpret_cast<const transform_params_t*>(arg);
    INPUT_TYPE const * const xtransform = reinterpret_cast<INPUT_TYPE* const>(t->xtr);
    INPUT_TYPE const * const * const in_arrays = reinterpret_cast<INPUT_TYPE const * const * const>(&t->in[0]);
    OUTPUT_TYPE * RESTRICT const * const out_arrays = reinterpret_cast<OUTPUT_TYPE * RESTRICT const * const>(&t->out[0]);
    size_t count = t->count;

    const INPUT_TYPE tx = xtransform[0];
    const INPUT_TYPE ty = xtransform[1];
    const INPUT_TYPE sx = xtransform[2];
    const INPUT_TYPE sy = xtransform[3];

    INPUT_TYPE const *x = in_arrays[0];
    INPUT_TYPE const *y = in_arrays[1];
    INPUT_TYPE const *w = in_arrays[2];
    INPUT_TYPE const *h = in_arrays[3];

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


template <typename IN_T, typename OUT_T>
inline void transform_dispatch(IN_T *xtr,
                               IN_T **in,
                               OUT_T **RESTRICT out,
                               size_t count,
                               size_t task_size)
{
    size_t task_count = (count + task_size - 1) / task_size;
    transform_params_t *params = reinterpret_cast<transform_params_t*>(alloca(sizeof(transform_params_t) * task_count));

    dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);

    dispatch_group_t group = dispatch_group_create();
    for (size_t i = 0; i < task_count; i++)
    {
        params[i].xtr = xtr;
        for (size_t j = 0; j < 4; j++)
        {
            params[i].in[j] = in[j] + (i*task_size);
            params[i].out[j] = out[j] + (i*task_size);
        }

        params[i].count = std::min(task_size, count - i*task_size);
        dispatch_group_async_f(group, queue, &params[i], transform_task<IN_T, OUT_T>);       
    }

    dispatch_group_wait(group, DISPATCH_TIME_FOREVER);
    dispatch_release(group);
}                                                                 

}

extern "C" void 
transform_f(float* xtr,
            float** in,
            int32_t** RESTRICT out,
            size_t count)
{
    transform_dispatch<float, int32_t>(xtr, in, out, count, 2<<13);
}

extern "C" void 
transform_d(double* xtr,
            double** in,
            int32_t** RESTRICT out,
            size_t count)
{
    transform_dispatch<double, int32_t>(xtr, in, out, count, 2<<13);
}
