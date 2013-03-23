package ar.app;

import javax.swing.*;

import ar.Aggregates;
import ar.app.util.AggregatesToJSON;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;

public class ARApp {
	private ARPanel<?,?> image;
	private final JFrame frame = new JFrame();
	private final JComboBox<WrappedTransfer<?>> transfers = new JComboBox<WrappedTransfer<?>>();
	private final JComboBox<WrappedReduction<?>> reductions = new JComboBox<WrappedReduction<?>>();
	
	private final JComboBox<Dataset> dataset = new JComboBox<Dataset>();
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
		controls.add(dataset);
		controls.add(export);
		final ARApp app = this;
		
		reductions.addItem(new WrappedReduction.OverplotFirst());
		reductions.addItem(new WrappedReduction.OverplotLast());
		reductions.addItem(new WrappedReduction.Count());
		reductions.addItem(new WrappedReduction.SolidBlue());
		reductions.addItem(new WrappedReduction.Gradient());
		
		transfers.addItem(new WrappedTransfer.EchoColor());
		transfers.addItem(new WrappedTransfer.RedWhiteInterpolate());
		transfers.addItem(new WrappedTransfer.RedBlueInterpolate());
		transfers.addItem(new WrappedTransfer.OutlierHighlight());
		
		dataset.addItem(new Dataset.SyntheticScatterplot());
		dataset.addItem(new Dataset.Checkers());
		dataset.addItem(new Dataset.Memory());
		dataset.addItem(new Dataset.MPIPhases());
		
		dataset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Dataset glyphs = (Dataset) dataset.getSelectedItem();
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
							(Dataset) dataset.getSelectedItem());
		
		frame.add(image, BorderLayout.CENTER);

		frame.setSize(390, 390);
		frame.invalidate();
		frame.setVisible(true);
		zoomFit();
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
