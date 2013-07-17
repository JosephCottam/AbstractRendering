package ar.app;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import ar.Renderer;
import ar.Glyphset;
import ar.app.components.*;
import ar.app.util.GlyphsetUtils;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;

public class ARApp implements PanelHolder {
	private ARPanel image;
	private final JFrame frame = new JFrame();
	private final JComboBox<WrappedTransfer<?,?>> transfers = new JComboBox<WrappedTransfer<?,?>>();
	private final JComboBox<WrappedAggregator<?,?>> reductions = new JComboBox<WrappedAggregator<?,?>>();
	
	private final GlyphsetOptions glyphsetOptions = new GlyphsetOptions();
	private final RendererOptions rendererOptions = new RendererOptions();
	private final FileOptions fileOptions;
	private final Status status = new Status();
	
	public ARApp() {
		ar.renderers.RenderUtils.RECORD_PROGRESS = true;
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Abstract Rendering Explore App");
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
		controls.add(status);
		final ARApp app = this;
		
		loadInstances(reductions, WrappedAggregator.class, "Count (int)");
		loadInstances(transfers, WrappedTransfer.class, "10% Alpha (int)");

		fileOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Glyphset glyphs = loadData();
				ARPanel newImage = image.withDataset(glyphs);
				app.changeImage(newImage);
				newImage.zoomFit();
			}
		});
		
		glyphsetOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Glyphset glyphs = loadData();
				ARPanel newImage = image.withDataset(glyphs);
				app.changeImage(newImage);
				newImage.zoomFit();
			}});
		
		rendererOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Renderer renderer = rendererOptions.renderer();
				ARPanel newImage = image.withRenderer(renderer);
				app.changeImage(newImage);
				newImage.zoomFit();
			}
		});
		
		reductions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				WrappedAggregator<?,?> r = (WrappedAggregator<?,?>) reductions.getSelectedItem();
				app.changeImage(image.withReduction(r.op()));
			}});
		
		transfers.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i=0;i<transfers.getItemCount(); i++) {
					transfers.getItemAt(i).deselected();
				}
				
				WrappedTransfer<?,?> t = (WrappedTransfer<?,?>) transfers.getSelectedItem();
				app.changeImage(image.withTransfer(t.op()));
				t.selected(ARApp.this);
			}});
	
		
		
		image = new ARPanel(((WrappedAggregator) reductions.getSelectedItem()).op(), 
							((WrappedTransfer) transfers.getSelectedItem()).op(), 
							loadData(),
							rendererOptions.renderer());
		
		frame.add(image, BorderLayout.CENTER);
		
		frame.setLocation(0,0);
		frame.setSize(500, 500);
		frame.invalidate();
		frame.setVisible(true);
		image.zoomFit();
	}
	
	public static <A,B> void loadInstances(JComboBox<B> target, Class<A> source, String defItem) {
		Class<?>[] clss = source.getClasses();
		for (Class<?> cls:clss) {
			try {
				B i = (B) cls.getConstructor().newInstance();
				target.addItem(i);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				
				//throw new RuntimeException("Error intializing GUI.", e);
			}
		}
		
		for (int i=0; i<target.getItemCount(); i++) {
			B item = target.getItemAt(i);
			if (item.toString().equals(defItem)) {target.setSelectedIndex(i); break;}
		}		
	}
	
	public void changeImage(ARPanel newImage) {
		JPanel old = this.image;		
		this.status.startMonitoring(newImage.getRenderer());
		frame.remove(old);
		frame.add(newImage, BorderLayout.CENTER);
		this.image = newImage;
		frame.revalidate();
		
		((WrappedTransfer<?,?>) transfers.getSelectedItem()).selected(this);
	}

	public Glyphset<?> loadData() {
		File dataFile = fileOptions.inputFile();
		if (dataFile  == null) {return null;}
		System.out.print("Loading " + dataFile.getName() + "...");
		double glyphSize = glyphsetOptions.glyphSize();
		Glyphset<?> glyphSet = glyphsetOptions.makeGlyphset();
		return GlyphsetUtils.autoLoad(dataFile, glyphSize, glyphSet);
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		if (args.length >0 && args[0].toUpperCase().equals("-EXT")) {
			new ARApp();
		} else {
			System.out.println("Execute with -ext for extended inteface.");
			new ARDemoApp();
		} 
	}

	@Override
	public ARPanel getPanel() {return image;}
}
