package ar.app.components;

import java.awt.Color;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ar.Transfer;
import ar.app.display.ARComponent;
import ar.rules.Advise.DrawDark;

public class DrawDarkControl extends JPanel {
	private static final long serialVersionUID = 1L;
	
	protected final JSpinner distance = new JSpinner();
	protected ARComponent.Holder source;
	protected DrawDark cached;

	public DrawDarkControl() {
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(distance);
		distance.setValue(10);
	
		distance.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {updateImage();}
		});
	}
	
	public void setSource(ARComponent.Holder source) {this.source=source;}
	public int distance() {return (Integer) distance.getValue();}
	public void updateImage() {
		source.getARComponent().transfer(DrawDarkControl.this.getTransfer());
	}

	public Transfer<Number,Color> getTransfer() {
		if (cached == null || distance() != cached.distance) {
			cached = new DrawDark(Color.black, Color.white, distance());
		}
		return cached;
	}
	
	
}
