package ar.glyphsets;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import ar.Glyph;
import ar.Glyphset;
import ar.util.Util;


/**Explicit geometry, spatially arranged glyphset with dynamically growing extent.
 * 
 * This class should be constructed using the "make" method, thus no constructor is exposed.
 * 
 * 
 * In this quad tree, items appear in each node that they touch (e.g., multi-homed).
 * Can split an existing node into sub-nodes or move "up" and make the root a sub-node with new siblings/parent.
 * No items are held in intermediate nodes.
 * 
 * This class can be used efficiently with the pixel serial or pixel parallel renderer.
 * 
 * There are four types of nodes:
 *   ** RootHolder is a proxy for a root node, 
 *      but enables the bounds of the tree to expand without the host program knowing.
 *   ** InnerNode is a node that has no items in it, but has quadrants below it that may have items
 *   ** LeafNode is a node that may have items but also may have one more set of nodes beneath it.
 *      The items directly held by the leaf nodes are those that touch multiple of its children.
 *   ** LeafQuad is a node that may have items but no children. All items held in a LeafQuad are 
 *      entirely contained by that LeafQuad.
 * 
 * The LeafNode vs. LeafQuad split is made so the decision to further divide the tree 
 * can be flexible and efficient.  A LeafNode may only have LeafQuads as its children.  
 * 
 * **/

public abstract class DynamicQuadTree<V> implements Glyphset<V> {
	/**Smallest quad that will be created.**/
	public static double MIN_DIM = .001d;
	
	/**Percentage of items that need to be uniquely assigned to a sub-quad for a split to occur.
	 * 
	 * Without a load-factor, a quad will split once it hits capacity.
	 * However, if all items touch all sub-quads, then the split may become an infinite loop.
	 * This load-factor addresses how many items are uniquely into one sub-quad before 
	 * the split is done.  
	 */
	public static double CROSS_LOAD_FACTOR = .25;
	
	/**How many items before splitting is considered.**/
	public static int LOADING = 10;
	
	/**Feather-factor for sub-quads.  Each sub-quad
	 * overlaps with its neighbors slightly.  This prevents 
	 * items from falling between quads.
	 */
	public static double FEATHER = MIN_DIM/4.0d;
	
	private static int NW = 0;
	private static int NE = 1;
	private static int SW = 2;
	private static int SE = 3;

	/**Structure to represent the bounds of the sub-quardants of a node**/
	private static final class Subs {
		public final Rectangle2D[] quads = new Rectangle2D[4];
		public Subs (final Rectangle2D current) {
			double w = (current.getWidth()/2)+(2*FEATHER);
			double h = (current.getHeight()/2)+(2*FEATHER);
			quads[NW] = new Rectangle2D.Double(current.getX()-FEATHER,       current.getY()-FEATHER,w,h);
			quads[NE] = new Rectangle2D.Double(current.getCenterX()-FEATHER, current.getY()-FEATHER, w,h);
			quads[SW] = new Rectangle2D.Double(current.getX()-FEATHER,       current.getCenterY()-FEATHER, w,h);
			quads[SE] = new Rectangle2D.Double(current.getCenterX()-FEATHER, current.getCenterY()-FEATHER, w,h);
		}
	}
	
	/**How many items before exploring subdivisions.**/
	protected final Rectangle2D concernBounds;

	/**Construct a dynamic quad tree for the given value type.*/
	public static <V> DynamicQuadTree<V> make() {return new DynamicQuadTree.RootHolder<V>();}

	protected DynamicQuadTree(Rectangle2D concernBounds) {
		this.concernBounds = concernBounds;
	}

	/**What space is this node responsible for?**/
	public Rectangle2D concernBounds() {return concernBounds;}

	/**Tight bounding of the items contained under this node.
	 * Will always be equal to or smaller than concernBounds.
	 * Where concernBounds is a statement of what may be, 
	 * bounds is a statement of what is.**/
	public abstract Rectangle2D bounds();

	/**Add an item to the node's sub-tree**/
	public abstract void add(Glyph<V> glyph);

	/**How many things are held in this sub-tree?**/
	public long size() {return items().size();}

	/**What are the items of the sub-tree?**/
	public Collection<Glyph<V> > items() {
		Collection<Glyph<V>> collector = new HashSet<Glyph<V>>();
		items(collector);
		return collector;		
	}

	/**Efficiency method for collecting items.**/
	protected abstract void items(Collection<Glyph<V>> collector);


	/**What items in this sub-tree contain the passed point?**/	
	public Collection<Glyph<V>> intersects(Rectangle2D pixel) {
		Collection<Glyph<V>> collector = new HashSet<Glyph<V>>();
		intersects(pixel, collector);
		return collector;
	}

	/**Efficiency method for collecting items touching a point**/
	protected abstract void intersects(Rectangle2D pixel, Collection<Glyph<V>> collector);

	protected boolean doSplit() {return false;}
	
	/**Convert the tree to a string where indentation indicates depth in tree.**/
	public abstract String toString(int indent);
	public String toString() {return toString(0);}


	protected static <V> DynamicQuadTree<V> addTo(DynamicQuadTree<V> target, final Glyph<V> item) {
		if (target.doSplit()) {target = new InnerNode<V>((LeafNode<V>) target);}
 		target.add(item);
		return target;
	}

	private static <G> Glyphset<G> subset(DynamicQuadTree<G>[] glyphs, int bottom, int top) {
		DynamicQuadTree<G>[] subset = Arrays.copyOfRange(glyphs, bottom, top);
		return new InnerNode<G>(subset);
	}
	
	@SuppressWarnings({ "unused", "rawtypes" })
	//Utility method, helpful in debugging tree splits
	private static String boundsReport(DynamicQuadTree<?> t) {
		if (t instanceof InnerNode) {
			InnerNode i = (InnerNode) t;
			return String.format("IB: %s\n\tNE: %s\n\tNW: %s\n\tSE: %s\n\tSW: %s\n", 
					i.concernBounds,
					i.quads[NE].concernBounds, i.quads[NW].concernBounds, i.quads[SE].concernBounds, i.quads[SW].concernBounds);
		} else if (t instanceof RootHolder) {
			return String.format("RB: %s\n", t.concernBounds);
		} else if (t instanceof LeafNode) {
			LeafNode l = (LeafNode) t;
			return String.format("LB: %s\n\tNE: %s\n\tNW: %s\n\tSE: %s\n\tSW: %s\n", 
					l.concernBounds, 
					l.quads[NE].concernBounds, l.quads[NW].concernBounds, l.quads[SE].concernBounds, l.quads[SW].concernBounds);
		}
		return "Not a know type: " + t.getClass().getName();
	}

	/**The root node does not actually hold an items, it is to facilitate the "up" direction splits.
	 * A node of this type is always the initial node of the tree.  Most operations are passed
	 * through it to its only child.**/
	private static final class RootHolder<V> extends DynamicQuadTree<V> {
		private DynamicQuadTree<V> child;
		
		public RootHolder() {
			super(null);
			child = new LeafNode<V>(new Rectangle2D.Double(0,0,0,0));
		}

		public void add(Glyph<V> glyph) {
			Rectangle2D b = glyph.shape().getBounds2D();

			if (!child.concernBounds().contains(b)) {
				if (child instanceof LeafNode) {
					//If the root is a leaf, then the tree has no depth, so we feel free to expand the root 
					//to fit the data until the loading limit has been reached

					Rectangle2D newBounds = Util.bounds(b, child.bounds());
					DynamicQuadTree<V> newChild = new LeafNode<V>(newBounds, (LeafNode<V>) child);
					this.child = newChild;
				} else {
					DynamicQuadTree<V> c = child;
					while (!c.concernBounds.contains(b)) {
						c = growUp((DynamicQuadTree.InnerNode<V>) c, b);
					}
					this.child = c;
				}
			}

			child = DynamicQuadTree.addTo(child, glyph);
		}
				
		/**Grow the tree so it covers more area than it does currently.
		 * The strategies are based on heuristics and have not been evaluated
		 * for optimality (only sufficiency).
		 * 
		 * Strategies:
		 *   (1) If the new thing touches the current bounds, the the current tree
		 *   is the "center" of new tree.  Each quad of the current tree becomes 
		 *   a sub-quad of each of the new tree's quad.  If a current quad 
		 *   is a leaf, that leaf is expanded to become a new leaf in the new tree.
		 *   
		 *   (2) Otherwise,  the current tree becomes a quad in the new tree.
		 *   Right and above are prioritized higher than left and below.
		 *   
		 *   TODO: Investigate better heuristics...
		 * @return
		 */
		private static final <V> InnerNode<V> growUp(DynamicQuadTree.InnerNode<V> current, Rectangle2D toward) {
			Rectangle2D currentBounds = current.concernBounds();
			int outCode = currentBounds.outcode(toward.getX(), toward.getY());
			
			//TODO: Expand to "touches or is close to"...for some meaning of close...probably proportion of tree's width/height 
			if (toward.intersects(currentBounds)
					|| outCode ==0) { 
				//If the new glyph touches the current bounds, then grow with the current data in the center. 
				Rectangle2D newBounds = new Rectangle2D.Double(
						currentBounds.getX()-currentBounds.getWidth()/2.0d,
						currentBounds.getY()-currentBounds.getHeight()/2.0d,
						currentBounds.getWidth()*2,
						currentBounds.getHeight()*2);
				
				//The following checks prevent empty nodes from proliferating as you split up.  
				//Leaf nodes in the old tree are rebounded for the new tree.  
				//Non-leaf nodes are replaced with a quad of nodes
				InnerNode<V> newChild = new InnerNode<V>(newBounds);
				if (current.quads[NE] instanceof InnerNode) {
					newChild.quads[NE] =new InnerNode<V>(newChild.quads[NE].concernBounds());
					((InnerNode<V>) newChild.quads[NE]).quads[SW] = current.quads[NE];
				} else if (!current.quads[NE].isEmpty()) {
					newChild.quads[NE] = new LeafNode<V>(newChild.quads[NE].concernBounds(), (LeafNode<V>) current.quads[NE]);
				}
	
				if (current.quads[NW] instanceof InnerNode) {
					newChild.quads[NW] = new InnerNode<V>(newChild.quads[NW].concernBounds());
					((InnerNode<V>) newChild.quads[NW]).quads[SE] = current.quads[NW];
				} else if (!current.quads[NW].isEmpty()) {
					newChild.quads[NW] = new LeafNode<V>(newChild.quads[NW].concernBounds(), (LeafNode<V>) current.quads[NW]);
				}
	
				if (current.quads[SW] instanceof InnerNode) {
					newChild.quads[SW] = new InnerNode<V>(newChild.quads[SW].concernBounds());
					((InnerNode<V>) newChild.quads[SW]).quads[NE] = current.quads[SW];
				} else if (!current.quads[SW].isEmpty()) {
					newChild.quads[SW] = new LeafNode<V>(newChild.quads[SW].concernBounds(), (LeafNode<V>) current.quads[SW]);
				}
	
				if (current.quads[SE] instanceof InnerNode) {
					newChild.quads[SE] = new InnerNode<V>(newChild.quads[SE].concernBounds());
					((InnerNode<V>) newChild.quads[SE]).quads[NW] = current.quads[SE];
				} else if (!current.quads[SE].isEmpty()) {
					newChild.quads[SE] = new LeafNode<V>(newChild.quads[SE].concernBounds(), (LeafNode<V>) current.quads[SE]);
				}
				return newChild;
			} else {
				double x,y;
				int replace;
				
				//TODO: Would more directions help?
				//TODO: Would analysis based on tree weights help?
				if ((outCode & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT
						|| (outCode & Rectangle2D.OUT_TOP) == Rectangle2D.OUT_TOP) {
					x = currentBounds.getX();
					y = currentBounds.getY()-currentBounds.getHeight();
					replace = SW;
					
				} else 	if ((outCode & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT
						|| (outCode & Rectangle2D.OUT_BOTTOM) == Rectangle2D.OUT_BOTTOM) {
					x = currentBounds.getX()-currentBounds.getWidth();
					y = currentBounds.getY();
					replace = NE;
					
				} else {throw new RuntimeException("Growing up encountered unexpected out-code:" + outCode);}

				Rectangle2D newBounds =new Rectangle2D.Double(x,y,currentBounds.getWidth()*2.0d,currentBounds.getHeight()*2.0d);
				InnerNode<V> newChild = new InnerNode<V>(newBounds);
				newChild.quads[replace] = current;
				return newChild;
			}
		}

		public boolean isEmpty() {return child.isEmpty();}
		public Rectangle2D concernBounds() {return child.concernBounds();}
		public Rectangle2D bounds() {return child.bounds();}
		public void items(Collection<Glyph<V>> collector) {child.items(collector);}
		public void intersects(Rectangle2D pixel, Collection<Glyph<V>> collector) {child.intersects(pixel, collector);}
		public String toString(int indent) {return child.toString(indent);}
		public long segments() {return child.segments();}
		public Glyphset<V> segment(long bottom, long top) {return child.segment(bottom, top);}
		public Iterator<Glyph<V>> iterator() {return items().iterator();}
	}

	private static final class InnerNode<V> extends DynamicQuadTree<V> {
		private final DynamicQuadTree<V>[] quads;

		/**Create a new InnerNode from an existing leaf node.
		 * The quads of the leaf can be copied directly to the children of this node.
		 * Only the spanning items of the leaf need to go through the regular add procedure.
		 */
		private InnerNode(LeafNode<V> source) {
			this(source.concernBounds);
			for (int i=0; i<quads.length;i++) {
				for (Glyph<V> g: source.quads[i].items) {quads[i].add(g);}
			}
			for (Glyph<V> g:source.spanningItems) {add(g);}
		}
		
		@SuppressWarnings("unchecked")
		private InnerNode(Rectangle2D concernBounds) {
			super(concernBounds);
			quads = new DynamicQuadTree[4];
			Subs subs = new Subs(concernBounds);
			for (int i=0; i< subs.quads.length; i++) {
				quads[i] = new DynamicQuadTree.LeafNode<V>(subs.quads[i]);
			}
		}
		private InnerNode(DynamicQuadTree<V>[] parts) {
			super(null);
			this.quads = parts;
		}

		public void add(Glyph<V> glyph) {
			boolean added = false;
			Rectangle2D glyphBounds = glyph.shape().getBounds2D();

			for (int i=0; i<quads.length; i++) {
				DynamicQuadTree<V> quad = quads[i];
				if (quad.concernBounds.intersects(glyphBounds)) {
					quads[i] = addTo(quad, glyph);
					added = true;
				}
			}

			if (!added && concernBounds.outcode(glyphBounds.getX(), glyphBounds.getY()) !=0) {
				throw new RuntimeException(String.format("Did not add glyph bounded %s to node with concern %s", glyphBounds, concernBounds));
			}
		}

		public void intersects(Rectangle2D pixel, Collection<Glyph<V>> collector) {
			for (DynamicQuadTree<V> q: quads) {
				if (q.concernBounds.intersects(pixel)) {q.intersects(pixel, collector);}
			}
		}

		public boolean isEmpty() {
			for (DynamicQuadTree<V> q: quads) {if (!q.isEmpty()) {return false;}}
			return true;
		}

		public void items(Collection<Glyph<V>> collector) {
			for (DynamicQuadTree<V> q: quads) {q.items(collector);}
		}

		public Rectangle2D bounds() {
			final Rectangle2D[] bounds = new Rectangle2D[quads.length];
			for (int i=0; i<bounds.length; i++) {
				bounds[i] = quads[i].bounds();
			}
			return Util.bounds(bounds);
		}
		
		public String toString(int indent) {
			StringBuilder b = new StringBuilder();
			for (DynamicQuadTree<V> q: quads) {b.append(q.toString(indent+1));}
			return String.format("%sNode: %d items\n", Util.indent(indent), size()) + b.toString();
		}
		public long segments() {return quads.length;}
		public Glyphset<V> segment(long bottom, long top) {
			return DynamicQuadTree.subset(quads, (int) bottom, (int) top);
		}
		public Iterator<Glyph<V>> iterator() {return items().iterator();}
	}
	
	private static final class LeafNode<V> extends DynamicQuadTree<V> {
		@SuppressWarnings("unchecked")
		private final LeafQuad<V>[] quads = new LeafQuad[4];
		private final List<Glyph<V>> spanningItems;
		private int size=0;

		private LeafNode(Rectangle2D concernBounds) {
			this(concernBounds, new ArrayList<Glyph<V>>());
			
		}

		//Re-bounding version.
		//WARNING: This introduces data sharing and should only be done if old will immediately be destroyed
		private LeafNode(Rectangle2D concernBounds, LeafNode<V> old) {
			this(concernBounds, old.items());
		}

		private LeafNode(Rectangle2D concernBounds, Collection<Glyph<V>> glyphs) {
			super(concernBounds);
			spanningItems = new ArrayList<Glyph<V>>(LOADING);
			Subs subs = new Subs(concernBounds);
			for (int i=0; i< subs.quads.length; i++) {
				quads[i] = new DynamicQuadTree.LeafQuad<V>(subs.quads[i]);
			}
			for (Glyph<V> g: glyphs) {add(g);}
		}

		public void add(Glyph<V> glyph) {
			boolean[] hits = new boolean[quads.length];
			int totalHits=0;

			Rectangle2D glyphBounds = glyph.shape().getBounds2D();
			for (int i=0; i<quads.length; i++) {
				LeafQuad<V> quad = quads[i];
				if (quad.concernBounds.intersects(glyphBounds)) {hits[i]=true; totalHits++;}
				if (totalHits>2) {break;}
			}
			
			if (totalHits>1) {spanningItems.add(glyph);}
			else {for (int i=0; i<hits.length;i++) {if (hits[i]) {quads[i].add(glyph);}}}
			size++;

			if (totalHits ==0 && concernBounds.outcode(glyphBounds.getX(), glyphBounds.getY()) !=0) {
				throw new RuntimeException(String.format("Did not add glyph bounded %s to node with concern %s", glyphBounds, concernBounds));
			}
		}

		
		public long size() {return size;}
		public String toString(int indent) {
			return String.format("%sLeafNode: %d items (%s spanning items)\n", Util.indent(indent), size(), spanningItems.size());
		}	
		
		/**Should this leaf become an inner node?  Yes...but only if it would help one of the quads.**/
		protected boolean doSplit() {
			if (size < LOADING && concernBounds.getWidth() < MIN_DIM) {return false;} 
			for (LeafQuad<V> q: quads) {
				if (q.size() > LOADING && (q.size()/(spanningItems.size()+1) > CROSS_LOAD_FACTOR)) {return true;}
			}
			return false;
		}
		
		public void intersects(Rectangle2D pixel, Collection<Glyph<V>> collector) {
			for (DynamicQuadTree<V> q: quads) {
				if (q.concernBounds.intersects(pixel)) {q.intersects(pixel, collector); break;}
			}
			for (Glyph<V> g:spanningItems) {if (g.shape().intersects(pixel)) {collector.add(g);}}
		}
		
		public boolean isEmpty() {
			for (DynamicQuadTree<V> q: quads) {if (!q.isEmpty()) {return false;}}
			return spanningItems.size() == 0;
		}

		//Copy
		public Rectangle2D bounds() {
			final Rectangle2D[] bounds = new Rectangle2D[quads.length+1];
			for (int i=0; i<quads.length; i++) {
				bounds[i] = quads[i].bounds();
			}
			bounds[quads.length] = Util.bounds(spanningItems);
			return Util.bounds(bounds);
		}

		//Copy
		public void items(Collection<Glyph<V>> collector) {
			collector.addAll(spanningItems);
			for (DynamicQuadTree<V> q: quads) {q.items(collector);}
		}
		
		public long segments() {return quads.length;}
		public Glyphset<V> segment(long bottom, long top) {
			return DynamicQuadTree.subset(quads, (int) bottom, (int) top);
		}
		public Iterator<Glyph<V>> iterator() {return items().iterator();}

	}	

	/**Sub-leaf is a quadrant of a leaf.
	 * It represents a set of items that would be entirely in one leaf quadrant if the leaf were to split.
	 * This class exists so leaf-split decisions can be made efficiently.
	 */
	private static final class LeafQuad<V> extends DynamicQuadTree<V> implements Glyphset.RandomAccess<V> {
		private final List<Glyph<V>> items;
		
		protected LeafQuad(Rectangle2D concernBounds) {
			super (concernBounds);
			items = new ArrayList<Glyph<V>>(LOADING);
		}

		//Assumes the geometry check was done by the parent
		public void add(Glyph<V> glyph) {
			items.add(glyph);
		}

		public Rectangle2D concernBounds() {return concernBounds;}
		public boolean isEmpty() {return items.isEmpty();}
		public List<Glyph<V>> items() {return items;}
		public String toString(int level) {return Util.indent(level) + "LeafQuad: " + items.size() + " items\n";}
		public Rectangle2D bounds() {return Util.bounds(items);}
		protected void items(Collection<Glyph<V>> collector) {collector.addAll(items);}
		protected void intersects(Rectangle2D pixel, Collection<Glyph<V>> collector) {
			for (Glyph<V> g: items) {if (g.shape().intersects(pixel)) {collector.add(g);}}
		}
		public long segments() {return items.size();}
		public Glyphset<V> segment(long bottom, long top) {
			return new GlyphSubset.Uncached<V>(this, bottom, top);
		}
		public Iterator<Glyph<V>> iterator() {return items().iterator();}
		public Glyph<V> get(long l) {return items.get((int) l);}
	}
	
}
