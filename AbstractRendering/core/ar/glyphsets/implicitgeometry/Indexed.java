package ar.glyphsets.implicitgeometry;

import java.awt.geom.Rectangle2D;

/**Interface designating something has an int-valued "get" function.
 * This interface is the basis for array-based and file-record conversions
 * where the only reliable field accessor is the index.
 * 
 * The subclasses are common ways of working with indexed records.
 * 
 * **/
public interface Indexed {
	public Object get(int f);
	

	/**Apply the passed valuer to the value at the indicated index.
	 * The default value is the "IdentityValuer" found in the valuer class.
	 * **/
	public static class ToValue<I,V> implements Valuer<Indexed,V> {
		private final int vIdx;
		private final Valuer<I,V> basis;
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public ToValue(int vIdx) {this(vIdx, new IdentityValuer());}
		public ToValue(int vIdx, Valuer<I, V> basis) {
			this.vIdx = vIdx;
			this.basis = basis;
		}
		
		@SuppressWarnings("unchecked")
		public V value(Indexed from) {
			return basis.value((I) from.get(vIdx));
		}
	}
	

	
	public static class ToRect implements Shaper<Indexed> {
		private final double width,height;
		private final boolean flipY;
		private final int xIdx, yIdx;
		
		public ToRect(double width, double height, boolean flipY, int xIdx, int yIdx) {
			this.width=width;
			this.height=height;
			this.flipY=flipY;
			this.xIdx = xIdx;
			this.yIdx = yIdx;
		}
		public Rectangle2D shape(Indexed from) {
			double x=((Number) from.get(xIdx)).doubleValue();
			double y=((Number) from.get(yIdx)).doubleValue();
			
			y = flipY ? -y : y; 
			return new Rectangle2D.Double(x, y, width, height);
		}	
	}
}
