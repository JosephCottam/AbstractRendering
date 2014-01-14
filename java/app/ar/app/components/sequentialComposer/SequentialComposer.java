package ar.app.components.sequentialComposer;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import ar.Aggregator;
import ar.Glyphset;
import ar.Transfer;
import ar.app.components.LabeledItem;
import ar.app.util.AppUtil;
import ar.app.util.WrappedAggregator;
import ar.app.util.WrappedTransfer;
import ar.glyphsets.implicitgeometry.Indexed;
import ar.glyphsets.implicitgeometry.Valuer;
import ar.glyphsets.implicitgeometry.Indexed.ToValue;
import ar.glyphsets.implicitgeometry.Valuer.Binary;
import ar.rules.CategoricalCounts;
import ar.rules.General;
import ar.rules.combinators.Seq;
import ar.util.Util;

public class SequentialComposer extends JPanel {
	private JComboBox<OptionDataset<?,?>> datasets = new JComboBox<>();
	private JComboBox<Aggregator<?,?>> aggregators  = new JComboBox<>();
	private List<JComboBox<WrappedTransfer<?,?>>> transfers = new ArrayList<>();
	
	static {WrappedTransfer.Echo.NAME = "End (*)";}
	
	public SequentialComposer() {
		AppUtil.loadStaticItems(datasets, SequentialComposer.class, OptionDataset.class, "BGL Memory");
		AppUtil.loadInstances(aggregators, WrappedAggregator.class, WrappedAggregator.class, "Count (int)");
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		this.add(new LabeledItem("Dataset:", datasets));
		this.add(new LabeledItem("Aggregator:", aggregators));
		addTransferBox();
	}	
	
	private void addTransferBox() {
		JComboBox<WrappedTransfer<?,?>> transferOptions = new JComboBox<WrappedTransfer<?,?>>();
		AppUtil.loadInstances(transferOptions, WrappedTransfer.class, WrappedTransfer.class, WrappedTransfer.Echo.NAME);
		transfers.add(transferOptions);
		transferOptions.addItemListener(new  ChangeTransfer(this, transfers));
		this.add(transferOptions);
		if (this.getParent() != null) {this.getParent().revalidate();}
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
		
	public static class ChangeTransfer implements ItemListener {
		final List<JComboBox<WrappedTransfer<?,?>>> listing;
		final SequentialComposer host;
		
		public ChangeTransfer(SequentialComposer host, List<JComboBox<WrappedTransfer<?,?>>> listing) {
			this.listing = listing;
			this.host = host;
		}
		
		@Override
		public void itemStateChanged(ItemEvent e) {
			@SuppressWarnings("unchecked")
			JComboBox<WrappedTransfer<?,?>> source = (JComboBox<WrappedTransfer<?,?>>) e.getSource();
			int idx = listing.indexOf(source);
			boolean end = e.getItem().toString().equals(WrappedTransfer.Echo.NAME);
			if (idx < listing.size()-1 && end) {
				listing.remove(source);
				host.remove(source);
				if (host.getParent() != null) {host.getParent().validate();}
			} else if (idx == listing.size()-1 && !end) {
				host.addTransferBox();
			}
			
		}
		
	}
	
	public Glyphset<?,?> dataset() {return datasets.getItemAt(aggregators.getSelectedIndex()).dataset();}
	public Aggregator<?,?> aggregator() {return aggregators.getItemAt(aggregators.getSelectedIndex());}
	
	public Transfer<?,?> transfer() {
		if (transfers.size() == 1) {return new General.Echo<>(aggregator().identity());}
		
		int idx = transfers.get(0).getSelectedIndex();
		Seq t = Seq.start(transfers.get(0).getItemAt(idx).op());
		for (int i=1; i<transfers.size()-1; i++) {
			idx = transfers.get(i).getSelectedIndex();
			t.then(transfers.get(i).getItemAt(idx).op());
		}
		return t;
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
