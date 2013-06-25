package ar.test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.junit.Test;

import ar.ext.server.ARServer;

public class ServerTest {
	@Test
	public void construct() throws Exception {
		ARServer server = new ARServer("localhost", 8080);
		server.start();
		assertEquals("Incorrect port", 8080, server.getListeningPort());
		assertTrue("Not alive", server.isAlive());
		server.stop();
		
		
		assertEquals(9, server.getAggregators().size());
		assertEquals(16, server.getTransfers().size());
		
	}
	
	@Test
	public void render() throws Exception {
		ARServer server = new ARServer("localhost", 8080);
		server.start();
		String r = sendMessage("circlepoints","Count","RedWhiteLinear", "localhost", 8080);
		server.stop();
		System.out.println(r);
		assertTrue(r.startsWith("DS"));
	}

	public String sendMessage(String dataset, String aggregate, String transfer, String host, int port) throws Exception {
		dataset = URLEncoder.encode(dataset, "UTF-8");
		aggregate = URLEncoder.encode(aggregate, "UTF-8");
		transfer = URLEncoder.encode(transfer, "UTF-8");

		String message = String.format("data=%s&aggregate=%s&transfers=%s", dataset,aggregate,transfer);
		HttpURLConnection connection=null;

		try {
			URL url = new URL(String.format("http://%s:%d", host, port));
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");

			connection.setRequestProperty("Content-Type", 
					"application/x-www-form-urlencoded");

			connection.setRequestProperty("Content-Length", "" + 
					Integer.toString(message.getBytes().length));
			connection.setRequestProperty("Content-Language", "en-US");  

			connection.setUseCaches (false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			//Send request
			DataOutputStream wr = new DataOutputStream (connection.getOutputStream ());
			wr.writeBytes (message);
			wr.flush ();
			wr.close ();

			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer(); 
			while((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();

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
