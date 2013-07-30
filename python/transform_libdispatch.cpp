/*
  This has been tested on mac using the following compile line:
  clang -O3 -march=native -std=c++11 -fPIC -dynamiclib transform_libdispatch.cpp -o libtransform_libdispatch.dylib

 */

#include <iostream>
#include <algorithm>
#include <dispatch/dispatch.h>

#define RESTRICT __restrict__

#define STRIDE (1<<4)

namespace {
    template <typename IT>
    inline void 
    transf_single(IT tx, IT ty, IT sx, IT sy,
                  IT x, IT y, IT w, IT h,
                  int32_t& x0, int32_t& y0, int32_t& x1, int32_t& y1)
    {
        x0 = x*sx + tx;
        y0 = y*sx + ty;
        x1 = (x + w)*sx + tx;
        y1 = (y + h)*sy + ty;
    } 

    ////////////////////////////////////////////////////////////////////////

    template <typename IT>
    struct transform_params_t
    {
        IT xtr[4];
        IT const *in[4];
        int32_t * RESTRICT out[4];
        size_t in_offset;
        size_t out_offset;
        size_t count;
    };

    template <typename IT>
    std::ostream& operator<<(std::ostream& ost, const transform_params_t<IT>& obj)
    {
        return ost << "transform params" << std::endl
                   << "xtr: " << obj.xtr[0] << ", " << obj.xtr[1] << ", " 
                   << obj.xtr[2] << ", " << obj.xtr[3] << std::endl
                   << "in_offset: " << obj.in_offset << std::endl
                   << "out_offset: " << obj.out_offset << std::endl
                   << "count: " << obj.count << std::endl;
    }
    ////////////////////////////////////////////////////////////////////////

    template <typename IN_T>
    void 
    transform(const transform_params_t<IN_T>* arg,
              size_t idx,
              size_t stride)
    {

        size_t count = arg->count;
        count = (idx != count/stride) ? stride : count % stride;
        size_t off = idx*stride;
        
        const IN_T tx = arg->xtr[0];
        const IN_T ty = arg->xtr[1];
        const IN_T sx = arg->xtr[2];
        const IN_T sy = arg->xtr[3];
        
        IN_T const *x = arg->in[0] + arg->in_offset + off;
        IN_T const *y = arg->in[1] + arg->in_offset + off;
        IN_T const *w = arg->in[2] + arg->in_offset + off;
        IN_T const *h = arg->in[3] + arg->in_offset + off;
        
        int32_t * RESTRICT x0 = arg->out[0] + arg->out_offset + off;
        int32_t * RESTRICT y0 = arg->out[1] + arg->out_offset + off;
        int32_t * RESTRICT x1 = arg->out[2] + arg->out_offset + off;
        int32_t * RESTRICT y1 = arg->out[3] + arg->out_offset + off;
        
        for (size_t i = 0; i < count; i++)
        {
            transf_single(tx, ty, sx, sy,
                          x[i], y[i], w[i], h[i],
                          x0[i], y0[i], x1[i], y1[i]);
        }
    }

    inline dispatch_queue_t 
    get_queue()
    {
        return dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0);
    }

    class semaphore
    {
    public:
        semaphore(long count);
        ~semaphore();
        void wait();
        void signal();
    private:
        dispatch_semaphore_t impl;
    };

    semaphore::semaphore(long count)
    {
        impl = dispatch_semaphore_create(count);
    }

    semaphore::~semaphore()
    {
        dispatch_release(impl);
    }

    void semaphore::wait()
    {
        dispatch_semaphore_wait(impl, DISPATCH_TIME_FOREVER);
    }

    void semaphore::signal()
    {
        dispatch_semaphore_signal(impl);
    }
    
    ////////////////////////////////////////////////////////////////////////

    template <typename IT>
    struct async_context_t
    {
        semaphore sema;
        transform_params_t<IT> tp;
        size_t chunk_size;
        size_t current_chunk;
        size_t total_size;
        bool task_running;

        async_context_t();
        ~async_context_t();
        bool init(IT* _xtr, IT** _in,size_t _count,size_t _cs);
        void alloc_buffer();
        void next(void** buff, size_t* current_count);

        static void task(void*);
    };

    template <typename IT>
    async_context_t<IT>::async_context_t():
        sema(1)
    {
        tp.out[0]=nullptr;
    }

    template <typename IT>
    async_context_t<IT>::~async_context_t()
    {
        free(tp.out[0]);
    }

    template <typename IT>
    void
    async_context_t<IT>::next(void** return_buff, size_t* count)
    {
        *return_buff = nullptr;
        *count = 0;

        if (task_running) {
            sema.wait();
            size_t done_chunk = current_chunk - 1;
            *return_buff = tp.out[0] + 4 * chunk_size * (done_chunk & 1);
            *count = std::min(chunk_size, total_size -
                              (done_chunk) * chunk_size);
            task_running = false;
        }

        // if not all the chunks have been processed, start a new task
        size_t curr_offset = current_chunk*chunk_size;
        if (curr_offset < total_size) {
            tp.in_offset = curr_offset;
            tp.out_offset = 4*(current_chunk & 1) * chunk_size;
            tp.count = std::min(chunk_size, total_size - curr_offset);
            dispatch_async_f(get_queue(), this, task);
            current_chunk++;
            task_running = true;
        }
    }

    template <typename IT>
    void
    async_context_t<IT>::task(void* arg)
    {
        async_context_t<IT>* ctxt = (async_context_t<IT>*) arg;
        transform(&ctxt->tp, 0, ctxt->tp.count);
        ctxt->sema.signal();
    }

    template <typename IT>
    bool
    async_context_t<IT>::init(IT *_xtr, IT** _in, 
                              size_t _total, size_t _cs)
    {
        int32_t* buff = (int32_t*) malloc(4*2*_cs*sizeof(int32_t));

        if (!buff)
            return false;

        chunk_size = _cs;
        current_chunk = 0;
        total_size = _total;
        
        // these 3 will be updated for each task run
        tp.in_offset = 0;
        tp.in_offset = 0;
        tp.count = 0;

        for (size_t i=0;i<4;i++) {
            tp.xtr[i] = _xtr[i];
            tp.in[i] = _in[i];
            tp.out[i] = buff + _cs*i;
        }

        task_running = false;

        void *dummy;
        size_t dummy2;
        next(&dummy, &dummy2);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////
    template <typename IN_T>
    void
    transform_task(void* ctxt, size_t idx)
    {
        transform<IN_T>((const transform_params_t<IN_T>*)ctxt, idx, STRIDE);
    }

    template <typename IN_T, typename OUT_T>
    void transform_dispatch(IN_T *xtr,
                            IN_T **in,
                            OUT_T **RESTRICT out,
                            size_t offset,
                            size_t count)
    {
    }                                                                 
} // namespace

extern "C" void 
transform_f(float* xtr,
            float** in,
            int32_t** RESTRICT out,
            size_t offset,
            size_t count)
{
    transform_params_t<float> params;

    for (size_t i=0; i<4;i++)
    {
        params.xtr[i] = xtr[i];
        params.in[i] = in[i] + offset;
        params.out[i] = out[i];
    }
    params.in_offset = offset;
    params.out_offset = offset;
    params.count = count;
        
    dispatch_queue_t queue = get_queue();
        
    dispatch_apply_f(count/STRIDE, queue, &params, transform_task<float>);
}

extern "C" void 
transform_d(double* xtr,
            double** in,
            int32_t** RESTRICT out,
            size_t offset,
            size_t count)
{
    transform_params_t<double> params;

    for (size_t i=0; i<4;i++)
    {
        params.xtr[i] = xtr[i];
        params.in[i] = in[i] + offset;
        params.out[i] = out[i];
    }
    params.in_offset = offset;
    params.out_offset = offset;
    params.count = count;
        
    dispatch_queue_t queue = get_queue();
        
    dispatch_apply_f(count/STRIDE, queue, &params, transform_task<double>);
}

extern "C"
void *
async_transform_f_start(float* xtr,
                        float** in,
                        size_t total,
                        size_t chunk_size)
{
    async_context_t<float>* rv = new async_context_t<float>();

    if (! rv->init(xtr, in, total, chunk_size)) {
        delete(rv);
        rv = nullptr;
    }

    return rv;
}

extern "C"
void
async_transform_f_end(void *arg)
{
    async_context_t<float> *ctxt = (async_context_t<float>*) arg;
    delete(ctxt);
}

extern "C"
void
async_transform_f_next(void *arg, void** buff_out, size_t* count_out)
{
    async_context_t<float> *ctxt = (async_context_t<float>*)arg;
    ctxt->next(buff_out, count_out);
}

extern "C"
void *
async_transform_d_start(double *xtr,
                        double **in,
                        size_t total,
                        size_t cs)
{
    async_context_t<double>* rv = new async_context_t<double>();

    if (! rv->init(xtr, in, total, cs)) {
        delete(rv);
        rv = nullptr;
    }
    
    return rv;
}

extern "C"
void
async_transform_d_end(void *arg)
{
    async_context_t<double> *ctxt = (async_context_t<double>*)arg;
    delete(ctxt);
}

extern "C"
void
async_transform_d_next(void *arg, void** buff_out, size_t* count_out)
{
    async_context_t<double> *ctxt = (async_context_t<double>*)arg;
    ctxt->next(buff_out, count_out);
}

