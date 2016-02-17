package com.me3tweaks.modmanager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.me3tweaks.modmanager.objects.ModType;

public class ModImportWindow extends JDialog implements ListSelectionListener {

	DefaultListModel<String> model = new DefaultListModel<>();
	JList<String> dlcModlist = new JList<String>(model);
	private String biogameDir;
	private JButton importButton;
	protected boolean reloadWhenClosed;

	public ModImportWindow(JFrame callingWindow, String biogameDir) {
    	ModManager.debugLogger.writeMessage("Opening ModImportWindow (DLC Import)");

		this.biogameDir = biogameDir;
		setupWindow(callingWindow);
		setVisible(true);
	}

	private void setupWindow(JFrame callingWindow) {
		setTitle("Imports installed mods into Mod Manager");
		setMinimumSize(new Dimension(300, 300));
		setIconImages(ModManager.ICONS);

		addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent e) {
		    	ModManager.debugLogger.writeMessage("Closing ModImportWindow. Should we reload? "+reloadWhenClosed);
		        if (reloadWhenClosed) {
		        	ModManagerWindow.ACTIVE_WINDOW = new ModManagerWindow(false); //reload
		        }
		    }
		});
		
		File mainDlcDir = new File(ModManager.appendSlash(biogameDir) + "DLC" + File.separator);
		String[] directories = mainDlcDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});

		for (String dir : directories) {
			if (ModType.isKnownDLCFolder(dir)) {
				continue;
			}
			//add to list
			model.addElement(dir);
		}
		
		dlcModlist.addListSelectionListener(this);
		dlcModlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JPanel panel = new JPanel(new BorderLayout());

		JLabel infoHeader = new JLabel(
				"<html>Import already-installed mods into Mod Manager to<br>install or uninstall them quickly and easily.</html>");
		panel.add(infoHeader, BorderLayout.NORTH);

		importButton = new JButton("Import");
		importButton.setEnabled(false);
		importButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String folder = mainDlcDir.getAbsolutePath() + File.separator + model.get(dlcModlist.getSelectedIndex());
				ImportEntryWindow iew = new ImportEntryWindow(ModImportWindow.this, folder);
				if (iew.getResult() == ImportEntryWindow.OK){
					reloadWhenClosed = true;
				}
			}
		});
		JPanel cPanel = new JPanel(new BorderLayout());
		if (model.getSize() > 0) {
			JScrollPane scroll = new JScrollPane(dlcModlist, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			cPanel.add(scroll, BorderLayout.CENTER);
		} else {
			cPanel.add(new JLabel("No custom DLC mods are installed", SwingConstants.CENTER));
		}
		
		cPanel.setBorder(new TitledBorder(new EtchedBorder(), "Installed Custom DLC mods"));
		panel.add(cPanel, BorderLayout.CENTER);

		panel.add(importButton, BorderLayout.SOUTH);
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(panel);
		pack();
		setLocationRelativeTo(callingWindow);
	}
	
	@Override
	public void valueChanged(ListSelectionEvent listChange) {
		if (listChange.getValueIsAdjusting() == false) {
			if (dlcModlist.getSelectedIndex() == -1) {
				importButton.setEnabled(false);
			} else {
				importButton.setEnabled(true);
			}
		}
	}
}
