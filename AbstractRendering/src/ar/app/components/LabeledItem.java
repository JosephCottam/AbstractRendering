package ar.app.components;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class LabeledItem extends JPanel {
	public LabeledItem(String label, JComponent item) {
		this.add(new JLabel(label));
		this.add(item);
	}
}
