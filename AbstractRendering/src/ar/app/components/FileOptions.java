package ar.app.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import ar.app.ARApp;
import ar.app.util.AggregatesToJSON;

public class FileOptions extends CompoundPanel {
	private static final long serialVersionUID = 1L;
	
	private final JButton export = new JButton("Export Aggregates");
	private final JFileChooser fc = new JFileChooser("./data");
	private final JButton chooseFile = new JButton("Input File");

	private File inputFile=null;
	private final ARApp parent;
	
	private static final FileFilter CSV = new FileFilter() {
		public boolean accept(File pathname) {
			return pathname.isFile() && pathname.getName().toUpperCase().endsWith(".CSV");
		}
		public String getDescription() {return "Comma separated values (*.csv)";} 
	};
	
	
	public File inputFile() {return inputFile;}
	
	public FileOptions(ARApp parnt) {
		this.add(chooseFile);
		this.add(export);
		this.parent = parnt;
		
		fc.setFileFilter(CSV);
		
		final CompoundPanel panel = this;
		chooseFile.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int rc = fc.showOpenDialog(panel);
				if (rc== JFileChooser.APPROVE_OPTION) {
					inputFile = fc.getSelectedFile();
				}
				panel.fireActionListeners();
			}});

		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				 JFileChooser fd = new JFileChooser("Export Aggregates (e.g., reduction results)");
				 fd.setSelectedFile(new File("../TransferJS/aggregates.json"));
				 int returnVal = fd.showDialog(panel, "Export");
				 if (returnVal == JFileChooser.APPROVE_OPTION) {
					 AggregatesToJSON.export(parent.getImage().getAggregates(),fd.getSelectedFile());
				 }
			}
		});

	}

}
