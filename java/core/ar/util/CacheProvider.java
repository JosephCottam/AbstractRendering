package ar.util;

import ar.Aggregates;
import ar.Renderer;

/**Utility class to provide results caching in complex transfer functions.*/
public class CacheProvider<IN,OUT> {
    private final Object guard = new Object() {};
    private Aggregates<OUT> cache;
    private Aggregates<? extends IN> key;
    private final CacheTarget<IN,OUT> target;

    public CacheProvider(final CacheTarget<IN,OUT> target) {this.target = target;}

    public Aggregates<OUT> get(Aggregates<? extends IN> key, Renderer rend) {
        synchronized(guard) {
            if (this.key != key) {
                cache = target.build(key, rend);
                this.key =key;
            }
        }
        return cache;
    }
    public void set(Aggregates<? extends IN> key, Aggregates<OUT> cache) {
        synchronized (guard) {
            this.key = key;
            this.cache = cache;
        }
    }

    public static interface CacheTarget<IN,OUT> {
        public Aggregates<OUT> build(Aggregates<? extends IN> aggs, Renderer rend);
    }
}
