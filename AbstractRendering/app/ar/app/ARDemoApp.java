package ar.app;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

import ar.app.components.*;

public class ARDemoApp implements PanelHolder {
	private ARPanel panel;
	private final JFrame frame = new JFrame();
	private final Presets presets = new Presets();
	private final Status status = new Status();
	private final ExportAggregates export;
	
	public ARDemoApp() {
		ar.renderers.RenderUtils.RECORD_PROGRESS = true;
		export = new ExportAggregates(this);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Abstract Rendering (Demo App)");
		frame.setLayout(new BorderLayout());
		
		JPanel controls = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill =  GridBagConstraints.HORIZONTAL;
		
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.weightx = 1;
		controls.add(presets, c);
		
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0.5;
		c.gridwidth = 1;
		controls.add(status,c);

		c.gridx = 1;
		c.gridy = 1;		
		c.weightx = 0.5;
		c.gridwidth = 1;
		controls.add(export,c);
		
		JLabel instructions = new JLabel("Double-click to zoom extends and/or force refresh.", JLabel.CENTER);
		c.gridx=0;
		c.gridy=2;
		c.weightx=2;
		c.weightx=1;
		controls.add(instructions,c);

		frame.add(controls, BorderLayout.SOUTH);
		
		
		final ARDemoApp app = this;
		presets.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean rezoom = presets.doZoomWith(app.panel);
				app.changeImage(presets.update(app.panel));
				if (rezoom) {panel.zoomFit();}
			}
		});
		
		app.changeImage(presets.update(app.panel));
		
		frame.add(panel, BorderLayout.CENTER);

		frame.setSize(500, 500);
		frame.validate();
		frame.setVisible(true);
		final ARPanel img = panel;
		try {
			SwingUtilities.invokeAndWait(new Runnable() {public void run() {img.zoomFit();}});
		} catch (InvocationTargetException | InterruptedException e1) {}
	}
	
	public static <A,B> void loadInstances(JComboBox<B> target, Class<A> source) {
		Class<?>[] clss = source.getClasses();
		for (Class<?> cls:clss) {
			try {
				@SuppressWarnings("unchecked") //Inherently not type-safe operation...
				B i = (B) cls.getConstructor().newInstance();
				target.addItem(i);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				throw new RuntimeException("Error intializing GUI.", e);
			}
		}
		
	}
	
	public <A,B> void changeImage(ARPanel newImage) {
		JPanel old = this.panel;		
		if (old != null) {frame.remove(old);}

		this.status.startMonitoring(newImage.getRenderer());
		frame.add(newImage, BorderLayout.CENTER);
		this.panel = newImage;
		frame.revalidate();
	}
	
	public ARPanel getPanel() {return panel;}
}
