package ar.app.components;

import javax.swing.*;

import java.awt.Color;
import java.awt.event.ActionListener;

import ar.Glyphset;
import ar.glyphsets.*;
import ar.util.ImplicitGeometry;

public class GlyphsetOptions extends CompoundPanel  {
	private static final long serialVersionUID = 1L;
	private final JComboBox<String> glyphsType = new JComboBox<String>();
	private final JComboBox<Double> size = new JComboBox<Double>();
	
	public GlyphsetOptions( ){

		glyphsType.addItem("Quad Tree");
		glyphsType.addItem("List");
		glyphsType.addItem("Matrix");
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
		
		ActionListener l = new CompoundPanel.DelegateAction(this);
		glyphsType.addActionListener(l);
		size.addActionListener(l);
	}
	
	public double glyphSize() {return (Double) size.getSelectedItem();}
	
	public Glyphset<?> makeGlyphset() {
		if (glyphsType.getSelectedItem().equals("Quad Tree")) {
			return DynamicQuadTree.make();
		} else if (glyphsType.getSelectedItem().equals("List")) {
			return new GlyphList<Object>();			
		} else if (glyphsType.getSelectedItem().equals("MemMap List")) {
			double sz = (Double) size.getSelectedItem();
			ImplicitGeometry.Shaper shaper = new ImplicitGeometry.IndexedToRect(sz, sz, false, 0,1);
			ImplicitGeometry.Valuer valuer = new ImplicitGeometry.Constant<Double>(Color.red);
			return new MemMapList(null, shaper, valuer);
		} else if (glyphsType.getSelectedItem().equals("Matrix")) {
			return new DirectMatrix<>(null, 1, 1, true);
		} else {
			throw new RuntimeException("Unknown glyphset type selected.");
		}
	}
}
