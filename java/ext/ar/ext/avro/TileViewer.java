package ar.ext.avro;

import javax.swing.*;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.function.*;

import org.apache.avro.generic.GenericRecord;

import ar.Aggregates;
import ar.aggregates.AggregateUtils;
import ar.app.display.TransferDisplay;
import ar.rules.General;
import ar.rules.combinators.Combinators;


public class TileViewer extends JPanel{
	
	public <A> TileViewer(File f, String converterName) throws Exception {
		Optional<Class<?>> converterClass = Arrays.stream(Converters.class.getClasses()).filter(c -> c.getSimpleName().equals(converterName)).findFirst();
		Function<GenericRecord, A> converter = (Function<GenericRecord, A>) converterClass.get().newInstance();
		
		Aggregates<A> aggs = AggregateSerializer.deserialize(f, converter);
		
		
		this.setLayout(new BorderLayout());
		JLabel desc = new JLabel(aggs.toString());
		this.add(desc, BorderLayout.SOUTH);
		
		JComponent p = new TransferDisplay(aggs, 
				Combinators.seq(new General.Present<>(Color.RED, Color.LIGHT_GRAY))
							.then(new General.Spread<>(new General.Spread.UnitCircle<Color>(2), 
													(A, B) -> A == Color.LIGHT_GRAY ? B :A, 
													Color.LIGHT_GRAY)));
		this.add(p, BorderLayout.CENTER);
		
		Rectangle aggBounds = AggregateUtils.bounds(aggs);
		this.setPreferredSize(new Dimension(aggBounds.width, aggBounds.height));
		this.setMinimumSize(new Dimension(aggBounds.width, aggBounds.height));
		
	}
	
	
	public static void main(String[] args) throws Exception {
		JFrame f = new JFrame();
		f.add(new TileViewer(new File(args[0]), args[1]));
		
		f.setVisible(true);
		f.setSize(f.getPreferredSize());
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
