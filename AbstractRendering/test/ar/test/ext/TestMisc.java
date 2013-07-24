package ar.test.ext;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import ar.ext.misc.*;
import ar.glyphsets.implicitgeometry.Indexed;

public class TestMisc {

	@Test
	public void broadcast(){
		Object key = "a";
		Collection<Indexed> vals = BroadcastEntries.broadcast(key, Arrays.asList(10,11,12,13,14,15));
		
		for (Indexed e:vals){
			assertThat(e.get(0), is(key));
			assertThat((int) e.get(1), is(((int)e.get(2))+10));
		}
	}
}
