package ar.util;

import java.io.File; 
import java.io.FileWriter;
import java.io.IOException;

import ar.Aggregates;
import static java.lang.String.format;

public class AggregatesToCSV {
	
	@SuppressWarnings("unchecked")
	public static void export(Aggregates<?> aggs, File file) {
		String content;
		if (aggs == null) {content = "Empty aggregate set.";}
		else if (!(aggs.at(0, 0) instanceof Integer)) {content = "Can only export integer aggregates.";}
		else {content = asCSV((Aggregates<Integer>)aggs);}
	
		try {
			FileWriter w = new FileWriter(file,false);
			w.write(content);
			w.close();
		} catch (IOException e) {throw new RuntimeException("Error writing JSON.", e);}
	}
	
	public static String asCSV(Aggregates<Integer> aggs) {
		StringBuilder b = new StringBuilder();
		b.append("%%%% Abstract rendering: Aggregates output\n");
		b.append(format("%%%% lowX/Y:(%d,%d)\n", aggs.lowX(), aggs.lowY()));
		b.append(format("%%%% highX/Y:(%d,%d)\n", aggs.highX(), aggs.highY()));
		for (int y=aggs.lowY(); y<aggs.highY();y++) {
			for (int x=aggs.lowX(); x<aggs.highX(); x++) {
				b.append(aggs.at(x, y));
				b.append(",");
			}
			b.deleteCharAt(b.length()-1);
			b.append("\n");
		}
		return b.toString();
	}
}
