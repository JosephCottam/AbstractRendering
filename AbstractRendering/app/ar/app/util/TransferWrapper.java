package ar.app.util;

import ar.Transfer;
import ar.app.ARApp;

/**Lightweight transfer wrapper (for non-display items)**/
public class TransferWrapper <A> implements WrappedTransfer<A> {
	private final Transfer<A> transfer;
	private final Class<A> c;
	public TransferWrapper(Transfer<A> t, Class<A> c) {this.transfer=t; this.c = c;}
	public void deselected() {}
	public void selected(ARApp app) {}
	public Transfer<A> op() {return transfer;}
	public Class<A> type() {return c;}
}
