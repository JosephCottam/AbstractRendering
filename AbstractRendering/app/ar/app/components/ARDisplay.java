package ar.app.components;

import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;

import ar.*;
import ar.app.util.MostRecentOnlyExecutor;
import ar.renderers.SerialSpatial;
import ar.util.Util;

/**Panel that will draw a set of aggregates on the screen with a given transfer function.**/
public class ARDisplay extends JPanel {
	private static final long serialVersionUID = 1L;
	
	/**Flag to enable/disable performance reporting messages to system.out (defaults to false)**/
	public static boolean PERF_REP = false;

	/**Transfer function to use in rendering*/
	private Transfer<?,?> transfer;
	
	/**Aggregates to render*/
	private Aggregates<?> aggregates;
	
	/**Aggregates to use for specialization of the transfer function.
	 * 
	 * If null, the regular aggregates will be used for transfer specialization.
	 * If non-null, this set of aggregates is used.*/
	private Aggregates<?> refAggregates;

	private Renderer renderer = new SerialSpatial();
	private BufferedImage image;
	private volatile boolean renderError = false;
	private volatile boolean renderAgain = false;
	
	private Rectangle2D selected;

	protected final ExecutorService renderPool = new MostRecentOnlyExecutor(1, "ARDisplay Render Thread");
	
	public ARDisplay(Aggregates<?> aggregates, Transfer<?,?> transfer) {
		super();
		this.transfer = transfer;
		this.aggregates = aggregates;
		this.addComponentListener(new ComponentListener(){
			public void componentResized(ComponentEvent e) {ARDisplay.this.renderAgain = true;}
			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
		
		AdjustRange r = new AdjustRange();
		this.addMouseListener(r);
		this.addMouseMotionListener(r);
		this.addKeyListener(r);
	}
	
	protected void finalize() {renderPool.shutdown();}
	
	public void setAggregates(Aggregates<?> aggregates) {
		this.aggregates = aggregates;
		renderAgain = true;
		renderError = false;
	}
	
	public void setRefAggregates(Aggregates<?> aggs) {
		if (this.refAggregates != aggs) {
			this.refAggregates = aggs;
			renderAgain = true;
			renderError = false;
		}
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
		
		if (selected != null) {((Graphics2D) g).draw(selected);}
	}
	
	public final class TransferRender implements Runnable {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void run() {
			try {
				Aggregates<?> aggs = aggregates;
				if (aggs == null) {return;}
				
				long start = System.currentTimeMillis();
				
				Aggregates specAggs = refAggregates == null ? aggs : refAggregates;
				Transfer.Specialized ts = transfer.specialize(specAggs);
				
				Aggregates<Color> colors = renderer.transfer(aggs, ts);
				
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
		frame.add(new ARDisplay(aggregates, transfer), BorderLayout.CENTER);
		frame.setVisible(true);
		frame.revalidate();
		frame.validate();
	}
	
	
	public void setSelection(Rectangle2D r) {
		this.selected = r;
	}
	

	public final class AdjustRange implements MouseListener, MouseMotionListener, KeyListener {
		Point2D start;
		
		public void mousePressed(MouseEvent e) {start = e.getPoint();}
		public void mouseReleased(MouseEvent e) {
			if (start != null) {
				Rectangle2D bounds =bounds(e);
				if (bounds.isEmpty() || bounds.getWidth()*bounds.getHeight()<1) {bounds = null;}
				ARDisplay.this.setSelection(bounds);
				ARDisplay.this.repaint();
			}
			start = null;
		}
		
		public void mouseMoved(MouseEvent e) {}

		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseDragged(MouseEvent e) {
		}
		
		private Rectangle2D bounds(MouseEvent e) {
			double w = Math.abs(start.getX()-e.getX());
			double h = Math.abs(start.getY()-e.getY());
			double x = Math.min(start.getX(), e.getX());
			double y = Math.min(start.getY(), e.getY());
					
			return new Rectangle2D.Double(x,y,w,h);
		}
	
		public void keyTyped(KeyEvent e) {
			if (e.getKeyChar() == 'c') {ARDisplay.this.setSelection(null);}
 		}

		public void keyPressed(KeyEvent e) {}
		public void keyReleased(KeyEvent e) {}
	}
}
