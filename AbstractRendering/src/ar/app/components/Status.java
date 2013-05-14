package ar.app.components;

import java.awt.GridLayout;

import javax.swing.*;

import ar.Renderer;

public class Status extends JPanel {
	private static final long serialVersionUID = -3243629269704290121L;
	private Monitor monitor;
	private JProgressBar progress = new JProgressBar();
	private Renderer watching;
	private final Thread t = new Thread(new Monitor(), "Status updater");

	public Status() {
		setLayout(new GridLayout(2,1));
		this.add(new LabeledItem("Rendering:", progress));
		progress.setMaximum(100);
		progress.setMinimum(0);
		progress.setStringPainted(true);

		t.setDaemon(true);
		t.start();
	}
	
	public void startMonitoring(Renderer renderer) {
		progress.setString("Starting...");
		this.watching = renderer;
	}
	
	public void stopMonitoring() {
		if (monitor != null) {monitor.signalStop();}
	}
	
	public void setStatus(double status, String message) {
		status = Math.max(status, 0);
		progress.setValue((int) (status*100));
		progress.setString(message);
	}
	
	private class Monitor implements Runnable {
		private boolean stop=false;
		
		public void run() {
			while (!stop) {
				try {Thread.sleep(100);}
				catch (InterruptedException e) {
					SwingUtilities.invokeLater(new UpdateTask(-1, "Interrupted monitoring"));
					return;
				}

				if (watching == null) {continue;}
				
				double progress = watching.progress();
				SwingUtilities.invokeLater(new UpdateTask(progress, null));
			}
		}
		
		public void signalStop() {stop=true;}
	}
	
	private class UpdateTask implements Runnable {
		private final double status;
		private final String message;
		
		public UpdateTask(double status, String message) {
			this.status=status; 
			this.message = message;
		}
		
		public void run() {
			Status.this.setStatus(status, message);
		}
	}
	
}
