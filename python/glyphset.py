import numpy as np

def enum(**enums): return type('Enum', (), enums)
ShapeCodes = enum(POINT=0, LINE=1, RECT=2)


class Glyphset(object):
  """shaper + shape-params + associated data ==> Glyphset

     fields:
        _points : Points held by this glyphset
        data : Data associated with the pionts.  _points[x] should associate with data[x]
        shapecode: Shapecode that tells how to interpret _points

  """
  _points = None
  _data = None
  shaper = None

  def __init__(self, points, data, shaper):
    self._points = points
    self._data = data
    self.shaper = shaper

  ##TODO: Add the ability to get points m...n 
  def points(self):
    if (type(self.shaper) is Literals):
      if type(self._points) is list:
        return np.array(self._points, order="F")
      elif type(self._points) is np.ndarray:
        return self._points
      else:
        ValueError("Unhandled (literal) points type: %s" % type(self._points))
    else:
      ##TODO: Setup the shaper utilities to go directly to the...save a copy 
      return np.array(self.shaper(self._points), order="F")

  def data(self): 
    return self._data
  
  def bounds(self):
    """Compute bounds of the glyph-set.  Returns (X,Y,W,H)"""
    minX=float("inf")
    maxX=float("-inf")
    minY=float("inf")
    maxY=float("-inf")
    for p in self._points:
      g = self.shaper(p)
      x = g[0]
      y = g[1]
      w = g[2]
      h = g[3]
      
      minX=min(minX, x)
      maxX=max(maxX, x+w)
      minY=min(minY, y)
      maxY=max(maxY, y+h)
    return (minX, minY, maxX-minX, maxY-minY)
  


### Shapers.....
class Shaper(object):
  fns = None #List of functions to apply 
  code = None
  def __call__(self, vals):
    import pdb; pdb.set_trace()
    return [map(lambda f: f(val,r), self.fns) for val,r in enumerate(vals)]

class Literals(Shaper):
  """Optimization marker, tested in Glyphset and conversions skipped if present.
     Using this class asserts that the _poitns value of the Glyphset is already the
     correct shape for the given glyph type.
     Use with caution...
  """
  def __init__(self, code):
    self.code = code

  def __call__(self, vals):
    return vals

class ToRect(Shaper):
  code = ShapeCodes.RECT
  def __init__(self, tox, toy, tow, toh):
    self.fns = [tox,toy,tow,toh]

class ToLine(Shaper):
  code = ShapeCodes.LINE
  def __init__(self, tox1, toy1, tox2, toy2):
    self.fns = [tox1, toy1, tox2, toy2]

class ToPoint(Shaper):
  code = ShapeCodes.POINT
  def __init__(self, tox, toy, tow, toh):
    self.fns = [tox, toy,0,0]

#### Utilities for shapers....
def const(v):
  def f(a,r): return v

def idx(i):
  """Return value at index in the given row"""
  def f(a,r): return a[r][i]

def colIdx(c):
  """De-reference a column from a, then gets the row (idx gets row then col)"""
  def f(a,r): return a[c][r]
