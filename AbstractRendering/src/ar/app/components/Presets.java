package ar.app.components;

import javax.swing.JComboBox;

import ar.app.ARApp;

public class Presets extends CompoundPanel {
	private static final long serialVersionUID = -5290930773909190497L;
	
	private final JComboBox presets = new JComboBox();
	public Presets() {
		this.add(new LabeledItem("Presets:", presets));
		presets.addActionListener(new CompoundPanel.DelegateAction(this));
		
		ARApp.loadInstances(presets, Presets.class);
	}
	
	public void update(ARApp app) {
		//app.changeImage(....);
	}
	
	
}
