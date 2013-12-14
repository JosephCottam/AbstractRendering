package ar.app.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ar.Aggregates;
import ar.Transfer;
import ar.app.display.ARComponent;
import ar.rules.Numbers;

public class ScatterControl extends JPanel {
	private static final long serialVersionUID = 4425716699286853617L;

	protected ARComponent.Holder source;
	protected final Plot plot;
	protected final JSpinner distance = new JSpinner();
	protected final JButton refresh = new JButton("Refresh");
	Transfer<Number, Color> basis = new Numbers.Interpolate<>(new Color(255,200,200), Color.RED); 

	
	public ScatterControl() {
		this.setLayout(new BorderLayout());
		this.plot = new Plot(this);
		distance.setValue(1);
		
		refresh.addActionListener(new ActionListener() {public void actionPerformed(ActionEvent e) {plot.reset();}});

		distance.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				plot.reset();
			}
		});
		
		this.addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent e) {plot.reset();}
			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
		
		
		this.add(plot, BorderLayout.CENTER);

		JPanel p = new JPanel();
		p.add(refresh);
		p.add(distance);
		this.add(p, BorderLayout.SOUTH);
		this.invalidate();
		this.repaint();
	}
	
	
	public void setSource(ARComponent.Holder source) {this.source=source;}

	public int distance() {return (Integer) distance.getValue();}
	
	public Transfer<Number,Color> getTransfer() {
		double minV,maxV,minDV,maxDV;
		
		if (plot.region == null || plot.region.isEmpty()) {
			return basis;
		} else {
		
			Rectangle2D r;
			try {r = plot.vt.createInverse().createTransformedShape(plot.region).getBounds2D();}
			catch (NoninvertibleTransformException e) {throw new RuntimeException(e);}
	
			maxV = r.getMaxY();
			minV = r.getMinY();
			maxDV = r.getMaxX();
			minDV = r.getMinX();
			Transfer<Number,Color> t = new DeltaTransfer(minV, maxV, minDV, maxDV, distance(),basis, new Color(250,250,250));
			return t;
		}
		
	}
	
	private static final class Plot extends JPanel {
		private static final long serialVersionUID = 1L;
		protected final ScatterControl parent;
		protected Rectangle2D region;
		protected AffineTransform vt = new AffineTransform();
		
		protected ArrayList<Point2D> points;
		protected double max;
		
		public Plot(ScatterControl parent) {
			this.parent=parent;
			this.addKeyListener(new ClearListener());
			AdjustRange l = new AdjustRange();
			this.addMouseListener(l);
			this.addMouseMotionListener(l);
		}
		
		public void reset() {
			points = null;
			this.repaint();
		}

		public void changeRegion(Rectangle2D region) {
			this.region = region;
			parent.source.getARComponent().transfer(parent.getTransfer());
		}
		
		private void configure() {
			Aggregates<?> src = parent.source == null ? null : parent.source.getARComponent().aggregates(); 

			if (src == null || !(src.get(src.lowX(), src.lowY()) instanceof Number)) {return;}
			
			points=new ArrayList<Point2D>();
			
			@SuppressWarnings("unchecked")
			Aggregates<? extends Number> source = (Aggregates<? extends Number>) src; 
			
			changeRegion(new Rectangle2D.Double(0,0,0,0));


			double max=Double.MIN_VALUE;
			
			for (int x=source.lowX(); x<source.highX(); x++) {
				for (int y=source.lowY(); y<source.highY(); y++) {
					double common = source.get(x, y).doubleValue();
					max = Math.max(common, max);
					
					for (int dx=-parent.distance(); dx<=parent.distance(); dx++) {
						for (int dy=-parent.distance(); dy<=parent.distance(); dy++) {
							int cx=x+dx;
							int cy=y+dy;
							if (cx < source.lowX() || cy < source.lowY() || cx>source.highX() || cy> source.highY()) {continue;}
							double specific = source.get(cx,cy).doubleValue();
							points.add(new Point2D.Double(common, specific));
						}
					}
						
				}
			}
			this.max=max;
		}
			
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0,this.getWidth(), this.getHeight());

			
			if (points == null) {configure();}
			if (points == null) {return;}
			
			//Calc an affine transform to fit the aggregates space
			double det = Math.min(this.getWidth(), this.getHeight());
			double scale = det/max;
			vt = AffineTransform.getScaleInstance(scale, scale);
			g2.setTransform(vt);
			
			
			//Plot all points into that transformed space
			g.setColor(Color.GRAY);
			Rectangle2D r= new Rectangle2D.Double(0,0,1,1);
			for (Point2D p: points) {
				r.setRect(p.getX(), p.getY(), 1, 1);
				g2.fill(r);
			}
			
			//Draw the selection rectangle
			if (!region.isEmpty()) {
				g2.setTransform(new AffineTransform());
				g2.setColor(new Color(97,0,0,50));
				g2.fill(region);
			} 
		}
		
		public final class AdjustRange implements MouseListener, MouseMotionListener {
			Point2D start;
			
			public void mousePressed(MouseEvent e) {start = e.getPoint();}
			public void mouseReleased(MouseEvent e) {
				if (start != null) {
					Plot.this.changeRegion(bounds(e));
					Plot.this.repaint();
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

		private final class ClearListener implements KeyListener {
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == '-') {
					Plot.this.region = new Rectangle2D.Double(0,0,0,0);
					Plot.this.repaint();
				}
			}
			public void keyPressed(KeyEvent e) {}
			public void keyReleased(KeyEvent e) {}
		}

	}
		
	//TODO: Extend to do additional transfer if it is 'in' instead of just return given color...possibly take in Aggregates+Image and set image to tansparent if out...
	private static class DeltaTransfer implements Transfer<Number,Color> {
		private static final long serialVersionUID = 2903644806615515638L;
		protected final double minV, maxV, minDV, maxDV;
		protected final int distance;
		protected final Color out;
		protected final Transfer<Number, Color> basis;
		
		public DeltaTransfer(double minV, double maxV, double minDV, double maxDV, int distance, Transfer<Number, Color> basis, Color out) {
			this.minV=minV;
			this.maxV=maxV;
			this.minDV=minDV;
			this.maxDV=maxDV;
			this.distance=distance;
			this.basis = basis;
			this.out = out;
		}


		public Color emptyValue() {return Color.white;}

		@Override
		public Specialized specialize(Aggregates<? extends Number> aggregates) {
			Transfer.Specialized<Number,Color> ts = basis.specialize(aggregates);
			return new Specialized(minV, maxV, minDV, maxDV, distance, ts, out);
		}
		
		public static final class Specialized extends DeltaTransfer implements Transfer.Specialized<Number,Color> {
			private static final long serialVersionUID = -6184809407036220961L;
			
			private final Transfer.Specialized<Number, Color> basis;
			public Specialized(
					double minV, double maxV, double minDV,
					double maxDV, int distance, Transfer.Specialized<Number, Color> basis,
					Color out) {
				super(minV, maxV, minDV, maxDV, distance, basis, out);
				this.basis = basis;
			}
			
			@Override public boolean localOnly() {return false;} 
			
			@Override
			public Color at(int x, int y, Aggregates<? extends Number> aggregates) {
				double v = aggregates.get(x, y).doubleValue();
				
				if (v==aggregates.defaultValue().doubleValue()) {return basis.emptyValue();}
				
				if (v >= minV && v <= maxV) {
					for (int d=-distance; d<=distance; d++) {
						for (int dx=0; dx<=d; dx++) {
							for (int dy=0; dy<=d; dy++) {
								int cx=x+dx;
								int cy=y+dy;
								if (cx < aggregates.lowX() || cy < aggregates.lowY() || cx>aggregates.highX() || cy> aggregates.highY()) {continue;}
								double dv = aggregates.get(cx,cy).doubleValue();
								if (dv >= minDV && dv < maxDV) {
									return basis.at(x, y, aggregates);
								}
							}
						}
					}
				}
				return out;
			}
		}		
	}
	
}
