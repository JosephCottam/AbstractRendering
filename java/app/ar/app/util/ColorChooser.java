package ar.app.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;

import ar.app.components.LabeledItem;

public final class ColorChooser extends JPanel {
	private Color current;
	private final String varName;
	private final JButton button = new ColorButton();
	private final ActionProvider actionProvider;

	public ColorChooser(Color initial, String varName) {
		this.varName =varName;
		this.actionProvider = new ActionProvider("ColorChange-"+varName);
		this.add(new LabeledItem(varName, button));
		
		setColor(initial);
		
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JColorChooser chooser = new JColorChooser(current);
				
				chooser.setPreviewPanel(new JPanel());
				JDialog d = JColorChooser.createDialog(ColorChooser.this, ColorChooser.this.varName, true, chooser, 
						new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								ColorChooser.this.setColor(chooser.getColor());
							}
						},
						new ActionListener() {
							@Override public void actionPerformed(ActionEvent e) {}
						});
				d.setVisible(true);
			}
		});
	}
	
	private void setColor(Color target) {
		current = target;
		button.setForeground(target);
		button.repaint();
		actionProvider.fireActionListeners();
	}
	
	public Color color() {return button.getForeground();}
	
	public void addActionListener(ActionListener l) {actionProvider.addActionListener(l);}
	
	public class ColorButton extends JButton {
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Insets ins = this.getInsets();
			
			g.setColor(Color.black);
			g.drawRect(ins.left-1, 2*ins.top-1, this.getWidth()-(ins.left+ins.right)+1, this.getHeight()-(2*(ins.top+ins.bottom))+1);
			g.setColor(this.getForeground());
			g.fillRect(ins.left, 2*ins.top, this.getWidth()-(ins.left+ins.right), this.getHeight()-(2*(ins.top+ins.bottom)));
		}
	}
}
