package ar.app.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class CompoundPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private List<ActionListener> listeners = new ArrayList<ActionListener>();
	private int count;

	public void addActionListener(ActionListener l) {listeners.add(l);}
	protected void fireActionListeners() {
		final ActionEvent e = new ActionEvent(this, count++, "");
		for (ActionListener l: listeners) {
			final ActionListener l2 = l;
			SwingUtilities.invokeLater(new Runnable() {public void run() {l2.actionPerformed(e);}});
		}
	}
	
	public static class DelegateAction implements ActionListener {
		private final CompoundPanel target;
		public DelegateAction(CompoundPanel target) {this.target = target;}
		public void actionPerformed(ActionEvent e) {target.fireActionListeners();}
	}
}
