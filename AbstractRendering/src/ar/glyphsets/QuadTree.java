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
 * No items are held in intermediate nodes**/

public abstract class QuadTree implements GlyphSet {
	private static final double MIN_DIM = .0001d;
	
	/**How many items before exploring subdivisions.**/
	private final int loading;

	public static QuadTree make(int loading) {return new QuadTree.RootNode(loading);}
	
	protected QuadTree(int loading) {this.loading=loading;}

  /**What space is this node responsible for?**/
	public abstract Rectangle2D concernBounds();
  
  
  /**Tight bounding of the items contained under this node.
   * Will always be equal to or smaller than concernBounds.
   * Where concernBounds is a statement of what may be, 
   * bounds is a statement of what is.**/
	public abstract Rectangle2D bounds();

  /**Add an item to the node's sub-tree**/
	public abstract boolean add(Glyph glyph);

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
		Collection<Glyph> collector = new HashSet<Glyph>();
		containing(p, collector);
		return collector;
	}
  /**Efficiency method for collecting items touching a point**/
	protected abstract void containing(Point2D p, Collection<Glyph> collector);

  /**Convert the tree to a string where indentation indicates depth in tree.**/
	public abstract String toString(int indent);
  public String toString() {return toString(0);}

  /**Structure to represent the bounds of the sub-quardants of a node**/
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


  protected static QuadTree addTo(QuadTree target, Glyph item) {
    boolean added = target.add(item);
    if (added) {return target;}
    else {
      QuadTree inner = new InnerNode(target.loading, target.concernBounds());
      for (Glyph g:target.items()) {inner.add(g);}
      inner.add(item);
      return inner;
    }
  }
		
		
  /**The root node does not actually hold an items, it is to faciliate the "up" direction splits.
   * A node of this type is always the initial node of the tree.  Most operations are passed
   * through it to its only child.**/
  private static final class RootNode extends QuadTree {
    private QuadTree child;
    public RootNode(int loading) {
      super(loading);
      child = new LeafNode(loading, new Rectangle2D.Double(0,0,0,0));
    }

    public boolean add(Glyph glyph) {
      Rectangle2D b = glyph.shape.getBounds2D();

      if (!child.concernBounds().contains(b)) {
        if (child instanceof LeafNode) {
          //If the root is a leaf, then the tree has no depth, so we feel free to expand the root 
          //to fit the data until the loading limit has been reached
          
          Rectangle2D newBounds = Util.fullBounds(b, child.bounds());
          this.child = new LeafNode(super.loading, newBounds, (LeafNode) child);
        } else {
          System.out.println("\tSplit up");
          //If the root node is not a leaf, then make new sibblings/parent for the current root until it fits.
          //Growth is from the center-out, so the new siblings each get one quadrant from the current root
          //and have three quadrants that start out vacant.
          QuadTree.InnerNode iChild = (QuadTree.InnerNode) child;
          Rectangle2D currentBounds = child.concernBounds(); 
          Rectangle2D newBounds = new Rectangle2D.Double(
              currentBounds.getX()-currentBounds.getWidth()/2.0d,
              currentBounds.getY()-currentBounds.getHeight()/2.0d,
              currentBounds.getWidth()*2,
              currentBounds.getHeight()*2);


          //The following checks prevent empty nodes from proliferating as you split up.  
          //Leaf nodes in the old tree are rebounded for the new tree.  
          //Non-leaf nodes are replaced with a quad of nodes
          InnerNode newChild = new InnerNode(super.loading, newBounds);
          if (iChild.NE instanceof InnerNode) {
            newChild.NE =new InnerNode(super.loading, newChild.NE.concernBounds());
            ((InnerNode) newChild.NE).SW = iChild.NE;
          } else if (!iChild.NE.isEmpty()) {
            newChild.NE = new LeafNode(super.loading, newChild.NE.concernBounds(), (LeafNode) iChild.NE);
          }

          if (iChild.NW instanceof InnerNode) {
            newChild.NW = new InnerNode(super.loading, newChild.NW.concernBounds());
            ((InnerNode) newChild.NW).SE = iChild.NW;
          } else if (!iChild.NW.isEmpty()) {
            newChild.NW = new LeafNode(super.loading, newChild.NW.concernBounds(), (LeafNode) iChild.NW);
          }

          if (iChild.SW instanceof InnerNode) {
            newChild.SW = new InnerNode(super.loading, newChild.SW.concernBounds());
            ((InnerNode) newChild.SW).NE = iChild.SW;
          } else if (!iChild.SW.isEmpty()) {
            newChild.SW = new LeafNode(super.loading, newChild.SW.concernBounds(), (LeafNode) iChild.SW);
          }

          if (iChild.SE instanceof InnerNode) {
            newChild.SE = new InnerNode(super.loading, newChild.SE.concernBounds());
            ((InnerNode) newChild.SE).NW = iChild.SE;
          } else if (!iChild.SE.isEmpty()) {
            newChild.SE = new LeafNode(super.loading, newChild.SE.concernBounds(), (LeafNode) iChild.SE);
          }
          this.child = newChild;
        }
      }

      child = child.addTo(child, glyph);
      return true;
    }


    public boolean isEmpty() {return child.isEmpty();}
    public Rectangle2D concernBounds() {return child.concernBounds();}
    public Rectangle2D bounds() {return child.bounds();}
    public void items(Collection<Glyph> collector) {child.items(collector);}
    public void containing(Point2D p, Collection<Glyph> collector) {child.containing(p, collector);}
    public String toString(int indent) {return child.toString(indent);}
  }

	private static final class LeafNode extends QuadTree {
		private final List<Glyph> items;
    protected final Rectangle2D concernBounds;

		private LeafNode(int loading, Rectangle2D concernBounds) {this(loading, concernBounds, new ArrayList<Glyph>());}

    //Rebounding version.
    //WARNING: This introduces data sharing and should only be done if old will immediately be destroyed
    private LeafNode(int loading, Rectangle2D concernBounds, LeafNode old) {this(loading, concernBounds, old.items);}

    private LeafNode(int loading, Rectangle2D concernBounds, List<Glyph> glyphs) {
			super(loading);
      this.items = glyphs;
      this.concernBounds = concernBounds;
    }
		
		/**Add an item to this node.  Returns true if the item was added.  False otherwise.
		 * Will return false only if the item count exceeds the load AND the bottom has not been reached AND 
		 * the split passes the "Advantage."
		 * **/
		public boolean add(Glyph glyph) {
			if (concernBounds.getWidth()>MIN_DIM && items.size() >= super.loading && advantageousSplit()) { 
				//TODO: Improve advantageousSplit efficiency; count the multi-touches as they ar added and pre-compute the subs dims
				return false;
			} else {
        System.out.printf("Adding to current lead: %s and %s and %s\n",concernBounds.getWidth()>MIN_DIM , items.size(), advantageousSplit());
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
		
    public Rectangle2D concernBounds() {return concernBounds;}
		
		protected void items(Collection<Glyph> collector) {collector.addAll(items);}
		public Rectangle2D bounds() {return Util.bounds(items);}
		public boolean isEmpty() {return items.size()==0;}
		public String toString(int level) {return Util.indent(level) + "Leaf: " + items.size() + " items\n";}
	}	
	
	private static final class InnerNode extends QuadTree {
		private QuadTree NW,NE,SW,SE;
    protected final Rectangle2D concernBounds;
		
		private InnerNode(int loading, Rectangle2D concernBounds) {
			super(loading);
      this.concernBounds = concernBounds;
			Subs subs = new Subs(concernBounds);

			NW = new QuadTree.LeafNode(loading, subs.NW);
			NE = new QuadTree.LeafNode(loading, subs.NE);
			SW = new QuadTree.LeafNode(loading, subs.SW);
			SE = new QuadTree.LeafNode(loading, subs.SE);
		}
		
		public boolean add(Glyph glyph) {
			boolean added = false;
			Rectangle2D glyphBounds = glyph.shape.getBounds2D();
			
			if (NW.concernBounds().intersects(glyphBounds)) {
				NW=addTo(NW, glyph);
				added=true;
			} if(NE.concernBounds().intersects(glyphBounds)) {
				NE=addTo(NE, glyph);
				added=true;
			} if(SW.concernBounds().intersects(glyphBounds)) {
				SW=addTo(SW, glyph);
				added=true;
			} if(SE.concernBounds().intersects(glyphBounds)) {
				SE=addTo(SE, glyph);
				added=true;
			} 
			
			if (!added) {
				throw new Error(String.format("Did not add glyph bounded %s to node with concern %s", glyphBounds, concernBounds));
			}
			else{return true;}
		}

		public void containing(Point2D p, Collection<Glyph> collector) {
			if (NW.concernBounds().contains(p)) {NW.containing(p,collector);}
			else if (NE.concernBounds().contains(p)) {NE.containing(p,collector);}
			else if (SW.concernBounds().contains(p)) {SW.containing(p,collector);}
			else if (SE.concernBounds().contains(p)) {SE.containing(p,collector);}
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

    public Rectangle2D concernBounds() {return concernBounds;}
		public Rectangle2D bounds() {return Util.fullBounds(NW.bounds(), NE.bounds(), SW.bounds(), SE.bounds());}
		public String toString(int indent) {
			return String.format("%sNode: %d items\n", Util.indent(indent), size())
						+ "NW " + NW.toString(indent+1)
						+ "NE " + NE.toString(indent+1)
						+ "SW " + SW.toString(indent+1)
						+ "SE " + SE.toString(indent+1);
		}
	}
}
