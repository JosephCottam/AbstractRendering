package ar.test;

import java.awt.geom.Rectangle2D;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class RectNear extends TypeSafeMatcher<Rectangle2D> {
	private final Rectangle2D ref;
	private final Double tollerance;
	
	public RectNear(Rectangle2D ref, Double tollerance) {
		this.ref = ref;
		this.tollerance = Math.abs(tollerance);
	}
	
	@Override
	public void describeTo(Description msg) {
		msg.appendText(" too far from");
		msg.appendValue(ref);
	}

	@Override
	protected boolean matchesSafely(Rectangle2D val) {
		return near(val.getX(), ref.getX(), tollerance)
				&& near(val.getY(), ref.getY(), tollerance)
				&& near(val.getWidth(), ref.getWidth(), tollerance)
				&& near(val.getHeight(), ref.getHeight(), tollerance);
	}
	
	protected boolean near(double a, double b, double tollerance) {
		return Math.abs(a-b) < tollerance;
	}
	
	public static RectNear rectNear(Rectangle2D ref, Double tollerance) {return new RectNear(ref, tollerance);}

}
