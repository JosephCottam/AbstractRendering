package ar.test.renderers;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;

import ar.renderers.ProgressRecorder;


public class TestProgressRecorder {
	@Test
	public void testNOP() {
		ProgressRecorder r = new ProgressRecorder.NOP();
		
		assertThat(r.elapse(), is(-1L));
		r.reset(1000);
		assertThat(r.elapse(), is(-1L));
		
		assertThat(r.reportStep(), is(-1L));
		r.update(10);
		assertThat(r.percent(), is(-1d));
		
		assertTrue(r.message() == null);
		r.message("Test");
		assertTrue(r.message() == null);
		
		assertThat(r.elapse(), is(-1L));
	}
	
	@Test
	public void testCounter() throws InterruptedException {
		ProgressRecorder r = new ProgressRecorder.Counter();

		assertThat(r.elapse(), is(-1L));
		r.reset(10);
		
		assertTrue(r.reportStep() > 0);
		r.update(5);
		assertThat(r.percent(), is(.5d));
		
		assertTrue(r.message() == null);
		r.message("Test");
		assertThat(r.message(), is("Test"));
		r.message(null);
		assertTrue(r.message() == null);
		
		Thread.sleep(100);
		assertTrue(r.elapse()+"", r.elapse() > 0);
	}
	
	@Test
	public void testStream() throws InterruptedException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);

		ProgressRecorder r = new ProgressRecorder.Stream(ps);
		
		assertThat(r.elapse(), is(-1L));
		r.reset(10);
		
		assertTrue(r.reportStep() > 0);
		r.update(5);
		assertThat(r.percent(), is(.5d));
		assertTrue(baos.toString(), baos.toString().endsWith("50.00%\n"));
		
		assertTrue(r.message() == null);
		r.message("Test");
		assertThat(r.message(), is("Test"));
		assertTrue(baos.toString(), baos.toString().endsWith("Test\n"));
		
		Thread.sleep(100);
		assertTrue(r.elapse()+"", r.elapse() > 0);
	}
}
