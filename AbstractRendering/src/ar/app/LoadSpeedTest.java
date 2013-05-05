package ar.app;

import java.lang.reflect.Constructor;

public class LoadSpeedTest {
	public static void main(String[] args) throws Exception {
		int iterations = args.length >0 ? Integer.parseInt(args[0]) : 10;

		Class<?>[] classes = Dataset.class.getClasses();
		
		for (Class<?> cls: classes) {
			Constructor<?> c = cls.getConstructor(); 
			String name = cls.getSimpleName();
			long total = 0;
			for (int i=0; i<iterations; i++) {
				long start = System.currentTimeMillis();
				c.newInstance();
				long end = System.currentTimeMillis();
				System.out.printf("%s, %d, %d\n", name, end-start, i);
				total += (end-start);
			}
			System.out.printf("\t\t%s (avg), %s\n",name, total/((double) iterations));
		}		
	}
}
