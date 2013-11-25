/**Transfer combinators allow complex transfer functions to be constructed out of simpler ones.
 * 
 * NOTE: In many cases, correct transfer function semantics require these functions to compute
 * and retain an intermediate state.  IF you are using these functions to create complex 
 * equations, building a complex VALUER first and then wrapping that as the transfer is better.
 * **/
package ar.rules.combinators;