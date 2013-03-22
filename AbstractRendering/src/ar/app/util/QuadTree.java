package ar.app.util;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import ar.GlyphSet;

public abstract class QuadTree implements GlyphSet {
	/**How many items before exploring subdivisions.**/
	private final int loading;
	private final List<Glyph> items = new ArrayList<Glyph>();
	protected final Rectangle2D concernBounds;
	protected Rectangle2D contentBounds = null;

	public static QuadTree make(int loading, Rectangle2D canvasBounds) {return new QuadTree.InnerNode(loading, canvasBounds);}
	public static QuadTree make(int loading, int centerX, int centerY, int span) {
		return make(loading, new Rectangle2D.Double(centerX-span,centerY-span,span*2,span*2));
	}
	
	protected QuadTree(int loading, Rectangle2D concernBounds) {
		this.loading=loading;
		this.concernBounds = concernBounds;
	}
	
	public Rectangle2D concernBounds() {return concernBounds;}
	public abstract boolean add(Glyph glyph);

	
	public Collection<Glyph> containing(Point2D p) {
		ArrayList<Glyph> collector = new ArrayList<Glyph>();
		containing(p, collector);
		return collector;
	}
	protected abstract void containing(Point2D p, Collection<Glyph> collector);
	public void itemsContains(Point2D p, Collection<Glyph> collector) {
		for (Glyph g: items) {if (g.shape.contains(p)) {collector.add(g);}}
	}
	
	private static String indent(int x) {
		char[] chars = new char[x*2];
		Arrays.fill(chars,' ');
		return new String(chars);
	}
	
	public abstract String toString(int indent);
	
	private static final class EmptyNode extends QuadTree {
		protected EmptyNode(int loading, Rectangle2D concernBounds) {super(loading, concernBounds);}
		public boolean isEmpty() {return true;}
		public int size() {return 0;}

		public Rectangle2D bounds() {return super.concernBounds;}
		public Iterator<Glyph> iterator() {return new EmptyIterator();}
		public boolean add(Glyph glyph) {return false;}
		protected void containing(Point2D p, Collection<Glyph> collector) {}
		public String toString(int indent) {return indent(indent) + "Empty node";}

		private static final class EmptyIterator implements Iterator<Glyph> {
			public boolean hasNext() {return false;}
			public Glyph next() {return null;}
			public void remove() {}
		}
	}
	
	private static final class InnerNode extends QuadTree {
		private QuadTree NW,NE,SW,SE;
		
		private InnerNode(int loading, Rectangle2D concernBounds) {
			super(loading,concernBounds);

			double w = concernBounds.getWidth()/2;
			double h = concernBounds.getHeight()/2;
			Rectangle2D r = new Rectangle2D.Double(concernBounds.getX(), concernBounds.getY(),w,h);
			NW = new QuadTree.LeafNode(loading, r);
			r = new Rectangle2D.Double(concernBounds.getCenterX(), concernBounds.getY(), w,h);
			NE = new QuadTree.LeafNode(loading, r);
			r = new Rectangle2D.Double(concernBounds.getX(), concernBounds.getCenterY(), w,h);
			SW = new QuadTree.LeafNode(loading, r);
			r = new Rectangle2D.Double(concernBounds.getCenterX(), concernBounds.getCenterY(), w,h);
			SE = new QuadTree.LeafNode(loading, r);
		}
		
		public boolean add(Glyph glyph) {
			Rectangle glyphBounds = glyph.shape.getBounds();
			if (NW.concernBounds.contains(glyphBounds)) {
				QuadTree q=addTo(NW, glyph);
				if (q!=NW) {NW = q;}
			} else if(NE.concernBounds.contains(glyphBounds)) {
				QuadTree q=addTo(NE, glyph);
				if (q!=NE) {NE = q;}
			} else if(SW.concernBounds.contains(glyphBounds)) {
				QuadTree q=addTo(SW, glyph);
				if (q!=SW) {SW = q;}
			} else if(SE.concernBounds.contains(glyphBounds)) {
				QuadTree q=addTo(SE, glyph);
				if (q!=SE) {SE = q;}
			} else {
				//Won't fit into any child location...so it must live here
				super.items.add(glyph);
			} 
			return true;
		}

		protected static QuadTree addTo(QuadTree target, Glyph item) {
			boolean added = target.add(item);
			if (added) {return target;}
			else {
				QuadTree inner = new InnerNode(target.loading, target.concernBounds);
				for (Glyph g: target.items) {
					inner.add(g);
				}
				inner.add(item);
				return inner;
			}
		}
		
		
		public void containing(Point2D p, Collection<Glyph> collector) {
			itemsContains(p, collector);
			if (NW.concernBounds.contains(p)) {NW.containing(p,collector);}
			else if (NE.concernBounds.contains(p)) {NE.containing(p,collector);}
			else if (SW.concernBounds.contains(p)) {SW.containing(p,collector);}
			else if (SE.concernBounds.contains(p)) {SE.containing(p,collector);}
		}

		@Override
		public boolean isEmpty() {
			return super.items.size() != 0
					&& NW.isEmpty()
					&& NE.isEmpty()
					&& SW.isEmpty()
					&& SE.isEmpty();
		}

		public int size() {return super.items.size() + NW.size() + NE.size() + SW.size() + SE.size();}

		public Rectangle2D bounds() {return Util.fullBounds(NW.bounds(), NE.bounds(), SW.bounds(), SE.bounds(), Util.bounds(super.items));}

		@Override
		public Iterator<Glyph> iterator() {
			return new ConcatenateIterator<Glyph>(super.items.iterator(), NW.iterator(), NE.iterator(), SW.iterator(), SE.iterator());
		}
		public String toString() {return toString(0);}
		public String toString(int indent) {
			return String.format("%sNode: %d items (%d local)\n", indent(indent), size(), super.items.size())
						+ "NW " + NW.toString(indent+1)
						+ "NE " + NE.toString(indent+1)
						+ "SW " + SW.toString(indent+1)
						+ "SE " + SE.toString(indent+1);
		}
	}
	
	private static final class LeafNode extends QuadTree {

		private LeafNode(int loading, Rectangle2D concernBounds) {super(loading,concernBounds);}
		public int size() {return super.items.size();}
		public Iterator<Glyph> iterator() {return super.items.iterator();}
		
		/**Add an item to this node.  Returns true if the item was added.  False otherwise.**/
		public boolean add(Glyph glyph) {
			if (super.items.size() == super.loading) {
				return false;
			} else {
				super.items.add(glyph);
				return true;
			}
		}
		
		/**Content bounds**/
		public Rectangle2D bounds() {return Util.bounds(super.items);}
		public boolean isEmpty() {return super.items.size()==0;}
		protected void containing(Point2D p, Collection<Glyph> collector) {super.itemsContains(p, collector);}
		public String toString() {return toString(0);}
		public String toString(int level) {return indent(level) + "Leaf: " + super.items.size() + " items\n";}
	}
}
