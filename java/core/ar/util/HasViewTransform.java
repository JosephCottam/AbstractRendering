package ar.util;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**Interface to indicate that there is a view transform associated with this object.**/
public interface HasViewTransform {
	/**Get the current view transform.  Should return a copy (not a reference).**/
	public AffineTransform viewTransform();
	
	/**Set a new view transform.  Should make a copy on acceptance.
	 * 
	 * @param vt -- New view transform
	 * @param provisional -- Is this a provisional change?  If so, some work may be skipped.  Final transform will be passed with 'true' ALWAYS.
	 * **/
	public void viewTransform(AffineTransform vt, boolean provisional);
	
	/**Non-provisional call to viewTransform.**/
	public default void viewTransform(AffineTransform vt) {viewTransform(vt, false);}
	
	/**Fit the contained data onto the screen.  
	 * 
	 * The result from viewTransform() must reflect any transform changed required to complete this operation.**/
	public void zoomFit();
	
	/** @return Bounding box for the underlying data.**/
	public Rectangle2D dataBounds();
}
