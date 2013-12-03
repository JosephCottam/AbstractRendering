package ar.renderers;


/**Utilities specific to the renderer implementations.  In general,
 * the contents of this class should be considered "unstable" and
 * used with care.  (The class is public to enable monitoring.)
 * @author jcottam
 *
 */
public class RenderUtils {
	/**Common location for controlling render progress reporting.
	 * Renderers are NOT required to respect this setting, but will
	 * if they use the "recorder" method also in this class. 
	 */
	public static boolean RECORD_PROGRESS = false;
	
	/**After how many processed items should progress be reported?
	 * This is a hint to tasks and not a binding item HOWEVER, if set to
	 * zero or less, then no intermediate reporting is requested. 
	 */
	public static long REPORT_STEP = -1;
	
	/**Instantiate a progress recorder according to the RECORD_PROGRESS setting.**/
	public static ProgressReporter recorder() {
		return RECORD_PROGRESS ? new ProgressReporter.Counter(REPORT_STEP) : new ProgressReporter.NOP(REPORT_STEP);
	}
}
