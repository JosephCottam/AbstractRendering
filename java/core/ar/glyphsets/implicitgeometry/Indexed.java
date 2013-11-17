package ar.glyphsets.implicitgeometry;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.lang.reflect.Array;

import ar.util.ColorNames;
import ar.util.Util;
import ar.util.memoryMapping.MemMapEncoder;

/**Interface designating something has an integer-valued "get" function.
 * This interface is the basis for array-based and file-record conversions
 * where the only reliable field accessor is the index.
 * 
 * The subclasses are common ways of working with indexed records.
 * 
 * **/
public interface Indexed extends Serializable {
	/**What value is at index i? */
	public Object get(int i);
	
	/**Wrap an array as an Indexed item.**/
	public static class ArrayWrapper implements Indexed {
		private static final long serialVersionUID = -7081805779069559306L;
		private final Object array;
		
		@SuppressWarnings("javadoc")
		public ArrayWrapper(Object parts) {this.array = parts;}
		public Object get(int i) {return Array.get(array, i);}
	}

	/**Converts the elements of the passed array to the given types.
	 * Uses toString and primitive parsers.
	 */
	public static class Converter implements Indexed {
		/**Types the converter understands. "X" means skip.**/
		public enum TYPE{INT, DOUBLE, LONG, SHORT, BYTE, CHAR, FLOAT, X, COLOR}
		private static final long serialVersionUID = 9142589107863879237L;
		private final TYPE[] types;
		private final Object[] values;
		
		public Converter(MemMapEncoder.TYPE... types) {this(Util.transcodeTypes(types));}
		
		/**Instantiate a converter with a null value-array.
		 * This converter is essentially a template for future converts built for specific data.
		 */
		public Converter(TYPE... types) {this(null, types);}
		
		/**Instantiate a converter for a specific set of values.*/
		public Converter(Object[] values, TYPE... types) {
			this.values = values;
			this.types = types;
		}

		@Override
		public Object get(int i) {
			String s = values[i].toString();
			switch (types[i]) {
				case INT: return Integer.valueOf(s);
				case SHORT: return Short.valueOf(s);
				case LONG: return Long.valueOf(s);
				case FLOAT: return Float.valueOf(s);
				case DOUBLE: return Double.valueOf(s);
				case COLOR: return ColorNames.byName(s, null);
				default: throw new UnsupportedOperationException("Cannot perform conversion to " + types[i]);
			}
		}
		
		/**Get the type array associated with this converter.**/
		public TYPE[] types() {return types;}
		public Converter applyTo(Object[] values) {return new Converter(values, types);}
	}
	
	
	/**Apply the passed valuer to the value at the indicated index.
	 * The default value is the "IdentityValuer" found in the valuer class.
	 * **/
	public static class ToValue<I,V> implements Valuer<Indexed,V>, Serializable {
		private static final long serialVersionUID = -3420649810382539859L;
		private final int vIdx;
		private final Valuer<I,V> basis;
		
		/**Extract a value from an indexed item without conversion.**/
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public ToValue(int vIdx) {this(vIdx, new IdentityValuer());}
		
		/**Extract a value from an indexed item, but do conversion using the valuer.**/
		public ToValue(int vIdx, Valuer<I, V> basis) {
			this.vIdx = vIdx;
			this.basis = basis;
		}
		
		@SuppressWarnings("unchecked")
		public V value(Indexed from) {
			return basis.value((I) from.get(vIdx));
		}
	}
	


	/**Convert an item to a fixed-sized rectangle at a variable
	 * position.  The passed value determines the position, but the size
	 * is set by the ToRect constructor. 
	 */
	public static class ToPoint implements Shaper.SafeApproximate<Point2D, Indexed>, Serializable {
		private static final long serialVersionUID = 2509334944102906705L;
		private final boolean flipY;
		private final int xIdx, yIdx;
		
		 /** @param flipY Multiply Y-values by -1 (essentially flip up and down directions)**/
		public ToPoint(boolean flipY, int xIdx, int yIdx) {
			this.flipY=flipY;
			this.xIdx = xIdx;
			this.yIdx = yIdx;
		}
		public Point2D shape(Indexed from) {
			double x=((Number) from.get(xIdx)).doubleValue();
			double y=((Number) from.get(yIdx)).doubleValue();
			
			y = flipY ? -y : y; 
			return new Point2D.Double(x, y);
		}	
	}
	
	
	/**Convert an item to a fixed-sized rectangle at a variable
	 * position.  The passed value determines the position, but the size
	 * is set by the ToRect constructor. 
	 */
	public static class ToRect implements Shaper.SafeApproximate<Rectangle2D, Indexed>, Serializable {
		private static final long serialVersionUID = 2509334944102906705L;
		private final double width,height;
		private final boolean flipY;
		private final int xIdx, yIdx;
		
		/**Square construction using the indexed values directly for x/y**/
		public ToRect(double size, int xIdx, int yIdx) {this(size,size,false,xIdx,yIdx);}
		
		/**Full control constructor for creating rectangles.
		 * 
		 * @param flipY Multiply Y-values by -1 (essentially flip up and down directions)
		 * **/
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
