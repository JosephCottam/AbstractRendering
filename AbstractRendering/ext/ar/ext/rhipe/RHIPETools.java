package ar.ext.rhipe;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import ar.Aggregates;
import ar.Aggregator;
import ar.Glyph;
import ar.Glyphset;
import ar.Renderer;
import ar.glyphsets.SimpleGlyph;
import ar.glyphsets.WrappedCollection;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.renderers.SerialSpatial;
import ar.rules.Numbers;

public class RHIPETools {
	/**Provide a delimited string and receive a glyphset.
	 * Lines are wrapped into indexed entries, so the shaper and valuer
	 * should operate on ar.glyphsets.implicitGeometry.Indexed items.
	 * 
	 * @param text A delimited set of entries
	 * @param lineTerminal String that delimits between entry
	 * @param fieldTerminal String that delimits a field (cannot be the same as lineTerminal) 
	 * @param glypher Object that converts String arrays into glyphs
	 */
	public static final Glyphset.RandomAccess<String> fromText(
			String text, 
			String lineTerminal, 
			String fieldTerminal, 
			TraceEntry glypher) {
		List<Indexed>  items = new ArrayList<Indexed>();
		for (String entry: text.split(lineTerminal)) {
			String[] raw = entry.split(fieldTerminal);
			Indexed item = new Indexed.ArrayWrapper(raw);
			items.add(item);
		}
		return new WrappedCollection.List<Indexed,String>(items, glypher, glypher, String.class);
	}
	

	/**Construct a TraceEntry object.  Provide for convenience working with rJava**/
	public static TraceEntry traceEntry(int x, int y, int cat, double size) {return new TraceEntry(x,y,cat, size);}
	
	/**Shaper/Valuer to convert the expected trace entries (as Indexed-wrapped arrays-of-strings) into glyphs.
	 */
	public static final class TraceEntry implements Shaper<Indexed>, Valuer<Indexed, String> {
		private final int xField, yField, catField;
		private final double size;
		public TraceEntry(){this(0,1,2, .1);}
		public TraceEntry(int xField, int yField, int catField, double size) {
			this.catField=catField;
			this.xField=xField;
			this.yField=yField;
			this.size = size;
		}
		public Shape shape(Indexed from) {
			double x = Double.parseDouble(from.get(xField).toString());
			double y = Double.parseDouble(from.get(yField).toString());
			return new Rectangle2D.Double(x,y,size,size);
		}
		public String value(Indexed from) {return (String) from.get(catField);} 
		public Glyph<String> glyph(Indexed from) {return new SimpleGlyph<String>(shape(from), value(from));}
	}
	
	
	/**Convert a set of aggregates into a set of reduction keys/values.
	 * For "convenience" the keys/values are returned as a csv-string with the value in the last spot.
	 * @param aggs
	 * @return Set of reduce keys
	 */
	public static String[] reduceKeys(Aggregates<?> aggs) {
		ArrayList<String> entries = new ArrayList<String>();
		for (int x=aggs.lowX(); x<aggs.highX(); x++) {
			for (int y=aggs.lowY(); y<aggs.highY(); y++) {
				entries.add(String.format("%d,%d,%d",x,y,aggs.at(x, y)));
			}
		}
		return entries.toArray(new String[entries.size()]);
	}
	
	/**Convenience method for using the RHIPE tools in their default configurations.
	 * Also servers as an example call sequence if being used from R.
	 * @param entries
	 * @param ivt
	 * @param width
	 * @param height
	 * @return
	 */
	public static String[] render(String entries, AffineTransform ivt, int width, int height) {
		TraceEntry te = new TraceEntry();
		Glyphset glyphs = fromText(entries, "\\s*\n", "\\s*,\\s*", te);
		Renderer r = new SerialSpatial();
		Aggregator agg = new Numbers.Count();
		Aggregates<?> aggs = r.aggregate(glyphs, agg, ivt, width, height);
		return reduceKeys(aggs);
	}
}
