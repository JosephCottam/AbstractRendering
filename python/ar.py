import re
import sys
import numpy as np
from math import floor 
import ctypes
from fast_project import _projectRects
import general
try:
  from numba import autojit
except ImportError:
  print "Error loading numba."
  autojit = lambda f: f

############################  Core System ####################
_lib = ctypes.CDLL('libtransform.dylib')

class Glyphset(list):
  def asarray(self):
    return np.array(self, order="F")

class GlyphAggregates:
  def __init__(self, glyph, val):
    self.glyph = glyph
    self.array = np.empty((glyph[2]-glyph[0], glyph[3]-glyph[1]), dtype=np.int32)
    self.array.fill(val)
    ##TODO: chane to fill with zero, then insert the glyph value where it actually touches
    ##TODO: Mking this object may really be the job of the info function


def _project(viewxform, glyphset):
  """Project the points found in the glyphset according tot he view transform."""
  points = glyphset[:,:4]
  out = np.empty_like(points,dtype=np.int32)
  _projectRects(viewxform.asarray(), points, out)
  return out

class Grid(object):
    width = 2000
    height = 2000
    viewxform = None   # array [tx, ty, sx, sy]

    _glyphset = None
    _projected = None
    _aggregates = None
    
    def __init__(self, w,h,viewxform):
      self.width=w
      self.height=h
      self.viewxform=viewxform

    def aggregate(self, glyphset, aggregator):
      """ 
      Returns ndarray of results of applying func to each element in 
      the grid.  Creates a new ndarray of the given dtype.

      Stores the results in _aggregates
      """

      self._glyphset = glyphset.asarray()
      projected = _project(self.viewxform, self._glyphset)
      self._projected = projected

      aggregates = aggregator.allocate(self.width, self.height, self._glyphset)
      for glyph in projected:
        glyph_grid = GlyphAggregates(glyph,1)  
        aggregator.combine(aggregates, glyph_grid)

      self._aggregates = aggregates 

    def transfer(self, transferer):
        """ Returns pixel grid of NxMxRGBA32 (for now) """
        return transferer.transfer(self)

        
class Aggregator(object):
  out_type = None
  in_type = None
  identity=None

  def combine(self, existing, update):
    """
    existing: outype npy array
    update: intype np array
    """
    pass

  def rollup(*vals):
    pass


class Transfer(object):
  input_spec = None # tuple of (shape, dtype)
  # For now assume output is RGBA32
  #output = None

  def makegrid(self, grid):
    return np.ndarray((grid.width, grid.height, 4), dtype=np.uint8)

  def transfer(self, grid):
    raise NotImplementedError

class PixelAggregator(Aggregator):
  def __init__(self, pixelfunc):
    self.pixelfunc = pixelfunc

  def aggregate(self, grid):
      outgrid = np.empty_like(self._projected, dtype=np.int32)
      #outgrid = np.empty_like(self._projected, dtype=aggregator.out_dtype)
      outgrid.ravel()[:] = map(lambda ids: self.pixelfunc(self._glyphset, ids), self._projected.flat)

    

 
class PixelTransfer(Transfer):
  """Transfer function that does non-vectorized per-pixel transfers."""

  def __init__(self, pixelfunc, prefunc):
    self.pixelfunc = pixelfunc
    self.prefunc = prefunc

  def transfer(self, grid):
    outgrid = self.makegrid(grid)
    self._pre(grid)
    (width,height) = (grid.width, grid.height)

    for x in xrange(0, width):
      for y in xrange(0, height):
        outgrid[x,y] = self.pixelfunc(grid, x, y)

    return outgrid


def render(glyphs, aggregator, trans, screen,ivt):
  """
  Render a set of glyphs under the specified condition to the described canvas.
  glyphs ---- Glyphs t render
  selector -- Function used to select which glyphs apply to which pixel
  aggregator  Function to combine a set of glyphs into a single aggregate value
  trans ----- Function for converting aggregates to colors
  screen ---- (width,height) of the canvas
  ivt ------- INVERSE view transform (converts pixels to canvas space)
  """

  grid = Grid(screen[0], screen[1], ivt.inverse())
  grid.aggregate(glyphs, aggregator)
  return grid.transfer(trans)


###############################  Graphics Components ###############

class AffineTransform(list):
  def __init__(self, tx, ty, sx, sy):
    list.__init__(self, [tx,ty,sx,sy])
    self.tx=tx
    self.ty=ty
    self.sx=sx
    self.sy=sy

  def trans(self, x, y):
    """Transform a passed point."""
    x = self.sx * x + self.tx
    y = self.sy * y + self.ty
    return (x, y)

  def transform(self, glyph):
    """Transform a passed glyph (somethign with x,y,w,h)"""
    (p1x,p1y) = self.trans(glyph.x, glyph.y)
    (p2x,p2y) = self.trans(glyph.x+glyph.width, glyph.y+glyph.height)
    w = p2x-p1x
    h = p2y-p1y
    return Glyph(p1x, p1y, w, h, glyph.props)

  def asarray(self): return np.array(self)

  def inverse(self):
    return AffineTransform(-self.tx/self.sx, -self.ty/self.sx, 1/self.sx, 1/self.sy)

class Color(list):
  def __init__(self,r,g,b,a):
    list.__init__(self,[r,g,b,a])
    self.r=r
    self.g=g
    self.b=b
    self.a=a

  def asarray(self): return np.array(self, dtype=np.uint8)

class Glyph(list):
  def __init__(self,x,y,w,h,*props):
    fl = [x,y,w,h]
    fl.extend(props)
    list.__init__(self,fl)
    self.x=x
    self.y=y
    self.width=w
    self.height=h
    self.props=props

  def asarray(self): return np.array(self)


############################  Support functions ####################


#Assumes x,y,w,h exist on glyph
#Does the glyph contain any part of the pixel?
def contains(px, glyph):
  return (px.x+px.w > glyph.x   #Really is >= if using "left/top is in, right/bottom is out" convention
      and px.y + px.h > glyph.y #Really is >= if using "left/top is in, right/bottom is out" convention
      and px.x < glyph.x + glyph.width
      and px.y < glyph.y + glyph.height)

def containing(px, glyphs):
  items = []
  for g in glyphs:
    if contains(px, g): 
      items.append(g)
      
  return items

def bounds(glyphs):
  """Compute bounds of the glyph-set.  Returns (X,Y,W,H)"""
  minX=float("inf")
  maxX=float("-inf")
  minY=float("inf")
  maxY=float("-inf")
  for g in glyphs:
    minX=min(minX, g.x)
    maxX=max(maxX, g.x+g.width)
    minY=min(minY, g.y)
    maxY=max(maxY, g.y+g.height)

  return (minX, minY, maxX-minX, maxY-minY)

def zoom_fit(screen, bounds):
  """What affine transform will zoom-fit the given items?
     screen: (w,h) of the viewing region
     bounds: (x,y,w,h) of the items to fit
     returns: AffineTransform object
  """
  (sw,sh) = screen
  (gx,gy,gw,gh) = bounds
  scale = max(gw/float(sw), gh/float(sh))
  return AffineTransform(gx,gy,scale,scale)


def load_csv(filename, skip, xc,yc,vc,width,height):
  source = open(filename, 'r')
  glyphs = Glyphset()
  
  for i in range(0, skip):
    source.readline()

  for line in source:
    line = re.split("\s*,\s*", line)
    x = float(line[xc].strip())
    y = float(line[yc].strip())
    v = float(line[vc].strip()) if vc >=0 else 1 
    g = Glyph(x,y,width,height,v)
    glyphs.append(g)

  source.close()
  return glyphs

def main():
  ##Abstract rendering function implementation modules (for demo purposes only)
  import numeric
  import categories
  import infos

  source = sys.argv[1]
  skip = int(sys.argv[2])
  xc = int(sys.argv[3])
  yc = int(sys.argv[4])
  vc = int(sys.argv[5])
  size = float(sys.argv[6])
  glyphs = load_csv(source,skip,xc,yc,vc,size,size)

  screen=(10,10)
  ivt = zoom_fit(screen,bounds(glyphs))

  image = render(glyphs, 
                 numeric.Count(), 
                 numeric.Segment(Color(0,0,0,0), Color(255,255,255,255), .5),
                 screen, 
                 ivt)

  print image


if __name__ == "__main__":
    main()
