package ar.app;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import ar.*;
import ar.Renderer;
import ar.app.util.ZoomPanHandler;

public class ARPanel<A,B> extends JPanel {
	private static final long serialVersionUID = 1L;
	private final WrappedReduction<A> reduction;
	private final WrappedTransfer<B> transfer;
	private final GlyphSet dataset;
	private Renderer renderer;
	
	private AffineTransform viewTransformRef = new AffineTransform();
	private AffineTransform inverseViewTransformRef = new AffineTransform();

	private BufferedImage image;
	private Aggregates<A> aggregates;
	
	public ARPanel(WrappedReduction<A> reduction, WrappedTransfer<B> transfer, GlyphSet D, Renderer renderer) {
		super();
		this.reduction = reduction;
		this.transfer = transfer;
		this.dataset = D;
		this.renderer = renderer;
		
		ZoomPanHandler h = new ZoomPanHandler();
		super.addMouseListener(h);
		super.addMouseMotionListener(h);
	}
	
	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;

		if (renderer == null 
				|| dataset == null ||  dataset.isEmpty() 
				|| transfer == null || reduction == null
				|| !transfer.type().equals(reduction.type())) {
			
			g.setColor(Color.GRAY);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			return;
		}

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, this.getWidth(), this.getHeight());

		if (aggregates == null || differentSizes(image, this)) { 
			long start = System.currentTimeMillis();
			aggregates = renderer.reduce(dataset, inverseViewTransform(), reduction.op(), this.getWidth(), this.getHeight());
			image = renderer.transfer(aggregates, (Transfer<A>) transfer.op(), this.getWidth(), this.getHeight(), Util.CLEAR);
			long end = System.currentTimeMillis();
			System.out.println((end-start) + " ms (full)");			
		} else {
			long start = System.currentTimeMillis();
			image = renderer.transfer(aggregates, (Transfer<A>) transfer.op(), this.getWidth(), this.getHeight(), Util.CLEAR);			
			long end = System.currentTimeMillis();
			System.out.println((end-start) + " ms (transfer)");			
		}
		
		g2.drawRenderedImage(image,g2.getTransform());
	}

	private static final boolean differentSizes(BufferedImage image, JPanel p) {
		return image == null || image.getWidth() != p.getWidth() || image.getHeight() != p.getHeight();
	}
	
	public GlyphSet dataset() {return dataset;}
	public ARPanel<A,B> withDataset(GlyphSet data) {return new ARPanel<A,B>(reduction, transfer, data, renderer);}
	public <C> ARPanel<A,C> withTransfer(WrappedTransfer<C> t) {
		ARPanel<A,C> p = new ARPanel<A,C>(reduction, t, dataset, renderer);
		p.viewTransformRef = this.viewTransformRef;
		p.inverseViewTransformRef = this.inverseViewTransformRef;
		p.aggregates = this.aggregates;
		p.image = image;
		return p;
	}
	public <C> ARPanel<C,B> withReduction(WrappedReduction<C> r) {
		ARPanel<C,B> p = new ARPanel<C,B>(r, transfer, dataset, renderer);
		p.viewTransformRef = this.viewTransformRef;
		p.inverseViewTransformRef = this.inverseViewTransformRef;
		return p;
	}
	
	public ARPanel<A,B> withRenderer(Renderer r) {
		ARPanel<A,B> p = new ARPanel<A,B>(reduction, transfer, dataset, r);
		return p;
	}
	
	public String toString() {
		return String.format("ARPanel[Dataset: %1$s, Ruleset: %2$s]", dataset, transfer, reduction);
	}
	
	public Aggregates<?> getAggregates() {return aggregates;}
	
	
	
	
	
	
	Point2D tempPoint = new Point2D.Double();
	/**Zooms anchored on the given screen point TO the given scale.*/
	public void zoomTo(final Point2D p, double scale) {
		inverseViewTransform().transform(p, tempPoint);
		zoomToAbs(tempPoint, scale);
	}

	/**Zooms anchored on the given screen point TO the given scale.*/
	public void zoomToAbs(final Point2D p, double scale) {
		zoomToAbs(p, scale, scale);
	}
	
	/**Zooms anchored on the given screen point TO the given scale.*/
	public void zoomToAbs(final Point2D p, double scaleX, double scaleY) {
		zoomAbs(p, scaleX/viewTransform().getScaleX(), scaleY/viewTransform().getScaleY());
	}



	
	/**Zoom anchored on the given screen point by the given scale.*/
	public void zoom(final Point2D p, double scale) {
		inverseViewTransform().transform(p, tempPoint);
		zoomAbs(tempPoint, scale);
	}
	
	/**Zoom anchored on the given absolute point (e.g. canvas 
	 * under the identity transform) to the given scale.
	 */
	public void zoomAbs(final Point2D p, double scale) {
		zoomAbs(p, scale, scale);
	}
	
	public void zoomAbs(final Point2D p, double scaleX, double scaleY) {
		double zx = p.getX(), zy = p.getY();
		AffineTransform vt = viewTransform();
        vt.translate(zx, zy);
        vt.scale(scaleX,scaleY);
        vt.translate(-zx, -zy);
        try {setViewTransform(vt);}
        catch (NoninvertibleTransformException e ) {
        	try {setViewTransform(new AffineTransform());}
			catch (NoninvertibleTransformException e1) {}	//Default transform is invertible...so everything is safe
        }
	}
	
    /**
     * Pans the view provided by this display in screen coordinates.
     * @param dx the amount to pan along the x-dimension, in pixel units
     * @param dy the amount to pan along the y-dimension, in pixel units
     */
    public void pan(double dx, double dy) {
    	tempPoint.setLocation(dx, dy);
    	inverseViewTransform().transform(tempPoint, tempPoint);
        double panx = tempPoint.getX();
        double pany = tempPoint.getY();
        tempPoint.setLocation(0, 0);
        inverseViewTransform().transform(tempPoint, tempPoint);
        panx -= tempPoint.getX();
        pany -= tempPoint.getY();
        panAbs(panx, pany);
    }
    
    /**
     * Pans the view provided by this display in absolute (i.e. item-space)
     * coordinates.
     * @param dx the amount to pan along the x-dimension, in absolute co-ords
     * @param dy the amount to pan along the y-dimension, in absolute co-ords
     */
    public void panAbs(double dx, double dy) {
    	AffineTransform vt = viewTransform();
    	vt.translate(dx, dy);
        try {setViewTransform(vt);}
        catch (NoninvertibleTransformException e ) {throw new Error("Supposedly impossible error occured.", e);}
    }
	
	/**Pan so the display is centered on the given screen point.*/
	public void panTo(final Point2D p) {
        inverseViewTransform().transform(p, tempPoint);
        panToAbs(tempPoint);
	}
	
	/**Pan so the display is centered on the given canvas
	 * point.
	 */
	public void panToAbs(final Point2D p) {
        double sx = viewTransform().getScaleX();
        double sy = viewTransform().getScaleY();
        double x = p.getX(); x = (Double.isNaN(x) ? 0 : x);
        double y = p.getY(); y = (Double.isNaN(y) ? 0 : y);
        x = getWidth() /(2*sx) - x;
        y = getHeight()/(2*sy) - y;
        
        double dx = x-(viewTransform().getTranslateX()/sx);
        double dy = y-(viewTransform().getTranslateY()/sy);

        AffineTransform vt = viewTransform();
        vt.translate(dx, dy);
        try {setViewTransform(vt);}
        catch (NoninvertibleTransformException e ) {throw new Error("Supposedly impossible error occured.", e);}
	}

	
    /**Get the current scale factor factor (in cases
     * where it is significant, this is the X-scale).
     */
    public double getScale() {return viewTransform().getScaleX();}
    
	/**What is the current center of the screen (in canvas coordinates).
	 * 
	 *  @param target Store in this point2D.  If null a new point2D will be created.
	 **/
	public Point2D getPanAbs(Point2D target) {
		if (target == null) {target = new Point2D.Double();}
		
		Rectangle2D viewBounds = inverseViewTransform().createTransformedShape(getBounds()).getBounds2D();

		target.setLocation(viewBounds.getCenterX(), viewBounds.getCenterY());
		return target; 
	}

	 
    /**Use this transform to convert values from the absolute system
     * to the screen system.
     */
	public AffineTransform viewTransform() {return new AffineTransform(viewTransformRef);}
	public void setViewTransform(AffineTransform vt) throws NoninvertibleTransformException {
		aggregates=null;
		this.viewTransformRef = vt;
		inverseViewTransformRef  = new AffineTransform(vt);
		inverseViewTransformRef.invert();
		this.repaint(this.getBounds());
//		System.out.println("###################");
//		System.out.println("Bounds:" + dataset.glyphs().bounds());
//		System.out.println("Viewport:" + this.getWidth() + " x " + this.getHeight());
//		System.out.println("Scale:" + viewTransformRef.getScaleX());
//		System.out.println("Translate: " + viewTransformRef.getTranslateX() + " x " + viewTransformRef.getTranslateY());
//		System.out.println("###################");
	}
	
	/**Use this transform to convert screen values to the absolute/canvas
	 * values.
	 */
	public AffineTransform inverseViewTransform() {return new AffineTransform(inverseViewTransformRef);}
}
