package ar.app.display;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;

public class EnhanceHost extends ARComponent.Aggregating {
	private static final long serialVersionUID = -6449887730981205865L;
	
	private JLayeredPane layers = new JLayeredPane(); 
	private ARComponent.Aggregating hosted;
	private JComponent overlay= new SelectionOverlay();;
	
	public EnhanceHost(ARComponent.Aggregating hosted) {	
		this.hosted = hosted;
		layers.setLayer(this.hosted, JLayeredPane.DEFAULT_LAYER);
		layers.setLayer(overlay, JLayeredPane.PALETTE_LAYER);
	}
	
	public void validate() {
		Rectangle b = this.getBounds();
		hosted.setBounds(b);
		overlay.setBounds(b);
	}
	

	public static class SelectionOverlay extends JPanel implements Selectable {
		private static final long serialVersionUID = 9079768489874376280L;
		
		private Rectangle2D selected;
		
		public SelectionOverlay() {
			AdjustRange r = new AdjustRange(this);
			this.addMouseListener(r);
			this.addMouseMotionListener(r);
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			if (selected != null) {
				System.out.println("Drawing...");
				g.setColor(Color.black);
				((Graphics2D) g).draw(selected);
			} else {
				System.out.println("Not Drawing...");
			}
		}

		public void setSelection(Rectangle2D bounds) {this.selected = bounds;}
	}
	

	public interface Selectable {public void setSelection(Rectangle2D bounds);}
	
	public final static class AdjustRange implements MouseListener, MouseMotionListener {
		Point2D start;
		final Selectable target;
		public AdjustRange(Selectable target) {this.target = target;}

		public void mousePressed(MouseEvent e) {start = e.getPoint();}
		public void mouseReleased(MouseEvent e) {
			if (start != null) {
				Rectangle2D bounds =bounds(e);
				if (bounds.isEmpty() || bounds.getWidth()*bounds.getHeight()<1) {bounds = null;}
				target.setSelection(bounds);
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
	}

	public Transfer<?, ?> transfer() {return hosted.transfer();}
	public void transfer(Transfer<?, ?> t) {hosted.transfer(t);}
	public Aggregates<?> aggregates() {return hosted.aggregates();}
	public void aggregates(Aggregates<?> aggregates) {hosted.aggregates(aggregates);}
	public Aggregates<?> refAggregates() {return hosted.refAggregates();}
	public void refAggregates(Aggregates<?> aggregates) {hosted.refAggregates(aggregates);}
	public Glyphset<?> dataset() {return hosted.dataset();}
	public void dataset(Glyphset<?> data) {hosted.dataset(data);}
	public Aggregator<?, ?> aggregator() {return hosted.aggregator();}
	public void aggregator(Aggregator<?, ?> aggregator) {hosted.aggregator(aggregator);}
	public Renderer renderer() {return hosted.renderer();}

	public void zoomFit() {hosted.zoomFit();}
	public AffineTransform viewTransform() {return hosted.viewTransform();}
	public void viewTransform(AffineTransform vt) throws NoninvertibleTransformException {hosted.viewTransform(vt);} 
}
