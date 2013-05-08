package ar.service;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import ar.Aggregates;
import ar.GlyphSet;
import ar.Aggregator;
import ar.Renderer;
import ar.renderers.ParallelSpatial;
import ar.rules.Aggregators;
import ar.app.util.CharityNetLoader;

public class CharityService {
	public static int TASK_SIZE = 40000;
	private static final GlyphSet glyphset;
	static {
		glyphset = CharityNetLoader.load("./data/dateStateXY.csv");
	}

	
	public String JSONaggregates(int width, int height, int lod) {
		//LOD does 0 -- Days; 1 -- Weeks; 2 -- Months

		
		//Zoom extent-----
		Rectangle2D content = glyphset.bounds();
		Rectangle2D image = new Rectangle2D.Double(0,0, width,height);
		double w = image.getWidth()/(content.getWidth());
		double h = image.getHeight()/(content.getHeight());
		double scale = Math.min(w, h);
		
		AffineTransform view = new AffineTransform();
		view.translate(content.getCenterX(), content.getCenterY());
		view.scale(scale, scale);
		view.translate(-content.getCenterX(), -content.getCenterY());


		double x = image.getWidth()/(2*scale)- content.getCenterX();
		double y = image.getHeight()/(2*scale)- content.getCenterY();
		double dx = x-(view.getTranslateX()/scale);
		double dy = y-(view.getTranslateY()/scale);		
		view.translate(dx, dy);
		
//		System.out.println("###################");
//		System.out.println("Bounds:" + content);
//		System.out.println("Viewport:" + image.getWidth() + " x " + image.getHeight());
//		System.out.println("Scale:" + view.getScaleX());
//		System.out.println("Translate: " + view.getTranslateX() + " x " + view.getTranslateY());
//		System.out.println("###################");

		AffineTransform inverseView;
		try {inverseView = view.createInverse();}
		catch (Exception e) {throw new RuntimeException(e);}
		
		//Generate aggregates (just the counts for now)
		Aggregator<Integer> r = new Aggregators.Count();
		Renderer renderer = new ParallelSpatial(TASK_SIZE);
		Aggregates<Integer> a = renderer.reduce(glyphset, inverseView, r, width, height);
		return ar.app.util.AggregatesToJSON.export(a);
	}
	
	public static void main(String[] args) {
		CharityService s = new CharityService();
		String aggregates = s.JSONaggregates(500,500,0);
		System.out.println(aggregates);
	}
}
