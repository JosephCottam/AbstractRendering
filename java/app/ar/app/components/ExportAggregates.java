package ar.app.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import ar.app.display.ARComponent;
import ar.app.util.AggregatesToJSON;

public class ExportAggregates extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private final JButton export = new JButton("Export Aggregates");
	private final JFileChooser fc = new JFileChooser("../data");

	private File inputFile=null;
	private final ARComponent.Holder holder;
	
	private static final FileFilter CSV = new FileFilter() {
		public boolean accept(File pathname) {
			return pathname.isFile() && pathname.getName().toUpperCase().endsWith(".CSV");
		}
		public String getDescription() {return "Comma separated values (*.csv)";} 
	};
	
	
	public File inputFile() {return inputFile;}
	
	public ExportAggregates(ARComponent.Holder target) {
		this.add(export);
		this.holder = target;
		
		fc.setFileFilter(CSV);
		
		export.addActionListener(new ActionListener() {
			@SuppressWarnings("unused")
			public void actionPerformed(ActionEvent e) {
				 JFileChooser fd = new JFileChooser("Export Aggregates (e.g., reduction results)");
				 fd.setSelectedFile(new File("../TransferJS/aggregates.json"));
				 int returnVal = fd.showDialog(ExportAggregates.this, "Export");
				 if (returnVal == JFileChooser.APPROVE_OPTION) {
					 AggregatesToJSON.export(holder.getARComponent().aggregates(),fd.getSelectedFile());
				 }
			}
		});

	}

}
