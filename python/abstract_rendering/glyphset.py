import numpy as np

def enum(**enums): return type('Enum', (), enums)
ShapeCodes = enum(POINT=0, LINE=1, RECT=2)  ##TODO: Add shapecodes: CIRCLE, POINT_RECT, POINT_CIRCLE, etc

class Glyphset(object):
  """shaper + shape-params + associated data ==> Glyphset

     fields:
        _points : Points held by this glyphset
        _ data : Data associated with the points (basis for 'info' values).  _points[x] should associate with data[x]
        shapecode: Shapecode that tells how to interpret _points

  """
  _points = None
  _data = None
  shaper = None

  def __init__(self, points, data, shaper, colMajor=False):
    self._points = points
    self._data = data
    self.shaper = shaper 
    self.shaper.colMajor = colMajor ##HACK: This modification is bad form...would rather do a copy

  ##TODO: Add the ability to get points m...n 
  def points(self):
    """Returns the set of [x,y,w,h] points for this glyphset.
       Access to raw data is through _points.
    """

    if (type(self.shaper) is Literals):
      if type(self._points) is list:
        return np.array(self._points, order="F")
      elif type(self._points) is np.ndarray:
        return self._points
      else:
        ValueError("Unhandled (literal) points type: %s" % type(self._points))
    else:
      ##TODO: Setup the shaper utilities to go directly to fortran order...save a copy 
      return np.array(self.shaper(self._points), order="F")

  def data(self): 
    return self._data
  
  def bounds(self):
    """Compute bounds of the glyph-set.  Returns (X,Y,W,H)
    
       Assumes a 'simple' layout where the min and max input values
       determine the min and max positional values.
    """
    minX=float("inf")
    maxX=float("-inf")
    minY=float("inf")
    maxY=float("-inf")
    for g in self.points():
      (x,y,w,h) = g

      minX=min(minX, x)
      maxX=max(maxX, x+w)
      minY=min(minY, y)
      maxY=max(maxY, y+h)
    return (minX, minY, maxX-minX, maxY-minY) 
#    points = self.points();
#    minX=points[:,0].min()
#    maxX=points[:,0].max()
#    minY=points[:,1].min()
#    maxY=points[:,1].max()
#    maxW=points[:,2].max()
#    maxH=points[:,3].max()
#
#    shapes = self.shaper([[minX,maxX],[minY,maxY],[maxW,maxW],[maxH,maxH]])
#    minX = shapes[0][0]
#    minY = shapes[0][1]
#    maxX = shapes[1][0]
#    maxY = shapes[1][1]
#    width = shapes[1][2]
#    height = shapes[1][3]
#
#    return (minX, minY, maxX+width, maxY+height) 
  


### Shapers.....
class Shaper(object):
  fns = None #List of functions to apply 
  code = None
  colMajor = False 
  
  ##TODO: When getting subsets of the data out of glyphset.points(), remove this colMajor and handle it up in glyphset instead
  def __call__(self, vals):
    if not self.colMajor:
      return [map(lambda f: f(val), self.fns) for val in vals]
    else:
      return [map(lambda f: f(val), self.fns) for val in zip(*vals)]

class Literals(Shaper):
  """Optimization marker, tested in Glyphset and conversions skipped if present.
     Using this class asserts that the _poitns value of the Glyphset is the set
     of points to feed into a geometry renderer, and thus no projection from
     data space to canvas space is required.
     Use with caution...
  """
  def __init__(self, code):
    self.code = code

  def __call__(self, vals):
    return vals

class ToRect(Shaper):
  """Creates rectangles using functions to build x,y,w,h."""
  code = ShapeCodes.RECT
  def __init__(self, tox, toy, tow, toh):
    self.fns = [tox,toy,tow,toh]

class ToLine(Shaper):
  """Creates lines using functions to build x1,y1,x2,y2"""
  code = ShapeCodes.LINE
  def __init__(self, tox1, toy1, tox2, toy2):
    self.fns = [tox1, toy1, tox2, toy2]

class ToPoint(Shaper):
  """Create a single point, using functions to build x,y,0,0"""
  code = ShapeCodes.POINT
  def __init__(self, tox, toy, tow, toh):
    self.fns = [tox, toy,lambda(x): 0,lambda(x): 0]

#### Utilities for shapers....
def const(v):
  """Create a function that always returns a specific value.
  
    * v -- The value the returned function always returns
  """
  def f(a): return v
  return f

def idx(i):
  """Create a function that expects an indexable thing and returns a value at the passed index
  
  * i -- The index that will be used 
  """
  def f(a): return a[i]
  return f
