package ar.ext.spark.hbin;

import java.io.DataInputStream;
import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

import ar.glyphsets.implicitgeometry.Indexed;
import ar.util.memoryMapping.MemMapEncoder;
import ar.util.memoryMapping.MemMapEncoder.TYPE;

public class HBINInputFormat extends FileInputFormat<Long, Indexed> {
	@Override
	public RecordReader<Long, Indexed> createRecordReader(
			InputSplit genericSplit,
			TaskAttemptContext context) throws IOException, InterruptedException {

		FileSplit split = (FileSplit) genericSplit;
		final Path file = split.getPath();
        FileSystem fs = file.getFileSystem(context.getConfiguration());
        
        MemMapEncoder.Header header;
        try (DataInputStream stream = fs.open(split.getPath())) {
        	header = MemMapEncoder.Header.from(stream);
        }

        long dataOffset = header.dataTableOffset;
		TYPE[] types =  header.types;
		IndexedReader r = new IndexedReader(dataOffset,types);
		r.initialize(split, context);
		return r;

		
	}


	
}