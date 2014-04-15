package ar.ext.spark.hbin;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.FileStatus;
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
		final Path path = split.getPath();
        
        MemMapEncoder.Header header;
        try (DataInputStream stream = path.getFileSystem(conf).open(path)) {
        	header = MemMapEncoder.Header.from(stream);
        }

        long dataOffset = header.dataTableOffset;
		TYPE[] types =  header.types;
		return new IndexedReader(dataOffset,types, split, conf);
	}	
	
	@Override
	public InputSplit[] getSplits(JobConf job, int numSplits) {
		FileStatus fileStatus;
        MemMapEncoder.Header header;
        Path path;
        numSplits = Math.max(job.getNumMapTasks(), numSplits);

        try {
        	FileStatus[] statuses = this.listStatus(job);
			if (statuses.length != 1) {throw new RuntimeException("HBIN input can only be used with single-file jobs.");}
			fileStatus = statuses[0];
			path = fileStatus.getPath(); 	        
	        try (DataInputStream stream = path.getFileSystem(job).open(path)) {
	        	header = MemMapEncoder.Header.from(stream);
	        }
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		long fileLength = fileStatus.getLen();

        long dataOffset = header.dataTableOffset;
        TYPE[] types =  header.types;
        long recordLength = MemMapEncoder.recordLength(types);

        
        long start = dataOffset;
        long dataLength = fileLength-start;
        long records = ((dataLength*fileStatus.getBlockSize()-dataOffset)/recordLength);
        long recordsPerSplit = (records/numSplits)+1;
        long splitLength = (recordsPerSplit*recordLength)/fileStatus.getBlockSize();
		
        List<InputSplit> splits = new ArrayList<InputSplit>(numSplits);
		for (int i=0; i<numSplits; i++) {
			long length = (start+splitLength < fileLength) ? splitLength : fileLength-start;
			splits.add(new FileSplit(path, start, length, (String[]) null));
			start = start+length;
		}
		return splits.toArray(new InputSplit[splits.size()]);
	}
	
}