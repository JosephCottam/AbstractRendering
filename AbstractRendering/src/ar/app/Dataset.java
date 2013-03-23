package ar.app;

import ar.GlyphSet;
import ar.app.util.CSVtoGlyphSet;

public abstract class Dataset {
	private final String name;
	private GlyphSet glyphs;
	
	protected Dataset(String name) {
		this.name = name;
		glyphs = load();
	}

	public String toString() {return name;}
	public GlyphSet glyphs() {return glyphs;}
	protected abstract GlyphSet load();
	
	public static final class Memory extends Dataset {
		public Memory() {super("BGL Memory");}
		protected GlyphSet load() {return null;}
	}
	
	public static final class MPIPhases extends Dataset {
		public MPIPhases() {super("MPIPhases");}
		protected GlyphSet load() {return null;}
	}
	
	public static final class SyntheticScatterplot extends Dataset {
		public SyntheticScatterplot() {super("Scatterplot (Synthetic)");}
		protected GlyphSet load() {
			return CSVtoGlyphSet.load("./data/circlepoints.csv", 1, .1, 2, 3,-1);
		}
	}
	
	public static final class Checkers extends Dataset{
		public Checkers() {super("Checkers");}
		protected GlyphSet load() {
			return CSVtoGlyphSet.load("./data/checkerboard.csv", 1, 1, 0,1,2);
		}
	}
}
