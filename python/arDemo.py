#!/usr/bin/env python
"""
Draws a colormapped image plot
 - Left-drag pans the plot.
 - Mousewheel up and down zooms the plot in and out.
 - Pressing "z" brings up the Zoom Box, and you can click-drag a rectangular
   region to zoom.  If you use a sequence of zoom boxes, pressing alt-left-arrow
   and alt-right-arrow moves you forwards and backwards through the "zoom
   history".
"""
# Abstract rendering imports
import ar
import numeric 
import categories 
import infos

from timer import Timer

# Enthought library imports
from enable.api import Component, ComponentEditor
from traits.api import HasTraits, Instance
from traitsui.api import Item, Group, View

# Chaco imports
from chaco.api import ArrayPlotData, Plot

#===============================================================================
# # Create the Chaco plot.
#===============================================================================
def _create_plot_component():
    red = ar.Color(255,0,0,255)
    green = ar.Color(0,255,0,255)
    blue = ar.Color(0,0,255,255)
    white = ar.Color(255,255,255,255)
    black = ar.Color(0,0,0,255)
    
    #glyphs = ar.load_csv("../data/checkerboard.csv", 2, 0, 1, 3,1,1)
    glyphs = ar.load_csv("../data/circlepoints.csv", 1, 2, 3, 4,.1,.1)
    #glyphs = ar.load_csv("../data/sourceforge.csv", 1, 1, 2, -1,.1,.1)
    
    screen = (800,800)
    ivt = ar.zoom_fit(screen,ar.bounds(glyphs))

    with Timer("Abstract-Render") as arTimer:   
#      image = ar.render(glyphs, 
#                        categories.CountCategories(), 
#                        categories.HDAlpha([red, blue]),
#                        screen,
#                        ivt)
      image = ar.render(glyphs, 
                        numeric.Count(), 
                        numeric.Interpolate(blue,red, empty=0),
                        screen,
                        ivt)

    # Create a plot data object and give it this data
    pd = ArrayPlotData()
    pd.set_data("imagedata", image)

    # Create the plot
    plot = Plot(pd)
    img_plot = plot.img_plot("imagedata")[0]

    # Tweak some of the plot properties
    plot.title = "Abstract Rendering"
    plot.padding = 50
    
    return plot


#===============================================================================
# Attributes to use for the plot view.
size=(800,600)
title="Basic Colormapped Image Plot"

#===============================================================================
# # Demo class that is used by the demo.py application.
#===============================================================================
class Demo(HasTraits):
    plot = Instance(Component)

    traits_view = View(
                    Group(
                        Item('plot', editor=ComponentEditor(size=size),
                             show_label=False),
                        orientation = "vertical"),
                    resizable=True, title=title
                    )
    def _plot_default(self):
         return _create_plot_component()

demo = Demo()

if __name__ == "__main__":
    demo.configure_traits()
