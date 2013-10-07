package ar.app.util;

import java.awt.Graphics2D;
import java.awt.Cursor;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.JComponent;

import ar.app.display.ARComponent;
import ar.util.HasViewTransform;

/**Utility for working with zoom/pan on a display component.
 * 
 * This class can only be registered with objects that are both JComponents 
 * and implement the HasViewTransform interface.
 * **/
public class ZoomPanHandler implements MouseListener, MouseMotionListener{
    private static final int ZOOM_BUTTON = InputEvent.BUTTON2_MASK;
    private static final int PAN_BUTTON = InputEvent.BUTTON1_MASK;
	
    private Point2D down = new Point2D.Float();
    private int yLast;
    
	/**
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    @Override
	public void mousePressed(MouseEvent e) { 	
        if (buttonEquals(e, ZOOM_BUTTON) ) {
        	JComponent canvas = (JComponent) e.getComponent();

            canvas.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
            
            AffineTransform af = ((Graphics2D) canvas.getGraphics()).getTransform();
            try {af.inverseTransform(e.getPoint(), down);}
			catch (NoninvertibleTransformException e1) {throw new RuntimeException(e1);}
            yLast = e.getY();
        } else if (buttonEquals(e, PAN_BUTTON)) {
            e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            down = e.getPoint();
        }
    }
    
    /**
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    @Override
	public void mouseDragged(MouseEvent e) {
    	HasViewTransform canvas = (HasViewTransform) e.getComponent();
    	JComponent component = (JComponent) e.getComponent();
        if (buttonEquals(e, ZOOM_BUTTON) ) {
            int y = e.getY();
            int dy = y-yLast;
            double zoom = 1 + ((double)dy) / 100;

            component.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
            
            zoom(canvas, zoom);
            
            yLast = y;
        } else if (buttonEquals(e, PAN_BUTTON)) {
            double x = e.getX(),   y = e.getY();
            double dx = x-down.getX(), dy = y-down.getY();

            pan(canvas, dx,dy);
            down = e.getPoint();
        }
    }

    protected static void pan(HasViewTransform canvas, double dx, double dy) {
    	AffineTransform vt = canvas.viewTransform();
    	double tx = vt.getTranslateX()+dx;
    	double ty = vt.getTranslateY()+dy;
    	AffineTransform t = AffineTransform.getTranslateInstance(tx,ty);
    	t.scale(vt.getScaleX(), vt.getScaleY());
        
    	try {canvas.viewTransform(t);}
		catch (NoninvertibleTransformException e) {
			try {canvas.viewTransform(new AffineTransform());}
			catch (NoninvertibleTransformException e1) {/**Impossible**/}
		}
    }
    
    /**
     * Zoom the given display at the given point by the zoom factor,
     * in either absolute (item-space) or screen co-ordinates.
     * @param canvas the canvas to zoom
     * @param p the point to center the zoom upon
     * @param zoom the scale factor by which to zoom
     * @param abs if true, the point p should be assumed to be in absolute
     * coordinates, otherwise it will be treated as screen (pixel) coordinates
     */
    protected static void zoom(HasViewTransform canvas, double zoom) {
    	AffineTransform vt = canvas.viewTransform();
        vt.scale(zoom, zoom);
        
        try {canvas.viewTransform(vt);}
		catch (NoninvertibleTransformException e) {
			try {canvas.viewTransform(new AffineTransform());}
			catch (NoninvertibleTransformException e1) {/**Impossible**/}
		}
    }
    
    /**
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    @Override
	public void mouseReleased(MouseEvent e) {
        if (buttonEquals(e, ZOOM_BUTTON) || buttonEquals(e, PAN_BUTTON)) {
            e.getComponent().setCursor(Cursor.getDefaultCursor());
        } 
    }
    
    private static final boolean buttonEquals(MouseEvent e, int button) {
    	return (e.getModifiers() & button) == button;
    }

	@Override
	public void mouseClicked(MouseEvent e) { 
		if (e.getClickCount() == 2) {
			ARComponent.Aggregating canvas = (ARComponent.Aggregating) e.getComponent();
			canvas.zoomFit();
		}
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {/*Ignored.*/}

	@Override
	public void mouseExited(MouseEvent e) {/*Ignored.*/}

	@Override
	public void mouseMoved(MouseEvent e) {/*Ignored.*/}
}
