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
import ar.app.display.ARComponent;
import ar.app.util.ActionProvider;
import ar.app.util.AppUtil;
import ar.app.util.LabeledItem;
import ar.util.HasViewTransform;

@SuppressWarnings("rawtypes")
public class SequentialComposer extends JPanel  {
	private final ActionProvider actionProvider = new ActionProvider();  

	private final JComboBox<OptionDataset> datasets = new JComboBox<>();
	private final JComboBox<OptionAggregator> aggregators  = new JComboBox<>();
	private final TransferBuilder transferBuilder;
	private final JButton transferDefaults = new JButton("Defaults");
	
	public SequentialComposer(HasViewTransform transformProvider) {
		transferBuilder = new TransferBuilder(transformProvider);
		AppUtil.loadStaticItems(datasets, OptionDataset.class, OptionDataset.class, "BGL Memory");
		AppUtil.loadStaticItems(aggregators, OptionAggregator.class, OptionAggregator.class, "Count (int)");
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		datasets.addActionListener(actionProvider.actionDelegate());
		aggregators.addActionListener(actionProvider.actionDelegate());
		transferBuilder.addActionListener(actionProvider.actionDelegate());
		
		JPanel ds = new JPanel();
		ds.setLayout(new BorderLayout());
		ds.add(datasets, BorderLayout.CENTER);
		ds.add(transferDefaults, BorderLayout.EAST);
		
		this.add(new LabeledItem("Dataset:", ds));
		this.add(new LabeledItem("Aggregator:", aggregators));
		this.add(transferBuilder);
		
		transferDefaults.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) {transferDefaults();}});
		transferDefaults();
		
	}	

	public void transferDefaults() {
		OptionDataset od = datasets.getItemAt(datasets.getSelectedIndex());
		aggregators.setSelectedItem(od.defaultAggregator());
		transferBuilder.configureTo(od.defaultTransfers());
		actionProvider.fireActionListeners();
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
	public Transfer<?,?> transfer() {return transferBuilder.transfer();}
}
