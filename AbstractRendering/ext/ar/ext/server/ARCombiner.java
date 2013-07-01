package ar.ext.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.avro.generic.GenericRecord;

import ar.AggregateReducer;
import ar.Aggregates;
import ar.ext.avro.AggregateSerailizer;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.util.Util;

/**Receives Avro-encoded aggregates from a remote and combines them.
 *
 * Produces aggregates incrementally upon request (therefore, it is stateful).
 *
 * This system relies on a "push" method for receiving values, so it is technically a server
 * (though it will probably logically be part of an abstract rendering 'client application').  
 * This implementation does not "know" how many parts to expect, so it cannot tell if
 * an aggregate set is "done".  However, it counts the number of items combined and can report that.
 * 
 * This is NOT a compliment to the ARServer.  The ARServer is a REST application that expects incoming
 * socket connections.  Use the ar.avro.AggregatesSerailizer.deserialize methods to handle
 * the results returned from ARServer.
 * 
 */
public class ARCombiner<A> {
	private final Listener<A> listener;
	private final Thread listenerThread;
	private final Queue<Aggregates<A>> incomming;
	private Aggregates<A> aggs;
	private int combinedCount=0;
	private AggregateReducer<A,A,A> reducer;

	public ARCombiner(String hostname, int port, Valuer<GenericRecord,A> converter, AggregateReducer<A,A,A> reducer) throws IOException {
		this.incomming = new ConcurrentLinkedQueue<Aggregates<A>>();
		this.listener = new Listener<A>(hostname, port, incomming, converter);
		this.reducer = reducer;
		listenerThread = new Thread(listener);
		listenerThread.setDaemon(true);
	}
	
	public void start() {listenerThread.start();}
	
	/**Signal that the server sould stop.*/
	public void stop() {
		listener.stop();
		try {listenerThread.join();}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**@return True if sever shutdown is currently running; false otherwise*/
	public boolean running() {return listener.running();}
	
	//TODO: More eager combining...perhaps use another thread and condition variable to awake when there is actual work.
	public Aggregates<A> combined() {
		while (!incomming.isEmpty()) {
			Aggregates<A> item = incomming.poll();
			if (item != null) {
				aggs = Util.reduceAggregates(aggs, item, reducer);
				combinedCount++;
			}
		}
		return aggs;
	}
	
	public int combinedCount() {return combinedCount;}
	
	public static class Listener<A> implements Runnable {
		public final int port;
		public final String hostname;
		private final ServerSocket serverSocket;
		private final Queue<Aggregates<A>> queue;
		private final Valuer<GenericRecord,A> elementConverter;
		private boolean running = false;
		
		public Listener(String hostname, int port, Queue<Aggregates<A>> queue, Valuer<GenericRecord, A> converter) throws IOException {
			this.port = port;
			this.hostname = hostname;
			elementConverter = converter;
			this.queue = queue;

			serverSocket = new ServerSocket();
			serverSocket.bind((hostname != null) ? new InetSocketAddress(hostname, port) : new InetSocketAddress(port));
		}
		
		public void stop() {
			safeClose(serverSocket);
		}
		
		public void run() {
			Socket finalAccept = null;
			do {
				running = true;
				try {
					finalAccept = serverSocket.accept();
		            final InputStream inputStream = finalAccept.getInputStream();
	                if (inputStream == null) {safeClose(finalAccept);}
	                
	                //TODO: Lazier deserialization (don't tie up the server here)
	                Aggregates<A> res = AggregateSerailizer.deserialize(inputStream, elementConverter);
	                queue.add(res);
				} catch (Exception e) {
					//Ignore all errors if server socket is closed
					if (!serverSocket.isClosed()) {e.printStackTrace();}
				} finally {
					safeClose(finalAccept);
				}
			} while (serverSocket != null && !serverSocket.isClosed());
			running = false;
		}
			
		public boolean running() {return running;}
	    private static final void safeClose(Closeable thing) {
	        if (thing != null) {
	            try {
	            	thing.close();
	            }
	            catch(IOException e) {
	            }
	        }
	    }
	}
	

}
