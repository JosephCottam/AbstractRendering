import ar
import numpy as np

######## Aggregators ########
class Count(ar.Aggregator):
  def aggregate(self, glyphset, indicies): 
    if (indicies == None):
      return 0
    return len(indicies)


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

  def f_vec(self, items, out):
    keys = (items >= self.divider) 
    out[keys] = self.high
    out[~keys] = self.low
    return out

  def transfer(self, grid):
    outgrid = self.makegrid(grid)
    self.f_vec(grid._projected, outgrid) 
    return outgrid



class HDInterpolate(ar.Transfer):
  """High-definition interpolation between two colors."""
  def __init__(self, low, high, reserve):
    self.low=low
    self.high=high
    self.reserve=reserve

  def transfer(self, grid):
    items = grid._aggregates
    min = items.min()
    max = items.max()
    span = float(max-min) 
    percents = (items-min)/span
    outgrid = self.makegrid(grid)

    outgrid[:][:][0] = (self.high.r - self.low.r) * percents + self.low.r
    outgrid[:][:][1] = (self.high.g - self.low.g) * percents + self.low.g
    outgrid[:][:][2] = (self.high.b - self.low.b) * percents + self.low.b
    outgrid[:][:][3] = (self.high.a - self.low.a) * percents + self.low.a

    #colors=map(lambda p: interpolateColors(p, self.low, self.high).asarray(), percents.flat) 
    return outgrid



###### Other utilities ########

def minmax(aggs):
  return (min(aggs.values), max(aggs.values))

def interpolateColors(low, high, min,  max, v):
  """low--Color for the lowest position
     high-- Color for the highest position
     min -- Smallest value v will take
     max -- largest value v will take
     v -- current value
  """

  if (v>max): v=max
  if (v<min): v=min
  distance = 1-((max-v)/float(max-min));

def interpolateColors(percent, low, high):
  if (percent<0): percent=0
  if (percent>1): percent=1
 
  r = int(weightedAverage(high.r, low.r, percent))
  g = int(weightedAverage(high.g, low.g, percent))
  b = int(weightedAverage(high.b, low.b, percent))
  a = int(weightedAverage(high.a, low.a, percent))
  return ar.Color(r,g,b,a);


#TODO: Look at the inMens perceptually-weighted average
def weightedAverage(v1, v2, weight): return (v1 -v2) * weight + v2
