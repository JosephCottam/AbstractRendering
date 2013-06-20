package ar.ext.avro;

import org.apache.avro.generic.GenericRecord;

import ar.glyphsets.implicitgeometry.Valuer;

public class CountConverter implements Valuer<GenericRecord, Integer> {
	public Integer value(GenericRecord from) {return (Integer) from.get(0);}
}
