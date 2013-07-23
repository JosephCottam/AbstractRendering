try:
  from numba import autojit
except ImportError:
  print "Error loading numba."
  autojit = lambda f: f

######## Info ###########
def const(x):
  def val(): return x
  return val


############ Aggregation Strategies ############
@autojit
def horizontalCombine(target, new, agg):
  xoff = new.xoffset
  yoff = new.yoffset
  vals = new.array

  ##TODO:vectorize, some sort of sub-set-offset-add-in-place in numpy
  for x in xrange(xoff, xoff+new.width):
    for y in xrange(yoff, yoff+new.height):
      v = vals[x-xoff,y-yoff] 
      nv = agg.combine(x,y,target[x,y],v)
      target[x,y] = nv
