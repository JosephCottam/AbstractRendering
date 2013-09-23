package ar.util;

import ar.Renderer;

/**Debugging utility for reporting render progress to the console.**/ 
public class TextProgress extends Thread {
	private final Renderer renderer;
	
	@SuppressWarnings("javadoc")
	public TextProgress(Renderer r) {
		this.renderer = r;
		this.setDaemon(true);
	}

	public void run() {
		while (renderer.progress()<1) {
			System.out.println(renderer.progress());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
		System.out.println("Done!");		
	}
}
