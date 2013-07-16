package ar.rules;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;

import ar.Aggregator;
import ar.Glyph;
import ar.Glyphset;

public final class Numbers {
	private Numbers() {/*Prevent instantiation*/}
	
	/**How many items present?**/
	public static final class Count implements Aggregator<Object, Integer> {
		public Integer combine(long x, long y, Integer left, Object update) {return left+1;}
		public Integer rollup(List<Integer> integers) {
			int acc=0;
			for (Integer v: integers) {acc+=v;}
			return acc;
		}
		
		public Integer identity() {return 0;}
		public boolean equals(Object other) {return other instanceof Count;}
		public Class<Object> input() {return Object.class;}
		public Class<Integer> output() {return Integer.class;}

	}
	
}
