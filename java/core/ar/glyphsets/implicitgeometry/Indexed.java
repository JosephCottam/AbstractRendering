package ar.glyphsets.implicitgeometry;

import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.List;

import ar.util.ColorNames;
import ar.util.memoryMapping.MemMapEncoder;
import ar.util.memoryMapping.MemMapEncoder.TYPE;

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
	public int size();
	

	public static final class Util {
		public static String toString(Indexed target) {
			StringBuilder b = new StringBuilder();
			b.append(target.getClass().getSimpleName());
			b.append("[");
			for (int i=0; i< target.size(); i++) {
				b.append(target.get(i));
				b.append(", ");
			}
			b.deleteCharAt(b.length()-1);
			b.deleteCharAt(b.length()-1);
			b.append("]");
			return b.toString();
		}
		
		/**Convert from the types understood by the memory mappers to the types understood by the converters system.
		 * 
		 * TODO: Figure out a better way to make the two type tag sets play together.  Perhaps the division is too artificial....Color is a bit problematic though...
		 * **/
		public static final Converter.TYPE[] transcodeTypes(TYPE... types) {
			Converter.TYPE[] newTypes = new Converter.TYPE[types.length];
			for (int i=0; i< types.length; i++) {
				switch(types[i]) {
					case X: newTypes[i] = Converter.TYPE.X; break;
					case INT: newTypes[i] = Converter.TYPE.INT; break;
					case SHORT: newTypes[i] = Converter.TYPE.SHORT; break;
					case LONG: newTypes[i] = Converter.TYPE.LONG; break;
					case DOUBLE: newTypes[i] = Converter.TYPE.DOUBLE; break;
					case FLOAT: newTypes[i] = Converter.TYPE.FLOAT; break;
					default: throw new UnsupportedOperationException("Cannot perform conversion to " + types[i]);
				}
			}
			return newTypes;
		}
	}
	
	/**Wrap an array as an Indexed item.**/
	public static class ArrayWrapper implements Indexed {
		private static final long serialVersionUID = -7081805779069559306L;
		private final Object[] array;
		
		public ArrayWrapper(Object[] parts) {this.array = parts;}

		@Override public Object get(int i) {return array[i];}
		@Override public int size() {return array.length;}
		@Override public String toString() {return Util.toString(this);}			
	}
	
	/**Wrap a list as an Indexed item.**/
	public static class ListWrapper implements Indexed {
		private static final long serialVersionUID = -7081805779069559306L;
		private final List<?> parts;
		
		@SuppressWarnings("javadoc")
		public ListWrapper(List<?> parts) {this.parts = parts;}
		
		@Override public Object get(int i) {return parts.get(i);}
		@Override public int size() {return parts.size();}
		@Override public String toString() {return Util.toString(this);}
	}


	/**Converts the elements of the passed array to the given types.
	 * Uses toString and primitive parsers.
	 */
	public static class Converter implements Indexed, Valuer<Indexed,Indexed> {
		/**Types the converter understands. "X" means skip.**/
		public enum TYPE{INT, DOUBLE, LONG, SHORT, BYTE, CHAR, FLOAT, X, COLOR, STRING}
		private static final long serialVersionUID = 9142589107863879237L;
		private final TYPE[] types;
		private final Indexed values;
		
		public Converter(MemMapEncoder.TYPE... types) {this(Util.transcodeTypes(types));}
		
		/**Instantiate a converter with a null value-array.
		 * This converter is essentially a template for future converts built for specific data.
		 */
		public Converter(TYPE... types) {this(null, types);}
		
		/**Instantiate a converter for a specific set of values.*/
		public Converter(Indexed values, TYPE... types) {
			this.values = values;
			this.types = types;
		}

		@Override
		public Object get(int i) {
			Object v = values.get(i);
			switch (types[i]) {
				case INT: return v instanceof Integer ? (Integer) v : Integer.valueOf(v.toString().trim());
				case SHORT: return v instanceof Short ? (Short) v : Short.valueOf(v.toString().trim());
				case LONG: return v instanceof Long ? (Long) v : Long.valueOf(v.toString().trim());
				case FLOAT: return v instanceof Float ? (Float) v : Float.valueOf(v.toString().trim());
				case DOUBLE: return v instanceof Double ? (Double) v : Double.valueOf(v.toString().trim());
				case COLOR: return v instanceof Color ? (Color) v : ColorNames.byName(v.toString(), null);
				case STRING: return v instanceof String ? (String) v : v.toString();  
				default: throw new UnsupportedOperationException("Cannot perform conversion to " + types[i]);
			}
		}
		
		@SuppressWarnings("unchecked")
		public <T> T get(int f, Class<T> type) {
			Object val = get(f);
			if (type.isInstance(val)) {return (T) val;}
			throw new IllegalArgumentException("Requested type that does not match encoded type.");
		}

		@Override public int size() {return types.length;}
		@Override public String toString() {return Util.toString(this);}
		
		/**Get the type array associated with this converter.**/
		public TYPE[] types() {return types;}
		
		/**Create a new converter instance using the current types but the passed value source.**/
		public Converter applyTo(Object[] values) {return new Converter(new ArrayWrapper(values), types);}
		
		/**Create a new converter instance using the current types but the passed value source.**/
		public Converter applyTo(Indexed values) {return new Converter(values, types);}

		@Override
		//TODO: Remove 'applyTo' just use 'value' from here on out...
		public Indexed value(Indexed from) {return new Converter(values, types);}
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
		public ToValue(int vIdx) {this(vIdx, new Identity());}
		
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
	


	/**Convert an item to a single point.*/
	public static class ToPoint implements Shaper.SafeApproximate<Indexed, Point2D>, Serializable {
		private static final long serialVersionUID = 2509334944102906705L;
		private final boolean flipY;
		private final int xIdx, yIdx;
		
		 /** @param flipY Multiply Y-values by -1 (essentially flip up and down directions)**/
		public ToPoint(boolean flipY, int xIdx, int yIdx) {
			this.flipY=flipY;
			this.xIdx = xIdx;
			this.yIdx = yIdx;
		}
		
		@Override 
		public Point2D shape(Indexed from) {
			double x=((Number) from.get(xIdx)).doubleValue();
			double y=((Number) from.get(yIdx)).doubleValue();
			
			y = flipY ? -y : y; 
			return new Point2D.Double(x, y);
		}	
	}
	
	
	/**Convert an item to a fixed-sized rectangle at a variable
	 * position.  The passed value determines the position, but the size
	 * is set by the constructor. 
	 */
	public static class ToRect implements Shaper.SafeApproximate<Indexed, Rectangle2D>, Serializable {
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
		
		@Override 
		public Rectangle2D shape(Indexed from) {
			double x=((Number) from.get(xIdx)).doubleValue();
			double y=((Number) from.get(yIdx)).doubleValue();
			
			y = flipY ? -y : y; 
			return new Rectangle2D.Double(x-width/2d, y-height/2d, width, height);
		}	
	}
	
	/**Convert an item to a fixed-sized circle at a variable
	 * position.  The passed value determines the position, but the size
	 * is set by the constructor. 
	 */
	public static class ToCircle implements Shaper.SafeApproximate<Indexed, Ellipse2D>, Serializable {
		private static final long serialVersionUID = 2509334944102906705L;
		private final double width,height;
		private final boolean flipY;
		private final int xIdx, yIdx;
		
		/**Square construction using the indexed values directly for x/y**/
		public ToCircle(double size, int xIdx, int yIdx) {this(size,size,false,xIdx,yIdx);}
		
		/**Full control constructor for creating rectangles.
		 * 
		 * @param flipY Multiply Y-values by -1 (essentially flip up and down directions)
		 * **/
		public ToCircle(double width, double height, boolean flipY, int xIdx, int yIdx) {
			this.width=width;
			this.height=height;
			this.flipY=flipY;
			this.xIdx = xIdx;
			this.yIdx = yIdx;
		}
		
		@Override 
		public Ellipse2D shape(Indexed from) {
			double x=((Number) from.get(xIdx)).doubleValue();
			double y=((Number) from.get(yIdx)).doubleValue();
			
			y = flipY ? -y : y; 
			return new Ellipse2D.Double(x-width/2d, y-width/2d, width, height);
		}	
	}
}
