package ar.app.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**Utility for producing action events.
 * 
 * Keeps a list of ActionListeners;  Fires them off when asked to.
 * **/
public class ActionProvider {
	private List<ActionListener> listeners = new ArrayList<ActionListener>();
	private int count;
	private final String prefix;
	private final Object source;

	public ActionProvider() {this(null, "");}
	public ActionProvider(String prefix) {this(null, prefix);}
	public ActionProvider(Object source, String prefix) {
		this.source = source != null ? source : this;
		this.prefix = prefix;
	}
	
	public void addActionListener(ActionListener l) {listeners.add(l);}
	public void fireActionListeners() {fireActionListeners("");}
	public void fireActionListeners(String command) {
		final ActionEvent e = new ActionEvent(source, count++, prefix + ":" + command);
		for (ActionListener l: listeners) {
			final ActionListener l2 = l;
			SwingUtilities.invokeLater(new Runnable() {public void run() {l2.actionPerformed(e);}});
		}
	}
	
	public ActionDelegate actionDelegate() {return new ActionDelegate(this);}
	public ChangeDelegate changeDelegate() {return new ChangeDelegate(this);}
	public ItemDelegate itemDelegate() {return new ItemDelegate(this);}
	
	/**Utility class that listens to an action and re-fires it as-if from a different source it occurs.**/
	public static final class ActionDelegate implements ActionListener {
		private final ActionProvider target;
		public ActionDelegate(ActionProvider target) {this.target = target;}
		public void actionPerformed(ActionEvent e) {target.fireActionListeners(e.getActionCommand());}
	}
	
	/**Utility class that listens to an item and generates action events in response.**/
	public static final class ItemDelegate implements ItemListener {
		private final ActionProvider target;
		public ItemDelegate(ActionProvider target) {this.target = target;}
		public void itemStateChanged(ItemEvent e) {target.fireActionListeners(e.paramString());}
	}

	
	/**Utility that listens to change events and generates action events in response.**/
	public static final class ChangeDelegate implements ChangeListener {
		private final ActionProvider target;
		public ChangeDelegate(ActionProvider target) {this.target = target;}
		
		@SuppressWarnings("unused")
		public void stateChanged(ChangeEvent e) {target.fireActionListeners(target.prefix + "--Property Change");}
	}	
}
