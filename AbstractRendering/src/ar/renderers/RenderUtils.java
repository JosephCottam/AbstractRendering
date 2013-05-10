package ar.renderers;

import java.util.concurrent.atomic.AtomicInteger;

public class RenderUtils {
	public static boolean RECORD_PROGRESS = false;
	
	public static Progress recorder() {
		return RECORD_PROGRESS ? new RenderUtils.Progress.Counter() : new RenderUtils.Progress.NOP();
	}

	
	public static interface Progress {
		public int count();
		public void update(int delta);
		public double percent();
		public void reset(int expected);
		
		
		public static final class NOP implements Progress {
			public int count() {return -1;}
			public void update(int delta) {}
			public void reset(int expected) {}
			public double percent() {return -1;}
		}
		
		public static final class Counter implements Progress {
			private final AtomicInteger counter = new AtomicInteger();
			private int expected=1;
			public int count() {return counter.get();}
			public void update(int delta) {counter.addAndGet(delta);}
			public void reset(int expected) {this.expected = expected; counter.set(0);}
			public double percent() {
				if (expected <=0) {return -1;}
				return counter.intValue()/((double) expected);
			} 
		}
	}
}
