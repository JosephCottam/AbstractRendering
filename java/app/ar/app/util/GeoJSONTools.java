package ar.app.util;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Geometry;
import org.geojson.LngLatAlt;
import org.geojson.MultiPolygon;
import org.geojson.Polygon;

import com.fasterxml.jackson.databind.ObjectMapper;

/**Utilities for loading GeoJSON files and converting to java shapes.**/
public class GeoJSONTools {
	
	/**Load all shapes out of the files in a given directory.
	 * 
	 * Only loads .json files.
	 * **/
	public static Map<String,Shape> loadShapesJSON(File source, boolean recursive) throws Exception {
		if (source.isFile()) {
			return Collections.singletonMap(source.getName(), loadShapeJSON(source));
		}
		Map<String, Shape> shapes = new HashMap<>();
		for (File f: source.listFiles()) {
			if (recursive && f.isDirectory()) {shapes.putAll(loadShapesJSON(f, true));}
			if (!f.getName().endsWith(".json")) {continue;}
			shapes.put(f.getName(), loadShapeJSON(f));
		}
		return shapes;
	}
	
	/**Load shape from the given file. 
	 * Contents of the file are assumed to be GeoJSON.**/
	public static Shape loadShapeJSON(File source) {
		try (FileInputStream fs = new FileInputStream(source)){
			FeatureCollection fc = new ObjectMapper().readValue(fs, FeatureCollection.class);
			Feature feature = fc.getFeatures().get(0);
			@SuppressWarnings("rawtypes")

			Geometry geometry = (Geometry) feature.getGeometry();
			if (geometry instanceof MultiPolygon) {
				return toArea((MultiPolygon) geometry);
			} else {
				return toArea((Polygon) geometry);
			}
		} catch (Exception e) {throw new RuntimeException("Error loading " + source.getName(), e);}
	}
	

	/**Convert a multi-polygon to a java.awt.geom.shape.**/
	protected static Shape toArea(MultiPolygon source) {
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

	/**Convert a polygon to a java.awt.geom.shape.**/
	protected static Area toArea(Polygon source) {
		Shape outer = toShape(source.getExteriorRing());
		Area a = new Area(outer);
		for (List<LngLatAlt> pts: source.getInteriorRings()) {
			Shape inner =toShape(pts);
			a.subtract(new Area(inner));
		}
		return a;
	}

	
	protected static Shape toShape(List<LngLatAlt> points) {
		Path2D.Double pg = new Path2D.Double();
		pg.moveTo(points.get(0).getLongitude(), points.get(0).getLatitude());
		for (int i=1; i<points.size();i++) {
			LngLatAlt pt = points.get(i);
			pg.lineTo(pt.getLongitude(), pt.getLatitude());			
		}
		pg.closePath();
		return pg;
	}
	

	/**Change the sign on all y-coordinates.  
	 * Useful for converting to/from scan-line y-convention (where positive values increase down).**/
	public static Map<String, Shape> flipY(Map<String, Shape> input) {
		AffineTransform flip = AffineTransform.getScaleInstance(1, -1);
		Map<String, Shape> output = new HashMap<>();
		for (Map.Entry<String, Shape> e: input.entrySet()) {
			output.put(e.getKey(), flip.createTransformedShape(e.getValue()));
		}
		return output;
		
	}
}
