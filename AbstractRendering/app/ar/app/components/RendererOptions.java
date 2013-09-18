package ar.app.components;

import javax.swing.*;

import ar.Renderer;
import ar.renderers.*;

public class RendererOptions extends PanelDelegator {
	private static final long serialVersionUID = 1L;
	private JComboBox<String> renderers = new JComboBox<String>();
	private JComboBox<Integer> taskSize = new JComboBox<Integer>();
	
	public RendererOptions() {
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		renderers.addItem("Parallel (Spatial)");
		renderers.addItem("Serial");
		renderers.addItem("Parallel (Glyphs)");
		renderers.addActionListener(new DelegateAction(this));
		renderers.setSelectedItem("Parallel (Glyphs)");
		
		taskSize.addItem(1);
		taskSize.addItem(10);
		taskSize.addItem(100);
		taskSize.addItem(1000);
		taskSize.addItem(100000);
		taskSize.addItem(1000000);
		taskSize.addItem(1000000000);
		taskSize.setSelectedItem(100000);
		taskSize.addActionListener(new DelegateAction(this));		
		
		JPanel upper = new JPanel(); 
		upper.add(new LabeledItem("Render:" , renderers));
		upper.add(new LabeledItem("Task Size:", taskSize));
		this.add(upper);
	}
	
	public Renderer renderer() {
		int size = (Integer) taskSize.getSelectedItem();
		if (renderers.getSelectedItem().equals("Parallel (Spatial)")) {
			return new ParallelSpatial(size);
		} else if (renderers.getSelectedItem().equals("Serial")) {
			return new SerialSpatial();
		} else if (renderers.getSelectedItem().equals("Parallel (Glyphs)")) {
			return new ParallelGlyphs(size);
		} else {
			throw new RuntimeException("Unknown renderer selected: " + renderers.getSelectedItem());
		}
	}
}
