package ar.renderers;

import java.util.concurrent.atomic.AtomicLong;

public class RenderUtils {
	public static boolean RECORD_PROGRESS = false;
	
	public static Progress recorder() {
		return RECORD_PROGRESS ? new RenderUtils.Progress.Counter() : new RenderUtils.Progress.NOP();
	}

	
	public static interface Progress {
		public long count();
		public void update(long delta);
		public double percent();
		public void reset(long expected);
		
		
		public static final class NOP implements Progress {
			public long count() {return -1;}
			public void update(long delta) {}
			public void reset(long expected) {}
			public double percent() {return -1;}
		}
		
		public static final class Counter implements Progress {
			private final AtomicLong counter = new AtomicLong();
			private long expected=1;
			public long count() {return counter.get();}
			public void update(long delta) {counter.addAndGet(delta);}
			public void reset(long expected) {this.expected = expected; counter.set(0);}
			public double percent() {
				if (expected <=0) {return -1;}
				return counter.intValue()/((double) expected);
			} 
		}
	}
}
