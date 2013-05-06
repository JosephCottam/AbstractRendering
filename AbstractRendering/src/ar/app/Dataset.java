package ar.app;

import ar.GlyphSet;
import ar.app.util.CSVtoGlyphSet;
import ar.app.util.CharityNetLoader;

public abstract class Dataset {
	private final String name;
	private GlyphSet glyphs;

	protected Dataset(String name) {
		this.name = name;
		try {glyphs = load();}
		catch (Exception e) {
			glyphs = null;
			System.err.printf("Error loading data for %s\n", name);
			e.printStackTrace();
		} 
	}

	public String toString() {
		if (glyphs == null) {return name + " (Load Failed)";}
		else {return name;}
	}

	public GlyphSet glyphs() {return glyphs;}
	protected abstract GlyphSet load();

//
//	public static final class FlowersGlyphs extends Dataset {
//		public FlowersGlyphs() {super("Flowerpoints");}
//		protected GlyphSet load() {
//			System.out.print("Loading Anderson's Flower Scatterplot...");
//			return CSVtoGlyphSet.load("./data/flowers.csv", 1, 1, true, 0, 1,2);
//		}
//	}
//
//	public static final class Sourceforge extends Dataset {
//		public Sourceforge() {super("Sourceforge (VxOrd layout)");}
//		protected GlyphSet load() {
//			System.out.print("Loading Sourceforge layout...");
//			return CSVtoGlyphSet.load("./data/sourceforge.csv", 1, .05, false, 1, 2,-1);
//		}
//	}
//
//	public static final class SyntheticScatterplot extends Dataset {
//		public SyntheticScatterplot() {super("Scatterplot (Synthetic)");}
//		protected GlyphSet load() {
//			System.out.print("Loading Sythetic Scatterplot...");
//			return CSVtoGlyphSet.load("./data/circlepoints.csv", 1, .1, false, 2, 3,-1);
//		}
//	}
//
//	public static final class Checkers extends Dataset{
//		public Checkers() {super("Checkers");}
//		protected GlyphSet load() {
//			System.out.print("Loading " + super.name + "...");
//			return CSVtoGlyphSet.load("./data/checkerboard.csv", 2, 1, false, 0,1,2);
//		}
//	}

	public static final class Wikipedia extends Dataset{
		public Wikipedia() {super("Wikipedia");}
		protected GlyphSet load() {
			System.out.print("Loading " + super.name + "...");
			return CSVtoGlyphSet.load("./data/wiki.subset.txt.fail", 0, .1, false, 0,1,2);
		}
	}
//
//	
//	public static final class CheckersMatrix extends Dataset{
//		public CheckersMatrix() {super("Checkers (Matrix)");}
//		protected GlyphSet load() {
//			System.out.print("Loading " + super.name + "...");
//			return CSVtoGlyphSet.loadMatrix("./data/checkerboard.csv", 1, 1, 0,1,3,-1, new CSVtoGlyphSet.ToInt(), false);
//		}
//	}
//
//	public static final class Memory extends Dataset {
//		public Memory() {super("BGL Memory");}
//		protected GlyphSet load() {
//			System.out.print("Loading BGL Memory...");
//			return CSVtoGlyphSet.load("./data/MemVisScaled.csv", 0, .005, true, 0, 1,2);
//		}
//	}
//	
//
//	public static final class CharityNet extends Dataset {
//		public CharityNet() {super("Charity Net");}
//		protected GlyphSet load() {
//			System.out.println("Loading " + super.name + "...");
//			return CharityNetLoader.load("./data/dateStateXY.csv");
//			//return CharityNetLoader.loadDirect("./data/date_state.csv");
//		}
//	}


//	public static final class MPIPhases extends Dataset {
//		public MPIPhases() {super("MPIPhases");}
//		protected GlyphSet load() {return null;}
//	}
}
