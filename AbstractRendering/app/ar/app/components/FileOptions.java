package ar.app.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import ar.app.ARApp;

public class FileOptions extends CompoundPanel {
	private static final long serialVersionUID = 1L;
	
	private final ExportAggregates export;
	private final JFileChooser fc = new JFileChooser("./data");
	private final JButton chooseFile = new JButton("Input File");

	private File inputFile=null;
	
	private static final FileFilter CSV_HBIN = new FileFilter() {
		public boolean accept(File pathname) {
			return pathname.isFile() && pathname.getName().toUpperCase().endsWith(".CSV")
					|| pathname.isFile() && pathname.getName().toUpperCase().endsWith(".HBIN");
		}
		public String getDescription() {return "CSV or Binary w/Header (*.csv | *.hbin)";} 
	};
	
	
	public File inputFile() {return inputFile;}
	
	public FileOptions(ARApp parnt) {
		export = new ExportAggregates(parnt);
		this.add(chooseFile);
		this.add(export);
		
		fc.setFileFilter(CSV_HBIN);
		
		final CompoundPanel panel = this;
		chooseFile.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int rc = fc.showOpenDialog(panel);
				if (rc== JFileChooser.APPROVE_OPTION) {
					inputFile = fc.getSelectedFile();
				}
				panel.fireActionListeners();
			}});
	}

}
