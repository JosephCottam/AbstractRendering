package ar.app.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import ar.app.display.EnhanceHost;
import ar.app.util.LabeledItem;

/**Panel for controlling the region-based transfer function specialization.**/
public class RegionOptions  extends JPanel {
	private static final long serialVersionUID = -5359708733083679997L;
	
	EnhanceHost.Control modSelection = new EnhanceHost.Control();
	JCheckBox enhance = new JCheckBox();
	JCheckBox limit = new JCheckBox();
	
	public RegionOptions() {
		this.add(modSelection);
		this.add(new LabeledItem("Enhance:", enhance));
		this.add(new LabeledItem("Limit:", limit));
		
		enhance.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JCheckBox b = (JCheckBox) e.getSource();
				modSelection.host().enableEnhance(b.isSelected());
			}
			
		});
		
		limit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JCheckBox b = (JCheckBox) e.getSource();
				modSelection.host().enableLimit(b.isSelected());
			}
			
		});
	}
	
	/**Is "limit mode" enabled?*/
	public boolean limit() {return limit.isSelected();}
	
	/**Is "enhance mode" enabled?*/
	public boolean enhance() {return enhance.isSelected();}
	
	/**Overlay host associated with this control.*/
	public EnhanceHost host() {return modSelection.host();}

	/**Set the target host control.**/
	public void host(EnhanceHost host) {
		if (host != modSelection.host()) {
			modSelection.clear();
			enhance.setSelected(false);
			modSelection.host(host);
		}
	}
}
