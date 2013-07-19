package ar.app.components;

import java.awt.Color;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ar.Transfer;
import ar.app.ARApp;
import ar.rules.Advise.DrawDark;

public class DrawDarkControl extends JPanel {
	private static final long serialVersionUID = 1L;
	
	protected final JSpinner distance = new JSpinner();
	protected ARApp source;
	protected DrawDark cached;

	public DrawDarkControl() {
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(distance);
		distance.setValue(10);
	
		distance.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {updateImage();}
		});
	}
	
	public void setSource(ARApp source) {this.source=source;}
	public int distance() {return (Integer) distance.getValue();}
	public void updateImage() {
		ARPanel p = source.getPanel().withTransfer(DrawDarkControl.this.getTransfer());
		source.changeImage(p);
	}

	public Transfer<Number,Color> getTransfer() {
		if (cached == null || distance() != cached.distance) {
			cached = new DrawDark(Color.black, Color.white, distance());
		}
		return cached;
	}
	
	
}
