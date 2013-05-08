package ar.app;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import ar.Renderer;
import ar.GlyphSet;
import ar.app.components.*;
import ar.app.util.CSVtoGlyphSet;

public class ARApp {
	private ARPanel<?,?> image;
	private final JFrame frame = new JFrame();
	private final JComboBox<WrappedTransfer<?>> transfers = new JComboBox<WrappedTransfer<?>>();
	private final JComboBox<WrappedReduction<?>> reductions = new JComboBox<WrappedReduction<?>>();
	
	private final GlyphsetOptions glyphsetOptions = new GlyphsetOptions();
	private final RendererOptions rendererOptions = new RendererOptions();
	private final FileOptions fileOptions;
	
	public ARApp() {
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Abstract Rendering Demo App");
		frame.setLayout(new BorderLayout());
		

		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
		JPanel outer = new JPanel();
		outer.setLayout(new BorderLayout());
		outer.add(controls, BorderLayout.WEST);
		frame.add(outer, BorderLayout.SOUTH);
		
		fileOptions = new FileOptions(this);
		
		controls.add(new LabeledItem("Aggregator:", reductions));
		controls.add(new LabeledItem("Transfer:", transfers));
		controls.add(glyphsetOptions);
		controls.add(rendererOptions);
		controls.add(fileOptions);
		final ARApp app = this;
		
		loadInstances(reductions, WrappedReduction.class);
		loadInstances(transfers, WrappedTransfer.class);

		fileOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GlyphSet glyphs = loadData();
				app.changeImage(image.withDataset(glyphs));
				app.zoomFit();
			}
		});
		
		glyphsetOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GlyphSet glyphs = loadData();
				app.changeImage(image.withDataset(glyphs));
				app.zoomFit();
			}});
		
		rendererOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Renderer renderer = rendererOptions.renderer();
				app.changeImage(image.withRenderer(renderer));
				app.zoomFit();
			}
		});
		
		reductions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				WrappedReduction<?> r = (WrappedReduction<?>) reductions.getSelectedItem();
				app.changeImage(image.withReduction(r));
			}});
		
		transfers.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				WrappedTransfer<?> t = (WrappedTransfer<?>) transfers.getSelectedItem();
				app.changeImage(image.withTransfer(t));
			}});
	
		
		
		image = new ARPanel(((WrappedReduction) reductions.getSelectedItem()), 
							((WrappedTransfer) transfers.getSelectedItem()), 
							loadData(),
							rendererOptions.renderer());
		
		frame.add(image, BorderLayout.CENTER);

		frame.setSize(500, 500);
		frame.invalidate();
		frame.setVisible(true);
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
		frame.remove(old);
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

	public GlyphSet loadData() {
		File dataFile = fileOptions.inputFile();
		if (dataFile  == null) {return null;}
		System.out.print("Loading " + dataFile.getName() + "...");
		double glyphSize = glyphsetOptions.glyphSize();
		GlyphSet glyphSet = glyphsetOptions.makeGlyphset();
		return CSVtoGlyphSet.autoLoad(dataFile, glyphSize, glyphSet);
	}
	
	public ARPanel<?,?> getImage() {return image;}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ARApp();
	}

}
