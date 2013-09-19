package ar.app.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

/**Utility for producing action events.
 * 
 * Keeps a list of ActionListeners;  Fires them off when asked to.
 * **/
public class ActionProvider {
	private List<ActionListener> listeners = new ArrayList<ActionListener>();
	private int count;

	public void addActionListener(ActionListener l) {listeners.add(l);}
	public void fireActionListeners() {
		final ActionEvent e = new ActionEvent(this, count++, "");
		for (ActionListener l: listeners) {
			final ActionListener l2 = l;
			SwingUtilities.invokeLater(new Runnable() {public void run() {l2.actionPerformed(e);}});
		}
	}
	
	public DelegateListener delegateListener() {return new DelegateListener(this);}
	
	/**Utility class that listens to an action and re-fires it as-if from a different source it occurs.**/
	public static final class DelegateListener implements ActionListener {
		private final ActionProvider target;
		public DelegateListener(ActionProvider target) {this.target = target;}
		public void actionPerformed(ActionEvent e) {target.fireActionListeners();}
	}
}
