package ar.app.util;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import ar.GlyphSet;

public abstract class MultiQuadTree implements GlyphSet {
	private static final double MIN_DIM = .01d;
	
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
	public abstract boolean add(Glyph glyph);

	
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
	
	private static String indent(int x) {
		char[] chars = new char[x*2];
		Arrays.fill(chars,' ');
		return new String(chars);
	}
	
	public abstract String toString(int indent);
	
	private static final class LeafNode extends MultiQuadTree {
		private final List<Glyph> items = new ArrayList<Glyph>();
		private boolean bottom;//If you're at bottom, you don't try to split anymore.

		private LeafNode(int loading, Rectangle2D concernBounds) {
			super(loading,concernBounds);
			bottom = concernBounds.getWidth()<=MIN_DIM;
		}
		protected void items(Collection<Glyph> collector) {collector.addAll(items);}
		public void itemsContains(Point2D p, Collection<Glyph> collector) {
			for (Glyph g: items) {if (g.shape.contains(p)) {collector.add(g);}}
		}
		
		/**Add an item to this node.  Returns true if the item was added.  False otherwise.**/
		public boolean add(Glyph glyph) {
			if (!bottom && items.size() == super.loading) {
				return false;
			} else {
				items.add(glyph);
				return true;
			}
		}
		
		/**Content bounds**/
		public Rectangle2D bounds() {return Util.bounds(items);}
		public boolean isEmpty() {return items.size()==0;}
		protected void containing(Point2D p, Collection<Glyph> collector) {itemsContains(p, collector);}
		public String toString() {return toString(0);}
		public String toString(int level) {return indent(level) + "Leaf: " + items.size() + " items\n";}
	}	
	
	private static final class InnerNode extends MultiQuadTree {
		private MultiQuadTree NW,NE,SW,SE;
		
		private InnerNode(int loading, Rectangle2D concernBounds) {
			super(loading,concernBounds);

			double w = concernBounds.getWidth()/2;
			double h = concernBounds.getHeight()/2;
			Rectangle2D r = new Rectangle2D.Double(concernBounds.getX(), concernBounds.getY(),w,h);
			NW = new MultiQuadTree.LeafNode(loading, r);
			r = new Rectangle2D.Double(concernBounds.getCenterX(), concernBounds.getY(), w,h);
			NE = new MultiQuadTree.LeafNode(loading, r);
			r = new Rectangle2D.Double(concernBounds.getX(), concernBounds.getCenterY(), w,h);
			SW = new MultiQuadTree.LeafNode(loading, r);
			r = new Rectangle2D.Double(concernBounds.getCenterX(), concernBounds.getCenterY(), w,h);
			SE = new MultiQuadTree.LeafNode(loading, r);
		}
		
		public boolean add(Glyph glyph) {
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
			else{return true;}
		}

		protected static MultiQuadTree addTo(MultiQuadTree target, Glyph item) {
			boolean added = target.add(item);
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
			return String.format("%sNode: %d items\n", indent(indent), size())
						+ "NW " + NW.toString(indent+1)
						+ "NE " + NE.toString(indent+1)
						+ "SW " + SW.toString(indent+1)
						+ "SE " + SE.toString(indent+1);
		}
	}
}
