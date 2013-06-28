import ar
import numpy as np

try:
  from numba import autojit
except ImportError:
  print "Error loading numba."
  autojit = lambda f: f

######## Aggregators ########
class Count(ar.Aggregator):
  """Count the number of items that fall into a particular grid element."""
  out_type=np.int32

  def aggregate(self, grid): 
    def myLen(o):
      if o == None:
        return 0
      return len(o)

    f = np.vectorize(myLen)
    return f(grid._projected)


@autojit
def _sum(projected, glyphset, validx):
  width, height = projected.shape
  outgrid=np.zeros((width, height), dtype=np.int32)
  outflat = outgrid.flat
  inflat = projected.flat

  for i in xrange(0, len(outgrid.flat)):
    glyphids = inflat[i]
    if (glyphids == None): continue
    for gidx in xrange(0, len(glyphids)):
      glyphid = glyphids[gidx]
      glyph = glyphset[glyphid]
      outflat[i]+=glyph[validx]
  
  return outgrid

class Sum(ar.Aggregator):
   """Sum the items in each grid element."""
   out_type=np.int32

   def __init__(self, validx=4):
     self._validx = validx

   def aggregate(self, grid):
     return _sum(grid._projected, grid._glyphset, self._validx)

######## Transfers ##########
class Segment(ar.Transfer):
  """
  Paint all pixels with aggregate value above divider one color 
  and below the divider another.
  """

  def __init__(self, low, high, divider):
    self.high = high
    self.low = low
    self.divider = float(divider)

  def transfer(self, grid):
    outgrid = np.ndarray((grid.width, grid.height, 4), dtype=np.uint8)
    mask = (grid._projected >= self.divider) 
    outgrid[mask] = self.high
    outgrid[~mask] = self.low
    return outgrid



class HDInterpolate(ar.Transfer):
  """High-definition interpolation between two colors."""
  def __init__(self, low, high, reserve=ar.Color(255,255,255,255)):
    self.low=low
    self.high=high
    self.reserve=reserve

  def transfer(self, grid):
    items = grid._aggregates
    min = items.min()
    max = items.max()
    span = float(max-min) 
    percents = (items-min)/span

    #TODO: mask in reserve for zero-values

    colorspan = self.high.asarray().astype(np.int32) - self.low.asarray().astype(np.int32)
    return (percents[:,:,np.newaxis] * colorspan[np.newaxis,np.newaxis,:] + self.low.asarray()).astype(np.uint8)


