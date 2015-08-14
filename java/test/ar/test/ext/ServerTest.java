package ar.test.ext;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Function;

import ar.ext.server.ARServer;

public class ServerTest {
	@Test
	public void construct() throws Exception {
		ARServer server = new ARServer("localhost", 8080);
		server.start();
		assertEquals("Incorrect port", 8080, server.getListeningPort());
		assertTrue("Not alive", server.isAlive());
		server.stop();
		
		
		assertEquals(8, server.getDatasets().size());
		assertEquals(7, server.getAggregators().size());
		assertEquals(18, server.getTransfers().size());
	}
	
	private final Object[][] ARGUMENTS = 
			new Object[][]{
				new Object[]{"localhost", 8080, "CENSUS_TRACTS", "MERGE_CATS", null},
				new Object[]{"localhost", 8080, "CENSUS_TRACTS",null, null}
			};
	
	@Test
	public void render() throws Exception {
		ARServer server = new ARServer("localhost", 8080);
		server.start();
		
		try {
			Method m = this.getClass().getMethod("sendMessage", String.class, int.class, String.class, String.class, String.class);
			for (Object[] args: ARGUMENTS) {
				String result = (String) m.invoke(this, args);
				assertTrue(result, result.startsWith("{\"xOffset"));
			}
		} finally {
			server.stop();
		}
	}

	public String sendMessage(String host, int port, String dataset, String aggregator, String transfer) throws Exception {
		HttpURLConnection connection=null;

		try {
			URL url = new URL(String.format("http://%s:%d/%s?aggregator=%s&transfer=%s&format=json", host, port,dataset,aggregator, transfer));
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");


			try (InputStream is = connection.getInputStream();
				BufferedReader rd = new BufferedReader(new InputStreamReader(is));) {
				
				String line;
				StringBuffer response = new StringBuffer(); 
				while((line = rd.readLine()) != null) {
					response.append(line);
					response.append('\r');
				}
				rd.close();
				return response.toString();
			}

		} catch (Exception e) {

			e.printStackTrace();
			return null;

		} finally {
			if(connection != null) {
				connection.disconnect(); 
			}
		}

	}

}
