package ar.app.display;

import javax.swing.JFrame;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;

import ar.*;
import ar.aggregates.AggregateUtils;
import ar.app.util.MostRecentOnlyExecutor;
import ar.renderers.ParallelRenderer;
import ar.util.Util;

/**Panel that will draw a set of aggregates on the screen with a given transfer function.**/
public class TransferDisplay extends ARComponent {
	private static final long serialVersionUID = 1L;
	
	/**Transfer function to use in rendering*/
	private Transfer<?,?> transfer;
	
	/**Aggregates to render*/
	private Aggregates<?> aggregates;

	/**What transform was used to produce the base aggregates.**/ 
	private volatile AffineTransform renderedTransform = new AffineTransform();

	
	/**Aggregates to use for specialization of the transfer function.
	 * 
	 * If null, the full set of aggregates will be used for transfer specialization.
	 **/
	private volatile Aggregates<?> refAggregates;

	private volatile Aggregates<?> postTransferAggregates;
	private volatile BufferedImage image;

	/**Transform used to display aggregates on the screen.
	 * 
	 * Because this class does NOT have access to raw geometry, this transform
	 * essentially describes how to position/scale the input aggregates with regards
	 * to the screen space.   
	 * **/
	private volatile AffineTransform viewTransform = new AffineTransform();
	
	
	private final Renderer renderer;
	private volatile boolean renderError = false;

	protected final ExecutorService renderPool = new MostRecentOnlyExecutor(1, "SimpleDisplay Render Thread");
	
	public TransferDisplay(Renderer renderer) {this(null, null, null, renderer);}
	
	public TransferDisplay(Aggregates<?> aggregates, Transfer<?,?> transfer) {
		this(aggregates, new AffineTransform(), transfer);
	}

	public TransferDisplay(Aggregates<?> aggregates, AffineTransform rendererd, Transfer<?,?> transfer) {
		this(aggregates, rendererd, transfer, new ParallelRenderer());
	}
	
	public TransferDisplay(Aggregates<?> aggregates, AffineTransform rendererdTransform, Transfer<?,?> transfer, Renderer renderer) {
		super();
		this.renderer = renderer;
		this.transfer = transfer;
		this.aggregates = aggregates;
		this.renderedTransform = rendererdTransform != null ? renderedTransform : new AffineTransform();
		this.addComponentListener(new ComponentListener(){
			public void componentResized(@SuppressWarnings("unused") ComponentEvent e) {repaint();}
			public void componentMoved(@SuppressWarnings("unused") ComponentEvent e) {}
			public void componentShown(@SuppressWarnings("unused") ComponentEvent e) {}
			public void componentHidden(@SuppressWarnings("unused") ComponentEvent e) {}
		});
	}
	
	protected void finalize() {renderPool.shutdown();}

	/**Set the aggregates set in transfer.  
	 * Used as default set of aggregates if refAggregates is null.
	 */
	public void aggregates(Aggregates<?> aggregates, AffineTransform renderedTransform) {
		this.aggregates = aggregates;
		this.renderedTransform = renderedTransform;
		renderError = false;
		postTransferAggregates = null;
		viewTransform(new AffineTransform());
		repaint();
	}
	
	public Aggregates<?> aggregates() {return aggregates;}
	public Aggregates<?> transferAggregates() {return postTransferAggregates;}

	
	public Aggregates<?> refAggregates() {
		 return refAggregates == null ? aggregates : refAggregates;
	}
	
	/**Set of aggregates to use in transfer-function specialization**/
	public void refAggregates(Aggregates<?> aggs) {
		if (this.refAggregates != aggs) {
			this.refAggregates = aggs;
			renderError = false;
			postTransferAggregates = null;
		}
		repaint();
	}
	
	public Transfer<?,?> transfer() {return transfer;}
	public void transfer(Transfer<?,?> transfer) {
		this.transfer = transfer;
		postTransferAggregates = null;
		renderError = false;
		repaint();
	}

	public void renderAgain() {
		postTransferAggregates = null;
		renderError=false;
		repaint();
	}
	
	public Renderer renderer() {return renderer;}
	
	
	@Override
	public void paintComponent(Graphics g) {
		
		if (postTransferAggregates == null 
				&& transfer !=null && aggregates !=null 
				&& !renderError) {
			renderPool.execute(new TransferRender());
		}

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		if (image != null) {
			double tx = viewTransform.getTranslateX()-renderedTransform.getTranslateX();
			double ty = viewTransform.getTranslateY()-renderedTransform.getTranslateY();
			
			AffineTransform draw = AffineTransform.getTranslateInstance(tx, ty);
			
			System.out.println("----------");
			System.out.println("R: " + renderedTransform.getTranslateX() + " : " + renderedTransform.getScaleX());
			System.out.println("V: " + viewTransform.getTranslateX() + ":" + viewTransform.getScaleX());
			System.out.println("D: " + draw.getTranslateX());

			Graphics2D g2 = (Graphics2D) g;
			g2.drawRenderedImage(image,new AffineTransform());

//			AffineTransform restore = g2.getTransform();
//			g2.setTransform(draw);
//			g2.setColor(Color.BLACK);
//			g2.draw(new Rectangle2D.Double(-.5,-.5,1,1));
//			g2.setTransform(restore);
		}
	}
	
	//TODO: Fix race condition between "aggregates" and "refAggregates".  May require that whenever you set "aggregates" then "refAggregates" gets cleared off and related shenanagans elsewhere
	protected final class TransferRender implements Runnable {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void run() {
			try {
				Aggregates<?> aggs = aggregates;
				if (aggs == null) {return;}
				
				long start = System.currentTimeMillis();
				
				Transfer.Specialized ts = transfer.specialize((Aggregates) refAggregates());
				postTransferAggregates = renderer.transfer(aggs, ts);
				
				int width = TransferDisplay.this.getWidth();
				int height = TransferDisplay.this.getHeight();
				if (postTransferAggregates.defaultValue() instanceof Color ) {
					image = AggregateUtils.asImage((Aggregates<Color>) postTransferAggregates, width, height, Util.CLEAR);
				} else {
					image = null;
				}
				
				long end = System.currentTimeMillis();
				if (PERFORMANCE_REPORTING) {
					System.out.printf("%d ms (transfer on %d x %d grid)\n", 
							(end-start), TransferDisplay.this.getWidth(), TransferDisplay.this.getHeight());
				}
			} catch (ClassCastException e) {
				renderError = true;
				if (PERFORMANCE_REPORTING) {e.printStackTrace();}
			} 
			
			TransferDisplay.this.repaint();
		}
	}
		
	/**Utility method to show a set of aggregates w.r.t. a transfer function in its own window.**/
	public static <A> void show(String title, int width, int height, Aggregates<? extends A> aggregates, Transfer<A,Color> transfer) {
		JFrame frame = new JFrame(title);
		frame.setLayout(new BorderLayout());
		frame.setSize(width,height);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.add(new TransferDisplay(aggregates, transfer), BorderLayout.CENTER);
		frame.setVisible(true);
		frame.revalidate();
		frame.validate();
	}

	@Override public AffineTransform viewTransform() {return new AffineTransform(viewTransform);}

	@Override
	public void viewTransform(AffineTransform vt) {
		if (vt == null) {vt = new AffineTransform();}
		this.viewTransform = new AffineTransform(vt);
		repaint();
	}

	@Override
	public void zoomFit() {
		Rectangle2D content = dataBounds();
		if (dataBounds() ==null || content.isEmpty()) {return;}
		viewTransform(Util.zoomFit(content, this.getWidth(), this.getHeight()));
	}

	@Override
	public Rectangle2D dataBounds() {
		Aggregates<?> aggs = aggregates;
		Rectangle2D content = (aggs == null ? null : AggregateUtils.bounds(aggs));
		return content;
	}
}
