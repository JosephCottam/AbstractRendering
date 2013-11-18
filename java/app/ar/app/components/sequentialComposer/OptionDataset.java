package ar.app.components.sequentialComposer;

import java.io.File;

import ar.Glyphset;
import ar.glyphsets.MemMapList;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Shaper;
import ar.glyphsets.implicitgeometry.Valuer;

public class OptionDataset<G,I> {
	final String name;
	final Glyphset<G,I> glyphs;
	public OptionDataset(String name, File file, Shaper<G,Indexed> shaper, Valuer<Indexed,I> valuer) {
		this.name = name;
		glyphs = new MemMapList<>(file, shaper, valuer);
	}
	public Glyphset<G,I> dataset() {return glyphs;}
	public String toString() {return name;}
}