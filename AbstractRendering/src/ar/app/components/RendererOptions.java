package ar.app.components;

import javax.swing.*;

public class RendererOptions extends CompoundPanel {
	private static final long serialVersionUID = 1L;
	private JComboBox<String> renderers = new JComboBox<String>();
	public RendererOptions() {
		this.add(renderers);
		renderers.addItem("Parallel (Pixel)");
		renderers.addItem("Serial");
		renderers.addActionListener(new DelegateAction(this));
	}
}
