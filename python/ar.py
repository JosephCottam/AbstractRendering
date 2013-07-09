import re
import sys
import numpy as np
from math import floor 
import ctypes

try:
  from numba import autojit
except ImportError:
  print "Error loading numba."
  autojit = lambda f: f

############################  Core System ####################
_lib = ctypes.CDLL('libtransform.dylib')

class Glyphset(list):
  def asarray(self):
    return np.array(self)

def _project(viewxform, glyphset):
  """Project the points found in the glyphset according tot he view transform."""
  points = _correctFormat(glyphset)
  out = np.empty_like(points,dtype=np.int32)
  _projectRects(viewxform.asarray(), points, out)
  return out

def _projectRects(viewxform, inputs, outputs):
    assert(len(inputs.shape) == 2 and inputs.shape[0] == 4)
    assert(inputs.shape == outputs.shape)
    t = ctypes.POINTER(ctypes.c_double)
    cast = ctypes.cast
    c_xforms = (ctypes.c_double * 4)(*viewxform)
    c_inputs = (t * 4)(*(cast(inputs[i].ctypes.data, t) 
                         for i in range(0, 4)))
    c_outputs = (t* 4)(*(cast(outputs[i].ctypes.data, t)
                         for i in range(0,4)))
    _lib.transform_d(ctypes.byref(c_xforms),
                     ctypes.byref(c_inputs),
                     ctypes.byref(c_outputs),
                     inputs.shape[1])


def _correctFormat(glyphset):
  """
  Converter a glyphset into a list of four-tuples (X,Y,W,H).
  The input array may be row-major or column-major.
  The input array must have at least four columns.
  The output is a column-major array, four columns wide.
  """
  out = np.empty((4, len(glyphset)), dtype=np.float32)
  subset = glyphset[:,:4].asfortranarray()
  return reorder.reshape((4,-1))
   

@autojit
def _store(width, height, projected):
  empty = []
  outgrid = np.ndarray((width, height), dtype=object)
  outgrid.fill(empty)
  for i in xrange(0, projected.shape[0]):
    x = projected[0,i]
    y = projected[1,i]
    x2 = projected[2,i]
    y2 = projected[3,i]
    for xx in xrange(x, x2):
      for yy in xrange(y, y2):
        ls = outgrid[xx,yy]
        if (ls == empty): 
          ls = []
          outgrid[xx,yy] = ls 
        ls.append(i)
  return outgrid
   

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
      self._storefun = _store

    def project(self, glyphset):
      """
      Parameters
      ==========
      glyphset: Numpy record array
      should be record array with at least the following named fields:
        x, y, width, height.
      Stores result in _projected.
      Stores the passed glyphset in _glyphset
      """
      self._glyphset = glyphset.asarray()

      projected = _project(self.viewxform, self._glyphset)
      import pdb; pdb.set_trace()
      self._projected = _store(self.width, self.height, projected)

    def aggregate(self, aggregator):
        """ 
        Returns ndarray of results of applying func to each element in 
        the grid.  Creates a new ndarray of the given dtype.

        Stores the results in _aggregates
        """
        self._aggregates = aggregator.aggregate(self)

    def transfer(self, transferer):
        """ Returns pixel grid of NxMxRGBA32 (for now) """
        return transferer.transfer(self)

        
class Aggregator(object):
    out_type = None

    def aggregate(self, grid):
        """ Returns the aggregated values from just the indicated fields and
        indicated elements of the glyphset
        """
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
  grid.project(glyphs)
  grid.aggregate(aggregator)
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
    v = float(line[vc].strip())
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
