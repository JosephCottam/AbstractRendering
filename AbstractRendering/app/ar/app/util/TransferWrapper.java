package ar.app.util;

import ar.Transfer;
import ar.app.ARApp;

/**Lightweight transfer wrapper (for non-display items)**/
public class TransferWrapper <A,B> implements WrappedTransfer<A,B> {
	private final Transfer<A,B> transfer;
	public TransferWrapper(Transfer<A,B> t) {this.transfer=t;}
	public void deselected() {}
	public void selected(ARApp app) {}
	public Transfer<A,B> op() {return transfer;}
}
