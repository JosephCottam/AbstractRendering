package ar.ext.spark.hbin;

import java.io.DataInputStream;
import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import ar.glyphsets.implicitgeometry.Indexed;
import ar.util.memoryMapping.MemMapEncoder;
import ar.util.memoryMapping.MemMapEncoder.TYPE;

//Based on http://hadoopi.wordpress.com/2013/05/27/understand-recordreader-inputsplit/

public class IndexedReader extends RecordReader<Long, Indexed> {
	long start, end;
	
	private final TYPE[] types;
	private final int recordLength;
	private final long dataOffset;
	
	private DataInputStream input;
	private long pos;
	private Indexed nextValue;
	private final LongWritable nextKey = new LongWritable();
	
	/**
	 * @param dataOffset  What byte does the first record start at?
	 * @param types The types of each record
	 */
	public IndexedReader(long dataOffset, TYPE[] types) {
		this.types =  types;
		this.recordLength = MemMapEncoder.recordLength(types);
		this.dataOffset = dataOffset;
	}

	@Override
	public void initialize(InputSplit genericSplit, TaskAttemptContext context)
			throws IOException, InterruptedException {
		FileSplit split = (FileSplit) genericSplit;
		end = split.getLength() + start;

		final Path file = split.getPath();
        FileSystem fs = file.getFileSystem(context.getConfiguration());
        FSDataInputStream input = fs.open(split.getPath());
        
		long start = split.getStart() -1; //Just in case we got passed a start at a record boundary, this will make subsequent math work out
        long recordID = (start-dataOffset)/recordLength; //Which record is the first byte in?
        long nextRecord = ((recordID+1)* recordLength) + start; //Start of the first record in the segment
        this.start = nextRecord;
        this.pos = start;
        input.seek(start);
	}
	

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		if (pos > end) {return false;}
		nextKey.set(pos);
		nextValue = new DataInputRecord(types, input);
		pos = pos + recordLength;
		return true;
	}
	
	
	@Override public void close() throws IOException {input.close();}

	@Override
	public Long getCurrentKey() throws IOException, InterruptedException {
		return nextKey.get();
	}

	@Override
	public Indexed getCurrentValue() throws IOException, InterruptedException {
		return nextValue;
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
	  if (start == end) {
            return 0.0f;
        } else {
            return Math.min(1.0f, (pos - start) / (float) (end - start));
        }
	}



}