
def const(v):
  def f(glyph):
    return v
  return f

def valAt(i, default=None):
  def f(glyph):
    try :
      return glyph[i] 
    except:
      return default
  return f 

def attribute(att, default=None):
  def f(glyph):
    try :
      return getattr(glyph, att)
    except:
      return default
  return f 
