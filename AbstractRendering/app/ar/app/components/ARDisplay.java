package ar.app.components;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.image.BufferedImage;

import ar.*;
import ar.renderers.SerialSpatial;
import ar.util.Util;

/**Panel that will draw a set of aggregates on the screen with a given transfer function.**/
public class ARDisplay extends JPanel {
	private static final long serialVersionUID = 1L;
	
	/**Flag to enable/disable performance reporting messages to system.out (defaults to false)**/
	public static boolean PERF_REP = false;

	private final Transfer<?,?> transfer;
	private final Aggregates<?> aggregates;
	private final Renderer renderer = new SerialSpatial();
	private BufferedImage image;
	private Thread renderThread;
	private volatile boolean renderError = false;



	public ARDisplay(Aggregates<?> aggregates, Transfer<?,?> transfer) {
		super();
		this.transfer = transfer;
		this.aggregates = aggregates;
		
	}
	

	@SuppressWarnings("deprecation")
	protected void finalize() {
		if (renderThread != null) {renderThread.stop();}
	}
	
	public ARDisplay withAggregates(Aggregates<?> aggregates) {
		return new ARDisplay(aggregates, transfer);
	}
	
	public  ARDisplay withTransfer(Transfer<?,?> t) {
		return new ARDisplay(aggregates, t);
	}
	
	public Aggregates<?> aggregates() {return aggregates;}
	public Transfer<?,?> transfer() {return transfer;}
	
	private final boolean differentSizes(BufferedImage image, JPanel p) {
		if (image == null) {return false;}
		else {return image.getWidth() != p.getWidth() || image.getHeight() != p.getHeight();}
	}
	
	@Override
	public void paintComponent(Graphics g) {
		
		boolean doRender = (image == null || differentSizes(image, ARDisplay.this)) 
				&& transfer != null && aggregates != null;
		
		if (doRender && ! renderError) {
			renderThread = new Thread(new TransferRender(), "Render Thread");
			renderThread.setDaemon(true);
			renderThread.start();
		}
	
		if (image != null) {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			Graphics2D g2 = (Graphics2D) g;
			g2.drawRenderedImage(image,g2.getTransform());
		} else {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
		}
	}
	
	public final class TransferRender implements Runnable {
		public void run() {
			long start = System.currentTimeMillis();
			int width = ARDisplay.this.getWidth();
			int height = ARDisplay.this.getHeight();

			try {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				Aggregates<Color> colors = renderer.transfer((Aggregates) aggregates, (Transfer) transfer);
				image = Util.asImage(colors, width, height, Util.CLEAR);
				long end = System.currentTimeMillis();
				if (PERF_REP) {System.out.printf("%,d ms (transfer on %d, %d grid)\n", (end-start), image.getWidth(), image.getHeight());}
			} catch (ClassCastException e) {
				renderError = true;
			}
			
			ARDisplay.this.repaint();
		}
	}
	
	public Renderer getRenderer() {return renderer;}
}
