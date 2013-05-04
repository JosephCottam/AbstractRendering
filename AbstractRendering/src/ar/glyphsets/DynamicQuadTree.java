package ar.glyphsets;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import ar.GlyphSet;
import ar.Util;


/**Quad tree where items appear in each node that they touch (e.g., multi-homed).
 * Can split an existing node into sub-nodes or move "up" and make the root a sub-node with new sibblings/parent.
 * No items are held in intermediate nodes
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

public abstract class DynamicQuadTree implements GlyphSet {
	public static double MIN_DIM = .0001d;
	public static double CROSS_LOAD_FACTOR = .25;
	public static int LOADING = 10;
	
	public static int NW = 0;
	public static int NE = 1;
	public static int SW = 2;
	public static int SE = 3;

	/**Structure to represent the bounds of the sub-quardants of a node**/
	private static final class Subs {
		public final Rectangle2D[] quads = new Rectangle2D[4];
		public Subs (final Rectangle2D current) {
			double w = current.getWidth()/2;
			double h = current.getHeight()/2;
			quads[NW] = new Rectangle2D.Double(current.getX(), current.getY(),w,h);
			quads[NE] = new Rectangle2D.Double(current.getCenterX(), current.getY(), w,h);
			quads[SW] = new Rectangle2D.Double(current.getX(), current.getCenterY(), w,h);
			quads[SE] = new Rectangle2D.Double(current.getCenterX(), current.getCenterY(), w,h);
		}
	}
	
	/**How many items before exploring subdivisions.**/
	protected final Rectangle2D concernBounds;

	public static DynamicQuadTree make() {return new DynamicQuadTree.RootHolder();}

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
	public abstract void add(Glyph glyph);

	/**How many things are held in this sub-tree?**/
	public int size() {return items().size();}

	/**What are the items of the sub-tree?**/
	public Collection<Glyph> items() {
		Collection<Glyph> collector = new HashSet<Glyph>();
		items(collector);
		return collector;		
	}

	/**Efficiency method for collecting items.**/
	protected abstract void items(Collection<Glyph> collector);


	/**What items in this sub-tree contain the passed point?**/
	public Collection<Glyph> containing(Point2D p) {
		Collection<Glyph> collector2 = new ArrayList<Glyph>();
		containing(p, collector2);
		return collector2;
	}
	/**Efficiency method for collecting items touching a point**/
	protected abstract void containing(Point2D p, Collection<Glyph> collector);

	protected boolean doSplit() {return false;}
	
	/**Convert the tree to a string where indentation indicates depth in tree.**/
	public abstract String toString(int indent);
	public String toString() {return toString(0);}


	protected static DynamicQuadTree addTo(DynamicQuadTree target, Glyph item) {
		target.add(item);
		
		if (target.doSplit()) {
			return new InnerNode((LeafNode) target);
		} else {
			return target;
		}
	}


	/**The root node does not actually hold an items, it is to faciliate the "up" direction splits.
	 * A node of this type is always the initial node of the tree.  Most operations are passed
	 * through it to its only child.**/
	private static final class RootHolder extends DynamicQuadTree {
		private DynamicQuadTree child;
		
		public RootHolder() {
			super(null);
			child = new LeafNode(new Rectangle2D.Double(0,0,0,0));
		}

		public void add(Glyph glyph) {
			Rectangle2D b = glyph.shape.getBounds2D();

			if (!child.concernBounds().contains(b)) {
				if (child instanceof LeafNode) {
					//If the root is a leaf, then the tree has no depth, so we feel free to expand the root 
					//to fit the data until the loading limit has been reached

					Rectangle2D newBounds = Util.fullBounds(b, child.bounds());
					DynamicQuadTree newChild = new LeafNode(newBounds, (LeafNode) child);
					this.child = newChild;
				} else {
					DynamicQuadTree c = child;
					while (!c.concernBounds.contains(b)) {c = growUp((DynamicQuadTree.InnerNode) c, b);}
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
		 * @return
		 */
		private static final InnerNode growUp(DynamicQuadTree.InnerNode current, Rectangle2D toward) {
			Rectangle2D currentBounds = current.concernBounds();

			//TODO: Expand to "touches or is close to"...for some meaning of close...probably proportion of tree size 
			if (toward.intersects(currentBounds)) { 
				//If the new glyph touches the current bounds, then grow with the current data in the center. 
				Rectangle2D newBounds = new Rectangle2D.Double(
						currentBounds.getX()-currentBounds.getWidth()/2.0d,
						currentBounds.getY()-currentBounds.getHeight()/2.0d,
						currentBounds.getWidth()*2,
						currentBounds.getHeight()*2);
				
				//The following checks prevent empty nodes from proliferating as you split up.  
				//Leaf nodes in the old tree are rebounded for the new tree.  
				//Non-leaf nodes are replaced with a quad of nodes
				InnerNode newChild = new InnerNode(newBounds);
				if (current.quads[NE] instanceof InnerNode) {
					newChild.quads[NE] =new InnerNode(newChild.quads[NE].concernBounds());
					((InnerNode) newChild.quads[NE]).quads[SW] = current.quads[NE];
				} else if (!current.quads[NE].isEmpty()) {
					newChild.quads[NE] = new LeafNode(newChild.quads[NE].concernBounds(), (LeafNode) current.quads[NE]);
				}
	
				if (current.quads[NW] instanceof InnerNode) {
					newChild.quads[NW] = new InnerNode(newChild.quads[NW].concernBounds());
					((InnerNode) newChild.quads[NW]).quads[SE] = current.quads[NW];
				} else if (!current.quads[NW].isEmpty()) {
					newChild.quads[NW] = new LeafNode(newChild.quads[NW].concernBounds(), (LeafNode) current.quads[NW]);
				}
	
				if (current.quads[SW] instanceof InnerNode) {
					newChild.quads[SW] = new InnerNode(newChild.quads[SW].concernBounds());
					((InnerNode) newChild.quads[SW]).quads[NE] = current.quads[SW];
				} else if (!current.quads[SW].isEmpty()) {
					newChild.quads[SW] = new LeafNode(newChild.quads[SW].concernBounds(), (LeafNode) current.quads[SW]);
				}
	
				if (current.quads[SE] instanceof InnerNode) {
					newChild.quads[SE] = new InnerNode(newChild.quads[SE].concernBounds());
					((InnerNode) newChild.quads[SE]).quads[NW] = current.quads[SE];
				} else if (!current.quads[SE].isEmpty()) {
					newChild.quads[SE] = new LeafNode(newChild.quads[SE].concernBounds(), (LeafNode) current.quads[SE]);
				}
				return newChild;
			} else {
				int outCode = currentBounds.outcode(toward.getX(), toward.getY());
				double x,y;
				int direction;
				
				if ((outCode & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT
						|| (outCode & Rectangle2D.OUT_TOP) == Rectangle2D.OUT_TOP) {
					x = currentBounds.getX();
					y = currentBounds.getY();
					direction = SW;
				} else 	if ((outCode & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT
						|| (outCode & Rectangle2D.OUT_BOTTOM) == Rectangle2D.OUT_BOTTOM) {
					x = currentBounds.getX()-currentBounds.getWidth();
					y = currentBounds.getY()+currentBounds.getHeight();
					direction = NE;
				} else {throw new RuntimeException("Growing up encountered unexpected out-code:" + outCode);}

				Rectangle2D newBounds =new Rectangle2D.Double(x,y,currentBounds.getWidth()*2,currentBounds.getHeight()*2);
				InnerNode newChild = new InnerNode(newBounds);
				newChild.quads[direction] = current;
				return newChild;
			}
		}

		public boolean isEmpty() {return child.isEmpty();}
		public Rectangle2D concernBounds() {return child.concernBounds();}
		public Rectangle2D bounds() {return child.bounds();}
		public void items(Collection<Glyph> collector) {child.items(collector);}
		public void containing(Point2D p, Collection<Glyph> collector) {child.containing(p, collector);}
		public String toString(int indent) {return child.toString(indent);}
	}

	private static final class InnerNode extends DynamicQuadTree {
		private final DynamicQuadTree[] quads =new DynamicQuadTree[4];

		/**Create a new InnerNode from an existing leaf node.
		 * The quads of the leaf can be copied directly to the children of this node.
		 * Only the spanning items of the leaf need to go through the regular add procedure.
		 */
		private InnerNode(LeafNode source) {
			this(source.concernBounds);
			for (int i=0; i<quads.length;i++) {
				for (Glyph g: source.quads[i].items) {quads[i].add(g);}
			}
			for (Glyph g:source.spanningItems) {add(g);}
		}
		private InnerNode(Rectangle2D concernBounds) {
			super(concernBounds);
			Subs subs = new Subs(concernBounds);
			for (int i=0; i< subs.quads.length; i++) {
				quads[i] = new DynamicQuadTree.LeafNode(subs.quads[i]);
			}
		}

		public void add(Glyph glyph) {
			boolean added = false;
			Rectangle2D glyphBounds = glyph.shape.getBounds2D();

			for (int i=0; i<quads.length; i++) {
				DynamicQuadTree quad = quads[i];
				if (quad.concernBounds.intersects(glyphBounds)) {
					quads[i] = addTo(quad, glyph);
					added = true;
				}
			}

			if (!added) {
				throw new Error(String.format("Did not add glyph bounded %s to node with concern %s", glyphBounds, concernBounds));
			}
		}

		public void containing(Point2D p, Collection<Glyph> collector) {
			for (DynamicQuadTree q: quads) {
				if (q.concernBounds.contains(p)) {q.containing(p, collector); break;}
			}
		}

		public boolean isEmpty() {
			for (DynamicQuadTree q: quads) {if (!q.isEmpty()) {return false;}}
			return true;
		}

		public void items(Collection<Glyph> collector) {
			for (DynamicQuadTree q: quads) {q.items(collector);}
		}

		public Rectangle2D bounds() {
			final Rectangle2D[] bounds = new Rectangle2D[quads.length];
			for (int i=0; i<bounds.length; i++) {
				bounds[i] = quads[i].bounds();
			}
			return Util.fullBounds(bounds);
		}
		
		public String toString(int indent) {
			StringBuilder b = new StringBuilder();
			for (DynamicQuadTree q: quads) {b.append(q.toString(indent+1));}
			return String.format("%sNode: %d items\n", Util.indent(indent), size()) + b.toString();
		}
	}
	
	private static final class LeafNode extends DynamicQuadTree {
		private final List<Glyph> spanningItems;
		private final LeafQuad[] quads = new LeafQuad[4];
		private int size=0;

		private LeafNode(Rectangle2D concernBounds) {
			this(concernBounds, new ArrayList<Glyph>());
			
		}

		//Re-bounding version.
		//WARNING: This introduces data sharing and should only be done if old will immediately be destroyed
		private LeafNode(Rectangle2D concernBounds, LeafNode old) {
			this(concernBounds, old.items());
		}

		private LeafNode(Rectangle2D concernBounds, Collection<Glyph> glyphs) {
			super(concernBounds);
			spanningItems = new ArrayList<Glyph>(LOADING);
			Subs subs = new Subs(concernBounds);
			for (int i=0; i< subs.quads.length; i++) {
				quads[i] = new DynamicQuadTree.LeafQuad(subs.quads[i]);
			}
			for (Glyph g: glyphs) {add(g);}
		}

		public void add(Glyph glyph) {
			boolean[] hits = new boolean[quads.length];
			int totalHits=0;

			Rectangle2D glyphBounds = glyph.shape.getBounds2D();
			for (int i=0; i<quads.length; i++) {
				LeafQuad quad = quads[i];
				if (quad.concernBounds.intersects(glyphBounds)) {hits[i]=true; totalHits++;}
				if (totalHits>2) {break;}
			}
			
			if (totalHits>1) {spanningItems.add(glyph);}
			else {for (int i=0; i<hits.length;i++) {if (hits[i]) {quads[i].add(glyph);}}}
			size++;

			if (totalHits ==0) {
				throw new Error(String.format("Did not add glyph bounded %s to node with concern %s", glyphBounds, concernBounds));
			}
		}

		
		public int size() {return size;}
		public String toString(int indent) {
			return String.format("%sLeafNode: %d items (%s spanning items)\n", Util.indent(indent), size(), spanningItems.size());
		}	
		
		/**Should this leaf become an inner node?  Yes...but only if it would help one of the quads.**/
		protected boolean doSplit() {
			if (size < LOADING && concernBounds.getWidth() < MIN_DIM) {return false;} 
			for (LeafQuad q: quads) {
				if (q.size() > LOADING && (q.size()/(spanningItems.size()+1) > CROSS_LOAD_FACTOR)) {return true;}
			}
			return false;
		}
		
		//NEAR Copy
		public void containing(Point2D p, Collection<Glyph> collector) {
			for (DynamicQuadTree q: quads) {
				if (q.concernBounds.contains(p)) {q.containing(p, collector); break;}
			}
			for (Glyph g:spanningItems) {if (g.shape.contains(p)) {collector.add(g);}}
		}
		
		//NEAR Copy
		public boolean isEmpty() {
			for (DynamicQuadTree q: quads) {if (!q.isEmpty()) {return false;}}
			return spanningItems.size() == 0;
		}

		//Copy
		public Rectangle2D bounds() {
			final Rectangle2D[] bounds = new Rectangle2D[quads.length+1];
			for (int i=0; i<quads.length; i++) {
				bounds[i] = quads[i].bounds();
			}
			bounds[quads.length] = Util.bounds(spanningItems);
			return Util.fullBounds(bounds);
		}

		//Copy
		public void items(Collection<Glyph> collector) {
			collector.addAll(spanningItems);
			for (DynamicQuadTree q: quads) {q.items(collector);}
		}
		
		
	}	

	/**Sub-leaf is a quadrant of a leaf.
	 * It represents a set of items that would be entirely in one leaf quadrant if the leaf were to split.
	 * This class exists so leaf-split decisions can be made efficiently.
	 */
	private static final class LeafQuad extends DynamicQuadTree {
		private final List<Glyph> items;
		
		protected LeafQuad(Rectangle2D concernBounds) {
			super (concernBounds);
			items = new ArrayList<Glyph>(LOADING);
		}

		//Assumes the geometry check was done by the parent
		public void add(Glyph glyph) {
			items.add(glyph);
		}

		public Rectangle2D concernBounds() {return concernBounds;}
		public boolean isEmpty() {return items.isEmpty();}
		public List<Glyph> items() {return items;}
		public String toString(int level) {return Util.indent(level) + "LeafQuad: " + items.size() + " items\n";}
		public Rectangle2D bounds() {return Util.bounds(items);}
		protected void items(Collection<Glyph> collector) {collector.addAll(items);}
		protected void containing(Point2D p, Collection<Glyph> collector) {
			for (Glyph g: items) {if (g.shape.contains(p)) {collector.add(g);}}
		}
	}
	
}
