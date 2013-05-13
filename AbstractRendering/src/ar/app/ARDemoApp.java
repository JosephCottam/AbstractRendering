package ar.app;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;

import ar.app.components.*;

public class ARDemoApp {
	private ARPanel<?,?> image;
	private final JFrame frame = new JFrame();
	private final Presets presets = new Presets();
	private final Status status = new Status();
	
	public ARDemoApp() {
		ar.renderers.RenderUtils.RECORD_PROGRESS = true;
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Abstract Rendering (Demo App)");
		frame.setLayout(new BorderLayout());
		frame.add(presets, BorderLayout.SOUTH);
		
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

	public ARPanel<?,?> getImage() {return image;}
}
