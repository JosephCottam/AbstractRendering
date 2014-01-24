package ar.app.components.sequentialComposer;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ar.Transfer;
import ar.app.util.ActionProvider;
import ar.app.util.AppUtil;
import ar.util.HasViewTransform;

@SuppressWarnings("rawtypes")
public class TransferBuilder extends JPanel {
	private final ActionProvider actionProvider = new ActionProvider("Transfer Changed");  
	private final List<TransferRow> transferRows = new ArrayList<>();
	private final JPanel center = new JPanel();
	private final HasViewTransform transformProvider;

	public TransferBuilder(HasViewTransform transferProvider) {
		this.transformProvider = transferProvider;
		this.setLayout(new BorderLayout());
		addTransferRow();
		
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
		
		this.add(center, BorderLayout.CENTER);

		

		
		JPanel sidebar = new JPanel();
		sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
		sidebar.add(new JLabel("Transfers:"));

		JButton add = new JButton("+");
		sidebar.add(add);
		add.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				TransferBuilder.this.addTransferRow();
			}
			
		});

		
		
		this.add(sidebar, BorderLayout.WEST);
		
	}
		
	public void addActionListener(ActionListener l) {actionProvider.addActionListener(l);}
	
	public void configureTo(final List<OptionTransfer> transfers) {
		for (TransferRow tr: transferRows) {center.remove(tr);}
		transferRows.clear();
		
		for (int i=0; i<transfers.size(); i++) {
			TransferRow tr = addTransferRow();
			tr.setTransfer(transfers.get(i));
		}
	}
	
	public Transfer<?,?> transfer() {
		List<OptionTransfer> transfers = new ArrayList<>();
		List<OptionTransfer.ControlPanel> panels = new ArrayList<>(); 
		for (TransferRow tr: transferRows) {
			transfers.add(tr.transfer());
			panels.add(tr.controls);
		}
		return OptionTransfer.toTransfer(transfers, panels);
	}
	
	private TransferRow addTransferRow() {
		TransferRow tr = new TransferRow(transformProvider);
		transferRows.add(tr);
		center.add(tr);
		tr.addActionListener(actionProvider.actionDelegate());
		tr.addRemoveListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				TransferRow tr = (TransferRow) e.getSource();
				transferRows.remove(tr);
				TransferBuilder.this.center.remove(tr);
				TransferBuilder.this.revalidate();
				TransferBuilder.this.actionProvider.fireActionListeners();
			}
		});
		revalidate();
		
		return tr;
	}
	
	public static final class TransferRow extends JPanel {
		private final JComboBox<OptionTransfer> transfers = new JComboBox<OptionTransfer>();
		private final JPanel center = new JPanel(new GridLayout(1,0));
		private OptionTransfer.ControlPanel controls;

		private final ActionProvider actionProvider = new ActionProvider();
		private final ActionProvider removeProvider = new ActionProvider(this, "Remove");
		private final HasViewTransform transferProvider;
		
		public TransferRow(HasViewTransform transferProvider) {
			this.transferProvider = transferProvider;
			AppUtil.loadInstances(transfers, OptionTransfer.class, OptionTransfer.class, "");
			transfers.addActionListener(new ChangeTransfer(this));
			
			this.setLayout(new BorderLayout());
			
			JLabel remove = new JLabel(" X ");
			remove.addMouseListener(new MouseListener() {
				@Override public void mouseClicked(MouseEvent e) {
					TransferRow.this.removeProvider.fireActionListeners();
				}
				@Override public void mousePressed(MouseEvent e) {}
				@Override public void mouseReleased(MouseEvent e) {}
				@Override public void mouseEntered(MouseEvent e) {}
				@Override public void mouseExited(MouseEvent e) {}
			});
			this.add(remove, BorderLayout.WEST);
			
			center.add(transfers);
			this.add(center, BorderLayout.CENTER);
			refreshControls();
		}
				
		public void setTransfer(OptionTransfer ot) {
			transfers.setSelectedItem(ot);
			refreshControls();
		}
		
		public OptionTransfer transfer() {return transfers.getItemAt(transfers.getSelectedIndex());} 
		
		public void refreshControls() {
			if (controls != null) {center.remove(controls);}  ///Remove, already present
			this.controls = transfer().control(transferProvider);
			center.add(controls);
			controls.addActionListener(actionProvider.actionDelegate());
			revalidate();
		}
		
		public void fireActionEvent(String command) {actionProvider.fireActionListeners(command);}
		
		public void addActionListener(ActionListener listener) {actionProvider.addActionListener(listener);}
		public void addRemoveListener(ActionListener listener) {removeProvider.addActionListener(listener);}
	}
	
	public static final class ChangeTransfer implements ActionListener {
		final TransferRow host;
		
		public ChangeTransfer(TransferRow host) {this.host = host;}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			host.refreshControls();
			host.fireActionEvent("Modified");
		}
	}
}
