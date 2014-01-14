package ar.app.components.sequentialComposer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import ar.Aggregator;
import ar.Glyphset;
import ar.Transfer;
import ar.app.components.LabeledItem;
import ar.app.display.ARComponent;
import ar.app.util.ActionProvider;
import ar.app.util.AppUtil;
import ar.app.util.WrappedTransfer;
import ar.rules.General;
import ar.rules.combinators.Seq;

public class SequentialComposer extends JPanel  {
	private JComboBox<OptionDataset<?,?>> datasets = new JComboBox<>();
	private JComboBox<OptionAggregator<?,?>> aggregators  = new JComboBox<>();
	private List<JComboBox<WrappedTransfer<?,?>>> transfers = new ArrayList<>();
	private final ActionProvider actionProvider = new ActionProvider();  

	static {WrappedTransfer.Echo.NAME = "End (*)";}
	
	public SequentialComposer() {
		AppUtil.loadStaticItems(datasets, OptionDataset.class, OptionDataset.class, "BGL Memory");
		AppUtil.loadStaticItems(aggregators, OptionAggregator.class, OptionAggregator.class, "Count (int)");
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		datasets.addActionListener(new ActionProvider.DelegateListener(actionProvider));
		aggregators.addActionListener(new ActionProvider.DelegateListener(actionProvider));
		
		this.add(new LabeledItem("Dataset:", datasets));
		this.add(new LabeledItem("Aggregator:", aggregators));
		addTransferBox();
	}	
	
	private void addTransferBox() {
		JComboBox<WrappedTransfer<?,?>> transferOptions = new JComboBox<WrappedTransfer<?,?>>();
		AppUtil.loadInstances(transferOptions, WrappedTransfer.class, WrappedTransfer.class, WrappedTransfer.Echo.NAME);
		transfers.add(transferOptions);
		transferOptions.addItemListener(new  ChangeTransfer(this, transfers));
		transferOptions.addActionListener(new ActionProvider.DelegateListener(actionProvider));
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
		
	public void addActionListener(ActionListener l) {actionProvider.addActionListener(l);}
	
	/**Should the display be re-zoomed?  
	 * Returns true when the new glyphset & aggregator is not the same as the old one.**/
	public boolean doZoomWith(ARComponent.Aggregating oldPanel) {
		return oldPanel == null
				|| oldPanel.dataset() != dataset()
				|| !oldPanel.aggregator().equals(aggregator());
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
	
	public Glyphset<?,?> dataset() {return datasets.getItemAt(datasets.getSelectedIndex()).dataset();}
	public Aggregator<?,?> aggregator() {return aggregators.getItemAt(aggregators.getSelectedIndex()).aggregator();}
	
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
		
}
