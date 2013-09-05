package ar.app.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MostRecentOnlyExecutor extends ThreadPoolExecutor {
	public MostRecentOnlyExecutor(int corePoolSize) {
		super(corePoolSize, corePoolSize, 
				1, TimeUnit.SECONDS, 
				new ArrayBlockingQueue<Runnable>(1), 
				Executors.defaultThreadFactory(), 
				new ThreadPoolExecutor.DiscardOldestPolicy());
	}
	
	public static final class DaemonFactory implements ThreadFactory {
		private final String baseName;
		private final AtomicInteger counter = new AtomicInteger(0);
		public DaemonFactory() {baseName = "Worker Thread";}
		public DaemonFactory(String baseName) {this.baseName = baseName;}
		public Thread newThread(Runnable r) {
			String name =  String.format("%s (%d)", baseName, counter.getAndIncrement());
			Thread t = new Thread(name);
			t.setDaemon(true);
			return t;
		}
		
	}
}
