package ar.test.ext;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ar.Aggregates;
import ar.Aggregator;
import ar.ext.tiles.TileUtils;
import ar.rules.Numbers;
import ar.test.aggregates.TestAggregates;

public class TileUtilsTest {
	@Test
	public void subset() {
		Aggregates<Integer> aggs = TestAggregates.simpleAggregates(10, 10, 100, 100, -1);
		
		Aggregates<Integer> subset = TileUtils.subset(aggs, 0, 0, 25, 25);
		
		for (int x=subset.lowX(); x<subset.highX(); x++) {
			for (int y=subset.lowY(); y<subset.highY(); y++) {
				if (x < aggs.lowX() || y < aggs.lowX()) {
					assertThat(subset.at(x,y), is(-1));
				} else {
					assertThat(subset.at(x,y), is(aggs.at(x,y))); 
				}
			}			
		}
		
	}
	
	@Test
	public void tileCascade() throws Exception {
		Aggregates<Integer> aggs = TestAggregates.simpleAggregates(0,0,1000,1000, -1);
		Aggregator<?,Integer> red = new Numbers.Count();
		File root = new File("./testResults/tileset");
		org.apache.commons.io.FileUtils.deleteDirectory(root);
		assertFalse("Failed to remove directory.", root.exists());
		
		TileUtils.makeTileCascae(aggs, red, root, 100, 100, 1);

		List<File> files = new ArrayList<File>();
		for (int x=0; x<10; x++) {
			for (int y=0; y<10; y++) {
				files.add(TileUtils.extend(root, "0", Integer.toString(x), Integer.toString(y),".avro"));
			}
		}

		for (File f: files) {assertTrue("File not found: " + f.getPath(), f.exists());}
		
		
		Aggregates<Integer> output = TileUtils.loadTiles(new ar.ext.avro.Converters.ToCount(), Integer.class, files.toArray(new File[0]));
		for (int x=output.lowX(); x<output.highX(); x++) {
			for (int y=output.lowY(); y<output.lowY(); y++) {
				assertThat(String.format("Error at %d, %d.", x,y), output.at(x,y), is(aggs.at(x, y)));
			}
		}
		
	}
}
