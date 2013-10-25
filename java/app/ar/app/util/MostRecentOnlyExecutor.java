package ar.app.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ar.util.NamedThreadFactory;

public class MostRecentOnlyExecutor extends ThreadPoolExecutor {
	public MostRecentOnlyExecutor(int corePoolSize, String threadNameBase) {
		super(corePoolSize, corePoolSize, 
				1, TimeUnit.SECONDS, 
				new ArrayBlockingQueue<Runnable>(1), 
				new NamedThreadFactory(threadNameBase, true),
				new ThreadPoolExecutor.DiscardOldestPolicy());
	}
}
