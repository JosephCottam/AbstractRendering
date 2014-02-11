package ar.app.util;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIDefaults;

/**Renders an item using the class's "SimpleName" (instead of the default "toString")**/
public class SimpleNameRenderer<A> implements ListCellRenderer<A> {
	@Override @SuppressWarnings("unused")
	public Component getListCellRendererComponent(JList<? extends A> list,
			A value, int index, boolean isSelected, boolean cellHasFocus) {
		
		JLabel rv = new JLabel();	
		rv.setText(value.getClass().getSimpleName());
		if (value instanceof Class) {rv.setText(((Class<?>) value).getSimpleName());}
		
		if (isSelected) {
			rv.setOpaque(true);
			UIDefaults defaults = javax.swing.UIManager.getDefaults();
			rv.setBackground(defaults.getColor("List.selectionBackground"));
			rv.setForeground(defaults.getColor("List.selectionForeground"));
		}
		
		return rv;
	}
	

}
