package ar.app.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class ConcatenateIterator<A> implements Iterator<A>{
	final ArrayList<Iterator<A>> bases;
	
	@SafeVarargs
	public ConcatenateIterator(Iterator<A>... bs) {
		this.bases = new ArrayList<Iterator<A>>(Arrays.asList(bs));

		//Ensure no empty iterators were passed
		for (int i=bases.size()-1;i>=0;i--) {
			if (!bases.get(i).hasNext()) {bases.remove(i);}
		}
	}
	
	public boolean hasNext() {return bases.size()>0;}
	
	@Override
	public A next() {
		A n = bases.get(0).next();
		if (!bases.get(0).hasNext()) {bases.remove(0);}
		return n;
	}

	@Override
	public void remove() {throw new UnsupportedOperationException();}

}
