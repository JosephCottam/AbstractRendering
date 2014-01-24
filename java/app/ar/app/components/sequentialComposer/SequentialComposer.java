package ar.app.components.sequentialComposer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import ar.Aggregator;
import ar.Glyphset;
import ar.Transfer;
import ar.app.components.LabeledItem;
import ar.app.display.ARComponent;
import ar.app.util.ActionProvider;
import ar.app.util.AppUtil;
import ar.util.HasViewTransform;

@SuppressWarnings("rawtypes")
public class SequentialComposer extends JPanel  {
	private final ActionProvider actionProvider = new ActionProvider();  

	private final JComboBox<OptionDataset> datasets = new JComboBox<>();
	private final JComboBox<OptionAggregator> aggregators  = new JComboBox<>();
	private final TransferBuilder transfers;
	private final JButton transferDefaults = new JButton("Defaults");
	
	public SequentialComposer(HasViewTransform transferProvider) {
		transfers = new TransferBuilder(transferProvider);
		AppUtil.loadStaticItems(datasets, OptionDataset.class, OptionDataset.class, "BGL Memory");
		AppUtil.loadStaticItems(aggregators, OptionAggregator.class, OptionAggregator.class, "Count (int)");
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		datasets.addActionListener(actionProvider.actionDelegate());
		//datasets.addItemListener(actionProvider.itemDelegate());
		aggregators.addActionListener(actionProvider.actionDelegate());
		//aggregators.addItemListener(actionProvider.itemDelegate());
		transfers.addActionListener(actionProvider.actionDelegate());
		
		JPanel ds = new JPanel();
		ds.setLayout(new BorderLayout());
		ds.add(datasets, BorderLayout.CENTER);
		ds.add(transferDefaults, BorderLayout.EAST);
		
		this.add(new LabeledItem("Dataset:", ds));
		this.add(new LabeledItem("Aggregator:", aggregators));
		this.add(new LabeledItem("Transfer:", transfers));
		
		transferDefaults.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OptionDataset od = datasets.getItemAt(datasets.getSelectedIndex());
				aggregators.setSelectedItem(od.defaultAggregator());
				transfers.configureTo(od.defaultTransfers());
				actionProvider.fireActionListeners();
			}
		});
		
	}	
	
	public void addActionListener(ActionListener l) {actionProvider.addActionListener(l);}
	
	/**Should the display be re-zoomed?  
	 * Returns true when the new glyphset & aggregator is not the same as the old one.**/
	public boolean doZoomWith(ARComponent.Aggregating oldPanel) {
		return oldPanel == null
				|| oldPanel.dataset() != dataset()
				|| !oldPanel.aggregator().equals(aggregator());
	}
	
	public Glyphset<?,?> dataset() {return datasets.getItemAt(datasets.getSelectedIndex()).dataset();}
	public Aggregator<?,?> aggregator() {return aggregators.getItemAt(aggregators.getSelectedIndex()).aggregator();}
	public Transfer<?,?> transfer() {return transfers.transfer();}
}
