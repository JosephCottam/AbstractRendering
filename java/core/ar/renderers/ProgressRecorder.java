package ar.renderers;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicLong;

/**Utility class for recording percent progress through a task.
 * 
 * Producer Side:
 * If the task size is known, then reset should be called with
 * the expected number of steps and update should be called
 * with a total number equaling the expected.
 * 
 * If the task size is not known, then reset should be called with zero as the expected size
 * at the start.  When done, reset should be called again with a negative value.
 * 
 * Consumer side:
 * Periodically call percent to find out the percentage of work complete.
 * If the task size was known, these will return appropriate values.
 * If the task size is not known, then percent will return NaN.
 * Return of a negative number indicates tracking is not in progress.
 * **/
public interface ProgressRecorder {
	/**Default reporting step.  Used as a default at construction time for classes defined in this interface.**/
	public static final long DEFAULT_REPORT_STEP = 1_000_000;
	
	/**Indicate a certain number of expected steps have been taken.**/
	public void update(long delta);
	
	/**Percentage of expected steps that have been seen.**/
	public double percent();
	
	/**Set how many steps are expected; also clears the count.**/
	public void reset(long expected);
	
	/**How much time has elapsed since the last reset (in ms)?
	 * If reset has not been called, returns -1.
	 * **/
	public long elapse();
	
	/**At a best-effort, how often should reports be made?
	 * If set to zero or less, then any unit will do.
	 * **/
	public long reportStep();
	
	/**Get a status message.  
	 * null is preferred for signaling "no message".**/
	public String message();
	public void message(String message);
	
	
	
	
	/**Dummy progress recorder.  Always returns -1 for status inquiries.**/
	public static final class NOP implements ProgressRecorder {
		public NOP() {}

		@Override public void update(long delta) {}
		@Override public void reset(long expected) {}
		@Override public long elapse() {return -1l;}
		@Override public double percent() {return -1;}
		@Override public long reportStep() {return -1;}
		@Override public String message() {return null;}
		@Override public void message(String message) {}		
	}
	
	/**Thread-safe progress reporter for.**/
	public static final class Counter implements ProgressRecorder {
		private final AtomicLong counter = new AtomicLong();
		private long expected=1;
		private long start = -1;
		private final long reportStep;
		private String message;
		
		public Counter() {this(DEFAULT_REPORT_STEP);}
		public Counter(long reportStep) {this.reportStep = reportStep;}

		@Override public void update(long delta) {counter.addAndGet(delta);}
		@Override public void reset(long expected) {
			this.expected = expected; 
			counter.set(0); 
			message = null;
			start = System.currentTimeMillis();
		}
		
		@Override public long elapse() {return start > 0 ? System.currentTimeMillis()-start : -1;}
		@Override public long reportStep() {return reportStep;}
		@Override public String message() {return message;}
		@Override public void message(String message) {this.message = message;}
		
		public double percent() {
			if (expected ==0) {return Double.NaN;}
			if (expected <0) {return -1;}
			return counter.intValue()/((double) expected);
		}
	}
	
	/**Wrap another reporter and show reports on an output stream (defaults to standard out).**/
	public static final class Stream implements ProgressRecorder {
		private final ProgressRecorder inner;
		private final PrintStream target;
		
		public Stream() {this(new Counter(), System.out);}
		public Stream(PrintStream stream) {this(new Counter(), stream);}
		public Stream(ProgressRecorder reporter) {this(reporter, System.out);}
		public Stream(ProgressRecorder reporter, java.io.PrintStream target) {
			this.inner = reporter;
			this.target = target;
		}

		@Override public void update(long delta) {
			inner.update(delta);
			target.printf("%.2f%%%n", percent()*100);
			elapse();
		}

		@Override public double percent() {return inner.percent();}

		@Override public void reset(long expected) {
			target.println("--------------- Reporter Reset ------------");
			inner.reset(expected);
		}
		
		@Override public long elapse() {
			long elapse = inner.elapse();
			target.printf("Elapse time: %,d ms%n", elapse);
			return elapse;
		}


		@Override public long reportStep() {return inner.reportStep();}

		@Override public String message() {return inner.message();}

		@Override public void message(String message) {
			target.printf("Message set: %s%n", message);
			inner.message(message);
		}
	}
}