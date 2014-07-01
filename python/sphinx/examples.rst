Abstract Rendering Examples
===========================

The four function roles of abstract rendering (AR) are combined into
recipies to do data analysis.  Here are some common AR recipies.


High Definition Heatmap
-----------------------

Heatmaps convert discrete events in a 2D space into a color-based representation
of the distribution of that event.  For example, population is a distribution of 
events in a geographic region (i.e., 'someone resides here' is an event and
physical space is the 2D space). A heatmap of population shows the distribution of 
people. Instead of trying to show individual people as dots, it shows the number
of people in each sub-region via colors.

Using naive alpha composition, individual pixels quickly saturate and additional values
no longer contribute additional color.  This is a silent failure of the system,
essentially mis-representing all values over a threshold as equal.  Surprisingly,
that threshold is often very low.  At 90% transparent, standard alpha composition
saturates at just 25 items.  Even at the minimum opacity (just above truely transparent),
only it is common to only get 256 items before saturation occurs.  

The abstract rendering recipie for doing HD

Perceptual Correction
---------------------

Linear color scales are easy to describe, but not perceptually accurate.
In a single-color heatmap, the color scale can adjusted to more directly
reflect perceptual effects by taking the cube-root of the aggregate values.


High Definition Alpha
---------------------

Extending the idea of heatmaps to multiple event types leads to overlapping distributions
of different types of events.  These different event types are often represented as
different colors.  Composing each color component separately is a straightfoward
change to the HD heatmap recipie.  (Perceptual correction in a multi-color environment
is not at all straightforward, conceptually nor computationally.)


ISO Contours
---------------

Taking value distribuions in another direction are ISO contours.  
ISO contours identify regions that all have *at least* a given value in them,
essentially identifying dividing lines between regions. 


