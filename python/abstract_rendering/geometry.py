def bressenham(canvas, line, val):
  """
  based on 'optimized' version at http://en.wikipedia.org/wiki/Bresenham's_line_algorithm
  """
  (x0,y0,x1,y1) = line
  
  steep = abs(y1 - y0) > abs(x1 - x0)
  if steep:
    x0, y0 = y0, x0  
    x1, y1 = y1, x1

  if x0 > x1:
    x0, x1 = x1, x0
    y0, y1 = y1, y0

  x0,x1=(x0-x0,x1-x0) #canvas is assumed to be a tight bounding box; 
  y0,y1=(y0-y0,y1-y0) #  offset the points relative to the top-left

  deltax = x1 - x0
  deltay = abs(y1 - y0)
  error = 0
  ystep = 1 if y0 < y1 else -1
  
  y = y0
  for x in range(x0, x1): #line only includes first endpoint
    #import pdb; pdb.set_trace()
    if steep:
      if x >= canvas.shape[1]: break
      if y >= canvas.shape[0]: break
      canvas[y,x]=val
    else:
      if x >= canvas.shape[0]: break
      if y >= canvas.shape[1]: break
      canvas[x,y]=val

    error = error - deltay
    if (error < 0):
      y = y + ystep
      error = error + deltax
