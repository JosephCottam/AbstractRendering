import core

class Id(core.Transfer):
  """Return the input unchanged.  
     This DOES NOT make a copy.  
     It is usually used a zero-cost placeholder.
  """
  def transfer(self, grid):
    return grid 

