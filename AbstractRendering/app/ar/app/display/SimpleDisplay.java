package ar.app.display;

import javax.swing.JFrame;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;

import ar.*;
import ar.app.util.MostRecentOnlyExecutor;
import ar.renderers.SerialSpatial;
import ar.util.Util;

/**Panel that will draw a set of aggregates on the screen with a given transfer function.**/
public class SimpleDisplay extends ARComponent {
	private static final long serialVersionUID = 1L;
	
	/**Transfer function to use in rendering*/
	private Transfer<?,?> transfer;
	
	/**Aggregates to render*/
	private Aggregates<?> aggregates;
	
	/**Aggregates to use for specialization of the transfer function.
	 * 
	 * If null, the regular aggregates will be used for transfer specialization.
	 * If non-null, this set of aggregates is used.*/
	private Aggregates<?> refAggregates;

	private final Renderer renderer;
	private BufferedImage image;
	private volatile boolean renderError = false;
	private volatile boolean renderAgain = false;

	protected final ExecutorService renderPool = new MostRecentOnlyExecutor(1, "ARDisplay Render Thread");
	
	public SimpleDisplay(Aggregates<?> aggregates, Transfer<?,?> transfer) {
		this(aggregates, transfer, new SerialSpatial());
	}
	
	public SimpleDisplay(Aggregates<?> aggregates, Transfer<?,?> transfer, Renderer renderer) {
		super();
		this.renderer = renderer;
		this.transfer = transfer;
		this.aggregates = aggregates;
		this.addComponentListener(new ComponentListener(){
			public void componentResized(ComponentEvent e) {SimpleDisplay.this.renderAgain = true;}
			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
	}
	
	protected void finalize() {renderPool.shutdown();}

	/**Set the aggregates set in transfer.  
	 * Used as default set of aggregates if refAggregates is null.
	 */
	public void aggregates(Aggregates<?> aggregates) {
		this.aggregates = aggregates;
		this.refAggregates = null;
		renderAgain = true;
		renderError = false;
		repaint();
	}
	public Aggregates<?> aggregates() {return aggregates;}

	
	public Aggregates<?> refAggregates() {
		 return refAggregates == null ? aggregates : refAggregates;
	}
	
	/**Set of aggregates to use in transfer-function specialization**/
	public void refAggregates(Aggregates<?> aggs) {
		if (this.refAggregates != aggs) {
			this.refAggregates = aggs;
			renderAgain = true;
			renderError = false;
		}
		repaint();
	}
	
	public Transfer<?,?> transfer() {return transfer;}
	public void transfer(Transfer<?,?> transfer) {
		this.transfer = transfer;
		renderAgain = true;
		renderError = false;
		repaint();
	}

	public Renderer renderer() {return renderer;}
	
	
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
	
	//TODO: Fix race condition between "aggregates" and "refAggregates".  May require that whenever you set "aggregtes" then "refAggregates" gets cleared off and related shenanagans elsewhere
	protected final class TransferRender implements Runnable {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void run() {
			try {
				Aggregates<?> aggs = aggregates;
				if (aggs == null) {return;}
				
				long start = System.currentTimeMillis();
				
				Transfer.Specialized ts = transfer.specialize((Aggregates) refAggregates());
				
				Aggregates<Color> colors = renderer.transfer(aggs, ts);
				
				image = Util.asImage(colors, SimpleDisplay.this.getWidth(), SimpleDisplay.this.getHeight(), Util.CLEAR);
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
			
			SimpleDisplay.this.repaint();
		}
	}
		
	/**Utility method to show a set of aggregates w.r.t. a transfer function in its own window.**/
	public static <A> void show(String title, int width, int height, Aggregates<? extends A> aggregates, Transfer<A,Color> transfer) {
		JFrame frame = new JFrame(title);
		frame.setLayout(new BorderLayout());
		frame.setSize(width,height);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.add(new SimpleDisplay(aggregates, transfer), BorderLayout.CENTER);
		frame.setVisible(true);
		frame.revalidate();
		frame.validate();
	}
}
