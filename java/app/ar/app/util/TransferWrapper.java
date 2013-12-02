package ar.app.util;

import ar.Transfer;
import ar.app.display.ARComponent;

/**Lightweight transfer wrapper (for non-display items)**/
public class TransferWrapper <A,B> implements WrappedTransfer<A,B> {
	private final Transfer<A,B> transfer;
	private final Class<?> in,out;
	public TransferWrapper(Transfer<A,B> t, Class<?> in, Class<?> out) {
		this.transfer=t;
		this.in = in;
		this.out = out;
	}
	
	public void deselected() {}
	public void selected(ARComponent.Holder app) {}
	public Transfer<A,B> op() {return transfer;}
	public Class<?> input() {return in;}
	public Class<?> output() {return out;}
}
