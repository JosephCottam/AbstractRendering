package ar.app.components;

import javax.swing.*;

import ar.*;
import ar.Renderer;

public class RendererOptions extends CompoundPanel {
	private static final long serialVersionUID = 1L;
	private JComboBox<String> renderers = new JComboBox<String>();
	private JComboBox<Integer> taskSize = new JComboBox<Integer>();
	
	public RendererOptions() {
		renderers.addItem("Parallel (Pixel)");
		renderers.addItem("Serial");
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
	}
	
	public Renderer renderer() {
		if (renderers.getSelectedItem().equals("Parallel (Pixel)")) {
			return new ParallelRenderer((Integer) taskSize.getSelectedItem());
		} else if (renderers.getSelectedItem().equals("Serial")) {
			return new SerialRenderer();
		} else {
			throw new RuntimeException("Unknown renderer selected: " + renderers.getSelectedItem());
		}
	}
}
