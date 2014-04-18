package ar.app.display;

import java.awt.BasicStroke;
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
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyphset;
import ar.Renderer;
import ar.Transfer;
import ar.aggregates.wrappers.SubsetWrapper;
import ar.app.util.LabeledItem;
import ar.glyphsets.BoundingWrapper;
import ar.util.Util;

/**Host a panel, add to it a draw-on overlay and enhance-region capability.**/ 
public class EnhanceHost extends ARComponent.Aggregating {
	private static final long serialVersionUID = -6449887730981205865L;
	
	private static final Color SHOW_ENHANCED  = new Color(100,149,237);
	
	private AggregatingDisplay hosted;
	private SelectionOverlay overlay;
	private EnhancedOverlay enhanced = new EnhancedOverlay();
	private boolean enhanceEnabled;
	private boolean limitEnabled;

	private boolean redoRefAggregates = true;
	
	/**Host the given component in the overlay.**/
	public EnhanceHost(AggregatingDisplay hosted) {	
		this.hosted = hosted;
		this.overlay = new SelectionOverlay(this);
		this.add(overlay);
		this.add(enhanced);
		this.add(hosted);
		
		overlay.setVisible(false);
		enhanced.setVisible(false);
		
		hosted.addAggregatesChangedListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {forceNewRefAggregates();}
		});
	}
	
	public void layout() {
		Rectangle b = this.getBounds();
		hosted.setBounds(b);
		enhanced.setBounds(b);
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
	public <A> Aggregates<?> subset() {
		if (overlay.selected == null) {return null;}
		
		@SuppressWarnings("unchecked")
		Aggregates<A> aggs = (Aggregates<A>) hosted.aggregates();
		AffineTransform rt = hosted.renderTransform();
		Area selection = overlay.selectedArea().createTransformedArea(rt);
		Rectangle bounds = selection.getBounds();
		
		int lowX = bounds.x;
		int lowY = bounds.y;
		int highX = (int) bounds.getMaxX();
		int highY = (int) bounds.getMaxY();
		
		return new SubsetWrapper<>(aggs, lowX, lowY, highX, highY);
	}
	
	public void enableEnhance(boolean enable) {
		this.enhanceEnabled = enable;
		redoRefAggregates  = true;
		enhanced.setVisible(enable);
		this.repaint();
		
	}
	
	///Subset related options ---------------------------------------------------------------------------
	public void updateLimit() {
		Glyphset<?,?> replacement;
		if (enableLimit()) {
			Glyphset<?,?> original = hosted.dataset();
			if (original instanceof BoundingWrapper) {original = ((BoundingWrapper<?,?>) original).base();}
			replacement = new BoundingWrapper<>(original, limitBounds());
		} else {
			replacement = hosted.dataset();
			if (replacement instanceof BoundingWrapper) {replacement = ((BoundingWrapper<?,?>) replacement).base();}
			
		}		
		hosted.dataset(replacement, hosted.aggregator, hosted.transfer(), false);
		hosted.zoomFit();
		this.repaint();
	}
	
	public Rectangle2D limitBounds() {return overlay.selected.getBounds2D();}
	public boolean enableLimit() {return limitEnabled;}
	public void enableLimit(boolean limit) {limitEnabled = limit; updateLimit();}

	
	public void paint(Graphics g) {
		if (redoRefAggregates) {
			if (enhanceEnabled) {
				Aggregates<?> subset = subset();
				hosted.refAggregates(subset);
			} else {
				hosted.refAggregates(hosted.aggregates());
			}
			redoRefAggregates = false;
		}
		super.paint(g);
		
		AffineTransform vt = hosted.viewTransform();
		if (enhanceEnabled && vt != null && overlay.selectedArea() != null) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setColor(SHOW_ENHANCED);
			g2.setStroke(new BasicStroke(1f));
			g2.draw(overlay.selectedArea().createTransformedArea(vt));
		}
	}

	public Transfer<?, ?> transfer() {return hosted.transfer();}
	public void transfer(Transfer<?, ?> t) {hosted.transfer(t);}
	public Aggregates<?> transferAggregates() {return hosted.transferAggregates();}
	public Aggregates<?> aggregates() {return hosted.aggregates();}
	public void aggregates(Aggregates<?> aggregates, AffineTransform renderTransform) {
		hosted.aggregates(aggregates, renderTransform);
	}
	public Aggregates<?> refAggregates() {return hosted.refAggregates();}
	public void refAggregates(Aggregates<?> aggregates) {hosted.refAggregates(aggregates);}
	public Glyphset<?,?> dataset() {
		if (hosted.dataset() instanceof BoundingWrapper) {
			return ((BoundingWrapper<?,?>) hosted.dataset()).base();
		} else {return hosted.dataset();}
	}
	
	public void renderAgain() {hosted.renderAgain();}
	
	public void dataset(Glyphset<?,?> data, Aggregator<?,?> aggregator, Transfer<?,?> transfer) {
		hosted.dataset(data, aggregator, transfer);
		overlay.clear();
	}
	
	public Aggregator<?, ?> aggregator() {return hosted.aggregator();}
	public Renderer renderer() {return hosted.renderer();}

	public void zoomFit() {hosted.zoomFit();}
	public AffineTransform viewTransform() {return hosted.viewTransform();}
	public AffineTransform renderTransform() {return hosted.renderTransform();}
	public void viewTransform(AffineTransform vt, boolean provisional) {hosted.viewTransform(vt, provisional);}
	public Rectangle2D dataBounds() {return hosted.dataBounds();}

	private static final int borderSize = 5;
	private static BufferedImage makeImage(int size, Color stripes, Color spaces) {
		BufferedImage img = new BufferedImage(size,size, BufferedImage.TYPE_4BYTE_ABGR);
		for (int x=0; x<size; x++) {
			for (int y=0; y<size; y++) {
				if (x ==y || x==y+1 || x==y-1 
						|| (x==0 && y==size-1) 
						|| (x==size-1 && y==0)) {img.setRGB(x, y, stripes.getRGB());}
				else {img.setRGB(x, y, spaces.getRGB());}
			}
		}
		return img;
	}

	private static BufferedImage flip(BufferedImage src) {
		BufferedImage img = new BufferedImage(src.getWidth(),src.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		for (int x=0; x<src.getWidth(); x++) {
			for (int y=0; y<src.getHeight(); y++) {
				img.setRGB(src.getWidth()-x-1,y, src.getRGB(x,y));
			}
		}
		return img;
	}

	
	private static class EnhancedOverlay extends JComponent {
		public EnhancedOverlay() {
			ImageIcon icn = new ImageIcon(flip(makeImage(borderSize+2, SHOW_ENHANCED, Util.CLEAR)));
			
			this.setBorder(BorderFactory.createMatteBorder(borderSize, borderSize, borderSize, borderSize, icn));
		}
	}
	
	/**Component to store and display a selection.**/
	private static class SelectionOverlay extends JComponent implements Selectable {
		private static final long serialVersionUID = 9079768489874376280L;
		
		/**Color to show 'delete' in enhancement.**/
		public Color PROVISIONAL = new Color(200,30,30);
		
		private Area selected;
		
		/**Current selection in dataset coordinates.**/
		private Rectangle2D provisional = null;
		private boolean provisionalRemove = false;
				
		/**Hosting object.**/
		private final EnhanceHost host;
		
		
		public SelectionOverlay(EnhanceHost host) {
			AdjustRange r = new AdjustRange(this);
			this.addMouseListener(r);
			this.addMouseMotionListener(r);
			this.host = host;
			this.setBorder(
					BorderFactory.createMatteBorder(
							borderSize,borderSize,borderSize,borderSize, 
							new ImageIcon(makeImage(borderSize+2, new Color(255,140,0), Util.CLEAR))));
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2= (Graphics2D) g;
			AffineTransform vt = host.viewTransform();
			

			g2.setColor(SHOW_ENHANCED);
			if (selected != null) {g2.draw(vt.createTransformedShape(selected));}

			if (provisionalRemove) {g2.setColor(PROVISIONAL);}
			if (provisional != null) {g2.draw(vt.createTransformedShape(provisional));}
		}

		public void clear() {
			this.selected = null;
			this.provisional = null;
		}
		
		/**Set the current selection to a new value.
		 * To indicate transient state, set the "provisional" flag.
		 * 
		 * @param bounds Selection bounds in screen-space
		 * @param provisional Flag passed bounds as provisional selection
		 ***/
		public void modSelection(Rectangle2D bounds, boolean provisional, boolean remove) {
			
			try	{
				if (bounds != null) {
					bounds = host.viewTransform().createInverse().createTransformedShape(bounds).getBounds2D();
				}
			} catch (NoninvertibleTransformException e) {/*Ignore...should be impossible...should be.*/}
			
			if (provisional) {
				this.provisional = bounds;
				this.provisionalRemove = remove;
			} else {
				this.provisional = null;
				if (bounds != null) {
					Area a =new Area(bounds);
					if (remove && selected != null) {
						selected.subtract(a);
					} else if (selected != null) {
						selected.add(a);						
					} else {
						selected = a;
					}
				}
				if (selected != null && selected.isEmpty()) {selected = null;}
				host.forceNewRefAggregates();
			}
			
			this.repaint();
		}
		
		public Area selectedArea() {return selected;}
	}
	
	/**Interface indicating a thing has a selection region associated with it.**/
	private interface Selectable {
		public void clear();
		public void modSelection(Rectangle2D bounds, boolean provisional, boolean remove);
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
				target.modSelection(bounds, false, altPressed(e));
			}
			start = null;
		}
		
		public void mouseClicked(MouseEvent e) {target.clear();}
		public void mouseDragged(MouseEvent e) {
			if (start != null) {
				Rectangle2D bounds =bounds(e);				
				if (bounds.isEmpty() || bounds.getWidth()*bounds.getHeight()<1) {bounds = null;}
				target.modSelection(bounds, true, altPressed(e));
			}
		}

		private static boolean altPressed(MouseEvent e) {
			return (e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) == MouseEvent.ALT_DOWN_MASK; 
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
			JPanel item = new LabeledItem("Modify Selection:", box);
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
		
		public void clear() {box.setSelected(false);}
		
		/**Get the target host control.**/
		public EnhanceHost host() {return host;}
		/**Set the target host control.**/
		public void host(EnhanceHost host) {this.host = host;}
	}
}
