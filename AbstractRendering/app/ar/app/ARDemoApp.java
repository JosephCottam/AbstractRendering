package ar.app;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;

import ar.app.components.*;

public class ARDemoApp implements PanelHolder {
	private ARPanel<?,?> image;
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

		frame.add(controls, BorderLayout.SOUTH);
		
		
		final ARDemoApp app = this;
		presets.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean rezoom = presets.doZoomWith(app.image);
				app.changeImage(presets.update(app.image));
				if (rezoom) {zoomFit();}
			}
		});
		
		app.changeImage(presets.update(app.image));
		
		frame.add(image, BorderLayout.CENTER);

		frame.setSize(500, 500);
		frame.invalidate();
		frame.setVisible(true);
		zoomFit();
		zoomFit();
	}
	
	public static <A,B> void loadInstances(JComboBox<B> target, Class<A> source) {
		Class<?>[] clss = source.getClasses();
		for (Class<?> cls:clss) {
			try {
				@SuppressWarnings("unchecked")
				B i = (B) cls.getConstructor().newInstance();
				target.addItem(i);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				
				throw new RuntimeException("Error intializing GUI.", e);
			}
		}
		
	}
	
	public <A,B> void changeImage(ARPanel<A,B> newImage) {
		JPanel old = this.image;		
		if (old != null) {frame.remove(old);}

		this.status.startMonitoring(newImage.getRenderer());
		frame.add(newImage, BorderLayout.CENTER);
		this.image = newImage;
		frame.revalidate();
	}
	
	public void zoomFit() {
		try {
			Thread.sleep(100); //Delay a beat to let layout (if any) occur, then do the zoom fit. 
			Rectangle2D content = image.dataset().bounds();
	
			double w = image.getWidth()/content.getWidth();
			double h = image.getHeight()/content.getHeight();
			double scale = Math.min(w, h);
			scale = scale/image.getScale();
			Point2D center = new Point2D.Double(content.getCenterX(), content.getCenterY());  
					
			image.zoomAbs(center, scale);
			image.panToAbs(center);
		} catch (Exception e) {} //Ignore all zoom-fit errors...they are usually caused by under-specified state
	}

	public ARPanel<?, ?> getPanel() {return image;}
}
