package ar.app.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ar.Aggregates;

public class AggregatesToJSON {
	
	@SuppressWarnings("unchecked")
	public static void export(Aggregates<?> aggs, File file) {
		String content;
		if (aggs == null) {content = "Empty aggregate set.";}
		else if (!(aggs.get(0, 0) instanceof Integer)) {content = "Can only export integer aggregates.";}
		else {content = export((Aggregates<Integer>)aggs);}
	
		try(FileWriter w = new FileWriter(file)) {
			w.write(content);
			w.close();
		} catch (IOException e) {throw new RuntimeException("Error writing JSON.", e);}
	}
	
	public static String export(Aggregates<Integer> aggs) {
		StringBuilder b = new StringBuilder();
		b.append("{");
		b.append("\"width\":" + aggs.highX() + ",\n");
		b.append("\"aggs\":");
		b.append("[");
		for (int y=0; y<aggs.highY();y++) {
			for (int x=0; x<aggs.highX(); x++) {
				b.append(aggs.get(x, y));
				b.append(",");
			}
			b.append("\n");
		}
		b.delete(b.length()-2, b.length()-1);
		b.append("]}");
		return b.toString();
	}
}
