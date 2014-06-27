package ar.app.display;

import javax.swing.JFrame;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import ar.*;
import ar.aggregates.AggregateUtils;
import ar.app.util.MostRecentOnlyExecutor;
import ar.renderers.ParallelRenderer;
import ar.util.Axis;
import ar.util.Util;

/**Panel that will draw a set of aggregates on the screen with a given transfer function.**/
public class TransferDisplay extends ARComponent {
	private static final long serialVersionUID = 1L;
	
	/**Transfer function to use in rendering*/
	private Transfer<?,?> transfer;
	
	/**Aggregates to render*/
	private Aggregates<?> aggregates;
	
	private Axis.Descriptor<?,?> axes;

	/**What transform was used to produce the base aggregates.**/ 
	private volatile AffineTransform renderedTransform = new AffineTransform();

	
	/**Aggregates to use for specialization of the transfer function.
	 * 
	 * If null, the full set of aggregates will be used for transfer specialization.
	 **/
	private volatile Aggregates<?> refAggregates;

	private volatile Aggregates<?> postTransferAggregates;
	private volatile BufferedImage image;

	/**Transform being used to put geometry on the screen.
	 * This is the full view transform, moving from geometry all the way to screen pixels,
	 * so some of this action ***may*** be captured in the renderedTransform.
	 * Otherwise said, you can use the view transform by itself to figure out where things 
	 * will be drawn.
	 * **/
	private volatile AffineTransform viewTransform = new AffineTransform();
	
	
	private final Renderer renderer;
	private volatile boolean renderError = false;

	protected final ExecutorService renderPool = new MostRecentOnlyExecutor(1, this.getClass().getSimpleName() + " Render Thread");
	
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
			public void componentResized(ComponentEvent e) {repaint();}
			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
	}
	
	protected void finalize() {renderPool.shutdown();}

	/**Set the aggregates set in transfer.  
	 * Used as default set of aggregates if refAggregates is null.
	 */
	public void aggregates(Aggregates<?> aggregates, AffineTransform renderedTransform, Axis.Descriptor<?,?> axes) {
		this.aggregates = aggregates;
		this.renderedTransform = renderedTransform;
		this.axes = axes;
		renderError = false;
		postTransferAggregates = null;
		//viewTransform(new AffineTransform());
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
		Graphics2D g2 = (Graphics2D) g;
		
		if (postTransferAggregates == null 
				&& transfer !=null && aggregates !=null 
				&& !renderError) {
			renderPool.execute(new TransferRender());
		}

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		if (image != null) {
			g2.drawRenderedImage(image,offsetTransform(viewTransform, renderedTransform));
		}
		
		if (axes != null) {Axis.drawAxes(axes, g2, viewTransform);}
	}	
	
	/**In some cases, some of the view transform is reflected in the rendered transform,
	 * the display needs a modified view transform to properly position/scale post-transfer results.
	 * This calculates that new view transform.
	 * 
	 * If the rendered transform is non-invertible/null or other exception occurs, 
	 * then the identity transform is returned.
	 *  
	 * @param vt The view transform required
	 * @param rt The rendered transform assumed to have been already performed
	 * @return
	 */
	private static AffineTransform offsetTransform(AffineTransform vt, AffineTransform rt) {
		AffineTransform nvt;
		try {
			nvt = rt.createInverse();
			nvt.preConcatenate(vt);
			return nvt;
		} catch (Exception e) {
			return new AffineTransform();
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
					Rectangle r = AggregateUtils.bounds(postTransferAggregates);
					System.out.printf("%d ms (transfer on %d x %d grid)\n", 
							(end-start), 
							Math.max(r.width, TransferDisplay.this.getWidth()), 
							Math.max(r.height, TransferDisplay.this.getHeight()));
				}
			} catch (ClassCastException e) {
				renderError = true;
				if (PERFORMANCE_REPORTING) {e.printStackTrace();}
			} 
			
			TransferDisplay.this.repaint();
		}
	}
		
	@Override public AffineTransform viewTransform() {return new AffineTransform(viewTransform);}

	@Override 
	public void viewTransform(AffineTransform vt, boolean provisional) {
		if (vt == null) {vt = new AffineTransform();}
		this.viewTransform = new AffineTransform(vt);
		repaint();
	}

	@Override
	public void zoomFit() {
		Rectangle2D content = dataBounds();
		if (dataBounds() ==null || content.isEmpty()) {return;}
		viewTransform(Util.zoomFit(content, this.getWidth(), this.getHeight()), false);
	}

	@Override
	public Rectangle2D dataBounds() {
		Aggregates<?> aggs = aggregates;
		Rectangle2D content = (aggs == null ? null : AggregateUtils.bounds(aggs));
		return content;
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

}
