package ar.app.components;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ar.*;
import ar.renderers.SerialSpatial;
import ar.util.Util;

/**Panel that will draw a set of aggregates on the screen with a given transfer function.**/
public class ARDisplay extends JPanel {
	private static final long serialVersionUID = 1L;
	
	/**Flag to enable/disable performance reporting messages to system.out (defaults to false)**/
	public static boolean PERF_REP = false;

	private Transfer<?,?> transfer;
	private Aggregates<?> aggregates;
	private Renderer renderer = new SerialSpatial();
	private BufferedImage image;
	private volatile boolean renderError = false;
	private volatile boolean renderAgain = false;
	protected final ExecutorService renderPool; 
	protected final boolean ownedPool; //TODO: Perhaps remove and use a render pool filled with daemon threads
	


	public ARDisplay(Aggregates<?> aggregates, Transfer<?,?> transfer, ExecutorService pool) {
		super();
		this.transfer = transfer;
		this.aggregates = aggregates;
		this.addComponentListener(new ComponentListener(){
			public void componentResized(ComponentEvent e) {ARDisplay.this.renderAgain = true;}
			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
		if (pool == null) {
			renderPool = Executors.newFixedThreadPool(1);
			ownedPool = true;
		}
		else {
			renderPool = pool;
			ownedPool = false;
		} //TODO: Redoing painting to use futures...
	}
	

	protected void finalize() {
		if (ownedPool) {renderPool.shutdown();}
	}
	
	public void setAggregates(Aggregates<?> aggregates) {
		this.aggregates = aggregates;
		renderAgain = true;
		renderError = false;
	}
	
	public  void withTransfer(Transfer<?,?> transfer) {
		this.transfer = transfer;
		renderAgain = true;
		renderError = false;
	}
	
	public void withRenderer(Renderer renderer) {
		this.renderer = renderer;
		renderAgain = true;
		renderError = false;
	}
	
	public Aggregates<?> aggregates() {return aggregates;}
	public Transfer<?,?> transfer() {return transfer;}
	
	@Override
	public void paintComponent(Graphics g) {
		boolean doRender = (renderAgain || image == null) 
				&& transfer != null && aggregates != null;
		
		if (doRender && ! renderError) {
			renderPool.execute(new TransferRender());
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
			try {
				long start = System.currentTimeMillis();

				@SuppressWarnings({ "rawtypes", "unchecked" })
				Aggregates<Color> colors = renderer.transfer((Aggregates) aggregates, (Transfer) transfer);
				image = Util.asImage(colors, ARDisplay.this.getWidth(), ARDisplay.this.getHeight(), Util.CLEAR);
				long end = System.currentTimeMillis();
				if (PERF_REP) {
					System.out.printf("%d ms (transfer on %d x %d grid)\n", 
							(end-start), image.getWidth(), image.getHeight());
				}
			} catch (ClassCastException e) {
				renderError = true;
			} finally {
				renderAgain = false;
			}
			
			ARDisplay.this.repaint();
		}
	}
	
	public Renderer getRenderer() {return renderer;}
	
	public static <A> void show(int width, int height, Aggregates<A> aggregates, Transfer<A,Color> transfer) {
		JFrame frame = new JFrame("ARDisplay");
		frame.setLayout(new BorderLayout());
		frame.setSize(width,height);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.add(new ARDisplay(aggregates, transfer, null), BorderLayout.CENTER);
		frame.setVisible(true);
		frame.revalidate();
		frame.validate();
	}
}
