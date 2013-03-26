package ar.app;

import javax.swing.*;

import ar.app.util.AggregatesToJSON;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class ARApp {
	private ARPanel<?,?> image;
	private final JFrame frame = new JFrame();
	private final JComboBox<WrappedTransfer<?>> transfers = new JComboBox<WrappedTransfer<?>>();
	private final JComboBox<WrappedReduction<?>> reductions = new JComboBox<WrappedReduction<?>>();
	
	private final JComboBox<Dataset> datasets = new JComboBox<Dataset>();
	private final JButton export = new JButton("Export Aggregates");

	
	public ARApp() {
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Abstract Rendering Demo App");
		frame.setLayout(new BorderLayout());
		

		JPanel controls = new JPanel();
		controls.setLayout(new GridLayout(2,2));
		JPanel outer = new JPanel();
		outer.setLayout(new BorderLayout());
		outer.add(controls, BorderLayout.WEST);
		frame.add(outer, BorderLayout.SOUTH);
		
		controls.add(reductions);
		controls.add(transfers);
		controls.add(datasets);
		controls.add(export);
		final ARApp app = this;
		
		loadInstances(reductions, WrappedReduction.class);
		loadInstances(transfers, WrappedTransfer.class);
		loadInstances(datasets, Dataset.class);

		
		//TODO: Select some sensible items so it render right away
//		datasets.setSelectedItem(new Dataset.SyntheticScatterplot());
//		reductions.setSelectedItem(new WrappedReduction.OverplotFirst());
//		transfers.setSelectedItem(new WrappedTransfer.EchoColor());

		datasets.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Dataset glyphs = (Dataset) datasets.getSelectedItem();
				app.changeImage(image.withDataset(glyphs));
				app.zoomFit();
			}});
		
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
	
		
		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				 JFileChooser fd = new JFileChooser("Export Aggregates (e.g., reduction results)");
				 fd.setSelectedFile(new File("aggregates.json"));
				 int returnVal = fd.showDialog(frame, "Export");
				 if (returnVal == JFileChooser.APPROVE_OPTION) {
					 AggregatesToJSON.export(image.getAggregates(),fd.getSelectedFile());
				 }
			}
			
		});
		
		image = new ARPanel(((WrappedReduction) reductions.getSelectedItem()), 
							((WrappedTransfer) transfers.getSelectedItem()), 
							(Dataset) datasets.getSelectedItem());
		
		frame.add(image, BorderLayout.CENTER);

		frame.setSize(500, 500);
		frame.invalidate();
		frame.setVisible(true);
		zoomFit();
	}
	
	private <A,B> void loadInstances(JComboBox<B> target, Class<A> source) {
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
		try {Thread.sleep(100);} //Delay a beat to let layout (if any) occur, then do the zoom fit. 
		catch (InterruptedException e1) {}

		Rectangle2D content = image.dataset().glyphs().bounds();

		double w = image.getWidth()/content.getWidth();
		double h = image.getHeight()/content.getHeight();
		double scale = Math.min(w, h);
		scale = scale/image.getScale();
		Point2D center = new Point2D.Double(content.getCenterX(), content.getCenterY());  
				
		image.zoomAbs(center, scale);
		image.panToAbs(center);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ARApp();
	}

}
