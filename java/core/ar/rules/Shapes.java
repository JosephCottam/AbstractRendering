package ar.rules;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ar.Aggregates;
import ar.Transfer;
import ar.rules.CategoricalCounts.CoC;
import ar.util.HasViewTransform;
import ar.util.Util;

/**Shape-aware transfer functions.**/
public class Shapes {
	
	/**Given a set of regions, combine all aggregates that are in that region and make that combined
	 * value the value of any point in the region.
	 * 
	 * TODO: Extend to more than CoC...
	 * TODO: Consider adding the view transform to the transfer function specialization process
	 * 
	 */
	public static class ShapeGather implements Transfer<CategoricalCounts<Color>, CoC<Color>> {
		private static final long serialVersionUID = 4664592034128237981L;
		final List<Shape> baseRegions;
		final HasViewTransform transformSource;
		
		/**
		 * @param reg Shapes to use in gathering
		 * @param transformSource Source of the view transform used to modify shapes
		 */
		public ShapeGather(List<Shape> reg, HasViewTransform transformSource) {
			this.transformSource = transformSource;
			this.baseRegions = reg;
		}

		@Override
		public CoC<Color> emptyValue() {return new CoC<Color>(Util.COLOR_SORTER);}
		
		@Override
		public ShapeGather.Specialized specialize(Aggregates<? extends CategoricalCounts<Color>> aggregates) {
			Map<Shape, CoC<Color>> values = new HashMap<>();
			AffineTransform vt = transformSource.viewTransform();
			List<Shape> viewRegions = new ArrayList<>();
			for (Shape s: baseRegions) {viewRegions.add(vt.createTransformedShape(s));}
			for (Shape region: viewRegions) {
				values.put(region, gather(region, aggregates));
			}
			
			return new Specialized(baseRegions, viewRegions, transformSource, values);
		}

		private CoC<Color> gather(Shape region, Aggregates<? extends CategoricalCounts<Color>> aggs) {
			Rectangle2D r = new Rectangle2D.Double(0,0,1,1);
			CoC<Color> acc = emptyValue();
			for (int x=aggs.lowX(); x<aggs.highX(); x++) {
				for (int y=aggs.lowY(); y < aggs.highY(); y++) {
					r.setRect(x, y, 1,1);
					if (region.contains(r)) {
						acc = CoC.rollupTwo(acc, (CoC<Color>) aggs.get(x, y));
					}
				}
			}
			return acc;
		}

		private static class Specialized extends ShapeGather implements Transfer.Specialized<CategoricalCounts<Color>, CoC<Color>> { 
			private final Map<Shape, CoC<Color>> regionVals;
			private final List<Shape> viewRegions;
			
			Specialized(List<Shape> baseRegions, List<Shape> viewRegions, HasViewTransform transformSource, Map<Shape, CoC<Color>> regionVals) {
				super(baseRegions, transformSource);
				this.regionVals = regionVals;
				this.viewRegions = viewRegions;
			}


			@Override
			public CoC<Color> at(int x, int y, Aggregates<? extends CategoricalCounts<Color>> aggregates) {
				Shape region = touches(x,y);
				if (region == null) {return emptyValue();}
				return regionVals.get(region);
			}
			

			protected Shape touches(int x, int y) {
				Rectangle2D r = new Rectangle2D.Double(x,y,1,1);
				for (Shape s: viewRegions) {
					if (s.intersects(r)) {
						return s;
					}
				}
				return null;
			}
		}
	}
}
