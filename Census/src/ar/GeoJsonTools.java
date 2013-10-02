package ar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Geometry;
import org.geojson.LngLatAlt;
import org.geojson.MultiPolygon;
import org.geojson.Polygon;

import ar.app.display.SimpleDisplay;
import ar.util.Util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GeoJsonTools {
	
	public static List<Shape> loadShapesJSON(File source) throws Exception {
		if (source.isFile()) {return Arrays.asList(loadShapeJSON(source));}
		List shapes = new ArrayList();
		for (File f: source.listFiles()) {
			shapes.add(loadShapeJSON(f));
		}
		return shapes;
	}
	
	public static Shape loadShapeJSON(File source) {
		try {
			FeatureCollection obj = 
					new ObjectMapper().readValue(new FileInputStream(source), FeatureCollection.class);
			FeatureCollection fc = (FeatureCollection) obj;
			Feature feature = fc.getFeatures().get(0);
			Geometry geometry = (Geometry) feature.getGeometry();
			if (geometry instanceof MultiPolygon) {
				return toArea((MultiPolygon) geometry);
			} else {
				return toArea((Polygon) geometry);
			}
		} catch (Exception e) {throw new RuntimeException("Error loading " + source.getName(), e);}
	}
	

	public static Shape toArea(MultiPolygon source) {
		Area a = new Area();
		for (List<List<LngLatAlt>> polyPoints: source.getCoordinates()) {
			Polygon part = new Polygon(polyPoints.get(0));
			for (int i=1; i<polyPoints.size();i++) {
				part.addInteriorRing(polyPoints.get(i));
			}
			a.add(toArea(part));
		}
		return a;
	}

	public static Area toArea(Polygon source) {
		Shape outer = toShape(source.getExteriorRing());
		Area a = new Area(outer);
		for (List<LngLatAlt> pts: source.getInteriorRings()) {
			Shape inner =toShape(pts);
			a.subtract(new Area(inner));
		}
		return a;
	}

	
	public static Shape toShape(List<LngLatAlt> points) {
		Path2D.Double pg = new Path2D.Double();
		pg.moveTo(points.get(0).getLongitude(), points.get(0).getLatitude());
		for (int i=1; i<points.size();i++) {
			LngLatAlt pt = points.get(i);
			pg.lineTo(pt.getLongitude(), pt.getLatitude());			
		}
		pg.closePath();
		return pg;
	}
	
	
	public static AffineTransform yPositiveUpTransform(AffineTransform vt) {
		AffineTransform t = (AffineTransform) vt.clone();
		t.scale(1,-1);
		return t;
	}
	
	public static List<Shape> flipY(List<Shape> input) {
		AffineTransform flip = AffineTransform.getScaleInstance(1, -1);
		List<Shape> output = new ArrayList<>(input.size());
		for (Shape s: input) {
			output.add(flip.createTransformedShape(s));
		}
		return output;
		
	}
	
	private static final class ShowPanel extends JPanel {
		List<Shape> shapes;
		AffineTransform vt;
		
		public ShowPanel(List<Shape> shapes, AffineTransform vt) {
			this.shapes = shapes;			
			this.vt = vt;
		}
		
		public void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();

			if (vt == null) {
				Rectangle2D bounds = null;
				for (int i=0; i<shapes.size(); i++) {
					bounds = Util.bounds(bounds, shapes.get(i).getBounds2D());
				}
				vt = Util.zoomFit(bounds, this.getWidth(), this.getHeight());
			}
			
			g2.setTransform(vt);
			g2.setPaint(Color.gray);
			for (Shape s: shapes) {g2.fill(s);}			
		}
	}
	public static void showAll(List<Shape> shapes, int width, int height, AffineTransform vt) {
		JFrame frame = new JFrame("Test Shape loading");
		frame.setLayout(new BorderLayout());
		frame.setSize(width,height);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.add(new ShowPanel(shapes, null), BorderLayout.CENTER);
		frame.setVisible(true);
		frame.revalidate();
		frame.validate();


	}
	

}
