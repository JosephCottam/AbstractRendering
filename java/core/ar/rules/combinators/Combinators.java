package ar.rules.combinators;

import ar.Renderer;
import ar.Transfer;
import ar.glyphsets.implicitgeometry.Valuer;


/** Central location for combinator construction based on functions.
 * 
 * TODO: Investigate a Path transfer that has then/choose/split as methods. 
 */
public class Combinators {
	
	/**Start a sequence**/
	public static Seq.SeqEmpty seq() {return new Seq.SeqEmpty(null);}
	
	/**Start a sequence**/
	public static <IN, OUT> Seq.SeqStub<IN, OUT> seq(Transfer<IN,OUT> t) {return new Seq.SeqStub<>(null, t);}

	
	/**Start a sequence that uses the given renderer.**/
	public static Seq.SeqEmpty seq(Renderer rend) {return new Seq.SeqEmpty(rend);}

	/**Start a sequence that uses the given renderer.**/
	public static <IN,OUT> Seq.SeqStub<IN,OUT> seq(Transfer<IN,OUT> t, Renderer rend) {return new Seq.SeqStub<>(rend, t);}

	
	/**Create a split**/
	public static <IN,L,R,OUT> Transfer<IN,OUT> split(Transfer<IN,L> l, Transfer<IN,R> r, ar.rules.combinators.Split.Merge<L,R,OUT> merge) {
		return new Split<>(l, r, merge);
	}
	
	public static <IN,OUT> If<IN,OUT> choose(Valuer<IN, Boolean> p, Transfer<IN,OUT> consq, Transfer<IN,OUT> alt) {
		return new If<>(p, consq, alt);
	}
	
}
