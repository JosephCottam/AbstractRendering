package ar.ext.spark.hbin;

import java.io.DataInputStream;
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
	
	private final DataInputStream input;
	private long pos;
	
	/**
	 * @param dataOffset  What byte does the first record start at?
	 * @param types The types of each record
	 */
	public IndexedReader(long dataOffset, TYPE[] types, FileSplit split, JobConf conf) throws IOException {
		this.types =  types;
		this.recordLength = MemMapEncoder.recordLength(types);

		end = split.getLength() + start;

		final Path file = split.getPath();
        FileSystem fs = file.getFileSystem(conf);
        FSDataInputStream input = fs.open(split.getPath());
        
		long start = split.getStart() -1; //Just in case we got passed a start at a record boundary, this will make subsequent math work out
        long recordID = (start-dataOffset)/recordLength; //Which record is the first byte in?
        long nextRecord = ((recordID+1)* recordLength) + start; //Start of the first record in the segment
        this.start = nextRecord;
        this.pos = start;
        input.seek(start);
        this.input = input;
	}
	
	
	@Override public void close() throws IOException {input.close();}
	@Override public LongWritable createKey() {return new LongWritable();}
	@Override public DataInputRecord createValue() {return new DataInputRecord(types);}
	@Override public long getPos() throws IOException {return pos;}

	@Override
	public boolean next(LongWritable key, DataInputRecord val) throws IOException {
		if (pos + recordLength > end) {return false;}  //HACK!!!!! HORRIBLE HACK!!!!! DROPS DATA !!!! FOR TESTING ONLY!!!!
		//if (pos > end) {return false;}
		
		
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