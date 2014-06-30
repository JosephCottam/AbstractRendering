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
import abstract_rendering.core as core
import abstract_rendering.numeric as numeric
import abstract_rendering.categories as categories
import abstract_rendering.infos as infos
import abstract_rendering.glyphset as glyphset
#import core
#import numeric
#import categories
#import infos
#import glyphset

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
    red = core.Color(255,0,0,255)
    green = core.Color(0,255,0,255)
    blue = core.Color(0,0,255,255)
    white = core.Color(255,255,255,255)
    black = core.Color(0,0,0,255)
    
    #glyphs = core.load_csv("../data/checkerboard.csv", 2, 0, 1, 3,1,1)
    glyphs = core.load_csv("../data/circlepoints.csv", 1, 2, 3, 4,.1,.1)
    #glyphs = core.load_csv("../data/sourceforge.csv", 1, 1, 2, -1,.1,.1)
    
    #glyphs.shaper.code = glyphset.ShapeCodes.LINE
    #glyphs.shaper.code = glyphset.ShapeCodes.RECT
    glyphs.shaper.code = glyphset.ShapeCodes.POINT

    screen = (800,800)
    ivt = core.zoom_fit(screen,glyphs.bounds())

    with Timer("Abstract-Render") as arTimer:   
      image = core.render(glyphs, 
                        infos.val(),
                        categories.CountCategories(), 
                        categories.HDAlpha([red, blue]),
                        screen,
                        ivt)
#      image = core.render(glyphs, 
#                        infos.valAt(4,0),
#                        numeric.Sum(), 
#                        numeric.Interpolate(blue,red, empty=0),
#                        screen,
#                        ivt)

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
