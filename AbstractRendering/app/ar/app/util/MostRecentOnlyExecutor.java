package ar.app.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MostRecentOnlyExecutor extends ThreadPoolExecutor {
	public MostRecentOnlyExecutor(int corePoolSize, String threadNameBase) {
		super(corePoolSize, corePoolSize, 
				1, TimeUnit.SECONDS, 
				new ArrayBlockingQueue<Runnable>(1), 
				new DaemonFactory(threadNameBase),
				new ThreadPoolExecutor.DiscardOldestPolicy());
	}
	
	public static final class DaemonFactory implements ThreadFactory {
		private final String baseName;
		private final AtomicInteger counter = new AtomicInteger(0);
		private final ThreadGroup group;
		
		public DaemonFactory() {this("Worker Thread");}
		public DaemonFactory(String baseName) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
			this.baseName = baseName;
		}
		public Thread newThread(Runnable r) {
			String name =  String.format("%s (%d)", baseName, counter.getAndIncrement());
			Thread t = new Thread(group, r, name, 0);
			t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
		
	}
}
