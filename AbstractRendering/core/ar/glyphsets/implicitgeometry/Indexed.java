package ar.glyphsets.implicitgeometry;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.lang.reflect.Array;

import ar.util.MemMapEncoder;

/**Interface designating something has an int-valued "get" function.
 * This interface is the basis for array-based and file-record conversions
 * where the only reliable field accessor is the index.
 * 
 * The subclasses are common ways of working with indexed records.
 * 
 * **/
public interface Indexed extends Serializable {
	public Object get(int i);
	
	public static class ArrayWrapper implements Indexed {
		private static final long serialVersionUID = -7081805779069559306L;
		private final Object array;
		public ArrayWrapper(Object parts) {this.array = parts;}
		public Object get(int i) {return Array.get(array, i);}
	}

	/**Converts the elements of the passed array to the given types.
	 * Uses toString and primitive parsers.
	 */
	public static class Converter implements Indexed {
		private static final long serialVersionUID = 9142589107863879237L;
		private final MemMapEncoder.TYPE[] types;
		private final Object[] values;
		public Converter(Object[] values, MemMapEncoder.TYPE... types) {
			this.values = values;
			this.types = types;
		}

		@Override
		public Object get(int i) {
			String s = values[i].toString();
			switch (types[i]) {
				case INT: return Integer.valueOf(s);
				case LONG: return Long.valueOf(s);
				case DOUBLE: return Double.valueOf(s);
				case FLOAT: return Float.valueOf(s);
				default: throw new UnsupportedOperationException("Cannot perform conversion to " + types[i]);
			}
		}
		
		public Converter makeFor(Object[] values) {return new Converter(values, types);}
	}
	
	
	/**Apply the passed valuer to the value at the indicated index.
	 * The default value is the "IdentityValuer" found in the valuer class.
	 * **/
	public static class ToValue<I,V> implements Valuer<Indexed,V>, Serializable {
		private static final long serialVersionUID = -3420649810382539859L;
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
	

	
	public static class ToRect implements Shaper<Indexed>, Serializable {
		private static final long serialVersionUID = 2509334944102906705L;
		private final double width,height;
		private final boolean flipY;
		private final int xIdx, yIdx;
		
		public ToRect(double size, int xIdx, int yIdx) {this(size,size,false,xIdx,yIdx);}
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
