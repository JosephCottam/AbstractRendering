package ar.util.combinators;

import ar.Aggregates;

/**Simple true/false test.
 *
 * TODO: Provide a 'valuer' wrapper for boolean valuers
 * TODO: Provide 'all' and 'some' utilities for applying a single-test to all aggregates.
 * @param <IN>
 */
public interface Predicate<IN> {
    public boolean test(IN arg);
}
