
"""Each info function returns a function that takes two arguments:
     shape and data.  The 'shape' is the projected shape information.
     The 'data' is the data package associated with that information.
"""


def const(v):
  """Return the value passed.""" 
  def f(glyph, data):
    return v
  return f

def val(default=None):
  """Return the entire data value.  This is the info-version of 'id'.
  """
  def f(glyph, data):
    if (data is None) :
      return default
    else:
      return data
  return f 

def valAt(i, default=None):
  """Return the value at a given index in the data part of the input.
     On error returns the default value.
  """
  def f(glyph, data):
    try :
      return data[i] 
    except:
      return default
  return f 

def attribute(att, default=None):
  """"Return the value under a given attribute in the data part of hte input."""
  def f(glyph, data):
    try :
      return getattr(data, att)
    except:
      return default
  return f 
