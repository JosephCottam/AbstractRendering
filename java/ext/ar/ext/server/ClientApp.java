package ar.ext.server;

import static ar.util.Util.argKey;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import ar.Aggregates;
import ar.Renderer;
import ar.Transfer;
import ar.app.display.*;
import ar.ext.avro.Converters;
import ar.renderers.ParallelRenderer;

public class ClientApp {
	
	public static class ClientUI extends JPanel {
		private final String host;
		private final int port;
		
		private final JFrame frame=new JFrame();
		private final JButton execute = new JButton("Execute");
		private final Renderer renderer = new ParallelRenderer();
		private ARComponent display;

		public ClientUI(String host, int port) {
			this.host = host;
			this.port = port;
			
			this.setLayout(new BorderLayout());			
			this.add(execute, BorderLayout.SOUTH);
			
			execute.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						ClientUI.this.changeDisplay();
					} catch (Exception e1) {
						System.err.println("Error updating UI from server");
						e1.printStackTrace();
					}
				}
				
			});

			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setTitle("Abstract Rendering (Client App)");
			frame.setLayout(new BorderLayout());
			frame.add(this, BorderLayout.CENTER);
			frame.setSize(800, 800);
			frame.validate();
			frame.setVisible(true);
		}
		
		public <A,B> void changeDisplay() throws Exception {
			ARComponent old = this.display;
			if (old != null) {frame.remove(old);}
			
			Aggregates<?> aggs = remoteRender(host, port, dataset(), aggregate(), remoteTransfers());
			
			ARComponent newDisplay = new SimpleDisplay(aggs, localTransfer(), renderer); 
			frame.add(newDisplay, BorderLayout.CENTER);
			this.display = newDisplay;
			frame.revalidate();
		}
		
		public String dataset() {return "...";}
		public String aggregate() {return "...";}
		public String[] remoteTransfers() {return new String[]{".."};}
		public Transfer<?,?> localTransfer() {return null;}
	}
	
	/**Communicate with remote server to perform rendering on the indicated dataset, 
	 * return the aggregate set the server returned.
	 * 
	 * @param host
	 * @param port
	 * @param dataset
	 * @param aggregate
	 * @param transfers
	 * @return
	 * @throws Exception
	 */
	public static Aggregates<?> remoteRender(String host, int port, String dataset, String aggregate, String... transfers) throws Exception {
		dataset = URLEncoder.encode(dataset, "UTF-8");
		aggregate = URLEncoder.encode(aggregate, "UTF-8");
		String transfer = Arrays.deepToString(transfers);
		transfer = URLEncoder.encode(transfer.substring(1, transfers.length-2), "UTF-8");

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
			try (DataOutputStream wr = new DataOutputStream (connection.getOutputStream ())){
				wr.writeBytes (message);
				wr.flush ();
				wr.close ();
			}

			try (InputStream is = connection.getInputStream();
				BufferedReader rd = new BufferedReader(new InputStreamReader(is));) {
				
				String line;
				StringBuffer response = new StringBuffer(); 
				while((line = rd.readLine()) != null) {
					response.append(line);
					response.append('\r');
				}
				rd.close();
				
				InputStream stream = new ByteArrayInputStream(response.toString().getBytes("UTF-8"));
				return ar.ext.avro.AggregateSerializer.deserialize(stream, new Converters.ToCount());
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
	
	public static void main(String[] args) throws Exception {
		int port = Integer.parseInt(argKey(args, "-port", "9874"));
		String host = argKey(args, "-host", "localhost");

		if (host.equals("localhost")) {
			ARServer server = new ARServer(host, port);
			server.start();
		}
		
	}
}
