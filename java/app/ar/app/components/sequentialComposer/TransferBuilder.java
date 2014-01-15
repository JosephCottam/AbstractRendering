package ar.app.components.sequentialComposer;

import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import ar.Transfer;
import ar.app.util.ActionProvider;
import ar.app.util.AppUtil;
import ar.rules.combinators.Seq;

@SuppressWarnings("rawtypes")
public class TransferBuilder extends JPanel {
	private final ActionProvider actionProvider = new ActionProvider("Transfer Changed");  

	private List<JComboBox<OptionTransfer>> transferLists = new ArrayList<>();
	private List<OptionTransfer.ControlPanel> optionPanels = new ArrayList<>();

	public TransferBuilder() {
		this.setLayout(new GridLayout(0,2));
		addTransferBox();
	}
		
	public void addActionListener(ActionListener l) {actionProvider.addActionListener(l);}
	
	public Transfer<?,?> transfer() {
		int idx = transferLists.get(0).getSelectedIndex();
		Seq t = Seq.start(transferLists.get(0).getItemAt(idx)
							.transfer(optionPanels.get(0)));
		for (int i=1; i<transferLists.size()-1; i++) {
			idx = transferLists.get(i).getSelectedIndex();
			t = t.then(transferLists.get(i).getItemAt(idx).transfer(optionPanels.get(i)));
		}
		return t;
	}
	
	private void rebuild() {
		this.removeAll();
		for (int i=0; i<transferLists.size();i++) {
			this.add(transferLists.get(i));
			this.add(optionPanels.get(i));
		}		
		revalidate();
		if (this.getParent() != null) {this.getParent().revalidate();}
	}
	
	private void addTransferBox() {
		JComboBox<OptionTransfer> transfers = new JComboBox<OptionTransfer>();
		AppUtil.loadInstances(transfers, OptionTransfer.class, OptionTransfer.class, OptionTransfer.Echo.NAME);
		transfers.addItemListener(new ChangeTransfer(this));
		transfers.addActionListener(actionProvider.actionDelegate());

		transferLists.add(transfers);
		
		OptionTransfer.ControlPanel controls = transfers.getItemAt(transfers.getSelectedIndex()).control(null);
		optionPanels.add(controls);
		controls.addActionListener(actionProvider.actionDelegate());
		rebuild();
	}
	
	public static class ChangeTransfer implements ItemListener {
		final TransferBuilder host;
		
		public ChangeTransfer(TransferBuilder host) {
			this.host = host;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public void itemStateChanged(ItemEvent e) {
			int size = host.transferLists.size();
			JComboBox<OptionTransfer> transferList = (JComboBox<OptionTransfer>) e.getSource();
			int idx = host.transferLists.indexOf(transferList);
			boolean end = e.getItem().toString().equals(OptionTransfer.Echo.NAME);
			
			if (idx < size-1 && end) {
				host.transferLists.remove(idx);
				host.optionPanels.remove(idx);
				host.rebuild();
			} else if (idx == size-1 && !end) {				
				host.addTransferBox();
			} else {
				OptionTransfer.ControlPanel params = transferList.getItemAt(transferList.getSelectedIndex()).control(null);
				host.optionPanels.remove(idx);
				host.optionPanels.add(idx, params);
				params.addActionListener(host.actionProvider.actionDelegate());
				host.rebuild();
			}
			
		}
		
	}
}
