package ar.glyphsets.implicitgeometry;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public interface Cartography {
	/**Convert values in the latitude, longitude to EPSG:900913/EPSG:3587/WGS 84 Web Mercator "meters" system (used by google maps)
	 * 
	 * based on: https://gist.github.com/onderaltintas/6649521
	 */
	public static final class DegreesToMeters {
		private static final double PI360 = Math.PI / 360;
		private static final double PI180 = Math.PI / 180;
		
		public static Rectangle2D from(Rectangle2D degrees) {
			Point2D topLeft = from(new Point2D.Double(degrees.getMaxX(), degrees.getMaxY()));
			Point2D bottomRight = from(new Point2D.Double(degrees.getMinX(), degrees.getMinY()));
			return from(topLeft, bottomRight);
		}
		
		public static Rectangle2D from(Point2D one, Point2D two) {
			Point2D a = from(one);
			Point2D b = from(two);
			return new Line2D.Double(a,b).getBounds2D();			
		}
		
		public static Point2D from(Point2D degrees) {
			final double lat = degrees.getY();
			final double lon = degrees.getX();
			final double x = lon * 20037508.34 / 180;
			double y = Math.log(Math.tan((90 + lat) * PI360)) / (PI180);
	        y = y * 20037508.34 / 180;
	        return new Point2D.Double(x,y);
		}
	}
	
	/**Convert values in the EPSG:900913/EPSG:3587/WGS 84 Web Mercator "meters" system (used by google maps) to latitude, longitude.
	 * 
	 * based on: https://gist.github.com/onderaltintas/6649521
	 */
	public static final class MetersToDegrees implements Shaper.SafeApproximate<Indexed, Point2D> {
		private final int xIdx, yIdx;
		private static final double PI2 = Math.PI / 20037508.34;
		
		public MetersToDegrees(int xIdx, int yIdx) {this.xIdx = xIdx; this.yIdx = yIdx;}
		
		@Override
		public Point2D apply(Indexed t) {
			final double x = ((Number) t.get(xIdx)).doubleValue();
			final double y = ((Number) t.get(yIdx)).doubleValue();
			final double lon = x *  180 / 20037508.34 ;
			final double lat = Math.atan(Math.exp(y * PI2)) * 360 / Math.PI - 90;
			final Point2D rslt = new Point2D.Double(lon, lat);
			return rslt;
		}
	}
}
