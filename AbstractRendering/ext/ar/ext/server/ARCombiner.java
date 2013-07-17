package ar.ext.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.avro.generic.GenericRecord;

import ar.Aggregates;
import ar.Aggregator;
import ar.ext.avro.AggregateSerializer;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.renderers.AggregationStrategies;

/**Receives Avro-encoded aggregates from a remote and combines them.
 *
 * Produces aggregates incrementally upon request (therefore, it is stateful).
 *
 * This system relies on a "push" method for receiving values, so it is technically a server
 * (though it will probably logically be part of an abstract rendering 'client application').  
 * This implementation does not "know" how many parts to expect, so it cannot tell if
 * an aggregate set is "done".  However, it counts the number of items combined and can report that.
 * 
 * When a new aggregate arrives, listeners can be notified.  Alternatively, polling can 
 * be achieved by monitoring the "count" method's value. 
 * 
 * This is NOT a compliment to the ARServer.  The ARServer is a REST application that expects incoming
 * socket connections.  Use the ar.avro.AggregatesSerailizer.deserialize methods to handle
 * the results returned from ARServer.
 * 
 */
public class ARCombiner<A> {
	private final Listener listener;
	private final Thread listenerThread;
	private final Queue<Aggregates<A>> queue;
	private final Aggregator<?,A> reducer;
	private Aggregates<A> aggs;
	protected AtomicInteger count = new AtomicInteger(0);
	private final List<ArrivalListener> listeners = new ArrayList<ArrivalListener>();
	
	public ARCombiner(String hostname, int port, Valuer<GenericRecord,A> converter, Aggregator<?,A> reducer) throws IOException {
		this.queue = new ConcurrentLinkedQueue<Aggregates<A>>();
		this.listener = new Listener(hostname, port, converter);
		this.reducer = reducer;
		listenerThread = new Thread(listener);
		listenerThread.setDaemon(true);
	}
	
	public void start() {listenerThread.start();}
	protected void addToQueue(Aggregates<A> a) {
		queue.add(a);
		count.incrementAndGet();
		notifyListeners();
	}
	
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
		while (!queue.isEmpty()) {
			Aggregates<A> item = queue.poll();
			if (item != null) {
				aggs = AggregationStrategies.foldLeft(aggs, item, reducer);
			}
		}
		return aggs;
	}
	
	/**How many items have arrived at this combiner?
	 * This is not the number of pending items, but the total number that have arrived, 
	 * regardless of their status as pending combining or not.
	 */
	public int count() {return count.get();}
	
	public class Listener implements Runnable {
		public final int port;
		public final String hostname;
		private final ServerSocket serverSocket;
		private final Valuer<GenericRecord,A> elementConverter;
		private boolean running = false;
		
		public Listener(String hostname, int port, Valuer<GenericRecord, A> converter) throws IOException {
			this.port = port;
			this.hostname = hostname;
			elementConverter = converter;

			serverSocket = new ServerSocket();
			serverSocket.bind((hostname != null) ? new InetSocketAddress(hostname, port) : new InetSocketAddress(port));
		}
		
		public void stop() {safeClose(serverSocket);}
		
		public void run() {
			Socket finalAccept = null;
			do {
				running = true;
				try {
					finalAccept = serverSocket.accept();
		            final InputStream inputStream = finalAccept.getInputStream();
	                if (inputStream == null) {safeClose(finalAccept);}
	                
	                //TODO: Lazier deserialization (don't tie up the server here)
	                Aggregates<A> aggs = AggregateSerializer.deserialize(inputStream, elementConverter);
	                ARCombiner.this.addToQueue(aggs);
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
	    private final void safeClose(Socket thing) {
	        if (thing != null) {
	            try {thing.close();}
	            catch(IOException e) {}
	        }
	    }
	    private final void safeClose(ServerSocket thing) {
	        if (thing != null) {
	            try {thing.close();}
	            catch(IOException e) {}
	        }
	    }

	}
	
	
	
	//----------------  Event Listener Support --------------------------------  
	/**Add a listener to be informed when a new aggregate has arrived.**/
	public void addListener(ArrivalListener l) {listeners.add(l);}
	
	/**Notify all items listening for aggregates here that a new one has arrived.**/
	protected void notifyListeners() {
		ArrivalEvent e = new ArrivalEvent(queue.size());
		for (ArrivalListener l:listeners) {l.arrival(e);}
	}
	
	/**Implement this interface to receive aggreate arrival events (and register implements vai addListener).**/
	public static interface ArrivalListener extends java.util.EventListener {
		public void arrival(ArrivalEvent e);
	}
	
	/**Arrival event type.**/
	public static class ArrivalEvent {
		public final int waiting;
		public ArrivalEvent(int waiting) {this.waiting = waiting;}
	}


	
}
