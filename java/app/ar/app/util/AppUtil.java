package ar.app.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import javax.swing.JComboBox;

public class AppUtil {

	@SuppressWarnings("unchecked") //Several inherently not type-safe operation...
	public static <B> void loadInstances(JComboBox<B> target, Class<?> source, Class<?> limit, Object defaultItem) {
		Class<?>[] clss = source.getClasses();
		for (Class<?> cls:clss) {
			try {
				if (!limit.isAssignableFrom(cls)) {continue;}
				if (cls.isInterface()) {continue;}

				try {
					B i = (B) cls.getConstructor().newInstance();
					target.addItem(i);
				} catch (NoSuchMethodException e) {continue;}
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					| SecurityException e) {
				System.err.println("Error intializing GUI:" + cls.getName());
				e.printStackTrace();
			}
		}
		
		
		if (defaultItem != null) {
			for (int i=0; i<target.getItemCount(); i++) {
				B item = target.getItemAt(i);
				if (item.toString().equals(defaultItem)) {target.setSelectedIndex(i); break;}
			}		
		}
	}

	public static <A,B> void loadStaticItems(JComboBox<B> target, Class<A> source, Class<?> limit, String defaultItem) {
		loadStaticItems(target, source, limit, defaultItem, a -> a);
	}

	@SuppressWarnings("unchecked")
	/** 
	 * @param target Combo box to populate
	 * @param source Class to load fields from
	 * @param limit Only load fields of this type
	 * @param defaultItem Name of the default item
	 * @param modify Before the item is loaded, it will be passed to this function for any patch-up work (default is identity)
	 */
	public static <A,B> void loadStaticItems(JComboBox<B> target, Class<A> source, Class<?> limit, String defaultItem, Function<B,B> modify) {
		Field[] fields = source.getFields();
		for (Field f: fields) {
			try {
				if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {continue;}
				Object v = f.get(null);
				if (v ==null) {continue;}
				if (limit.isInstance(v)) {
					target.addItem(modify.apply((B) v));
				}
			} catch (IllegalAccessException | IllegalArgumentException
					| SecurityException e) {
				System.err.println("Error intializing GUI:" + f.getName());
				e.printStackTrace();
			}
		}
		
		for (int i=0; i<target.getItemCount(); i++) {
			B item = target.getItemAt(i);
			if (item.toString().equals(defaultItem)) {target.setSelectedIndex(i); break;}
		}		
	}
}
