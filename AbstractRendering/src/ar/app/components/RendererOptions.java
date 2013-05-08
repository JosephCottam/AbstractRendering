package ar.app.components;

import javax.swing.*;

import ar.*;
import ar.Renderer;
import ar.app.ARApp;
import ar.renderers.*;
import ar.rules.AggregateReducers;

public class RendererOptions extends CompoundPanel {
	private static final long serialVersionUID = 1L;
	private JComboBox<String> renderers = new JComboBox<String>();
	private JComboBox<Integer> taskSize = new JComboBox<Integer>();
	private JComboBox<AggregateReducer<?,?,?>> reducers = new JComboBox<AggregateReducer<?,?,?>>();
	
	public RendererOptions() {
		renderers.addItem("Parallel (Spatial)");
		renderers.addItem("Serial");
		renderers.addItem("Parallel (Glyphs)");
		renderers.addActionListener(new DelegateAction(this));
		renderers.setSelectedItem("Parallel (Pixel)");
		this.add(new LabeledItem("Render:" , renderers));
		
		taskSize.addItem(1);
		taskSize.addItem(10);
		taskSize.addItem(100);
		taskSize.addItem(1000);
		taskSize.addItem(10000);
		taskSize.setSelectedItem(100);
		taskSize.addActionListener(new DelegateAction(this));		
		this.add(new LabeledItem("Task Size:", taskSize));
		
		ARApp.loadInstances(reducers, AggregateReducers.class);
	}
	
	public Renderer renderer() {
		int size = (Integer) taskSize.getSelectedItem();
		if (renderers.getSelectedItem().equals("Parallel (Spatial)")) {
			return new ParallelSpatial(size);
		} else if (renderers.getSelectedItem().equals("Serial")) {
			return new Serial();
		} else if (renderers.getSelectedItem().equals("Parallel (Glyphs)")) {
			return new ParallelGlyphs(size, (AggregateReducer<?,?,?>) reducers.getSelectedItem());
		} else {
			throw new RuntimeException("Unknown renderer selected: " + renderers.getSelectedItem());
		}
	}
}
