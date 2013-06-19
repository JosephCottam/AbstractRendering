import ar
import numpy as np
from collections import OrderedDict 

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
  #Note: Generalize order by-way of argsort...which would need to be stored somewhere
  def aggregate(self, grid):
    return _count(grid._projected, grid._glyphset, 4) ## HACK --- Hard coded offset for now....


def RLE(x):
  rle = RunLengthEncode() 
  if type(x) is None:
    pass
  elif type(x) is list:
    for i in x:
      rle.add(i)
  else:
    rle.add(x)
  return rle

##### Transfers ########


def hdalpha(colors, background):
  def gen(aggs):
    (min,max) = minmax(aggs)
    def f(rle):
      if len(rle) == 0:
        c=background
      else:
        c=blend(rle,colors)
        alpha = omin + ((1-omin) * (rle.fullSize()/max));

      return Color(c.r,c.g,c.b,alpha)
    return f
  return gen

class MinPercent(ar.Transfer):
  def __init__(self, cutoff, above, below, background):
    self.cutoff = cutoff
    self.above = above.asarray()
    self.below = below.asarray()
    self.background = background.asarray()

  def transfer(self, grid):
    outgrid = np.empty((grid.width, grid.height, 4), dtype=np.uint8)
    
    sums = np.sum(grid._aggregates, axis=2, dtype=np.float32)  ## Total values in all categories
    maskbg = sums == 0 
    mask = (grid._aggregates[:,:,0]/sums) >= self.cutoff

    outgrid[mask] = self.above
    outgrid[~mask] = self.below
    outgrid[maskbg] = self.background
    return outgrid

def minPercent(cutoff, above, below, background):
  def gen(aggs):
    def f(rle):
      if len(rle) == 0:
        return background
      else:
        first = rle[rle.first()]
        percentFirst = first/(float(rle.total()))
        return above if percentFirst >= cutoff else below
    return f
  return gen




##### Utilities #######


class RunLengthEncode:
  """Like a dictionary, except the same key can occur multiple times
     and the order of the keys is preserved.  Is built by stateful 
     accumulation.
  """

  def __init__(self):
      self.counts = []
      self.keys=[]

  def add(self, key):
    if (len(self.keys)==0 or key != self.keys[len(self.keys)-1]):
        self.keys.append(key)
        self.counts.append(0)
    self.counts[len(self.counts)-1] = self.counts[len(self.counts)-1]+1

  def first(self): return self.keys[0]
  def total(self): return reduce(lambda x,y:x+y, self.counts)
  
  def __len__(self): return len(self.counts)  
  def __getitem__(self, key):
    for (k,v) in self:
      if (k == key) : return v
    return None
  
  def __iter__(self): 
    return zip(self.keys, self.counts).__iter__()

  def __str__(self):
    return str(zip(self.keys,self.counts))

def minmax(aggs):
  sizes = map(len, aggs)
  return (min(sizes), max(sizes))


def blend(rle, colors):
  """rle --  A run-length encoding
     colors -- An associative collection from the categories in the rle to colors
  """

  total = len(rle)
  r = 0;
  g = 0;
  b = 0;
  
  for (key,val) in rle:
    c = colors[key]

    a2 = rle.count(i)/total;
    r2 = (c.r/255.0) * a2;
    g2 = (c.g/255.0) * a2;
    b2 = (c.b/255.0) * a2;

    r += r2;
    g += g2;
    b += b2;

  return [r*255,g*255,b*255]
