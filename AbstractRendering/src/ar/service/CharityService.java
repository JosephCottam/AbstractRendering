package ar.service;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import ar.Aggregates;
import ar.GlyphSet;
import ar.ParallelRenderer;
import ar.Reduction;
import ar.Renderer;
import ar.rules.Reductions;

public class CharityService {
	public static int TASK_SIZE = 40000;
	private static final GlyphSet glyphset;
	static {
		ar.app.Dataset set =new ar.app.Dataset.CharityNet();
		glyphset = set.glyphs();
	}
	public String JSONaggregates(int width, int height, int lod) {
		//LOD does 0 -- Days; 1 -- Weeks; 2 -- Months

		
		//Zoom extent-----
		Rectangle2D space = new Rectangle2D.Double(0,0, width,height);
		Rectangle2D content = glyphset.bounds();
		double w = space.getWidth()/content.getWidth();
		double h = space.getHeight()/content.getHeight();
		double scale = Math.min(w, h);
		AffineTransform view = AffineTransform.getTranslateInstance(content.getCenterX(), content.getCenterX());
		view.scale(scale, scale);
		
		AffineTransform inverseView;
		try {inverseView = view.createInverse();}
		catch (Exception e) {throw new RuntimeException(e);}
		
		//Generate aggregates (just the counts for now)
		Reduction<Integer> r = new Reductions.Count();
		Renderer renderer = new ParallelRenderer(TASK_SIZE);
		Aggregates<Integer> a = renderer.reduce(glyphset, inverseView, r, width, height);
		return ar.app.util.AggregatesToJSON.export(a);
	}
	
	public static void main(String[] args) {
		CharityService s = new CharityService();
		System.out.println(s.JSONaggregates(100,100,0));
	}
}
