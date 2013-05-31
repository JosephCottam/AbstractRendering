package ar;

import java.util.ArrayList;
import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JFrame;

import ar.app.components.ARPanel;
import ar.app.util.WrappedReduction;
import ar.app.util.WrappedTransfer;
import ar.glyphsets.WrappedCollection;
import ar.renderers.*;
import ar.util.ImplicitGlyphs;

public class WrappedCollectionTest {
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		ArrayList<Integer> vs = new ArrayList<Integer>();
		
		for (int i=0; i< 1000; i++) {vs.add(i);}
		WrappedCollection<Integer,Color> gs = new WrappedCollection<Integer,Color>(vs, new ImplicitGlyphs.RainbowCheckerboard<Integer>(11, 1));
		
		ARPanel p = new ARPanel(new WrappedReduction.OverplotFirst(), 
								new WrappedTransfer.EchoColor(), 
								gs, 
								new ParallelSpatial<Number, Color>(100));
		
		frame.setLayout(new BorderLayout());
		frame.add(p, BorderLayout.CENTER);
		
		frame.setSize(500, 500);
		frame.invalidate();
		p.zoomFit();
		
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
