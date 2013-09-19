package ar.app.display;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
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
import ar.aggregates.FlatAggregates;
import ar.app.components.LabeledItem;

/**Host a panel, add to it a draw-on overlay and enhance-region capability.**/ 
public class EnhanceHost extends ARComponent.Aggregating {
	private static final long serialVersionUID = -6449887730981205865L;
	
	private SubsetDisplay hosted;
	private SelectionOverlay overlay;
	private boolean enableEnhance;

	private boolean redoRefAggregates = false;
	
	/**Host the given component in the overlay.**/
	public EnhanceHost(SubsetDisplay hosted) {	
		this.hosted = hosted;
		this.overlay = new SelectionOverlay(this);
		this.add(overlay);
		this.add(hosted);
		overlay.setVisible(false);
		
		hosted.addAggregatesChangedListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {forceNewRefAggregates();}
		});
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

	/**Force a refresh of the ref aggregates and trigger a repaint.**/
	public void forceNewRefAggregates() {
		redoRefAggregates = true;
		this.repaint();
	}
	
	/**Get the subset of aggregates that corresponds to the currently selected region.
	 * @return null if no selection; otherwise a subset of aggregates.
	 */
	public Aggregates<?> subset() {
		if (overlay.selection == null || overlay.selection.isEmpty()) {return null;}
		
		Aggregates<?> aggs = hosted.aggregates();
		AffineTransform rt = hosted.renderTransform();
		Rectangle selection = rt.createTransformedShape(overlay.selection).getBounds();
		
		Aggregates<?> subset = FlatAggregates.subset(
				aggs, 
				selection.x, selection.y, 
				selection.x+selection.width, selection.y+selection.height);
		return subset;
	}
	
	public void enableEnhance(boolean enable) {
		this.enableEnhance = enable;
		redoRefAggregates  = true;
		this.repaint();
	}
	
	public void paintComponent(Graphics g) {
		if (redoRefAggregates) {
			if (enableEnhance) {
				Aggregates<?> subset = subset();
				hosted.refAggregates(subset);
			} else {
				hosted.refAggregates(null);
			}
			redoRefAggregates = false;
		}
		
		super.paintComponent(g);
	}

	public Transfer<?, ?> transfer() {return hosted.transfer();}
	public void transfer(Transfer<?, ?> t) {hosted.transfer(t);}
	public Aggregates<?> aggregates() {return hosted.aggregates();}
	public void aggregates(Aggregates<?> aggregates) {hosted.aggregates(aggregates);}
	public Aggregates<?> refAggregates() {return hosted.refAggregates();}
	public void refAggregates(Aggregates<?> aggregates) {hosted.refAggregates(aggregates);}
	public Glyphset<?> dataset() {return hosted.dataset();}
	
	public void dataset(Glyphset<?> data) {
		hosted.dataset(data);
		overlay.clear();
	}
	
	public Aggregator<?, ?> aggregator() {return hosted.aggregator();}
	public void aggregator(Aggregator<?, ?> aggregator) {hosted.aggregator(aggregator);}
	public Renderer renderer() {return hosted.renderer();}

	public void zoomFit() {hosted.zoomFit();}
	public AffineTransform viewTransform() {return hosted.viewTransform();}
	public void viewTransform(AffineTransform vt) throws NoninvertibleTransformException {hosted.viewTransform(vt);}
	

	/**Component to store and display a selection.**/
	private static class SelectionOverlay extends JComponent implements Selectable {
		private static final long serialVersionUID = 9079768489874376280L;
		
		/**Color to make 'masked off' areas.**/
		public Color MASKED = new Color(100,100,100,50);
		/**Color to make 'selected' areas.**/
		public Color SELECTED = new Color(200,0,0,50);
		
		/**Color to indicate a provisional selection.**/
		public Color PROVISIONAL = Color.black;
		
		/**Current selection in dataset coordinates.**/
		private Rectangle2D selection = null;
		
		/**Is this currently a provisional selection?**/
		private boolean provisional = false;
		
		/**Hosting object.**/
		private final EnhanceHost host;
		
		public SelectionOverlay(EnhanceHost host) {
			AdjustRange r = new AdjustRange(this);
			this.addMouseListener(r);
			this.addMouseMotionListener(r);
			this.host = host;
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2= (Graphics2D) g;
			Area a =new Area(this.getBounds());
			
			Shape s = host.viewTransform().createTransformedShape(selection);
			
			if (selection != null) {
				g.setColor(SELECTED);
				g2.fill(s);
				a.subtract(new Area(s));

				if (provisional) {
					g2.setColor(PROVISIONAL);
					g2.draw(s);
				}

				g2.setColor(MASKED);
				g2.fill(a);
			} else {
				g2.setColor(SELECTED);
				g2.fill(a);
			}
			
		}

		public void clear() {
			this.selection = null; 
			provisional = false;
		}
		
		/**Set the current selection to a new value.
		 * To indicate transient state, set the "provisional" flag.
		 * 
		 * TODO: Add flag for "extend" vs "replace"
		 * 
		 * @param bounds Selection bounds in screen-space
		 * @param provisional Flag passed bounds as provisional selection
		 ***/
		public void setSelection(Rectangle2D bounds, boolean provisional) {
			try	{
				//Convert from screen-space to canvas space
				this.selection = host.viewTransform().createInverse().createTransformedShape(bounds).getBounds2D();
			} catch (Exception e) {/*Ignore...should be impossible...should be.*/}
			this.provisional=provisional;
			
			if (!provisional) {host.forceNewRefAggregates();}
			
			this.repaint();
		}
	}
	
	/**Interface indicating a thing has a selection region associated with it.**/
	private interface Selectable {
		public void clear();
		public void setSelection(Rectangle2D bounds, boolean provisional);
	}

	//TODO: Add keyboard support for "clear" and "invert"
	//TODO: Add multi-region selection
	private final static class AdjustRange implements MouseListener, MouseMotionListener {
		Point2D start;
		final Selectable target;
		public AdjustRange(Selectable target) {this.target = target;}

		public void mousePressed(MouseEvent e) {start = e.getPoint();}
		public void mouseReleased(MouseEvent e) {
			if (start != null) {
				Rectangle2D bounds =bounds(e);
				if (bounds.isEmpty() || bounds.getWidth()*bounds.getHeight()<1) {bounds = null;}
				target.setSelection(bounds, false);
			}
			start = null;
		}
		
		public void mouseClicked(MouseEvent e) {target.clear();}
		public void mouseDragged(MouseEvent e) {
			if (start != null) {
				Rectangle2D bounds =bounds(e);
				if (bounds.isEmpty() || bounds.getWidth()*bounds.getHeight()<1) {bounds = null;}
				target.setSelection(bounds, true);
			}
		}

		private Rectangle2D bounds(MouseEvent e) {
			double w = Math.abs(start.getX()-e.getX());
			double h = Math.abs(start.getY()-e.getY());
			double x = Math.min(start.getX(), e.getX());
			double y = Math.min(start.getY(), e.getY());
					
			return new Rectangle2D.Double(x,y,w,h);
		}
		
		public void mouseMoved(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
	}
	
	/**Toggle control for the given overlay.**/
	public static final class Control extends JPanel {
		private static final long serialVersionUID = -4922729009197379804L;
		private final JCheckBox box = new JCheckBox();
		private EnhanceHost host=null;
		
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
		public EnhanceHost host() {return host;}
		/**Set the target host control.**/
		public void host(EnhanceHost host) {this.host = host;}
	}
}
