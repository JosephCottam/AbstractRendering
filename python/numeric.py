import ar
import numpy as np
import math

try:
  from numba import autojit
except ImportError:
  print "Error loading numba."
  autojit = lambda f: f


######## Aggregators ########
class Count(ar.Aggregator):
  """Count the number of items that fall into a particular grid element."""
  out_type=np.int32
  identity=0

  def allocate(self, width, height, glyphset, infos):
    return np.zeros((width, height), dtype=self.out_type)

  def combine(self, existing, points, shapecode, val):
    update = ar.glyphAggregates(points, shapecode, 1, self.identiity)  
    existing[points[0]:points[2],points[1]:points[3]] += update

  def rollup(*vals):
    return reduce(lambda x,y: x+y,  vals)



class Sum(ar.Aggregator):
  """Count the number of items that fall into a particular grid element."""
  out_type=np.int32
  identity=0

  def allocate(self, width, height, glyphset, infos):
    return np.zeros((width, height), dtype=self.out_type)

  def combine(self, existing, points, shapecode, val):
    update = ar.glyphAggregates(points, shapecode, val, self.identity)  
    existing[points[0]:points[2],points[1]:points[3]] += update

  def rollup(*vals):
    return reduce(lambda x,y: x+y,  vals)


######## Transfers ##########
class FlattenCategories(ar.Transfer):
  """Convert a set of category-counts into just a set of counts"""
  out_type=(1,np.int32)
  in_type=("A",np.int32) #A is for "any", all cells must be the same size, but the exact size doesn't matter

  def transfer(self, grid):
    return grid._aggregates.sum(axis=1)


class AbsSegment(ar.Transfer):
  """
  Paint all pixels with aggregate value above divider one color 
  and below the divider another.
  """
  in_type=(1,np.number)
  out_type=(4,np.int32)

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



class Interpolate(ar.Transfer):
  """
  High-definition interpolation between two colors.
  Zero-values are treated separately from other values.
 
  low -- Color ot use for lowest value
  high -- Color to use for highest values 
  log -- Set to desired log base to use log-based interpolation 
         (use True or "e" for base-e; default is False)
  reserve -- color to use for empty cells
  """
  in_type=(1,np.number)
  out_type=(4,np.int32)

  def __init__(self, low, high, log=False, reserve=ar.Color(255,255,255,255), empty=np.nan):
    self.low=low
    self.high=high
    self.reserve=reserve
    self.log=log
    self.empty=empty

  
  ##TODO: there are issues with zeros here....
  def _log(self,  grid):
    items = grid._aggregates
    mask = (grid._aggregates == self.empty)
    min = items[~mask].min()
    max = items[~mask].max()
    
    items[mask] = 1
    if (self.log==10):
      min = math.log10(min)
      max = math.log10(max)
      span = float(max-min)
      percents = (np.log10(items)-min)/span
    elif (self.log == math.e or self.log == True):
      min = math.log(min)
      max = math.log(max)
      span = float(max-min)
      percents = (np.log(items)-min)/span
    elif (self.log==2):
      min = math.log(min, self.log)
      max = math.log(max, self.log)
      span = float(max-min)
      percents = (np.log2(items)-min)/span
    else:
      rebase = math.log(self.log)
      min = math.log(min, self.log)
      max = math.log(max, self.log)
      span = float(max-min)
      percents = ((np.log(items)/rebase)-min)/span
    
    items[mask] = 0
    
    colorspan = self.high.asarray().astype(np.int32) - self.low.asarray().astype(np.int32)

    outgrid = (percents[:,:,np.newaxis] * colorspan[np.newaxis,np.newaxis,:] + self.low.asarray()).astype(np.uint8)
    outgrid[mask] = self.reserve
                 
    return outgrid


  def _linear(self, grid):
    items = grid._aggregates
    mask = (grid._aggregates == self.empty)
    min = items[~mask].min()
    max = items[~mask].max()
    span = float(max-min) 
    percents = (items-min)/span
    
    colorspan = self.high.asarray().astype(np.int32) - self.low.asarray().astype(np.int32)
    outgrid = (percents[:,:,np.newaxis] * colorspan[np.newaxis,np.newaxis,:] + self.low.asarray()).astype(np.uint8)
    outgrid[mask] = self.reserve
    return outgrid

    
  def transfer(self, grid):
    if (self.log):
      return self._log(grid)
    else :
      return self._linear(grid)

    
