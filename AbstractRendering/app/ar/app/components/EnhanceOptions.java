package ar.app.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import ar.Aggregates;
import ar.app.display.EnhanceHost;

/**Panel for controlling the region-based transfer function specialization.**/
public class EnhanceOptions  extends JPanel {
	private static final long serialVersionUID = -5359708733083679997L;
	
	EnhanceHost.Control showOverlay = new EnhanceHost.Control();
	JCheckBox enhance = new JCheckBox();
	
	public EnhanceOptions() {
		this.add(showOverlay);
		this.add(new LabeledItem("Enhance:", enhance));
		
		enhance.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JCheckBox b = (JCheckBox) e.getSource();
				showOverlay.host().enableEnhance(b.isSelected());
			}
			
		});
	}
	
	/**Is "enhance mode" enabled?*/
	public boolean enhance() {return enhance.isSelected();}
	
	public Aggregates<?> subset() {return showOverlay.host().subset();}
	
	/**Overlay host associated with this control.*/
	public EnhanceHost host() {return showOverlay.host();}

	/**Set the target host control.**/
	public void host(EnhanceHost host) {
		if (host != showOverlay.host()) {
			enhance.setSelected(false);
			showOverlay.host(host);
		}
	}
}
