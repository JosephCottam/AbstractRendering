package ar.app.display;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.app.components.LabeledItem;
import ar.util.Util;

/**Host a panel with an optional draw-on overlay**/ 
public class OverlayHost extends ARComponent.Aggregating {
	private static final long serialVersionUID = -6449887730981205865L;
	
	private ARComponent.Aggregating hosted;
	private JComponent overlay;
	
	/**Host the given component in the overlay.**/
	public OverlayHost(ARComponent.Aggregating hosted) {	
		this.hosted = hosted;
		this.overlay = new SelectionOverlay(this);
		this.add(overlay);
		this.add(hosted);
		overlay.setVisible(false);
	}
	
	public void layout() {
		Rectangle b = this.getBounds();
		hosted.setBounds(b);
		overlay.setBounds(b);
	}

	/**Is the overlay currently visible?**/
	public boolean showOverlay() {return overlay.isVisible();}
	
	/**Set the overlay visibility state.**/
	public void showOverlay(boolean show) {overlay.setVisible(show);}
	

	private static class SelectionOverlay extends JComponent implements Selectable {
		private static final long serialVersionUID = 9079768489874376280L;
		
		public Color MASKED = new Color(100,100,100,50);
		public Color SELECTED = new Color(200,0,0,50);
		public Color PROVISIONAL = Util.CLEAR;
		
		private Rectangle2D selection = null;
		private boolean provisional = false;
		private final OverlayHost host;
		
		
		public SelectionOverlay(OverlayHost host) {
			AdjustRange r = new AdjustRange(this);
			this.addMouseListener(r);
			this.addMouseMotionListener(r);
			this.host = host;
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2= (Graphics2D) g;
			Area a =new Area(this.getBounds());
			
			if (selection != null) {
				g2.setColor(MASKED);
				g2.fill(a);

				g.setColor(SELECTED);
				g2.fill(selection);
				a.subtract(new Area(selection));
			} else {
				g2.setColor(SELECTED);
				g2.fill(a);
			}
		}

		public void setSelection(Rectangle2D bounds) {
			this.selection = bounds;
			this.provisional=false;
			this.repaint();
		}
		
		public void setProvisional(Rectangle2D bounds) {
			this.selection = bounds;
			this.provisional=true;
			this.repaint();
		}
	}
	
	private interface Selectable {
		public void setSelection(Rectangle2D bounds);
		public void setProvisional(Rectangle2D bounds);
	}
	
	private final static class AdjustRange implements MouseListener, MouseMotionListener {
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
			if (start != null) {
				Rectangle2D bounds =bounds(e);
				if (bounds.isEmpty() || bounds.getWidth()*bounds.getHeight()<1) {bounds = null;}
				target.setProvisional(bounds);
			}
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
	
	/**Toggle control for the given overlay.**/
	public static final class Control extends JPanel {
		private static final long serialVersionUID = -4922729009197379804L;
		private final JCheckBox box = new JCheckBox();
		private OverlayHost host=null;
		
		/**Create a toggle control.  Will have no effect until 'host' is set.**/
		public Control() {
			JPanel item = new LabeledItem("Show Overlay:", box);
			this.add(item);
			
			box.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						boolean show = ((JCheckBox)e.getSource()).isSelected();
						host().showOverlay(show);
						host().repaint();
					} catch (Exception ex) {/**Ignored**/}
				}
				
			});
		}
		
		/**Get the target host control.**/
		public OverlayHost host() {return host;}
		/**Set the target host control.**/
		public void host(OverlayHost host) {this.host = host;}
	}
}
