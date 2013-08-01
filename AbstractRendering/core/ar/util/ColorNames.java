package ar.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.awt.Color;
import java.util.Map;
import java.util.HashMap;

/**Tools for getting color entries from color names.**/
public abstract class ColorNames {
	private static Map<String, Color> NAMES = new HashMap<String, Color>();
	static {
		for (Field f: Color.class.getFields()) {
			if (f.getType().equals(Color.class) && Modifier.isStatic(f.getModifiers())) {
				try {NAMES.put(f.getName().toUpperCase(), (Color) f.get(null));}
				catch (Exception e) {
					System.out.println("Error attempting to load color " + f.getName());
				}
			}
		}
	}
	
	/**
	 * @param name Name of a color
	 * @param onError Color to return if the name isn't really a color
	 * @return Color named, or onError 
	 */
	public static Color byName(String name, Color onError) {
		name = name.toUpperCase();
		if (NAMES.containsKey(name)) {return NAMES.get(name);}
		else {return onError;}
	}

}
