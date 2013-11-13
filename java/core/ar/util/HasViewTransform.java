package ar.util;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;

/**Interface to indicate that there is a view transform associated with this object.**/
public interface HasViewTransform {
	/**Get the current view transform.  Should return a copy (not a reference).**/
	public AffineTransform viewTransform();
	
	/**Set a new view transform.  Should make a copy on acceptance.**/
	public void viewTransform(AffineTransform vt) throws NoninvertibleTransformException;
	
	public void zoomFit();
	public Rectangle2D dataBounds();
}
