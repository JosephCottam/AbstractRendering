import ar
import numpy as np

######## Aggregators ########
class Count(ar.Aggregator):
  out_type=np.int32

  def aggregate(self, grid): 
    def myLen(o):
      if o == None:
        return 0
      return len(o)

    f = np.vectorize(myLen)
    return f(grid._projected)


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

    colorspan = self.high.asarray().astype(np.int32) - self.low.asarray().astype(np.int32)
    return (percents[:,:,np.newaxis] * colorspan[np.newaxis,np.newaxis,:] + self.low.asarray()).astype(np.uint8)




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
