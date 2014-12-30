package ar.test;

import java.util.Iterator;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import ar.Aggregates;

/**Are the values returned by two iterables pair-wise equal?**/
public class AllEqual<T> extends TypeSafeMatcher<Iterable<T>> {
	private final Iterable<T> ref;
	
	public AllEqual(Aggregates<T> ref) {this.ref = ref;}
	
	@Override
	public void describeTo(Description msg) {
		msg.appendText("Does not match");
		msg.appendValue(ref);
	}

	@Override
	protected boolean matchesSafely(Iterable<T> item) {
		Iterator<T> ref_vals = ref.iterator();
		Iterator<T> res_vals = ref.iterator();
		
		while (ref_vals.hasNext() && res_vals.hasNext()) {
			T ref_v = ref_vals.next();
			T res_v = res_vals.next();
			
			if (!ref_v.equals(res_v)) {return false;}
		}
		return true;
	}

}
