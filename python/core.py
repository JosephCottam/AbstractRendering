import re
import sys
import os
import numpy as np
import ctypes
from fast_project import _projectRects
import geometry

try:
  from numba import autojit
except ImportError:
  print "Error loading numba."
  autojit = lambda f: f

_lib = ctypes.CDLL(os.path.join(os.path.dirname(__file__), 'transform.so'))

############################  Core System ####################
def enum(**enums): return type('Enum', (), enums)
ShapeCodes = enum(POINT=0, LINE=1, RECT=2)

class Glyphset():
  """shapeocde + shape-params + associated data ==> Glyphset

     fields:
        _points : Points held by this glyphset
        data : Data associated with the pionts.  _points[x] should associate with data[x]
        shapecode: Shapecode that tells how to interpret _points
  """
  _points = None
  data = None
  shapecode = None

  def __init__(self, points, data, shapecode=ShapeCodes.RECT):
    self._points = points
    self.data = data
    self.shapecode = shapecode

  ##TODO: Get rid of the necessity to represent as numpy...else it is an in-core system...currently only used in projection... 
  def points(self):
    if type(self._points) is list:
      return np.array(self._points, order="F")
    elif type(self._points) is np.ndarray:
      return self._points
    else:
      ValueError("Unhandled points type: %s" % type(self._points))
   
  def __getattr__(self, name):
    return getattr(self._points, name)

##TODO: change to fill with default value, then insert the glyph value where it actually touches
def glyphAggregates(points, shapeCode, val, default):
  def scalar(array, val): array.fill(val)
  def nparray(array,val): array[:] = val
  
 
  if type(val) == np.ndarray:
    fill = nparray 
    extShape = val.shape
  else:
    fill = scalar 
    extShape = ()

  #TODO: Handle ShapeCode.POINT here....
  array = np.empty((points[2]-points[0],points[3]-points[1])+extShape, dtype=np.int32)

  if shapeCode == ShapeCodes.RECT:
    fill(array, val)
  elif shapeCode == ShapeCodes.LINE:
    fill(array, default)
    geometry.bressenham(array, points, val)

  return array


############ Core process functions #################

def render(glyphs, info, aggregator, shader, screen,ivt):
  """
  Render a set of glyphs under the specified condition to the described canvas.
  glyphs ---- Glyphs t render
  selector -- Function used to select which glyphs apply to which pixel
  aggregator  Function to combine a set of glyphs into a single aggregate value
  trans ----- Function for converting aggregates to colors
  screen ---- (width,height) of the canvas
  ivt ------- INVERSE view transform (converts pixels to canvas space)
  """
  projected = project(glyphs, ivt.inverse())
  aggregates = aggregate(projected, info, aggregator, screen)
  shaded = shade(aggregates, shader)
  return shaded


def project(glyphset, viewxform):
  """Project the points found in the glyphset according to the view transform.
     viewxform -- convert canvas space to pixel space
     glyphset -- set of glyphs (represented as [x,y,w,h,...]
  """
  points = glyphset.points()
  out = np.empty_like(points, dtype=np.int32)
  _projectRects(viewxform.asarray(), points, out)
  
  #Ensure visilibity, make sure w/h are always at least one
  #TODO: There is probably a more numpy-ish way to do this...(and it might not be needed for Shapecode.POINT)
  for i in xrange(0,out.shape[0]):
    if out[i,0] == out[i,2]: out[i,2] += 1
    if out[i,1] == out[i,3]: out[i,3] += 1

  return Glyphset(out, glyphset.data, glyphset.shapecode)

def aggregate(glyphs, info, aggregator, screen):
    (width, height) = screen

    infos = [info(point, data) for point, data in zip(glyphs.points(), glyphs.data)] #TODO: vectorize
    aggregates = aggregator.allocate(width, height, glyphs, infos)
    for idx, points in enumerate(glyphs):
      aggregator.combine(aggregates, points, glyphs.shapecode, infos[idx])
    return aggregates


#TODO: Add specialization here.  Take a 3rd argument 'specailizer'  if ommited, just use aggregates
def shade(aggregates, shader):
   """Convert a set of aggregate into another set of aggregates
      according to some data shader.  Many common cases, the result
      aggregates is an image, but it does not need to be.

      aggregates -- input aggregaets
      shader -- data shader used in the conversion
   """
   return shader.shade(aggregates)


class Aggregator(object):
  out_type = None
  in_type = None
  identity=None
  
  def allocate(self, width, height, glyphset, infos):
    pass

  def combine(self, existing, points, shapecode, val):
    """
    existing: outype npy array
    update: intype np array
    """
    pass

  def rollup(*vals):
    pass


#TODO: Add specialization to Shaders....
class Shader(object):
  def makegrid(self, grid):
    """Create an output grid.  
       Default implementation creates one of the same width/height of the input
       suitable for colors (dept 4, unit8).
    """
    (width, height) = grid.shape[0], grid.shape[1]
    return np.ndarray((width, height, 4), dtype=np.uint8)

  def shade(self, grid):
    """Execute the actual data shader operation."""
    raise NotImplementedError
  
  def __add__(self, other): 
    """Extend this shader by executing another in sequence."""
    if (not isinstance(other, Shader)): 
        raise TypeError("Can only extend with a shader.  Received a " + str(type(other)))
    return Seq(self, other) 


class Seq(Shader):
  """Shader that does a sequence of other shaders."""
     
  def __init__(self, *args):
    self._parts = args

  def makegrid(self, grid):
    for t in self._parts:
      grid = t.makegrid(grid)
    return grid

  def shade(self, grid):
    for t in self._parts:
      grid = t.shade(grid)
    return grid

  def __add__(self, other):
    if (other is None) : return self
    if (not isinstance(other, Shader)): 
        raise TypeError("Can only extend shader with another shader.  Received a " + str(type(other)))
    return Seq(list(self._parts) + other) 


class PixelAggregator(Aggregator):
  def __init__(self, pixelfunc):
    self.pixelfunc = pixelfunc

  def aggregate(self, grid):
      outgrid = np.empty_like(self._projected, dtype=np.int32)
      #outgrid = np.empty_like(self._projected, dtype=aggregator.out_dtype)
      outgrid.ravel()[:] = map(lambda ids: self.pixelfunc(self._glyphset, ids), self._projected.flat)


class PixelShader(Shader):
  """Data shader that does non-vectorized per-pixel shading."""

  def __init__(self, pixelfunc, prefunc):
    self.pixelfunc = pixelfunc
    self.prefunc = prefunc

  def shade(self, grid):
    outgrid = self.makegrid(grid)
    self._pre(grid)
    (width,height) = (grid.width, grid.height)

    for x in xrange(0, width):
      for y in xrange(0, height):
        outgrid[x,y] = self.pixelfunc(grid, x, y)

    return outgrid


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
  glyphs = []
  data = []
  
  for i in range(0, skip):
    source.readline()

  for line in source:
    line = re.split("\s*,\s*", line)
    x = float(line[xc].strip())
    y = float(line[yc].strip())
    v = float(line[vc].strip()) if vc >=0 else 1 
    g = Glyph(x,y,width,height)
    glyphs.append(g)
    data.append(v)

  source.close()
  return Glyphset(glyphs,data)

def main():
  ##Abstract rendering function implementation modules (for demo purposes only)
  import numeric
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
                 infos.id(),
                 numeric.Count(), 
                 numeric.AbsSegment(Color(0,0,0,0), Color(255,255,255,255), .5),
                 screen, 
                 ivt)

  print image


if __name__ == "__main__":
    main()
