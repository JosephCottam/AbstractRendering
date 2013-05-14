package ar.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

public class CSVReader {
	private BufferedReader reader;
	private final Pattern splitter = Pattern.compile("\\s*,\\s*");

	public CSVReader(File file, int skip) {
		try {
			reader = new BufferedReader(new FileReader(file));
			while (skip-- > 0) {reader.readLine();}
		} catch (IOException e) {throw new RuntimeException("Error intializing glyphset from " + file.getName(), e);}
	}
	
	public boolean hasNext() {return reader != null;}
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
}
