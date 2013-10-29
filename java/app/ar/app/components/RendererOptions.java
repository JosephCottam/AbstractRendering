package ar.app.components;

import java.awt.event.ActionListener;

import javax.swing.*;

import ar.Renderer;
import ar.app.util.ActionProvider;
import ar.renderers.*;

public class RendererOptions extends JPanel {
	private static final long serialVersionUID = 1L;
	private JComboBox<String> renderers = new JComboBox<String>();
	private final ActionProvider actionProvider = new ActionProvider();
	
	public RendererOptions() {
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		renderers.addItem("Parallel (Spatial)");
		renderers.addItem("Serial");
		renderers.addItem("Parallel (Glyphs)");
		renderers.addActionListener(actionProvider.delegateListener());
		renderers.setSelectedItem("Parallel (Glyphs)");
		
		JPanel upper = new JPanel(); 
		upper.add(new LabeledItem("Render:" , renderers));
		this.add(upper);
	}
	
	public void addActionListener(ActionListener l) {actionProvider.addActionListener(l);}
	
	public Renderer renderer() {
		if (renderers.getSelectedItem().equals("Serial")) {
			return new SerialRenderer();
		} else if (renderers.getSelectedItem().equals("Parallel")) {
			return new ParallelRenderer();
		} else {
			throw new RuntimeException("Unknown renderer selected: " + renderers.getSelectedItem());
		}
	}
}
