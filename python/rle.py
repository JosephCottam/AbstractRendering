import ar
import numpy as np
from math import log

try:
  from numba import autojit
except ImportError:
  print "Error loading numba."
  autojit = lambda f: f

##### Aggregator ##########
@autojit
def _search(item, items):
  for i in xrange(0,len(items)):
    val = items[i]
    if (val == item): return i

  return -1

@autojit
def _count(projected, glyphset, catidx):
  width, height = projected.shape
  categories = np.unique(glyphset[:,catidx])
  outgrid=np.zeros((width, height, len(categories)), dtype=np.int32)

  for x in xrange(0, width):
    for y in xrange(0, height):
      glyphids = projected[x,y]
      if (glyphids == None) : continue
      for gidx in xrange(0, len(glyphids)):
        glyphid = glyphids[gidx]
        glyph = glyphset[glyphid]
        cat = glyph[catidx]
        catnum = _search(cat, categories) 
        outgrid[x,y,catnum] += 1
  
  return outgrid

class CountCategories(ar.Aggregator):
  #TODO: Store the catogories list somewhere
  def aggregate(self, grid):
    return _count(grid._projected, grid._glyphset, 4) ## HACK --- Hard coded offset for now....


##### Transfers ########
class ToCounts(ar.Transfer):
  """Convert from count-by-categories to just raw counts.
     Then transfer functions from the count module can be used.
  """
  @staticmethod
  def transfer(grid, dtype=np.int32):
    return np.sum(grid._aggregates, axis=2, dtype=dtype)


class MinPercent(ar.Transfer):
  """
  If the item in the specified bin represents more than a certain percent
  of the total number of items, color it as "above" otherwise, color as "below"
  
     cutoff -- percent value to split above and below coloring
     cat -- integer indicating which category number to use  
            TODO: Change from idx to category label, lookup idx
     above  -- color to paint above (default a red)
     below  -- color to paint below (default a blue)
     background -- color to paint when there are no values (default is clear)
  """
     
  def __init__(self, 
               cutoff, 
               cat=0,
               above=ar.Color(228, 26, 28,255), 
               below=ar.Color(55, 126, 184,255), 
               background=ar.Color(255,255,255,0)):

    self.cutoff = cutoff
    self.cat = cat  
    self.above = above.asarray()
    self.below = below.asarray()
    self.background = background.asarray()

  def transfer(self, grid):
    outgrid = np.empty((grid.width, grid.height, 4), dtype=np.uint8)
    
    sums = ToCounts.transfer(grid, dtype=np.float32)
    maskbg = sums == 0 
    mask = (grid._aggregates[:,:,self.cat]/sums) >= self.cutoff

    outgrid[mask] = self.above
    outgrid[~mask] = self.below
    outgrid[maskbg] = self.background
    return outgrid


class HDAlpha(ar.Transfer):
  def __init__(self, colors, 
               background=ar.Color(255,255,255,255), alphamin=0, log=False, logbase=10):
    """colors -- a list of colors in cateogry-order.
                 TODO: Change to a dictionary of category-to-color mapping
       alphamin -- minimum alpha value when (default is 0)
       log -- Log-based interpolate? (default is false)
       logbase -- Base to use if log is true (default is 10)
       background -- Color when the category list is empty (default is white)
    """
    #self.colors = dict(zip(colors.keys(), map(lambda v: v.asarray(), colors.values)))
    self.colors = map(lambda v: v.asarray(), colors)
    self.background = background.asarray()
    self.alphamin = alphamin
    self.log = log
    self.logbase = logbase

  def transfer(self, grid):
    outgrid = np.empty((grid.width, grid.height, 4), dtype=np.uint8)
    sums = ToCounts.transfer(grid, dtype=np.float32)
    maxsum = sums.max()
    
    for x in xrange(0, grid.width):
      for y in xrange(0, grid.height):
        base = opaqueblend(grid._aggregates[x,y], sums[x,y], self.colors)
        final = alpha(base, sums[x,y], maxsum, self.alphamin, self.background, self.log, self.logbase)
        outgrid[x,y] = final 
    return outgrid

    


##### Utilities #######
@autojit
def alpha(color, count, maxval, alphamin, background, dolog=False, base=10):
  if count == 0: return background
  if (dolog):
    base = log(base)
    count = log(count)/base
    maxval = log(maxval)/base
  
  alpha = alphamin + ((1-alphamin) * (count/maxval));
  color[3] = alpha*255
  return color

@autojit
def opaqueblend(counts, total, colors):
  """counts --  A run-length encoding
     total  -- toal in counts (passed in because I happen to have it handy)
     colors -- A list of [r,g,b], one for each category in the category-cannonical order 
  """
  racc = 0;
  gacc = 0;
  bacc = 0;
 
  for i in xrange(0, len(counts)):
    count = counts[i]
    color = colors[i]
    r = color[0]
    g = color[1]
    b = color[2]

    p = count/total;
    r2 = (r/255.0) * p 
    g2 = (g/255.0) * p 
    b2 = (b/255.0) * p 

    racc += r2
    gacc += g2
    bacc += b2

  return [racc*255,gacc*255,bacc*255,255]
