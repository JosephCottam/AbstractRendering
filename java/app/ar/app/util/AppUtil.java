package ar.app.util;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComboBox;

public class AppUtil {

	@SuppressWarnings("unchecked") //Several inherently not type-safe operation...
	public static <A,B> void loadInstances(JComboBox<B> target, Class<A> source, Class<?> limit, String defaultItem) {
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
		
		for (int i=0; i<target.getItemCount(); i++) {
			B item = target.getItemAt(i);
			if (item.toString().equals(defaultItem)) {target.setSelectedIndex(i); break;}
		}		
	}
}
