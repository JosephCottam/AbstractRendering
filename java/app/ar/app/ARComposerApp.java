package ar.app;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import ar.Aggregates;
import ar.aggregates.AggregateUtils;
import ar.app.components.*;
import ar.app.components.sequentialComposer.SequentialComposer;
import ar.app.display.ARComponent;
import ar.app.display.AggregatingDisplay;
import ar.app.display.EnhanceHost;
import ar.renderers.ParallelRenderer;
import ar.renderers.RenderUtils;
import ar.util.Util;


//TODO: Add "subset input", useful for contours
//TODO: Add "Specialize From Here"
public class ARComposerApp implements ARComponent.Holder, ar.util.HasViewTransform {
	private final EnhanceHost display = new EnhanceHost(new AggregatingDisplay(new ParallelRenderer()));
	private final JFrame frame = new JFrame();

	private final RegionOptions enhanceOptions = new RegionOptions();
	private final Status status = new Status();
	private final SequentialComposer composer = new SequentialComposer(this);
	
	private final JMenu fileMenu = new JMenu("File");
	private final JMenuItem saveImage = new JMenuItem("Save Image", KeyEvent.VK_S);
	
	private final JCheckBox axes = new JCheckBox("Legend");
	
	public ARComposerApp() {
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Abstract Rendering (Demo App)");
		frame.setLayout(new BorderLayout());
		
		JMenuBar bar = new JMenuBar();
		frame.setJMenuBar(bar);
		bar.add(fileMenu);
		fileMenu.add(saveImage);
		saveImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				ARComponent arc = ARComposerApp.this.getARComponent();
				int rv = chooser.showSaveDialog(arc);
				if (rv != JFileChooser.APPROVE_OPTION) {return;}
				File file = chooser.getSelectedFile();
	
				@SuppressWarnings("unchecked") //Will fail if the transfer didn't end up with colors
				BufferedImage img = AggregateUtils.asImage((Aggregates<Color>) arc.transferAggregates(), arc.getWidth(), arc.getHeight(), Color.white);
				Util.writeImage(img, file);
			}
		});
		
		saveImage.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		
		JPanel topRow = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill =  GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth =1;
		c.weightx = 1;
		topRow.add(enhanceOptions,c);

		c.fill =  GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 1;
		topRow.add(status,c);
		
		c.fill =  GridBagConstraints.HORIZONTAL;
		c.gridx = 3;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 1;
		topRow.add(axes);
		axes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				display.includeAxes(axes.isSelected());
			}
		});

		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
		controls.add(topRow);
		controls.add(composer);
		
		frame.add(controls, BorderLayout.SOUTH);

		

		final ARComposerApp app = this;
		composer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean rezoom = composer.doZoomWith(app.display);
				update(app.display);
				if (rezoom) {
					display.zoomFit();
				}
			}
		});
		
		update(app.display);
		
		frame.add(display, BorderLayout.CENTER);


		frame.setLocation(200, 0);
		frame.setSize(800, 800);
		frame.validate();
		frame.setVisible(true);
		final ARComponent.Aggregating img = display;
		try {
			SwingUtilities.invokeAndWait(
				new Runnable() {
					public void run() {
						img.zoomFit();
						img.renderAgain();
					}
				}
			);
		} catch (InvocationTargetException | InterruptedException e1) {}
	
		//Plumbing for the top row...
		this.status.startMonitoring(display.renderer());
        enhanceOptions.host(display);	
    }
	
	public void update(ARComponent.Aggregating panel) {
		if (panel.dataset() == composer.dataset()
			&& panel.aggregator().equals(composer.aggregator())) {
		    panel.transfer(composer.transfer());
		} else {
			panel.dataset(composer.dataset(), composer.aggregator(), composer.transfer());
			panel.zoomFit();
		}
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
	
	public ARComponent getARComponent() {return display;}
	public AffineTransform viewTransform() {return display.viewTransform();}
	public void zoomFit() {display.zoomFit();}
	public Rectangle2D dataBounds() {return display.dataBounds();}

	@Override
	public void viewTransform(AffineTransform vt, boolean provisional) {display.viewTransform(vt, provisional);}
	
	

	/**
	 * @param args
	 * @throws Exception 
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {
		String osName = System.getProperty("os.name");
	    if (osName.contains("OS X")) {
	    	System.setProperty("apple.laf.useScreenMenuBar", "true");
	    }

		ARComponent.PERFORMANCE_REPORTING = true;
		RenderUtils.RECORD_PROGRESS = true;
		RenderUtils.REPORT_STEP=1_000_000;
		new ARComposerApp();
	} 
}
