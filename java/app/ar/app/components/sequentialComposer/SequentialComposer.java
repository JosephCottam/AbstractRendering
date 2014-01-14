package ar.app.components.sequentialComposer;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import ar.Aggregator;
import ar.app.util.AppUtil;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.glyphsets.implicitgeometry.Indexed.ToValue;
import ar.glyphsets.implicitgeometry.Valuer.Binary;
import ar.rules.CategoricalCounts;
import ar.util.Util;

public class SequentialComposer extends JPanel {
	private JComboBox<OptionDataset<?,?>> datasets = new JComboBox<>();
	private JComboBox<Aggregator<?,?>> aggregators  = new JComboBox<>();
	private List<JComboBox<WrappedTransfer<?,?>>> transfers = new ArrayList<>();
	
	public SequentialComposer() {
		AppUtil.loadStaticItems(datasets, SequentialComposer.class, OptionDataset.class, "BGL Memory");
		AppUtil.loadInstances(aggregators, WrappedAggregator.class, WrappedAggregator.class, "Count (int)");
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		this.add(datasets);
		this.add(aggregators);
	}	
	
	private void addTransferBox() {
		JComboBox<WrappedTransfer<?,?>> transferOptions = new JComboBox<WrappedTransfer<?,?>>();
		AppUtil.loadInstances(transferOptions, WrappedTransfer.class, WrappedTransfer.class, "10% Alpha (int)");
		transfers.add(transferOptions);
		this.add(transferOptions);
		this.validate();
	}
	
	private void clearTransfers() {
		for (JComboBox<WrappedTransfer<?,?>> transfer: transfers) {
			this.remove(transfer);
		}
		transfers.clear();
		this.validate();
	}
	
	public static class TransferGrow implements ActionListener {
		final SequentialComposer target;
		public TransferGrow(SequentialComposer target) {this.target = target;}
		public void actionPerformed(ActionEvent e) {target.addTransferBox();}
	}
	
	public static class TransferClear implements ActionListener {
		final SequentialComposer target;
		public TransferClear(SequentialComposer target) {this.target = target;}
		public void actionPerformed(ActionEvent e) {target.clearTransfers();}
	}
	
	public static class TransferClean implements ActionListener {
		final SequentialComposer target;
		public TransferClean(SequentialComposer target) {this.target = target;}
		public void actionPerformed(ActionEvent e) {
			target.addTransferBox();
		}
	}
		
	public static OptionDataset<Point2D, Color> BOST_MEMORY = new OptionDataset<> (
					"BGL Memory", 
					new File("../data/MemVisScaled.hbin"), 
					new Indexed.ToPoint(true, 0, 1),
					new ToValue<>(2, new Binary<Integer,Color>(0, Color.BLUE, Color.RED)));
	
	public static OptionDataset<Point2D, CategoricalCounts<String>> CENSUS_TRACTS = new OptionDataset<>(
			"US Census Tracts", 
			new File("../data/2010Census_RaceTract.hbin"), 
			new Indexed.ToPoint(true, 0, 1),
			new Valuer.CategoryCount<>(new Util.ComparableComparator<String>(), 3,2));
	
	public static OptionDataset<Point2D, Character> CENSUS_SYN_PEOPLE = new OptionDataset<>(
			"US Census Synthetic People", 
			new File("../data/2010Census_RacePersonPoints.hbin"), 
			new Indexed.ToPoint(true, 0, 1),
			new Indexed.ToValue<Indexed,Character>(2));
	
	public static OptionDataset<Point2D, Color> WIKIPEDIA = new OptionDataset<>(
			"Wikipedia BFS adjacnecy", 
			new File("../data/wiki-adj.hbin"), 
			new Indexed.ToPoint(false, 0, 1),
			new Valuer.Constant<Indexed, Color>(Color.RED));
	
	public static OptionDataset<Point2D, Color> KIVA = new OptionDataset<>(
			"Kiva", 
			new File("../data/kiva-adj.hbin"),
			new Indexed.ToPoint(false, 0, 1),
			new Valuer.Constant<Indexed, Color>(Color.RED)); 
}
