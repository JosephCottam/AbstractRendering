package ar.app.components;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import ar.Transfer;
import ar.app.display.ARComponent;
import ar.rules.Advise;

/**Panel for controlling the region-based transfer function specialization.**/
public class ClipwarnControl  extends JPanel {
	private static final long serialVersionUID = -5359708733083679997L;
	
	JCheckBox clipwarn = new JCheckBox();
	ARComponent target;
	
	public ClipwarnControl() {
		this.add(new LabeledItem("Clipping warning:", clipwarn));
		
		clipwarn.addActionListener(new ActionListener() {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			public void actionPerformed(ActionEvent e) {
				JCheckBox b = (JCheckBox) e.getSource();
				Transfer<?,?> old = target.transfer();
				if (b.isSelected() && !(old instanceof Advise.OverUnder)) {
					target.transfer(new Advise.OverUnder(Color.BLACK, Color.gray, old, 1));
				} else if (!b.isSelected() && (old instanceof Advise.OverUnder)) {
					target.transfer(((Advise.OverUnder) old).baseTransfer());
				}
				
			}
			
		});
	}
	
	public boolean isEnabled() {return clipwarn.isSelected();}
	
	/**Overlay host associated with this control.*/
	public ARComponent target() {return target;}

	/**Set the target host control.**/
	public void target(ARComponent target) {
		if (target != this.target) {
			clipwarn.setSelected(false);
			this.target=target;
		}
	}
}
