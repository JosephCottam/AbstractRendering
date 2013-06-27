package ar;

import java.util.ArrayList;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;

import ar.Glyphset.Glyph;
import ar.app.components.ARPanel;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import ar.glyphsets.SimpleGlyph;
import ar.glyphsets.WrappedCollection;
import ar.glyphsets.implicitgeometry.Glypher;
import ar.renderers.*;

public class WrappedCollectionTest {

	/**Demo geometry creator.**/
	public static final class RainbowCheckerboard<T> implements Glypher<Integer, Color> {
		private static final Color[] COLORS = new Color[]{Color.RED, Color.BLUE, Color.GREEN,Color.PINK,Color.ORANGE};
		private final int columns;
		private final double size;
		
		public RainbowCheckerboard(int columns, double size) {
			this.columns = columns;
			this.size = size;
		}

		public Shape shape(Integer from) {
			from = from*2;
			int row = from/columns;
			int col = from%columns;
			
			if (row%2==0) {col=col-1;}
			
			return new Rectangle2D.Double(col*size, row*size, size,size);
		}
		
		public Color value(Integer from) {
			return COLORS[from%COLORS.length];
		}

		@Override
		public Glyph<Color> glyph(Integer from) {
			return new SimpleGlyph<>(this.shape(from), this.value(from));
		}
	}
	
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		ArrayList<Integer> vs = new ArrayList<Integer>();
		
		for (int i=0; i< 1000; i++) {vs.add(i);}
		Glypher<Integer,Color> g = new RainbowCheckerboard<Integer>(11, 1);
		WrappedCollection<Integer,Color> gs = new WrappedCollection<>(vs, g,g, Color.class);
		
		ARPanel p = new ARPanel(new WrappedAggregator.OverplotFirst().op(), 
								new WrappedTransfer.EchoColor().op(), 
								gs, 
								new ParallelSpatial(100));
		
		frame.setLayout(new BorderLayout());
		frame.add(p, BorderLayout.CENTER);
		
		frame.setSize(500, 500);
		frame.invalidate();
		p.zoomFit();
		
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
