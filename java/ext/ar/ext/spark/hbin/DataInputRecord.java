package ar.ext.spark.hbin;

import java.io.DataInputStream;

import ar.glyphsets.implicitgeometry.Indexed;
import ar.util.memoryMapping.MemMapEncoder.TYPE;

public class DataInputRecord implements Indexed {

	private final Object[] values;
	
	
	public DataInputRecord(TYPE[] types, DataInputStream source) {
		super();
		this.values = new Object[types.length];
		
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
	}


	@Override public Object get(int i) {return values[i];}

}
