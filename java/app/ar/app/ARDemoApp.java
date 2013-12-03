package ar.app;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;

import ar.Resources;
import ar.app.components.*;
import ar.app.display.ARComponent;
import ar.app.display.AggregatingDisplay;
import ar.app.display.EnhanceHost;
import ar.renderers.RenderUtils;

public class ARDemoApp implements ARComponent.Holder, ar.util.HasViewTransform {
	private final EnhanceHost display = new EnhanceHost(new AggregatingDisplay(Resources.DEFAULT_RENDERER));
	private final JFrame frame = new JFrame();

	private final EnhanceOptions enhanceOptions = new EnhanceOptions();
	private final ClipwarnControl clipwarnControl = new ClipwarnControl();
	private final Presets presets = new Presets(this);
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
		c.gridwidth =1;
		c.weightx = 1;
		controls.add(enhanceOptions,c);

		c.fill =  GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth =1;
		c.weightx = 1;
		controls.add(clipwarnControl,c);

		
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 2;
		c.weightx = 1;
		controls.add(presets, c);
		
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0.5;
		c.gridwidth = 1;
		controls.add(status,c);

		c.gridx = 1;
		c.gridy = 2;		
		c.weightx = 0.5;
		c.gridwidth = 1;
		controls.add(export,c);
		
		JLabel instructions = new JLabel("Double-click zooms to fit.", JLabel.CENTER);
		c.gridx=0;
		c.gridy=3;
		c.weightx=2;
		c.weightx=1;
		controls.add(instructions,c);

		frame.add(controls, BorderLayout.SOUTH);
		
		
		this.status.startMonitoring(display.renderer());
        clipwarnControl.target(display);
		enhanceOptions.host(display);

		
		final ARDemoApp app = this;
		presets.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean rezoom = presets.doZoomWith(app.display);
				presets.update(app.display);
				if (rezoom) {
					display.zoomFit();
				}
			}
		});
		
		presets.update(app.display);
		
		frame.add(display, BorderLayout.CENTER);

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
	public void viewTransform(AffineTransform vt) throws NoninvertibleTransformException {display.viewTransform(vt);}
	
	

	/**
	 * @param args
	 * @throws Exception 
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {
		ARComponent.PERF_REP = true;
		RenderUtils.RECORD_PROGRESS = true;
		new ARDemoApp();
	} 
}
