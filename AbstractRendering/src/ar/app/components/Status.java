package ar.app.components;

import java.awt.GridLayout;

import javax.swing.*;

import ar.Renderer;

public class Status extends JPanel {
	private static final long serialVersionUID = -3243629269704290121L;
	private Monitor monitor;
	private JProgressBar progress = new JProgressBar();
	private volatile int current=0;

	public Status() {
		setLayout(new GridLayout(2,1));
		this.add(new LabeledItem("Rendering:", progress));
		progress.setMaximum(100);
		progress.setMinimum(0);
		progress.setStringPainted(true);
	}
	
	public void startMonitoring(Renderer renderer) {
		stopMonitoring();
		progress.setString("Starting...");
		Thread t = new Thread(new Monitor(this, ++current, renderer));
		t.start();
	}
	
	public void stopMonitoring() {
		if (monitor != null) {monitor.signalStop();}
	}
	
	public void setStatus(int id, double status, String message) {
		if (id != this.current) {return;}
		status = Math.max(status, 0);
		progress.setValue((int) (status*100));
		progress.setString(message);
	}
	
	private static class Monitor implements Runnable {
		private final Renderer watching;
		private final Status target;
		private final int id;
		private boolean stop=false;
		public Monitor(Status target, int id, Renderer watching) {
			this.watching = watching;
			this.target = target;
			this.id = id;
		}
		
		public void run() {
			while (!stop) {
				double progress = watching.progress();
				SwingUtilities.invokeLater(new UpdateTask(target, id, progress, null));
				try {Thread.sleep(100);}
				catch (InterruptedException e) {
					SwingUtilities.invokeLater(new UpdateTask(target, id, -1, "Interrupted monitoring"));
					return;
				}
				new UpdateTask(target, id, -1, "Render complete");
			}
		}
		
		public void signalStop() {stop=true;}
	}
	
	private static class UpdateTask implements Runnable {
		private final Status target;
		private final double status;
		private final String message;
		private final int id;
		
		public UpdateTask(Status target, int id,  double status, String message) {
			this.target=target; 
			this.id =id;
			this.status=status; 
			this.message = message;
		}
		
		public void run() {
			target.setStatus(id, status, message);
		}
	}
	
}
