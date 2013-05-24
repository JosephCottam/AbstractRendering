package ar.app.components;

import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class LabeledItem extends JPanel {
	private static final long serialVersionUID = 1L;
	public LabeledItem(String labeltext, JComponent item) {
		this.setLayout(new BorderLayout());
		this.add(new JLabel(labeltext), BorderLayout.WEST);
		this.add(item, BorderLayout.CENTER);
	}
	
}
