package ar.test.ext;

import static org.junit.Assert.*;

import org.junit.Test;

import static org.hamcrest.Matchers.*;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.*;

import ar.ext.server.CacheManager;

public class CacheManagerTest {

	@Test
	public void tileBounds() {
		CacheManager m = new CacheManager(new File("cache"), 500);
		
		assertThat(m.tileBounds(new Rectangle(0,0,10,10)).size(), is(1));
		assertThat(m.tileBounds(new Rectangle(0,0,10,10)), contains(new Rectangle(0,0,m.tileSize(),m.tileSize())));
		assertThat(m.tileBounds(new Rectangle(600,600,10,10)), contains(new Rectangle(600,600,m.tileSize(),m.tileSize())));
		assertThat(m.tileBounds(new Rectangle(500,500,10,10)), contains(new Rectangle(500,500,m.tileSize(),m.tileSize())));
		
		m = new CacheManager(new File("cache"), 100);
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
		AffineTransform vt = new AffineTransform();
		CacheManager m = new CacheManager(new File("cache"), 100);
		
		assertThat(m.renderBounds(vt, new Rectangle(0,0,10,0)), is(new Rectangle(0,0,m.tileSize(),m.tileSize())));
		assertThat(m.renderBounds(vt, new Rectangle(0,0,101,101)), is(new Rectangle(0,0,m.tileSize()*2,m.tileSize()*2)));
		assertThat(m.renderBounds(vt, new Rectangle(1,1,10,0)), is(new Rectangle(0,0,m.tileSize(),m.tileSize())));
		assertThat(m.renderBounds(vt, new Rectangle(-1,-1,1,1)), is(new Rectangle(-m.tileSize(),-m.tileSize(),m.tileSize()*2,m.tileSize()*2)));
		assertThat(m.renderBounds(vt, new Rectangle(-10,-10,1,1)), is(new Rectangle(-m.tileSize(),-m.tileSize(),m.tileSize(),m.tileSize())));
	}
	
}
