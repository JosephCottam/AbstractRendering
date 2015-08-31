package ar.test.ext;

import static org.junit.Assert.*;

import org.junit.Test;

import static org.hamcrest.Matchers.*;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.nio.file.Path;
import java.util.List;

import ar.Renderer;
import ar.ext.server.CacheManager;
import ar.rules.Numbers;
import static java.util.stream.Collectors.*;

public class CacheManagerTest {

	@Test
	public void tileBounds() {
		CacheManager m = new CacheManager(new File("cache"), 500, Renderer.defaultInstance());
		
		assertThat(m.tileBounds(new Rectangle(0,0,10,10)).size(), is(1));
		assertThat(m.tileBounds(new Rectangle(0,0,10,10)), contains(new Rectangle(0,0,m.tileSize(),m.tileSize())));
		assertThat(m.tileBounds(new Rectangle(600,600,10,10)), contains(new Rectangle(600,600,m.tileSize(),m.tileSize())));
		assertThat(m.tileBounds(new Rectangle(500,500,10,10)), contains(new Rectangle(500,500,m.tileSize(),m.tileSize())));
		
		m = new CacheManager(new File("cache"), 100, Renderer.defaultInstance());
		assertThat(m.tileBounds(new Rectangle(0,0,10,10)).size(), is(1));
		assertThat(m.tileBounds(new Rectangle(0,0,10,10)), contains(new Rectangle(0,0,m.tileSize(),m.tileSize())));
		assertThat(m.tileBounds(new Rectangle(600,600,10,10)), contains(new Rectangle(600,600,m.tileSize(),m.tileSize())));
		assertThat(m.tileBounds(new Rectangle(500,500,10,10)), contains(new Rectangle(500,500,m.tileSize(),m.tileSize())));
		
		assertThat(m.tileBounds(new Rectangle(0,0,200,200)).size(), is(4));
		assertThat(m.tileBounds(new Rectangle(0,0,200,200)), 
				hasItems(new Rectangle(0,0,100,100), new Rectangle(100,100,100,100), new Rectangle(0,100,100,100), new Rectangle(100,0,100,100)));
		assertThat(m.tileBounds(new Rectangle(0,0,10,300)), 
				hasItems(new Rectangle(0,0,100,100), new Rectangle(0,100,100,100), new Rectangle(0,200,100,100)));
		assertThat(m.tileBounds(new Rectangle(0,0,300,10)), 
				hasItems(new Rectangle(0,0,100,100), new Rectangle(100,0,100,100), new Rectangle(200,0,100,100)));
	}
	
	@Test 
	public void renderBounds() {
		CacheManager m = new CacheManager(new File("cache"), 100, Renderer.defaultInstance());
		
		assertThat(m.renderBounds(new Rectangle(0,0,10,0)), is(new Rectangle(0,0,m.tileSize(),m.tileSize())));
		assertThat(m.renderBounds(new Rectangle(0,0,101,101)), is(new Rectangle(0,0,m.tileSize()*2,m.tileSize()*2)));
		assertThat(m.renderBounds(new Rectangle(1,1,10,0)), is(new Rectangle(0,0,m.tileSize(),m.tileSize())));
		assertThat(m.renderBounds(new Rectangle(-1,-1,1,1)), is(new Rectangle(-m.tileSize(),-m.tileSize(),m.tileSize()*2,m.tileSize()*2)));
		assertThat(m.renderBounds(new Rectangle(-10,-10,1,1)), is(new Rectangle(-m.tileSize(),-m.tileSize(),m.tileSize(),m.tileSize())));
	}
	
	@Test
	public void tileFiles() {
		CacheManager m = new CacheManager(new File("cache"), 10, Renderer.defaultInstance());
		List<Rectangle> tiles = m.tileBounds(new Rectangle(0,0,100,100));
		Path root = m.tilesetPath("test", new Numbers.Count<>(), new AffineTransform());
		List<File> files = tiles.stream().map(t -> CacheManager.tileFile(root, t)).collect(toList());
		
		assertThat("Unexected number of tiles", files.size(), is(100));
		assertThat("Non-unique file path created", 
				files.size(), 
				is(files.stream().map(f -> f.toString()).collect(toSet()).size())); 
	}
	
	@Test 
	public void globalBinTransform() {
		AffineTransform gbt = CacheManager.globalBinTransform(new Rectangle(0,0,100,100), new AffineTransform());
		assertThat(gbt, is(new AffineTransform()));
		
		gbt = CacheManager.globalBinTransform(new Rectangle(10,10,100,100), new AffineTransform());
		assertThat(gbt, is(AffineTransform.getTranslateInstance(10, -10)));
		
		gbt = CacheManager.globalBinTransform(new Rectangle(0,0,100,100), AffineTransform.getTranslateInstance(100, 100));
		assertThat(gbt, is(new AffineTransform()));
		
		gbt = CacheManager.globalBinTransform(new Rectangle(0,0,100,100), AffineTransform.getScaleInstance(10, 3));
		assertThat(gbt, is(AffineTransform.getScaleInstance(10,3)));
	}
	
}
