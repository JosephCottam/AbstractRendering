package ar.ext.cartogram;

import java.awt.Shape;
import java.io.File;
import java.util.Map;

import ar.Glyphset;
import ar.app.util.GeoJSONTools;

public class Cartogram {

	public static void main(String[] args) throws Exception {
		//Glyphset populationSource = ar.app.components.sequentialComposer.OptionDataset.CENSUS_SYN_PEOPLE.dataset();
		Glyphset populationSource = ar.app.components.sequentialComposer.OptionDataset.CENSUS_TRACTS.dataset();

		File statesSource = new File("../data/maps/USStates/");
		final Map<String, Shape> shapes = GeoJSONTools.flipY(GeoJSONTools.loadShapesJSON(statesSource, false));
		
	}

}
