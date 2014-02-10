package ar.app;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;

import ar.app.components.*;
import ar.app.display.ARComponent;
import ar.app.display.AggregatingDisplay;
import ar.app.display.EnhanceHost;
import ar.renderers.ParallelRenderer;
import ar.renderers.RenderUtils;

public class ARApp implements ARComponent.Holder, ar.util.HasViewTransform {
	private final EnhanceHost display = new EnhanceHost(new AggregatingDisplay(new ParallelRenderer()));
	private final JFrame frame = new JFrame();

	private final EnhanceOptions enhanceOptions = new EnhanceOptions();
	private final Presets presets = new Presets(this);
	private final Status status = new Status();

	public ARApp() {
		ar.renderers.RenderUtils.RECORD_PROGRESS = true;
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Abstract Rendering (Demo App)");
		frame.setLayout(new BorderLayout());
		
		JPanel controls = new JPanel(new GridLayout(0,1));
		controls.add(enhanceOptions);
		controls.add(presets);
		controls.add(status);
		controls.add(new JLabel("Double-click zooms to fit.", JLabel.CENTER));

		frame.add(controls, BorderLayout.SOUTH);
		
		
		this.status.startMonitoring(display.renderer());
		enhanceOptions.host(display);

		
		presets.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean rezoom = presets.doZoomWith(ARApp.this.display);
				presets.update(ARApp.this.display);
				if (rezoom) {
					display.zoomFit();
				}
			}
		});
		
		presets.update(display);
		
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
		ARComponent.PERFORMANCE_REPORTING = true;
		RenderUtils.RECORD_PROGRESS = true;
		RenderUtils.REPORT_STEP=1_000_000;
		if (args.length >0 && args[0]!=null && args[0].toUpperCase().equals("-EXT")) {
			ARComposerApp.main(args);
		} else {
			new ARApp();
		}
	} 
}
