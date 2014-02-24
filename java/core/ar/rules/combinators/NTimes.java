package ar.rules.combinators;

import ar.Aggregates;
import ar.Transfer;
import ar.Renderer;
import ar.rules.General;

/** Apply an operator N times.
 * 
 * TODO: Develop an item-wise variant... (like 'if')
 * */
public class NTimes<IN> implements Transfer<IN,IN> {
    protected final Transfer<IN,IN> base;
    protected final int n;

    public NTimes(int n, Transfer<IN,IN> base) {
        this.base=base;
        this.n = n;
    }
    public IN emptyValue() {return base.emptyValue();}
    public Specialized<IN> specialize(Aggregates<? extends IN> aggregates) {return new Specialized<>(n, base, aggregates);}

    public static class Specialized<IN> extends NTimes<IN> implements Transfer.Specialized<IN, IN> {
	    final Transfer.Specialized<IN, IN> spec;

		public Specialized(int n, Transfer.Specialized<IN, IN> base) {
			super(n,base);
			this.spec = base;
		}

	    public Specialized(int n, Transfer<IN, IN> base, Aggregates<? extends IN> aggregates) {
			super(n, base);
	    	this.spec = base.specialize(aggregates);
	    }

		@Override
		public Aggregates<IN> process(Aggregates<? extends IN> aggregates, Renderer rend) {
			if (n ==0) {
				//To avoid heap pollution, a copy must be made on zero-iteration N-Times
				//If just a cast is used, then any other references to aggregates could become polluted
				//  if a method that takes the return value of this method modifies the aggregate set.
				IN dv = aggregates.defaultValue();
				return rend.transfer(aggregates, new General.Echo<>(dv));
			} else {
		    	Aggregates<IN> out = rend.transfer(aggregates, spec);
		
		        for (int i=0; i<n-1; i++){  //Did it once to initialize the loop
		        	out = rend.transfer(out, spec);
		        }
		        return out;
			}
		}
    }
}
