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

import ar.app.components.*;
import ar.app.components.sequentialComposer.SequentialComposer;
import ar.app.display.ARComponent;
import ar.app.display.AggregatingDisplay;
import ar.app.display.EnhanceHost;
import ar.renderers.ParallelRenderer;
import ar.renderers.RenderUtils;


//TODO: Add "subset input", useful for contours
//TODO: Add "Specialize From Here"
public class ARDemoApp implements ARComponent.Holder, ar.util.HasViewTransform {
	private final EnhanceHost display = new EnhanceHost(new AggregatingDisplay(new ParallelRenderer()));
	private final JFrame frame = new JFrame();

	private final EnhanceOptions enhanceOptions = new EnhanceOptions();
	private final ClipwarnControl clipwarnControl = new ClipwarnControl();
	private final Status status = new Status();
	private final SequentialComposer composer = new SequentialComposer();
	
	public ARDemoApp() {
		ar.renderers.RenderUtils.RECORD_PROGRESS = true;
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Abstract Rendering (Demo App)");
		frame.setLayout(new BorderLayout());
		
		JPanel topRow = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill =  GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth =1;
		c.weightx = 1;
		topRow.add(enhanceOptions,c);

		c.fill =  GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth =1;
		c.weightx = 1;
		topRow.add(clipwarnControl,c);

		c.fill =  GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 1;
		topRow.add(status,c);
				

		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
		controls.add(topRow);
		controls.add(composer);
		
		frame.add(controls, BorderLayout.SOUTH);

		

		final ARDemoApp app = this;
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
		
		this.status.startMonitoring(display.renderer());
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
	public void viewTransform(AffineTransform vt) throws NoninvertibleTransformException {display.viewTransform(vt);}
	
	

	/**
	 * @param args
	 * @throws Exception 
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {
		ARComponent.PERF_REP = true;
		RenderUtils.RECORD_PROGRESS = true;
		RenderUtils.REPORT_STEP=1_000_000;
		new ARDemoApp();
	} 
}
