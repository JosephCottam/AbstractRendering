package ar.app.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import ar.app.PanelHolder;
import ar.app.util.AggregatesToJSON;

public class ExportAggregates extends CompoundPanel {
	private static final long serialVersionUID = 1L;
	
	private final JButton export = new JButton("Export Aggregates");
	private final JFileChooser fc = new JFileChooser("./data");

	private File inputFile=null;
	private final PanelHolder container;
	
	private static final FileFilter CSV = new FileFilter() {
		public boolean accept(File pathname) {
			return pathname.isFile() && pathname.getName().toUpperCase().endsWith(".CSV");
		}
		public String getDescription() {return "Comma separated values (*.csv)";} 
	};
	
	
	public File inputFile() {return inputFile;}
	
	public ExportAggregates(PanelHolder containing) {
		this.add(export);
		this.container = containing;
		
		fc.setFileFilter(CSV);
		
		final CompoundPanel panel = this;

		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				 JFileChooser fd = new JFileChooser("Export Aggregates (e.g., reduction results)");
				 fd.setSelectedFile(new File("../TransferJS/aggregates.json"));
				 int returnVal = fd.showDialog(panel, "Export");
				 if (returnVal == JFileChooser.APPROVE_OPTION) {
					 AggregatesToJSON.export(container.getPanel().aggregates(),fd.getSelectedFile());
				 }
			}
		});

	}

}
