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
import ar.app.display.ARComponent;
import ar.app.display.SubsetDisplay;
import ar.app.util.GlyphsetUtils;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;

public class ARApp implements ARComponent.Holder {
	private ARComponent.Aggregating display;
	
	private final JFrame frame = new JFrame();
	private final JComboBox<WrappedTransfer<?,?>> transfers = new JComboBox<WrappedTransfer<?,?>>();
	private final JComboBox<WrappedAggregator<?,?>> aggregators = new JComboBox<WrappedAggregator<?,?>>();
	
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
		
		controls.add(new LabeledItem("Aggregator:", aggregators));
		controls.add(new LabeledItem("Transfer:", transfers));
		controls.add(glyphsetOptions);
		controls.add(rendererOptions);
		controls.add(fileOptions);
		controls.add(status);
		
		loadInstances(aggregators, WrappedAggregator.class, "Count (int)");
		loadInstances(transfers, WrappedTransfer.class, "10% Alpha (int)");

		fileOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Glyphset<?> glyphs = loadData();
				display.dataset(glyphs);
				display.zoomFit();
			}
		});
		
		glyphsetOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Glyphset<?> glyphs = loadData();
				display.dataset(glyphs);
				display.zoomFit();
			}});
		
		rendererOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Renderer renderer = rendererOptions.renderer();
				displayWithRenderer(renderer);
			}
		});
		
		aggregators.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				WrappedAggregator<?,?> r = (WrappedAggregator<?,?>) aggregators.getSelectedItem();
				display.aggregator(r.op());
			}});
		
		transfers.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i=0;i<transfers.getItemCount(); i++) {
					transfers.getItemAt(i).deselected();
				}
				
				WrappedTransfer<?,?> t = (WrappedTransfer<?,?>) transfers.getSelectedItem();
				display.transfer(t.op());
				t.selected(ARApp.this);
			}});
			
		displayWithRenderer(rendererOptions.renderer());
		
		frame.add(display, BorderLayout.CENTER);
		
		frame.setLocation(0,0);
		frame.setSize(500, 500);
		frame.invalidate();
		frame.setVisible(true);
		display.zoomFit();
	}
	
	public static <A,B> void loadInstances(JComboBox<B> target, Class<A> source, String defItem) {
		Class<?>[] clss = source.getClasses();
		for (Class<?> cls:clss) {
			try {
				@SuppressWarnings("unchecked") //Inherently not type-safe operation...
				B i = (B) cls.getConstructor().newInstance();
				target.addItem(i);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				System.err.println("Error intializing GUI:");
				e.printStackTrace();
			}
		}
		
		for (int i=0; i<target.getItemCount(); i++) {
			B item = target.getItemAt(i);
			if (item.toString().equals(defItem)) {target.setSelectedIndex(i); break;}
		}		
	}
	
	public void displayWithRenderer(Renderer renderer) {
		ARComponent.Aggregating newDisplay = new SubsetDisplay(((WrappedAggregator<?,?>) aggregators.getSelectedItem()).op(), 
				((WrappedTransfer<?,?>) transfers.getSelectedItem()).op(), 
				loadData(),
				rendererOptions.renderer());
		if (this.display != null) {frame.remove(this.display);}
		frame.add(newDisplay, BorderLayout.CENTER);
		this.status.startMonitoring(renderer);
		this.display = newDisplay;
		frame.invalidate();
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
	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {
		ARComponent.PERF_REP = true;
		if (args.length >0 && args[0].toUpperCase().equals("-EXT")) {
			new ARApp();
		} else {
			System.out.println("Execute with -ext for extended inteface.");
			new ARDemoApp();
		} 
	}
	
	public ARComponent getARComponent() {return display;}
}
