package ar.ext.spark.hbin;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;

import ar.util.memoryMapping.MemMapEncoder;
import ar.util.memoryMapping.MemMapEncoder.TYPE;

//Influenced by http://hadoopi.wordpress.com/2013/05/27/understand-recordreader-inputsplit/, but using a different base classe

public class IndexedReader implements RecordReader<LongWritable, DataInputRecord> {
	long start, end;
	
	private final TYPE[] types;
	private final int recordLength;
	
	private final FSDataInputStream input;
	private long pos;
	
	/**
	 * @param dataOffset  What byte does the first record start at?
	 * @param types The types of each record
	 */
	public IndexedReader(long dataOffset, TYPE[] types, FileSplit split, JobConf conf) throws IOException {
		this.types =  types;
		this.recordLength = MemMapEncoder.recordLength(types);

		this.start = split.getStart();
		end = start + split.getLength();

		final Path file = split.getPath();
        FileSystem fs = file.getFileSystem(conf);
        FSDataInputStream input = fs.open(split.getPath());
        
		long shift = dataOffset%recordLength;
        long recordCount = start/recordLength; 
        long nextRecord = Math.max((recordCount*recordLength)+shift, dataOffset);
        
        this.pos = nextRecord;
        this.input = input;
        input.seek(pos);
	}
	
	
	@Override public void close() throws IOException {input.close();}
	@Override public LongWritable createKey() {return new LongWritable();}
	@Override public DataInputRecord createValue() {return new DataInputRecord(types);}
	@Override public long getPos() throws IOException {return pos;}

	
	@Override
	public boolean next(LongWritable key, DataInputRecord val) throws IOException {
		if (pos + recordLength > end) {
			if (pos != end) {System.out.printf("WARNING: Dropping %d bytes\n", end-pos);}
			return false;
		}  
		
		
		input.seek(pos);
		val.fill(input);
		key.set(pos);
		pos = pos + recordLength;
		return true;
	}


	@Override
	public float getProgress() throws IOException {
		if (start == end) {
            return 0.0f;
        } else {
            return Math.min(1.0f, (pos - start) / (float) (end - start));
        }
	}



}