package ar.ext.spark.hbin;

import java.io.DataInputStream;

import ar.glyphsets.implicitgeometry.Indexed;
import ar.util.memoryMapping.MemMapEncoder.TYPE;

public class DataInputRecord implements Indexed {

	private final TYPE[] types;
	private final Object[] values;
	
	public DataInputRecord(TYPE[] types) {
		this.types = types;
		values = new Object[types.length];
	}
	
	public DataInputRecord(TYPE[] types, DataInputStream source) {
		this(types);
		fill(source);
	}

	@Override public Object get(int i) {return values[i];}
	
	/**Populate this record and return it.**/
	public DataInputRecord fill(DataInputStream source) {
		try {
			for (int i=0; i< values.length; i++) {
				switch (types[i]) {
					case BYTE: values[i] = source.readByte(); break;
					case CHAR: values[i] = source.readChar(); break;
					case DOUBLE: values[i] = source.readDouble(); break;
					case FLOAT: values[i] = source.readFloat(); break;
					case INT: values[i] = source.readInt(); break;
					case LONG: values[i] = source.readLong(); break;
					case SHORT: values[i] = source.readShort(); break;
					default: 
						throw new IllegalArgumentException("Unsupported data type: " + types[i]); 
				}
			}
		} catch (Exception e) {throw new RuntimeException(e);}
		return this;
	}

	@Override public int size() {return types.length;}
}
