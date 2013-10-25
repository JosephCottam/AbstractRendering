package ar.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**Create threads with a given name-prefix.  Can also create daemon threads.
 */
public final class NamedThreadFactory implements ThreadFactory {
	private final String baseName;
	private final AtomicInteger counter = new AtomicInteger(0);
	private final ThreadGroup group;
	private final boolean daemon;
	
	public NamedThreadFactory() {this("Worker Thread");}
	public NamedThreadFactory(boolean daemon) {this("Worker Thread", daemon);}
	public NamedThreadFactory(String baseName) {this(baseName, false);}
	public NamedThreadFactory(String baseName, boolean daemon) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
            Thread.currentThread().getThreadGroup();
		this.daemon = daemon;
		this.baseName = baseName;
	}
	
	public Thread newThread(Runnable r) {
		String name =  String.format("%s (%d)", baseName, counter.getAndIncrement());
		Thread t = new Thread(group, r, name);
		t.setDaemon(daemon);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}
}