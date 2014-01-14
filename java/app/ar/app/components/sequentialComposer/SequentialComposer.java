package ar.app.components.sequentialComposer;

import java.awt.event.ActionListener;

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

@SuppressWarnings("rawtypes")
public class SequentialComposer extends JPanel  {
	private final JComboBox<OptionDataset> datasets = new JComboBox<>();
	private final JComboBox<OptionAggregator> aggregators  = new JComboBox<>();
	private final TransferBuilder transfers = new TransferBuilder();
	private final ActionProvider actionProvider = new ActionProvider();  

	static {OptionTransfer.Echo.NAME = "End (*)";}
	
	public SequentialComposer() {
		AppUtil.loadStaticItems(datasets, OptionDataset.class, OptionDataset.class, "BGL Memory");
		AppUtil.loadStaticItems(aggregators, OptionAggregator.class, OptionAggregator.class, "Count (int)");
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		datasets.addActionListener(new ActionProvider.DelegateListener(actionProvider));
		aggregators.addActionListener(new ActionProvider.DelegateListener(actionProvider));
		transfers.addActionListener(new ActionProvider.DelegateListener(actionProvider));
		
		this.add(new LabeledItem("Dataset:", datasets));
		this.add(new LabeledItem("Aggregator:", aggregators));
		this.add(new LabeledItem("Transfer:", transfers));
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
