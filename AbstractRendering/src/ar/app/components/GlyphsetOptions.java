package ar.app.components;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import ar.GlyphSet;
import ar.app.util.CSVtoGlyphSet;
import ar.glyphsets.*;

public class GlyphsetOptions extends CompoundPanel  {
	private static final long serialVersionUID = 1L;
	private final JComboBox<String> glyphsType = new JComboBox<String>();
	private final JComboBox<String> size = new JComboBox<String>();
	
	public GlyphsetOptions( ){

		glyphsType.addItem("Quad Tree");
		glyphsType.addItem("List");
		glyphsType.addItem("Matrix");
		this.add(new LabeledItem("Glyphset:", glyphsType));
		
		size.addItem("1");
		size.addItem(".1");
		size.addItem(".01");
		size.addItem(".001");
		this.add(new LabeledItem("Size: ", size));

		
		
		ActionListener l = new CompoundPanel.DelegateAction(this);
		glyphsType.addActionListener(l);
		size.addActionListener(l);
	}
	
	public double glyphSize() {return Double.parseDouble(size.getSelectedItem().toString());}
	
	public GlyphSet makeGlyphset() {
		if (glyphsType.getSelectedItem().equals("Quad Tree")) {
			return DynamicQuadTree.make();
		} else if (glyphsType.getSelectedItem().equals("List")) {
			return new GlyphList();			
		} else if (glyphsType.getSelectedItem().equals("Matrix")) {
			return new DirectMatrix(null, 1, 1, true);
		} else {
			throw new RuntimeException("Unknown glyphset type selected.");
		}
	}
}
