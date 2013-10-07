package ar.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

/**Simple delimited text file reader for returning string-arrays from each line.
 * Works by splitting the file using regular expressions.
 * Assumes any validation happens by the consumer of the reader.
 * **/
public class DelimitedReader implements Iterator<String[]> {
	/**Expression for simple CSV splitting (does not handle escaped strings).**/
	public static final String CSV = "\\s*,\\s*";
	
	private BufferedReader reader;
	private final Pattern splitter;


	/** @param file Source file
	 * @param skip Number of lines to skip (i.e., header lines)
	 * @param delimiter Regular expression to split fields appart
	 */
	public DelimitedReader(File file, int skip, String delimiter) {
		try {
			splitter = Pattern.compile(delimiter);
			reader = new BufferedReader(new FileReader(file));
			while (skip > 0) {reader.readLine(); skip--;}
		} catch (IOException e) {throw new RuntimeException("Error intializing glyphset from " + file.getName(), e);}
	}
	
	/**Are there any more elements expected from this file?**/
	public boolean hasNext() {return reader != null;}
	
	/**What is the next set of values?
	 * @return The next file line, broken by the delimiter or null if at end of file.
	 */
	public String[] next() {
		String line = null;
		try {line = reader.readLine();}
		catch (Exception e) {return done();}
		if (line == null) {return done();}
		else {return splitter.split(line);}
	}

	//Always returns null...
	protected String[] done() {
		try {reader.close();}
		catch (IOException e) {throw new RuntimeException(e);}
		finally {reader = null;}
		return null;
	}

	/**Not supported**/
	public void remove() {throw new UnsupportedOperationException();}
}
