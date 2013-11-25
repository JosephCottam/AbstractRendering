package ar.util;

import ar.Aggregates;

/**Utility class to provide results caching in complex transfer functions.*/
public class CacheProvider<IN,OUT> {
    private final Object guard = new Object() {};
    private Aggregates<? extends OUT> cache;
    private Aggregates<? extends IN> key;
    private final CacheTarget<IN,OUT> target;

    public CacheProvider(final CacheTarget<IN,OUT> target) {this.target = target;}

    public Aggregates<? extends OUT> get(Aggregates<? extends IN> key) {
        synchronized(guard) {
            if (this.key != key) {
                cache = target.build(key);
                this.key =key;
            }
        }
        return cache;
    }
    public void set(Aggregates<? extends IN> key, Aggregates<? extends OUT> cache) {
        synchronized (guard) {
            this.key = key;
            this.cache = cache;
        }
    }

    public static interface CacheTarget<IN,OUT> {
        public Aggregates<? extends OUT> build(Aggregates<? extends IN> aggs);
    }
}
