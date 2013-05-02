package ar.glyphsets;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import ar.GlyphSet;
import ar.Util;


/**Quad tree where items appear in each node that they touch.  
 * No items are held in intermediate nodes**/
public abstract class MultiQuadTree implements GlyphSet {
	private static final double MIN_DIM = .0001d;
	
	/**How many items before exploring subdivisions.**/
	private final int loading;
	protected final Rectangle2D concernBounds;

	public static MultiQuadTree make(int loading, Rectangle2D canvasBounds) {return new MultiQuadTree.InnerNode(loading, canvasBounds);}
	public static MultiQuadTree make(int loading, int centerX, int centerY, int span) {
		return make(loading, new Rectangle2D.Double(centerX-span,centerY-span,span*2,span*2));
	}
	
	protected MultiQuadTree(int loading, Rectangle2D concernBounds) {
		this.loading=loading;
		this.concernBounds = concernBounds;
	}
	
	public Rectangle2D concernBounds() {return concernBounds;}
	public abstract void add(Glyph glyph);
	public abstract boolean maybeAdd(Glyph glyph);

	
	public int size() {return items().size();}
	public Collection<Glyph> items() {
		Collection<Glyph> collector = new HashSet<Glyph>();
		items(collector);
		return collector;		
	}
	protected abstract void items(Collection<Glyph> collector);
	
	public Collection<Glyph> containing(Point2D p) {
		Collection<Glyph> collector = new HashSet<Glyph>();
		containing(p, collector);
		return collector;
	}
	protected abstract void containing(Point2D p, Collection<Glyph> collector);
	
	public abstract String toString(int indent);

	private static final class Subs {
		public final Rectangle2D NW, NE, SW,SE;
		public final Rectangle2D[] subs = new Rectangle2D[4];
		public Subs (final Rectangle2D current) {
			double w = current.getWidth()/2;
			double h = current.getHeight()/2;
			NW = new Rectangle2D.Double(current.getX(), current.getY(),w,h);
			NE  = new Rectangle2D.Double(current.getCenterX(), current.getY(), w,h);
			SW  = new Rectangle2D.Double(current.getX(), current.getCenterY(), w,h);
			SE  = new Rectangle2D.Double(current.getCenterX(), current.getCenterY(), w,h);
			subs[0] = NW;
			subs[1] = NE;
			subs[2] = SW;
			subs[3] = SE;
		}
	}
	
	private static final class LeafNode extends MultiQuadTree {
		private final List<Glyph> items = new ArrayList<Glyph>();
		private boolean bottom;//If you're at bottom, you don't try to split anymore.

		private LeafNode(int loading, Rectangle2D concernBounds) {
			super(loading,concernBounds);
			bottom = concernBounds.getWidth()<=MIN_DIM;
		}
		
		@Override
		public void add(Glyph glyph) {throw new UnsupportedOperationException("Must call maybeAdd instead.");}
		
		/**Add an item to this node.  Returns true if the item was added.  False otherwise.
		 * Will return false only if the item count exceeds the load AND the bottom has not been reached AND 
		 * the split passes the "Advantage."
		 * **/
		public boolean maybeAdd(Glyph glyph) {
			if (!bottom && items.size() == super.loading && advantageousSplit()) { 
				//TODO: Improve advantageousSplit efficiency; count the multi-touches as they ar added and pre-compute the subs dims
				return false;
			} else {
				items.add(glyph);
				return true;
			}
		}
		
		/**Check that at least half of the items will be uniquely assigned to a sub-region.**/
		public boolean advantageousSplit() {
			int multiTouch = 0;
			final Subs subs = new Subs(concernBounds);
			for (Glyph item:items) {
				Rectangle2D b = item.shape.getBounds2D();
				if (touchesSubs(b, subs.subs) >1) {multiTouch++;}
			}
			return (multiTouch <= (items.size()/2));
		}

		private final int touchesSubs(Rectangle2D bounds, Rectangle2D[] quads) {
			int count = 0;
			for (Rectangle2D quad: quads) {if (bounds.intersects(quad)) {count++;}}
			return count;
		}

		protected void containing(Point2D p, Collection<Glyph> collector) {
			
			for (Glyph g: items) {if (g.shape.contains(p)) {collector.add(g);} g.shape.contains(3d,4d);}
		}
		
		
		protected void items(Collection<Glyph> collector) {collector.addAll(items);}
		public Rectangle2D bounds() {return Util.bounds(items);}
		public boolean isEmpty() {return items.size()==0;}
		public String toString() {return toString(0);}
		public String toString(int level) {return Util.indent(level) + "Leaf: " + items.size() + " items\n";}
	}	
	
	private static final class InnerNode extends MultiQuadTree {
		private MultiQuadTree NW,NE,SW,SE;
		
		private InnerNode(int loading, Rectangle2D concernBounds) {
			super(loading,concernBounds);
			Subs subs = new Subs(concernBounds);

			NW = new MultiQuadTree.LeafNode(loading, subs.NW);
			NE = new MultiQuadTree.LeafNode(loading, subs.NE);
			SW = new MultiQuadTree.LeafNode(loading, subs.SW);
			SE = new MultiQuadTree.LeafNode(loading, subs.SE);
		}
		
		public boolean maybeAdd(Glyph glyph) {add(glyph); return true;}
		public void add(Glyph glyph) {
			boolean added = false;
			Rectangle2D glyphBounds = glyph.shape.getBounds2D();
			
			if (NW.concernBounds.intersects(glyphBounds)) {
				MultiQuadTree q=addTo(NW, glyph);
				if (q!=NW) {NW = q;}
				added=true;
			} if(NE.concernBounds.intersects(glyphBounds)) {
				MultiQuadTree q=addTo(NE, glyph);
				if (q!=NE) {NE = q;}
				added=true;
			} if(SW.concernBounds.intersects(glyphBounds)) {
				MultiQuadTree q=addTo(SW, glyph);
				if (q!=SW) {SW = q;}
				added=true;
			} if(SE.concernBounds.intersects(glyphBounds)) {
				MultiQuadTree q=addTo(SE, glyph);
				if (q!=SE) {SE = q;}
				added=true;
			} 
			
			if (!added) {
				throw new Error(String.format("Did not add glyph bounded %s to node with concern %s", glyphBounds, concernBounds));
			}
		}

		protected static MultiQuadTree addTo(MultiQuadTree target, Glyph item) {
			boolean added = target.maybeAdd(item);
			if (added) {return target;}
			else {
				MultiQuadTree inner = new InnerNode(target.loading, target.concernBounds);
				for (Glyph g:target.items()) {inner.add(g);}
				inner.add(item);
				return inner;
			}
		}
		
		
		public void containing(Point2D p, Collection<Glyph> collector) {
			if (NW.concernBounds.contains(p)) {NW.containing(p,collector);}
			else if (NE.concernBounds.contains(p)) {NE.containing(p,collector);}
			else if (SW.concernBounds.contains(p)) {SW.containing(p,collector);}
			else if (SE.concernBounds.contains(p)) {SE.containing(p,collector);}
		}

		@Override
		public boolean isEmpty() {
			return  NW.isEmpty()
					&& NE.isEmpty()
					&& SW.isEmpty()
					&& SE.isEmpty();
		}

		public void items(Collection<Glyph> collector) {
			NW.items(collector);
			NE.items(collector);
			SW.items(collector);
			SE.items(collector);
		}

		public Rectangle2D bounds() {return Util.fullBounds(NW.bounds(), NE.bounds(), SW.bounds(), SE.bounds());}

		public String toString() {return toString(0);}
		public String toString(int indent) {
			return String.format("%sNode: %d items\n", Util.indent(indent), size())
						+ "NW " + NW.toString(indent+1)
						+ "NE " + NE.toString(indent+1)
						+ "SW " + SW.toString(indent+1)
						+ "SE " + SE.toString(indent+1);
		}
	}
}