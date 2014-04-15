package ar.ext.spark.hbin;

import java.io.DataInputStream;
import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.FileInputFormat;

import ar.util.memoryMapping.MemMapEncoder;
import ar.util.memoryMapping.MemMapEncoder.TYPE;

public class HBINInputFormat extends FileInputFormat<LongWritable, DataInputRecord> {
	@Override
	public RecordReader<LongWritable, DataInputRecord> getRecordReader(
			InputSplit genericSplit, 
			JobConf conf,
			Reporter reporter) throws IOException {

		FileSplit split = (FileSplit) genericSplit;
		final Path file = split.getPath();
        FileSystem fs = file.getFileSystem(conf);
        
        MemMapEncoder.Header header;
        try (DataInputStream stream = fs.open(split.getPath())) {
        	header = MemMapEncoder.Header.from(stream);
        }

        long dataOffset = header.dataTableOffset;
		TYPE[] types =  header.types;
		return new IndexedReader(dataOffset,types, split, conf);
	}	
//	
//	@Override
//	public InputSplit[] getSplits(JobConf job, int numSplits) {
//		InputSplit[] splits = new InputSplit[numSplits];
//		for (int i=0; i<numSplits; i++) {
//			Path path = job.getWorkingDirectory();
//			long start = ;
//			long length;
//			FileSplit s = new FileSplit(path, start, length, (String[]) null);
//		}
//	}
	
}