package ar.app.util;

import java.awt.Graphics2D;
import java.awt.Cursor;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.JPanel;

import ar.app.components.ARPanel;

public class ZoomPanHandler implements MouseListener, MouseMotionListener{
    public static final double MIN_SCALE = Double.MIN_VALUE; //Minimum single-step change
    public static final double MAX_SCALE = Double.MAX_VALUE;   //Maximum single-step change
	
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
            JPanel canvas = (JPanel) e.getComponent();

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
        if (buttonEquals(e, ZOOM_BUTTON) ) {
        	ARPanel canvas = (ARPanel)e.getComponent();
            
            int y = e.getY();
            int dy = y-yLast;
            double zoom = 1 + ((double)dy) / 100;

            int cursor = Cursor.N_RESIZE_CURSOR;
            canvas.setCursor(Cursor.getPredefinedCursor(cursor));
            
            zoom(canvas, down, zoom, false);
            
            yLast = y;
        } else if (buttonEquals(e, PAN_BUTTON)) {
        	ARPanel canvas = (ARPanel)e.getComponent();
            double x = e.getX(),   y = e.getY();
            double dx = x-down.getX(), dy = y-down.getY();

            canvas.pan(dx,dy);
            down = e.getPoint();
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
    protected void zoom(ARPanel canvas, Point2D p, double zoom, boolean abs) {
        double scale = canvas.getScale();
        double result = scale * zoom;

        if ( result < MIN_SCALE ) {
            zoom = MIN_SCALE/scale;
        } else if ( result > MAX_SCALE ) {
            zoom = MAX_SCALE/scale;
        }       
        
        if ( abs ) {canvas.zoomAbs(p,zoom);}
        else {canvas.zoom(p,zoom);}
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
			ARPanel canvas = (ARPanel) e.getComponent();
			Rectangle2D content = canvas.dataset().bounds();
			Rectangle2D space = canvas.getBounds();

			if (!content.isEmpty()) {
				double w = space.getWidth()/content.getWidth();
				double h = space.getHeight()/content.getHeight();
				double scale = Math.min(w, h);
				scale = scale/canvas.getScale();
				Point2D center = new Point2D.Double(content.getCenterX(), content.getCenterY());  
						
				canvas.zoomAbs(center, scale);
				canvas.panToAbs(center);
			}
		}
		
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {/*Ignored.*/}

	@Override
	public void mouseExited(MouseEvent e) {/*Ignored.*/}

	@Override
	public void mouseMoved(MouseEvent e) {/*Ignored.*/}
}
