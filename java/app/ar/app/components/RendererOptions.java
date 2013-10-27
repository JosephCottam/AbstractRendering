package ar.app.components;

import java.awt.event.ActionListener;

import javax.swing.*;

import ar.Renderer;
import ar.app.util.ActionProvider;
import ar.renderers.*;

public class RendererOptions extends JPanel {
	private static final long serialVersionUID = 1L;
	private JComboBox<String> renderers = new JComboBox<String>();
	private JComboBox<Integer> taskSize = new JComboBox<Integer>();
	private final ActionProvider actionProvider = new ActionProvider();
	
	public RendererOptions() {
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		renderers.addItem("Parallel (Spatial)");
		renderers.addItem("Serial");
		renderers.addItem("Parallel (Glyphs)");
		renderers.addActionListener(actionProvider.delegateListener());
		renderers.setSelectedItem("Parallel (Glyphs)");
		
		taskSize.addItem(1);
		taskSize.addItem(10);
		taskSize.addItem(100);
		taskSize.addItem(1000);
		taskSize.addItem(100000);
		taskSize.addItem(1000000);
		taskSize.addItem(1000000000);
		taskSize.setSelectedItem(100000);
		taskSize.addActionListener(actionProvider.delegateListener());		
		
		JPanel upper = new JPanel(); 
		upper.add(new LabeledItem("Render:" , renderers));
		upper.add(new LabeledItem("Task Size:", taskSize));
		this.add(upper);
	}
	
	public void addActionListener(ActionListener l) {actionProvider.addActionListener(l);}
	
	public Renderer renderer() {
		int size = (Integer) taskSize.getSelectedItem();
		if (renderers.getSelectedItem().equals("Serial")) {
			return new SerialRenderer();
		} else if (renderers.getSelectedItem().equals("Parallel (Glyphs)")) {
			return new ParallelRenderer(size);
		} else {
			throw new RuntimeException("Unknown renderer selected: " + renderers.getSelectedItem());
		}
	}
}
