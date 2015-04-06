package ar.app.components;

import javax.swing.*;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;

import ar.Glyphset;
import ar.app.util.ActionProvider;
import ar.app.util.LabeledItem;
import ar.glyphsets.*;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer.Constant;
import ar.glyphsets.implicitgeometry.Indexed.ToRect;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;

public class GlyphsetOptions extends JPanel  {
	private static final long serialVersionUID = 1L;
	private final JComboBox<String> glyphsType = new JComboBox<String>();
	private final JComboBox<Double> size = new JComboBox<Double>();
	private final ActionProvider actionProvider = new ActionProvider();
	
	public GlyphsetOptions( ){

		glyphsType.addItem("Quad Tree");
		glyphsType.addItem("List");
		glyphsType.addItem("MemMap List");
		glyphsType.setSelectedItem("MemMap List");
		this.add(new LabeledItem("Glyph Storage:", glyphsType));
		
		size.addItem(.001);
		size.addItem(.005);
		size.addItem(.01);
		size.addItem(.05);
		size.addItem(.1);
		size.addItem(.5);
		size.addItem(1d);
		size.setSelectedItem(.01);
		this.add(new LabeledItem("Size: ", size));
		
		ActionListener l = actionProvider.actionDelegate();
		glyphsType.addActionListener(l);
		size.addActionListener(l);
	}
	
	public double glyphSize() {return (double) size.getSelectedItem();}
	public void addActionListener(ActionListener l) {actionProvider.addActionListener(l);}

	
	public Glyphset<?,?> makeGlyphset() {
		if (glyphsType.getSelectedItem().equals("List")) {
			return new GlyphList<Rectangle2D, Color>();			
		} else if (glyphsType.getSelectedItem().equals("MemMap List")) {
			double sz = (double) size.getSelectedItem();
			Shaper<Indexed, Rectangle2D> shaper = new ToRect(sz, sz, false, 0,1);
			Valuer<Indexed, Color> valuer = new Constant<Indexed,Color>(Color.red);
			return new MemMapList<>(null, shaper, valuer);
		} else {
			throw new RuntimeException("Unknown glyphset type selected.");
		}
	}
}
