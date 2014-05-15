package ar.glyphsets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Indexed.Converter;

/**Given a file with line-oriented, regular-expression delimited values,
 * provides a list-like (read-only) interface.
 * 
 * @author josephcottam
 *
 *
 * NOT THREAD SAFE!!!!
 * TODO: Implement Glyphset instead of List<Indexed>...
 */
public class DelimitedFileList implements List<Indexed> {
	public static int DEFAULT_SKIP =0;

	
	/**Source file.**/
	private final File source;
	
	/**Pattern used to delimit fields of the rows.**/
	private final String delimiters;
	
	/**Types of the fields.**/
	private final Converter.TYPE[] types;
	
	/**Number of lines to skip at the start of the file.**/
	private final int skip;

	/**How many entries in this list?  
	 * 
	 * Calculated once and then cached.
	 */
	private int size = Integer.MIN_VALUE; 

	private Iterator internal;
		
	public DelimitedFileList(File source, String delimiters, Converter.TYPE[] types) {this(source, delimiters, types, DEFAULT_SKIP);}
	public DelimitedFileList(File source, String delimiters, Converter.TYPE[] types, int skip) {
		this.source = source;
		this.delimiters = delimiters;
		this.types = types;
		this.skip = skip;
		internal = iterator();
	}

	
	@Override public boolean isEmpty() {return size ==0;}
	@Override public Iterator iterator() {return new Iterator(0);}
	@Override public ListIterator<Indexed> listIterator() {return new Iterator(0);}
	@Override public ListIterator<Indexed> listIterator(int index) {return new Iterator(index);}

	@Override
	public int size() {
		if (size <0) {
			try (BufferedReader r = new BufferedReader(new FileReader(source))) {
				size=0;
				while(r.readLine() != null) {size++;}
			} catch (IOException e) {
				throw new RuntimeException("Error processing file: " + source.getName());
			}
		}
		size = size-skip;
		return size;
	}

	@Override
	public Indexed get(int index) {
		if (internal.index > index) {
			System.out.println("New iterator...");
			internal = iterator();
		}
		while (internal.index < index) {internal.advance();}
		return internal.next();
	}
	
	
	private final class Iterator implements ListIterator<Indexed> {
		final BufferedReader base;
		String cache;
		int index;
		
		public Iterator(int index) {
			 try {
				base = new BufferedReader(new FileReader(source));
			 } catch (FileNotFoundException e) {
				throw new RuntimeException("Error processing file: " + source.getName());
			 }
			 this.index = index;
			 
			//advance to requested position
			 for (int i=0; i<index+skip; i++) {
				 try {base.readLine();} 
				 catch (IOException e) {
					 throw new RuntimeException(String.format("Could not advance to %d, failed at %d.", index, i));
				 }
			 }
		}
				
		@Override
		public boolean hasNext() {
			if (cache == null) {
				try {cache = base.readLine();}
				catch (IOException e) {throw new RuntimeException("Error processing file: " + source.getName());}
			}
			return cache != null;
		}

		public void advance() {cache = null; hasNext(); index++;}
		
		@Override
		public Indexed next() {
			if (cache == null && !hasNext()) {throw new NoSuchElementException();}

			StringTokenizer t = new StringTokenizer(cache, delimiters);
			ArrayList<String> parts = new ArrayList<>();
			while (t.hasMoreTokens()) {parts.add(t.nextToken());}
			Indexed base = new Indexed.ListWrapper(parts);
			cache = null;
			index++;

			return new Converter(base, types);
		}

		@Override public boolean hasPrevious() {return previousIndex() >= 0;}
		@Override public int nextIndex() {return index;}
		@Override public int previousIndex() {return index-1;}
		
		//Items that MAY be implementable if we switch to RandomAccess file type 
		@Override public Indexed previous() {throw new UnsupportedOperationException();}
		@Override public void remove() {throw new UnsupportedOperationException();}
		@Override public void set(Indexed e) {throw new UnsupportedOperationException();}
		@Override public void add(Indexed e) {throw new UnsupportedOperationException();}
	}



	//Items that could be supported...but we're in a hurry right now
	//TODO: Implement the below
	@Override public Object[] toArray() {throw new UnsupportedOperationException();}
	@Override public <T> T[] toArray(T[] a) {throw new UnsupportedOperationException();}
	@Override public boolean contains(Object o) {throw new UnsupportedOperationException();}
	@Override public List<Indexed> subList(int fromIndex, int toIndex)  {throw new UnsupportedOperationException();}
	@Override public boolean containsAll(Collection<?> c)  {throw new UnsupportedOperationException();}
	@Override public int indexOf(Object o)  {throw new UnsupportedOperationException();}
	@Override public int lastIndexOf(Object o)  {throw new UnsupportedOperationException();}


	//Items that WILL NEVER be supported because this is a read-only format
	@Override public boolean remove(Object o) {throw new UnsupportedOperationException();}
	@Override public boolean add(Indexed e) {throw new UnsupportedOperationException();}
	@Override public boolean addAll(Collection<? extends Indexed> c)  {throw new UnsupportedOperationException();}
	@Override public boolean addAll(int index, Collection<? extends Indexed> c)  {throw new UnsupportedOperationException();}
	@Override public boolean removeAll(Collection<?> c)  {throw new UnsupportedOperationException();}
	@Override public boolean retainAll(Collection<?> c)  {throw new UnsupportedOperationException();}
	@Override public void clear()  {throw new UnsupportedOperationException();}
	@Override public Indexed set(int index, Indexed element)  {throw new UnsupportedOperationException();}
	@Override public void add(int index, Indexed element)  {throw new UnsupportedOperationException();}
	@Override public Indexed remove(int index)  {throw new UnsupportedOperationException();}

}
