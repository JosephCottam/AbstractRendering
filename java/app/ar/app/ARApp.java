package ar.app;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import ar.Renderer;
import ar.Glyphset;
import ar.app.components.*;
import ar.app.display.ARComponent;
import ar.app.display.EnhanceHost;
import ar.app.display.SubsetDisplay;
import ar.app.util.GlyphsetUtils;
import ar.app.util.Util;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;

public class ARApp implements ARComponent.Holder {
	private ARComponent.Aggregating display;
	
	private final JFrame frame = new JFrame();
	private final JComboBox<WrappedTransfer<?,?>> transfers = new JComboBox<WrappedTransfer<?,?>>();
	private final JComboBox<WrappedAggregator<?,?>> aggregators = new JComboBox<WrappedAggregator<?,?>>();
	
	private final GlyphsetOptions glyphsetOptions = new GlyphsetOptions();
	private final RendererOptions rendererOptions = new RendererOptions();
	private final EnhanceOptions enhanceOptions = new EnhanceOptions();
	private final FileOptions fileOptions;
	private final Status status = new Status();
	
	public ARApp() {
		ar.renderers.RenderUtils.RECORD_PROGRESS = true;
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Abstract Rendering: Explore");
		frame.setLayout(new BorderLayout());

		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
		JPanel outer = new JPanel();
		outer.setLayout(new BorderLayout());
		outer.add(controls, BorderLayout.WEST);
		frame.add(outer, BorderLayout.SOUTH);
		
		fileOptions = new FileOptions(this);
		
		controls.add(enhanceOptions);
		controls.add(new LabeledItem("Aggregator:", aggregators));
		controls.add(new LabeledItem("Transfer:", transfers));
		controls.add(glyphsetOptions);
		controls.add(rendererOptions);
		controls.add(fileOptions);
		controls.add(status);
		
		Util.loadInstances(aggregators, WrappedAggregator.class, WrappedAggregator.class, "Count (int)");
		Util.loadInstances(transfers, WrappedTransfer.class, WrappedTransfer.class, "10% Alpha (int)");

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
	
	
	public void displayWithRenderer(Renderer renderer) {
		SubsetDisplay innerDisplay = new SubsetDisplay(((WrappedAggregator<?,?>) aggregators.getSelectedItem()).op(), 
				((WrappedTransfer<?,?>) transfers.getSelectedItem()).op(), 
				loadData(),
				renderer);
		
		EnhanceHost newDisplay = new EnhanceHost(innerDisplay);
		
		if (this.display != null) {frame.remove(this.display);}
		
		enhanceOptions.host(newDisplay);
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
