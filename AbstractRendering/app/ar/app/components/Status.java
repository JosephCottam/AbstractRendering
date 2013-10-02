package ar.app.components;

import java.awt.BorderLayout;
import javax.swing.*;

import ar.Renderer;

public class Status extends JPanel {
	private static final long serialVersionUID = -3243629269704290121L;
	private final JProgressBar progress = new JProgressBar();
	private final Thread t = new Thread(new Monitor(), "Status updater");
	private Renderer watching;

	public Status() {
		
		this.add(new LabeledItem("Aggregate Creation:", progress), BorderLayout.CENTER);
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
	
	public void setStatus(double status, String message) {
		status = Math.max(status, 0);
		progress.setValue((int) (status*100));
		progress.setString(message);
	}
	
	private class Monitor implements Runnable {
		private double cached;
		public void run() {
			while (true) {
				try {Thread.sleep(50);}
				catch (InterruptedException e) {
					SwingUtilities.invokeLater(new UpdateTask(-1, "Interrupted monitoring"));
					return;
				}

				if (watching == null) {continue;}
				double progress = watching.progress();
				if (progress == cached) {continue;}
				cached = progress;
				SwingUtilities.invokeLater(new UpdateTask(progress, null));
			}
		}
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
