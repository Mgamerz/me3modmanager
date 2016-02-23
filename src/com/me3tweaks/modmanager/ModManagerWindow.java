package com.me3tweaks.modmanager;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.me3tweaks.modmanager.help.HelpMenu;
import com.me3tweaks.modmanager.modmaker.ModMakerCompilerWindow;
import com.me3tweaks.modmanager.modmaker.ModMakerEntryWindow;
import com.me3tweaks.modmanager.modupdater.AllModsUpdateWindow;
import com.me3tweaks.modmanager.modupdater.ModUpdateWindow;
import com.me3tweaks.modmanager.modupdater.ModXMLTools;
import com.me3tweaks.modmanager.modupdater.UpdatePackage;
import com.me3tweaks.modmanager.objects.Mod;
import com.me3tweaks.modmanager.objects.ModDelta;
import com.me3tweaks.modmanager.objects.ModJob;
import com.me3tweaks.modmanager.objects.Patch;
import com.me3tweaks.modmanager.objects.RestoreMode;
import com.me3tweaks.modmanager.repairdb.BasegameHashDB;
import com.me3tweaks.modmanager.utilities.EXEFileInfo;
import com.me3tweaks.modmanager.utilities.MD5Checksum;
import com.me3tweaks.modmanager.utilities.ResourceUtils;
import com.me3tweaks.modmanager.valueparsers.bioai.BioAIGUI;
import com.me3tweaks.modmanager.valueparsers.biodifficulty.DifficultyGUI;
import com.me3tweaks.modmanager.valueparsers.consumable.ConsumableGUI;
import com.me3tweaks.modmanager.valueparsers.powercustomaction.PowerCustomActionGUI;
import com.me3tweaks.modmanager.valueparsers.powercustomaction.PowerCustomActionGUI2;
import com.me3tweaks.modmanager.valueparsers.wavelist.WavelistGUI;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

@SuppressWarnings("serial")
public class ModManagerWindow extends JFrame implements ActionListener, ListSelectionListener {
	public static ModManagerWindow ACTIVE_WINDOW;
	boolean isUpdate;
	public JTextField fieldBiogameDir;
	JTextArea fieldDescription;
	JScrollPane scrollDescription;
	JButton buttonBioGameDir, buttonApplyMod, buttonStartGame;
	JFileChooser dirChooser;
	JMenuBar menuBar;
	JMenu actionMenu, modMenu, modManagementMenu, modDeltaMenu, toolsMenu, backupMenu, restoreMenu, sqlMenu, helpMenu, openToolMenu;
	JMenuItem actionModMaker, actionVisitMe, modManagementImportFromArchive, modManagementImportAlreadyInstalled, actionOptions, toolME3Explorer, actionReload, actionExit;
	JMenuItem modutilsHeader, modutilsInfoEditor, modNoDeltas, modutilsVerifyDeltas, modutilsInstallCustomKeybinds, modutilsAutoTOC, modutilsCheckforupdate;
	JMenuItem backupBackupDLC, backupCreateGDB;
	JMenuItem modManagementModMaker, toolsMergeMod, modManagementPatchLibary, toolsOpenME3Dir, toolsInstallLauncherWV, toolsInstallBinkw32, toolsUninstallBinkw32, toolsMountdlcEditor;
	JMenuItem restoreSelective, restoreRevertEverything, restoreDeleteUnpacked, restoreRevertBasegame, restoreRevertAllDLC, restoreRevertSPDLC, restoreRevertMPDLC,
			restoreRevertMPBaseDLC, restoreRevertSPBaseDLC, restoreRevertCoal, restoreVanillifyDLC;
	JMenuItem sqlWavelistParser, sqlDifficultyParser, sqlAIWeaponParser, sqlPowerCustomActionParser, sqlPowerCustomActionParser2, sqlConsumableParser, sqlGearParser;
	JList<Mod> modList;
	JProgressBar progressBar;
	ListSelectionModel listSelectionModel;
	JSplitPane splitPane;
	public JLabel labelStatus;
	final String selectAModDescription = "Select a mod on the left to view its description.";
	DefaultListModel<Mod> modModel;
	private ArrayList<Patch> patchList;
	private JMenuItem modutilsUpdateXMLGenerator;
	private JMenuItem modManagementCheckallmodsforupdate;
	private JMenuItem restoreRevertUnpacked;
	private JMenuItem restoreRevertBasegameUnpacked;
	private JMenuItem restoredeleteAllCustomDLC;
	private JMenuItem restoreCustomDLCManager;
	private JMenuItem backupBasegameUnpacked;
	private JMenuItem toolsUnpackDLC;
	private JMenuItem modDeltaRevert;
	private JMenuItem toolTankmasterCoalFolder;
	private JMenuItem toolTankmasterCoalUI;
	private JMenuItem toolTankmasterTLK;
	private JMenuItem modManagementOpenModsFolder;
	private JMenu mountMenu;
	private JButton modWebsiteLink;

	/**
	 * Opens a new Mod Manager window. Disposes of old ones if one is open.
	 * Automatically makes window visible so it will block the UI thread.
	 * 
	 * @param isUpdate
	 *            If this is an upgrade from a previous version of mod manager
	 */
	public ModManagerWindow(boolean isUpdate) {
		if (ACTIVE_WINDOW != null) {
			ACTIVE_WINDOW.dispose();
			ACTIVE_WINDOW = null;
		}
		this.isUpdate = isUpdate;
		ModManager.debugLogger.writeMessage("Starting Mod Manager UI (ModManagerWindow)");
		boolean reload = false;
		try {
			initializeWindow();
			ACTIVE_WINDOW = this;
			reload = processPendingPatches();
		} catch (Exception e) {
			ModManager.debugLogger.writeErrorWithException("UNKNOWN CRITICAL STARTUP EXCEPTION FOR MOD MANAGER WINDOW:", e);
			ModManager.debugLogger.writeError("Mod Manager has crashed!");
			JOptionPane.showMessageDialog(null,
					"<html><div style=\"width:330px;\">Mod Manager's interface (post-startup) encountered a critical unknown error and was unable to start:<br>" + e.getMessage()
							+ "<br>"
							+ "<br>This has been logged to the me3cmm_last_run_log.txt file if you didn't explicitly turn logging off.<br>Please report this to femshep.</div></html>",
					"Critical Interface Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (reload) {
			new ModManagerWindow(false);
		} else {
			ModManager.debugLogger.writeMessage("Mod Manager GUI: Now setting visible.");
			try {
				this.setVisible(true);
			} catch (Exception e) {
				ModManager.debugLogger.writeErrorWithException("Uncaught runtime exception:", e);
				ModManager.debugLogger.writeError("Mod Manager hit exception!");
				JOptionPane.showMessageDialog(null,
						"<html><div style=\"width:330px;\">Mod Manager's interface has just encountered an error<br>" + e.getMessage() + "<br>"
								+ "<br>This has been logged to the me3cmm_last_run_log.txt file if you didn't explicitly turn logging off.<br>The application will attempt to ignore this error.</div></html>",
						"Mod Manager Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		//new GUITransplanterWindow(this);
	}

	/**
	 * This method scans all mod files and sees if any ones have imported
	 * patches that need to be applied
	 * 
	 * @return should reload mods and patches
	 */
	private boolean processPendingPatches() {
		boolean reload = false;
		ArrayList<String> modsBuild = new ArrayList<String>();
		for (int i = 0; i < modModel.size(); i++) {
			Mod mod = modModel.getElementAt(i);
			if (mod.getRequiredPatches().size() > 0) {
				ModManager.debugLogger.writeMessage("Mod being built with included MixIns:");
				reload = true;
				new PatchApplicationWindow(this, mod.getRequiredPatches(), mod);
				modsBuild.add(mod.getModName());
			}
		}
		if (reload) {
			String dispStr = "The following mods had included mixins and have been built for use:\n";
			for (String str : modsBuild) {
				dispStr += " - " + str + "\n";
			}
			JOptionPane.showMessageDialog(null, dispStr);
			dispose();
		}
		return reload;
	}

	private void initializeWindow() {
		setupWindow();

		this.pack();
		setLocationRelativeTo(null);
		if (isUpdate) {
			JOptionPane.showMessageDialog(this, "Update successful: Updated to Mod Manager " + ModManager.VERSION + " (Build " + ModManager.BUILD_NUMBER + ").", "Update Complete",
					JOptionPane.INFORMATION_MESSAGE);
		}

		new NetworkThread().execute();
		ModManager.debugLogger.writeMessage("Mod Manager GUI: Initialize() has completed.");
	}

	/**
	 * This thread executes multiple items: Check For Mod Manager Updates Check
	 * for Mod Updates Check for 7za file + download Check for ME3Explorer
	 * Updates (if needed)
	 * 
	 * @author Michael
	 *
	 */
	class NetworkThread extends SwingWorker<Void, Object> {

		@Override
		public Void doInBackground() {
			File f7za = new File(ModManager.getToolsDir() + "7za.exe");
			if (!f7za.exists()) {
				publish("Downloading 7za Unzipper");
				ModManager.debugLogger.writeMessage("7za.exe does not exist at the following path, downloading new copy: " + f7za.getAbsolutePath());
				String url = "http://me3tweaks.com/modmanager/tools/7za.exe";
				try {
					File updateDir = new File(ModManager.getToolsDir());
					updateDir.mkdirs();
					FileUtils.copyURLToFile(new URL(url), new File(ModManager.getToolsDir() + "7za.exe"));
					publish("Downloaded 7za Unzipper into tools directory");
					ModManager.debugLogger.writeMessage("Downloaded missing 7za.exe file for updating Mod Manager");

				} catch (IOException e) {
					ModManager.debugLogger.writeErrorWithException("Error downloading 7za into tools folder", e);
					publish("Error downloading 7za");
				}
			} else {
				ModManager.debugLogger.writeMessage("7za.exe is present in tools/ directory");
			}

			//Tankmaster TLK, Coalesce
			File tmc = new File(ModManager.getTankMasterCompilerDir() + "MassEffect3.Coalesce.exe");
			File tmtlk = new File(ModManager.getTankMasterTLKDir() + "MassEffect3.TlkEditor.exe");

			if (!tmtlk.exists() || !tmc.exists()) {
				publish("Downloading Tankmaster Tools");
				ModManager.debugLogger.writeMessage("Tankmaster's TLK/COALESCE tools are missing, downloading new copy: " + tmtlk.getAbsolutePath());
				String url = "http://me3tweaks.com/modmanager/tools/tankmastertools.7z";
				try {
					File updateDir = new File(ModManager.getTempDir());
					updateDir.mkdirs();
					FileUtils.copyURLToFile(new URL(url), new File(ModManager.getTempDir() + "tankmastertools.7z"));
					ModManager.debugLogger.writeMessage("7z downloaded.");

					//run 7za on it
					ArrayList<String> commandBuilder = new ArrayList<String>();
					commandBuilder.add(ModManager.getToolsDir() + "7za.exe");
					commandBuilder.add("-y"); //overwrite
					commandBuilder.add("x"); //extract
					commandBuilder.add(ModManager.getTempDir() + "tankmastertools.7z");//7z file
					commandBuilder.add("-o" + ModManager.getDataDir()); //extraction path

					// System.out.println("Building command");
					String[] command = commandBuilder.toArray(new String[commandBuilder.size()]);
					// Debug stuff
					StringBuilder sb = new StringBuilder();
					for (String arg : command) {
						sb.append(arg + " ");
					}
					ModManager.debugLogger.writeMessage("Executing 7z extraction command: " + sb.toString());
					Process p = null;
					int returncode = 1;
					try {
						ProcessBuilder pb = new ProcessBuilder(command);
						long timeStart = System.currentTimeMillis();
						p = pb.start();
						ModManager.debugLogger.writeMessage("Executed command, waiting...");
						returncode = p.waitFor();
						long timeEnd = System.currentTimeMillis();
						ModManager.debugLogger.writeMessage("Process has finished. Took " + (timeEnd - timeStart) + " ms.");
					} catch (IOException | InterruptedException e) {
						ModManager.debugLogger.writeException(e);
					}

					ModManager.debugLogger.writeMessage("Unzip completed successfully (code 0): " + (p != null && returncode == 0));
					if (returncode == 0) {
						publish("Downloaded Tankmaster Tools into data directory");
					} else {
						publish("Unknown error downloading Tankmaster tools");
					}
					FileUtils.deleteQuietly(new File(ModManager.getTempDir() + "tankmastertools.7z"));
				} catch (IOException e) {
					ModManager.debugLogger.writeErrorWithException("Error downloading 7za into tools folder", e);
					publish("Error downloading 7za for updating");
				}
			} else {
				ModManager.debugLogger.writeMessage("7za.exe is present in tools/ directory");
			}

			/*
			 * File guitransplanter = new File(ModManager.getTransplantDir() +
			 * "Transplanter-CLI.exe"); if (!f7za.exists()) { publish(
			 * "Downloading 7za Unzipper"); ModManager.debugLogger.writeMessage(
			 * "7za.exe does not exist at the following path, downloading new copy: "
			 * + f7za.getAbsolutePath()); String url =
			 * "http://me3tweaks.com/modmanager/tools/7za.exe"; try { File
			 * updateDir = new File(ModManager.getToolsDir());
			 * updateDir.mkdirs(); FileUtils.copyURLToFile(new URL(url), new
			 * File(ModManager.getToolsDir() + "7za.exe")); publish(
			 * "Downloaded 7za Unzipper into tools directory");
			 * ModManager.debugLogger.writeMessage(
			 * "Downloaded missing 7za.exe file for updating Mod Manager");
			 * 
			 * } catch (IOException e) {
			 * ModManager.debugLogger.writeErrorWithException (
			 * "Error downloading 7za into tools folder", e); publish(
			 * "Error downloading 7za"); } } else { ModManager.debugLogger
			 * .writeMessage("7za.exe is present in tools/ directory"); }
			 */

			if (ModManager.AUTO_UPDATE_MOD_MANAGER && !ModManager.CHECKED_FOR_UPDATE_THIS_SESSION) {
				checkForUpdates();
			}
			checkForME3ExplorerUpdates();
			if ((ModManager.AUTO_UPDATE_MODS == false && !ModManager.ASKED_FOR_AUTO_UPDATE) || ModManager.AUTO_UPDATE_MODS) {
				checkForModUpdates();
			}
			return null;
		}

		private void checkForME3ExplorerUpdates() {
			String me3explorer = ModManager.getME3ExplorerEXEDirectory(false) + "ME3Explorer.exe";
			File f = new File(me3explorer);
			if (!f.exists()) {
				ModManager.debugLogger.writeMessage("ME3Explorer is missing. Downloading from ME3TWeaks.");
				if (ModManager.AUTO_UPDATE_ME3EXPLORER) {
					new ME3ExplorerUpdaterWindow(ModManagerWindow.this);
				} else {
					ModManager.debugLogger.writeError("ME3Explorer missing but cannot download due to settings!");
					JOptionPane.showMessageDialog(ModManagerWindow.this,
							"ME3Explorer is missing from data/ME3Explorer.\nMod Manager requires ME3Explorer but you have auto updates for it turned off.\nIf there are errors in this session, they are not supported by FemShep.",
							"Missing ME3Explorer", JOptionPane.ERROR_MESSAGE);
				}
			} else {
				int rev = EXEFileInfo.getBuildOfProgram(me3explorer);
				if (rev < ModManager.MIN_REQUIRED_ME3EXPLORER_REV) {
					//we must update it
					ModManager.debugLogger.writeMessage("ME3Explorer is outdated, local:" + rev + " required" + ModManager.MIN_REQUIRED_ME3EXPLORER_REV + "+");
					if (ModManager.AUTO_UPDATE_ME3EXPLORER) {
						new ME3ExplorerUpdaterWindow(ModManagerWindow.this);
					} else {
						ModManager.debugLogger.writeError("ME3Explorer outdated but cannot download due to settings!");
						JOptionPane.showMessageDialog(ModManagerWindow.this,
								"Mod Manager requires a newer version of ME3Explorer for this build.\nYou have auto updates for it turned off. You will need to turn them back on to download this update.\nIf there are errors in this session, they are not supported by FemShep.",
								"ME3Explorer Outdated", JOptionPane.ERROR_MESSAGE);
					}
				} else {
					ModManager.debugLogger.writeMessage("Current ME3Explorer version satisfies requirements for Mod Manager");
				}
			}
		}

		private void checkForModUpdates() {
			//should not do this on background thread but... yaknow...
			if (ModManager.AUTO_UPDATE_MODS == false && !ModManager.ASKED_FOR_AUTO_UPDATE) {
				ModManager.debugLogger.writeMessage("Prompting user for auto-updates");
				publish("NOTIFY_UPDATE_PROMPT");
				while (ModManager.ASKED_FOR_AUTO_UPDATE == false) {
					try {
						Thread.sleep(200); //wait for prompt
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} else {
				ModManager.debugLogger.writeMessage("User has accepted mod updates or has been asked before");
			}

			//Check for updates
			if (ModManager.AUTO_UPDATE_MODS) {
				if (System.currentTimeMillis() - ModManager.LAST_AUTOUPDATE_CHECK > ModManager.AUTO_CHECK_INTERVAL_MS) {
					ModManager.debugLogger.writeMessage("Running auto-updater, it has been "
							+ ModManager.getDurationBreakdown(System.currentTimeMillis() - ModManager.LAST_AUTOUPDATE_CHECK) + " since the last help/mods update check.");
					publish("Downloading latest help information");
					HelpMenu.getOnlineHelp();
					publish("UPDATE_HELP_MENU");
					publish("Updated help files from server");
					if (modModel.getSize() > 0) {
						publish("Checking for updates to mods");
						checkAllModsForUpdates(false);
					}
				}
			}
		}

		@Override
		protected void process(List<Object> chunks) {
			for (Object latest : chunks) {
				if (latest instanceof String) {
					String update = (String) latest;
					switch (update) {
					case "NOTIFY_UPDATE_PROMPT":
						//ask user
						int result = JOptionPane.showConfirmDialog(ModManagerWindow.this, "Mod Manager can automatically keep your mods up to date\nwith ME3Tweaks, checking every "
								+ ModManager.AUTO_CHECK_INTERVAL_DAYS + " days.\nTurn this feature on?", "Mod Auto Updates", JOptionPane.YES_NO_CANCEL_OPTION);
						ModManager.ASKED_FOR_AUTO_UPDATE = true;
						if (result != JOptionPane.CANCEL_OPTION) {
							Wini ini;
							try {
								File settings = new File(ModManager.SETTINGS_FILENAME);
								if (!settings.exists())
									settings.createNewFile();
								ini = new Wini(settings);
								ini.put("Settings", "autoupdatemods", result == JOptionPane.YES_OPTION ? "true" : "false");
								ini.put("Settings", "declinedautoupdate", result == JOptionPane.YES_OPTION ? "false" : "true");
								ini.store();
								ModManager.AUTO_UPDATE_MODS = (result == JOptionPane.YES_OPTION);
							} catch (InvalidFileFormatException e) {
								e.printStackTrace();
							} catch (IOException e) {
								ModManager.debugLogger.writeErrorWithException("Settings file encountered an I/O error while attempting to write it. Settings not saved.", e);
							}
						}
						break;
					case "UPDATE_HELP_MENU":
						menuBar.remove(helpMenu);
						menuBar.add(HelpMenu.constructHelpMenu());
						break;
					default:
						labelStatus.setText(update);
						break;
					}

				}
			}
		}

		@Override
		protected void done() {
			try {
				get();
			} catch (Exception e) {
				ModManager.debugLogger.writeErrorWithException("Exception in the network thread: ", e);
			}
		}
	}

	class SingleModUpdateCheckThread extends SwingWorker<Void, Object> {
		Mod mod;

		public SingleModUpdateCheckThread(Mod mod) {
			this.mod = mod;
			labelStatus.setText("Checking for " + mod.getModName() + " updates");
		}

		@Override
		public Void doInBackground() {
			ArrayList<Mod> modsToValidate = new ArrayList<Mod>();
			modsToValidate.add(mod);
			ArrayList<UpdatePackage> upackages = ModXMLTools.validateLatestAgainstServer(modsToValidate,null);
			if (upackages != null) {
				publish("Update available for " + mod.getModName());
				publish(upackages.get(0)); //singleu pdate
			} else {
				publish(mod.getModName() + " is up to date");
			}
			return null;
		}

		@Override
		protected void process(List<Object> chunks) {
			for (Object latest : chunks) {
				if (latest instanceof String) {
					String update = (String) latest;
					labelStatus.setText(update);
				}
				if (latest instanceof UpdatePackage) {
					UpdatePackage upackage = (UpdatePackage) latest;
					String updatetext = mod.getModName() + " has an update available from ME3Tweaks:\n";
					updatetext += "Version " + upackage.getMod().getVersion() + " => " + upackage.getVersion() + " (" + upackage.getUpdateSizeMB() + ")\n";
					updatetext += "Update this mod?";
					int result = JOptionPane.showConfirmDialog(ModManagerWindow.this, updatetext, "Mod update available", JOptionPane.YES_NO_OPTION);
					if (result == JOptionPane.YES_OPTION) {
						ModManager.debugLogger.writeMessage("Starting manual single-mod updater");
						ModUpdateWindow muw = new ModUpdateWindow(upackage);
						muw.startUpdate(ModManagerWindow.this);
					}
				}
			}
		}

		@Override
		protected void done() {
			try {
				get();
			} catch (Exception e) {
				ModManager.debugLogger.writeErrorWithException("Exception in the single mod update thread: ", e);
			}
		}
	}

	private Void checkForUpdates() {
		labelStatus.setText("Checking for Mod Manager updates");
		ModManager.debugLogger.writeMessage("Checking for update...");
		// Check for update
		try {
			String update_check_link;
			if (ModManager.IS_DEBUG) {
				update_check_link = "http://webdev-c9-mgamerz.c9.io/modmanager/updatecheck?currentversion=" + ModManager.BUILD_NUMBER;
			} else {
				update_check_link = "https://me3tweaks.com/modmanager/updatecheck?currentversion=" + ModManager.BUILD_NUMBER;
			}
			String serverJSON = null;
			try {
				serverJSON = IOUtils.toString(new URL(update_check_link), StandardCharsets.UTF_8);
			} catch (Exception e) {
				ModManager.debugLogger.writeError("Error checking for updates:");
				ModManager.debugLogger.writeException(e);
				labelStatus.setText("Error checking for update (check logs)");
				ModManager.CHECKED_FOR_UPDATE_THIS_SESSION = true;
				return null;
			}
			if (serverJSON == null) {
				ModManager.debugLogger.writeError("No response from server");
				labelStatus.setText("Updater: No response from server");
				ModManager.CHECKED_FOR_UPDATE_THIS_SESSION = true;
				return null;
			}

			JSONParser parser = new JSONParser();
			JSONObject latest_object = (JSONObject) parser.parse(serverJSON);
			String buildHash = (String) latest_object.get("build_md5");
			boolean hashMismatch = false;
			if (new File("ME3CMM.exe").exists()) {
				try {
					String currentHash = MD5Checksum.getMD5Checksum("ME3CMM.exe");
					if (buildHash != null && !buildHash.equals("") && !currentHash.equals(buildHash)) {
						//hash mismatch
						hashMismatch = true;
						ModManager.debugLogger.writeMessage("Local hash (" + currentHash + ") does not match server hash (" + buildHash + ")");
					} else {
						ModManager.debugLogger.writeMessage("Local hash matches server hash: " + buildHash);
					}
				} catch (Exception e1) {
					ModManager.debugLogger.writeErrorWithException("Unable to hash ME3CMM.exe:", e1);
				}
			} else {
				ModManager.debugLogger.writeMessage("Skipping hash check for updates. Likely running in IDE or some other wizardry");
			}

			long latest_build = (long) latest_object.get("latest_build_number");
			if (latest_build < ModManager.BUILD_NUMBER) {
				//build is newer than current
				labelStatus.setVisible(true);
				ModManager.debugLogger.writeMessage("No updates, at latest version. (or could not contact update server.)");
				labelStatus.setText("No Mod Manager updates available");
				ModManager.CHECKED_FOR_UPDATE_THIS_SESSION = true;
				return null;
			}

			if (latest_build == ModManager.BUILD_NUMBER && !hashMismatch) {
				//build is same as server version
				labelStatus.setVisible(true);
				ModManager.debugLogger.writeMessage("No updates, at latest version.");
				labelStatus.setText("Mod Manager is up to date");
				ModManager.CHECKED_FOR_UPDATE_THIS_SESSION = true;
				return null;
			}

			ModManager.debugLogger
					.writeMessage("Update check: Local:" + ModManager.BUILD_NUMBER + " Latest: " + latest_build + ", is less? " + (ModManager.BUILD_NUMBER < latest_build));

			boolean showUpdate = true;
			// make sure the user hasn't declined this one.
			Wini settingsini;
			try {
				settingsini = new Wini(new File(ModManager.SETTINGS_FILENAME));
				String showIfHigherThan = settingsini.get("Settings", "nextupdatedialogbuild");
				long build_check = ModManager.BUILD_NUMBER;
				if (showIfHigherThan != null && !showIfHigherThan.equals("")) {
					try {
						build_check = Integer.parseInt(showIfHigherThan);
						if (latest_build > build_check) {
							// update is newer than one stored in ini, show the
							// dialog.
							ModManager.debugLogger.writeMessage("Advertising build " + latest_build);
							showUpdate = true;
						} else {
							ModManager.debugLogger.writeMessage("User isn't seeing updates until build " + build_check);
							// don't show it.
							showUpdate = false;
						}
					} catch (NumberFormatException e) {
						ModManager.debugLogger.writeMessage("Number format exception reading the build number updateon in the ini. Showing the dialog.");
					}
				}
			} catch (InvalidFileFormatException e) {
				ModManager.debugLogger.writeErrorWithException("Invalid INI! Did the user modify it by hand?", e);
				e.printStackTrace();
			} catch (IOException e) {
				ModManager.debugLogger.writeMessage("I/O Error reading settings file. It may not exist yet. It will be created when a setting stored to disk.");
			}

			if (showUpdate) {
				// An update is available!
				labelStatus.setVisible(true);
				labelStatus.setText("Update available");
				new UpdateAvailableWindow(latest_object, this);
				ModManager.CHECKED_FOR_UPDATE_THIS_SESSION = true;
			} else {
				labelStatus.setVisible(true);
				labelStatus.setText("No updates available");
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			ModManager.debugLogger.writeErrorWithException("Error parsing server response:", e);
		}
		ModManager.CHECKED_FOR_UPDATE_THIS_SESSION = true;
		return null;
	}

	/**
	 * Checks if the keybindings override file exists.
	 * 
	 * @return true if exists, false otherwise
	 */
	private boolean checkForKeybindsOverride() {
		File bioinputxml = new File(ModManager.getOverrideDir() + "bioinput.xml");
		return bioinputxml.exists();
	}

	private void setupWindow() {
		this.setTitle("Mass Effect 3 Mod Manager");
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setIconImages(ModManager.ICONS);
		Dimension minSize = new Dimension(560, 520);
		this.setPreferredSize(minSize);
		this.setMinimumSize(minSize);

		// Menubar
		menuBar = makeMenu();
		ModManager.debugLogger.writeMessage("Menu system has initialized.");
		// Main Panel
		JPanel contentPanel = new JPanel(new BorderLayout());

		// North Panel
		JPanel northPanel = new JPanel(new BorderLayout());

		// Title Panel
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.add(new JLabel("Mass Effect 3 Mod Manager " + ModManager.VERSION, SwingConstants.LEFT), BorderLayout.WEST);

		// BioGameDir Panel
		JPanel cookedDirPanel = new JPanel(new BorderLayout());
		TitledBorder cookedDirTitle = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Mass Effect 3 BIOGame Directory");
		fieldBiogameDir = new JTextField();
		fieldBiogameDir.setText(getInitialBiogameDirText());
		buttonBioGameDir = new JButton("Browse...");
		buttonBioGameDir.setToolTipText(
				"<html>Browse and set the BIOGame directory.<br>This is located in the installation directory for Mass Effect 3.<br>Typically this is in the Origin Games folder.</html>");
		fieldBiogameDir.setColumns(37);
		buttonBioGameDir.setPreferredSize(new Dimension(90, 14));

		buttonBioGameDir.addActionListener(this);
		cookedDirPanel.setBorder(cookedDirTitle);
		cookedDirPanel.add(fieldBiogameDir, BorderLayout.CENTER);
		cookedDirPanel.add(buttonBioGameDir, BorderLayout.EAST);

		northPanel.add(titlePanel, BorderLayout.NORTH);
		northPanel.add(cookedDirPanel, BorderLayout.CENTER);

		ModManager.debugLogger.writeMessage("Setting up modlist UI");
		// ModsList
		JPanel modsListPanel = new JPanel(new BorderLayout());
		// JLabel availableModsLabel = new JLabel("  Available Mods:");
		TitledBorder modListsBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Available Mods");

		modList = new JList<Mod>();
		modList.addListSelectionListener(this);
		modList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		modList.setLayoutOrientation(JList.VERTICAL);
		JScrollPane listScroller = new JScrollPane(modList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		// modsListPanel.add(availableModsLabel, BorderLayout.NORTH);
		modsListPanel.setBorder(modListsBorder);
		modsListPanel.add(listScroller, BorderLayout.CENTER);

		modModel = new DefaultListModel<Mod>();
		modList.setModel(modModel);
		ModManager.debugLogger.writeMessage("Loading mods.");
		ArrayList<Mod> modList = ModManager.getModsFromDirectory();
		Collections.sort(modList);
		for (Mod mod : modList) {
			modModel.addElement(mod);
		}
		ModManager.debugLogger.writeMessage("Mods have loaded.");

		//load patches
		ModManager.debugLogger.writeMessage("Loading mixins");

		setPatchList(ModManager.getPatchesFromDirectory());
		ModManager.debugLogger.writeMessage("Mixins have loaded.");

		// DescriptionField
		JPanel descriptionPanel = new JPanel(new BorderLayout());
		// JLabel descriptionLabel = new JLabel("Mod Description:");
		TitledBorder descriptionBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Mod Description");
		descriptionPanel.setBorder(descriptionBorder);
		fieldDescription = new JTextArea(selectAModDescription);
		scrollDescription = new JScrollPane(fieldDescription);

		fieldDescription.setLineWrap(true);
		fieldDescription.setWrapStyleWord(true);

		fieldDescription.setEditable(false);
		scrollDescription.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollDescription.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		modWebsiteLink = new JButton();
		modWebsiteLink.setText("<HTML>Click the <FONT color=\"#000099\"><U>link</U></FONT>" + " to go to the Java website.</HTML>");
		modWebsiteLink.setBorderPainted(false);
		modWebsiteLink.setBackground(UIManager.getColor("Panel.background"));
		modWebsiteLink.setFocusPainted(false);
		modWebsiteLink.setMargin(new Insets(0, 0, 0, 0));
		modWebsiteLink.setContentAreaFilled(false);
		modWebsiteLink.setBorderPainted(false);
		modWebsiteLink.setOpaque(false);
		modWebsiteLink.setOpaque(false);
		modWebsiteLink.setVisible(false);
		modWebsiteLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
		modWebsiteLink.addActionListener(this);

		// descriptionPanel.add(descriptionLabel, BorderLayout.NORTH);
		descriptionPanel.add(scrollDescription, BorderLayout.CENTER);
		descriptionPanel.add(modWebsiteLink, BorderLayout.SOUTH);

		fieldDescription.setCaretPosition(0);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, modsListPanel, descriptionPanel);
		splitPane.setOneTouchExpandable(false);
		splitPane.setDividerLocation(185);

		// SouthPanel
		JPanel southPanel = new JPanel(new BorderLayout());
		JPanel applyPanel = new JPanel(new BorderLayout());

		// ApplyPanel
		labelStatus = new JLabel("Loaded mods");
		labelStatus.setVisible(true);

		// ProgressBar

		// ButtonPanel
		JPanel buttonPanel = new JPanel(new FlowLayout());

		buttonApplyMod = new JButton("Apply Mod");
		updateApplyButton();
		buttonApplyMod.addActionListener(this);
		buttonApplyMod.setEnabled(false);

		if (ModManager.NET_FRAMEWORK_IS_INSTALLED) {
			buttonApplyMod.setToolTipText("Select a mod on the left");
		} else {
			buttonApplyMod.setToolTipText("Mod Manager requires .NET Framework 4.5 or higher in order to install mods");
		}
		buttonStartGame = new JButton("Start Game");
		buttonStartGame.addActionListener(this);
		buttonStartGame.setToolTipText(
				"<html>Starts the game.<br>If LauncherWV DLC bypass is installed, it will launch instead to patch out the DLC verifiction test.<br>The game will then start.</html>");

		buttonPanel.add(buttonApplyMod);
		buttonPanel.add(buttonStartGame);
		applyPanel.add(labelStatus, BorderLayout.WEST);
		applyPanel.add(buttonPanel, BorderLayout.EAST);

		southPanel.add(applyPanel, BorderLayout.SOUTH);

		// add all panels
		contentPanel.add(northPanel, BorderLayout.NORTH);
		contentPanel.add(splitPane, BorderLayout.CENTER);
		contentPanel.add(southPanel, BorderLayout.SOUTH);
		this.setJMenuBar(menuBar);
		contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		this.add(contentPanel);
		verifyBackupCoalesced();
		ModManager.debugLogger.writeMessage("Mod Manager GUI: SetupWindow() has completed.");

	}

	protected void updateApplyButton() {
		if (ModManager.NET_FRAMEWORK_IS_INSTALLED) {
			buttonApplyMod.setText("Apply Mod");
			if (modList.getSelectedIndex() == -1) {
				buttonApplyMod.setToolTipText("Select a mod on the left");
			} else {
				buttonApplyMod.setToolTipText(
						"<html>Apply this mod to the game.<br>If another mod is already installed, restore your game first!<br>You can merge Mod Manager mods in the Tools menu.</html>");

			}
		} else {
			buttonApplyMod.setText(".NET Missing");
			buttonApplyMod.setToolTipText("Mod Manager requires .NET Framework 4.5 or higher in order to install mods");
		}
	}

	private JMenuBar makeMenu() {
		menuBar = new JMenuBar();
		// Actions
		actionMenu = new JMenu("Actions");
		actionModMaker = new JMenuItem("Create a mod");
		actionModMaker.setToolTipText("Opens ME3Tweaks ModMaker");
		actionVisitMe = new JMenuItem("Open ME3Tweaks.com");
		actionVisitMe.setToolTipText("Opens ME3Tweaks.com");

		JMenu actionImportMenu = new JMenu("Import mod");
		modManagementImportAlreadyInstalled = new JMenuItem("Import installed DLC mod");
		modManagementImportAlreadyInstalled.setToolTipText("<html>Import an already installed DLC mod.<br>You can then manage that mod with Mod Manager.</html>");
		modManagementImportFromArchive = new JMenuItem("Import mod from .7z/.rar/.zip");
		modManagementImportFromArchive.setToolTipText(
				"<html>Import a mod that has been packaged for importing into Mod Manager.<br>For directions on how to make mods in this format, please see the Mod Manager moddesc page.</html>");

		actionOptions = new JMenuItem("Options");
		actionOptions.setToolTipText("Configure Mod Manager Options");

		toolME3Explorer = new JMenuItem("ME3Explorer");
		toolME3Explorer.setToolTipText("Runs the bundled ME3Explorer program");
		toolTankmasterCoalFolder = new JMenuItem("TankMaster Coalesce Folder");
		toolTankmasterCoalFolder.setToolTipText("Opens Tankmaster Coalesce folder");
		toolTankmasterCoalUI = new JMenuItem("TankMaster Coalesce Interface");
		toolTankmasterCoalUI.setToolTipText("Opens interface for Tankmaster's Coalesced compiler");
		toolTankmasterTLK = new JMenuItem("TankMaster ME2/ME3 TLK Tool");
		toolTankmasterTLK.setToolTipText("Runs the bundled TLK tool provided by TankMaster");
		actionReload = new JMenuItem("Reload Mod Manager");
		actionReload.setToolTipText("Reloads Mod Manager to refresh mods, mixins, and help documentation");
		actionExit = new JMenuItem("Exit");
		actionExit.setToolTipText("Closes Mod Manager");

		actionMenu.add(actionModMaker);
		actionMenu.add(actionVisitMe);
		actionMenu.add(actionOptions);
		actionMenu.addSeparator();
		actionMenu.add(actionReload);
		actionMenu.add(actionExit);

		actionModMaker.addActionListener(this);
		actionVisitMe.addActionListener(this);
		actionOptions.addActionListener(this);


		actionReload.addActionListener(this);
		actionExit.addActionListener(this);
		menuBar.add(actionMenu);

		//MOD MANAGEMENT
		modManagementMenu = new JMenu("Mod Management");
		modManagementModMaker = new JMenuItem("Download ME3Tweaks ModMaker Mod");
		modManagementModMaker.setToolTipText("Allows you to download and compile ME3Tweaks ModMaker mods");
		modManagementCheckallmodsforupdate = new JMenuItem("Check eligible mods for updates");
		modManagementCheckallmodsforupdate.setToolTipText("Checks eligible mods for updates on ME3Tweaks and prompts to download an update if one is available");
		modManagementPatchLibary = new JMenuItem("MixIn Library");
		modManagementPatchLibary.setToolTipText("Add premade mixins to mods using patches in your patch library");
		modManagementOpenModsFolder = new JMenuItem("Open mods/ folder");
		modManagementOpenModsFolder.setToolTipText("Opens the mods/ folder so you can inspect and add/remove mods");
		
		actionImportMenu.add(modManagementImportFromArchive);
		actionImportMenu.add(modManagementImportAlreadyInstalled);
		modManagementMenu.add(actionImportMenu);
		modManagementMenu.add(modManagementModMaker);
		modManagementMenu.add(modManagementCheckallmodsforupdate);
		modManagementMenu.add(modManagementPatchLibary);
		modManagementMenu.add(modManagementOpenModsFolder);
		menuBar.add(modManagementMenu);
		
		modManagementOpenModsFolder.addActionListener(this);
		modManagementImportFromArchive.addActionListener(this);
		modManagementImportAlreadyInstalled.addActionListener(this);

		
		// MOD TOOLS
		modMenu = new JMenu("Mod Utils");
		mountMenu = new JMenu("Manage Custom DLC Mount files");
		mountMenu.setVisible(false);
		modutilsHeader = new JMenuItem("No mod selected");
		modutilsHeader.setEnabled(false);
		modutilsInstallCustomKeybinds = new JMenuItem("Install custom keybinds into this mod");
		modutilsInstallCustomKeybinds.addActionListener(this);
		//check if BioInput.xml exists.
		if (!checkForKeybindsOverride()) {
			ModManager.debugLogger.writeMessage("No keybinds file in the override directory (bioinput.xml)");
			modutilsInstallCustomKeybinds.setEnabled(false);
			modutilsInstallCustomKeybinds.setToolTipText("<html>To enable installing custom keybinds put a<br>BioInput.xml file in the data/override/ directory.</html>");
		} else {
			ModManager.debugLogger.writeMessage("Found keybinds file in the override directory (bioinput.xml)");
			modutilsInstallCustomKeybinds.setToolTipText("<html>Replace BioInput.xml in the BASEGAME Coalesced file</html>");
		}

		modutilsUpdateXMLGenerator = new JMenuItem("Generate Server Manifest XML");
		modutilsUpdateXMLGenerator.addActionListener(this);
		modutilsInfoEditor = new JMenuItem("Edit name/description");
		modutilsInfoEditor.addActionListener(this);
		modutilsInfoEditor.setToolTipText("Rename this mod and change the description shown in the descriptions window");

		modDeltaMenu = new JMenu("0 available variants");
		modDeltaMenu.setToolTipText(
				"<html>This mod has variants that allow quick changes to the mod without shipping a full new version.<br>Variants are Coalesced patches that can make small changes like turning on motion blur.<br>See the FAQ on how to create them.</html>");
		modDeltaMenu.setVisible(false);

		modNoDeltas = new JMenuItem("No included variants");
		modNoDeltas.setToolTipText("<html>Variants are Coalesced patches that can make small changes like turning on motion blur.<br>See the FAQ on how to create them.</html>");
		modNoDeltas.setEnabled(false);

		modutilsVerifyDeltas = new JMenuItem("Verify variants");
		modutilsVerifyDeltas.setToolTipText("<html>Verifies all parts of deltas are applicable to mod</html>");
		modutilsVerifyDeltas.addActionListener(this);

		modDeltaRevert = new JMenuItem("Revert to original version");
		modDeltaRevert.setToolTipText("<html>Restores the mod to the original version, without variants applied</html>");
		modDeltaRevert.addActionListener(this);

		modutilsAutoTOC = new JMenuItem("Run AutoTOC on this mod");
		modutilsAutoTOC.addActionListener(this);
		modutilsAutoTOC.setToolTipText("Automatically update all TOC files this mod uses with proper sizes to prevent crashes");

		modutilsCheckforupdate = new JMenuItem("Check for newer version (ME3Tweaks)");
		modutilsCheckforupdate.addActionListener(this);

		modMenu.add(modutilsHeader);
		if (ModManager.IS_DEBUG) {
			modMenu.add(modutilsUpdateXMLGenerator);
		}
		modMenu.add(modutilsCheckforupdate);
		modMenu.add(modDeltaMenu);
		modMenu.add(modNoDeltas);
		modMenu.add(mountMenu);
		modMenu.addSeparator();
		modMenu.add(modutilsInstallCustomKeybinds);
		modMenu.add(modutilsInfoEditor);
		modMenu.add(modutilsAutoTOC);
		modMenu.setEnabled(false);
		menuBar.add(modMenu);

		// Tools
		toolsMenu = new JMenu("Tools");
		

		toolsMergeMod = new JMenuItem("Mod Merging Utility");
		toolsMergeMod.setToolTipText("Allows you to merge CMM3+ mods together and resolve conflicts between mods");

		

		toolsOpenME3Dir = new JMenuItem("Open BIOGame directory");
		toolsOpenME3Dir.setToolTipText("Opens a Windows Explorer window in the BIOGame Directory");

		toolsInstallLauncherWV = new JMenuItem("Install LauncherWV DLC Bypass");
		toolsInstallLauncherWV.setToolTipText("<html>Installs an in-memory patcher giving you console and allowing modified DLC.<br>This does not does not modify files</html>");
		toolsInstallBinkw32 = new JMenuItem("Install Binkw32 DLC Bypass");
		toolsInstallBinkw32.setToolTipText(
				"<html>Installs a startup patcher giving you console and allowing modified DLC.<br>This modifies your game and is erased when doing an Origin Repair</html>");
		toolsUninstallBinkw32 = new JMenuItem("Uninstall Binkw32 DLC Bypass");
		toolsUninstallBinkw32.setToolTipText("<html>Removes the Binkw32.dll startup patcher, reverting the original file</html>");

		

		toolsUnpackDLC = new JMenuItem("DLC Unpacker");
		toolsUnpackDLC.setToolTipText("Opens the Unpack DLC window so you can unpack DLC automatically");

		toolsMountdlcEditor = new JMenuItem("Mount.dlc Editor");
		toolsMountdlcEditor.setToolTipText("Allows you to modify or create new Mount.dlc files easily");

		modManagementModMaker.addActionListener(this);
		toolsMergeMod.addActionListener(this);
		modManagementCheckallmodsforupdate.addActionListener(this);
		toolsUnpackDLC.addActionListener(this);
		toolsInstallLauncherWV.addActionListener(this);
		modManagementPatchLibary.addActionListener(this);
		toolsMountdlcEditor.addActionListener(this);

		toolsOpenME3Dir.addActionListener(this);
		toolsInstallBinkw32.addActionListener(this);
		toolsUninstallBinkw32.addActionListener(this);
		toolME3Explorer.addActionListener(this);
		toolTankmasterTLK.addActionListener(this);
		toolTankmasterCoalFolder.addActionListener(this);
		toolTankmasterCoalUI.addActionListener(this);
		toolsMenu.add(toolsMergeMod);
		toolsMenu.add(toolsMountdlcEditor);
		toolsMenu.add(toolsUnpackDLC);
		toolsMenu.add(toolsOpenME3Dir);
		toolsMenu.addSeparator();
		toolsMenu.add(toolME3Explorer);
		toolsMenu.add(toolTankmasterCoalFolder);
		toolsMenu.add(toolTankmasterCoalUI);
		toolsMenu.add(toolTankmasterTLK);
		toolsMenu.addSeparator();
		toolsMenu.add(toolsInstallLauncherWV);
		toolsMenu.add(toolsInstallBinkw32);
		toolsMenu.add(toolsUninstallBinkw32);
		menuBar.add(toolsMenu);

		// BACKUP
		backupMenu = new JMenu("Backup");

		backupBackupDLC = new JMenuItem("Backup DLCs");
		backupBackupDLC.setToolTipText("Backs up your DLC to .bak files. When installing a mod it will ask if a .bak files does not exist if you want to backup");

		backupBasegameUnpacked = new JMenuItem("Backup basegame/unpacked files");
		backupBasegameUnpacked.setToolTipText("An Unpacked and basegame file will be automatically backed up when Mod Manager replaces or removes that file");

		backupCreateGDB = new JMenuItem("Update game repair database");
		backupCreateGDB.setToolTipText("Creates/updates a database of checksums for basegame and unpacked DLC files for verifying restoring and backing up");

		backupBackupDLC.addActionListener(this);
		backupBasegameUnpacked.addActionListener(this);
		backupCreateGDB.addActionListener(this);

		backupMenu.add(backupBackupDLC);
		backupMenu.add(backupBasegameUnpacked);
		backupMenu.add(backupCreateGDB);
		menuBar.add(backupMenu);

		// RESTORE
		restoreMenu = new JMenu("Restore");

		restoreSelective = new JMenuItem("Custom Restore");
		restoreSelective.setToolTipText("Allows you to restore specific basegame, DLC, and unpacked files");

		restoreRevertEverything = new JMenuItem("Restore everything");
		restoreRevertEverything.setToolTipText(
				"<html>Restores all basegame files, deletes unpacked DLC files, and restores all SFAR files.<br>This will delete any non standard DLC folders.</html>");

		restoreDeleteUnpacked = new JMenuItem("Delete all unpacked DLC files");
		restoreDeleteUnpacked
				.setToolTipText("<html>Deletes unpacked DLC files, leaving PCConsoleTOC,<br>.sfar and .bak files in the DLC folders.<br>Does not modify Custom DLC.</html>");

		restoreRevertBasegame = new JMenuItem("Restore basegame files");
		restoreRevertBasegame.setToolTipText("<html>Restores all basegame files that have been modified by installing mods</html>");

		restoredeleteAllCustomDLC = new JMenuItem("Remove all Custom DLC");
		restoredeleteAllCustomDLC.setToolTipText("<html>Deletes all non standard DLC folders in the DLC directory</html>");

		restoreCustomDLCManager = new JMenuItem("Custom DLC Manager");
		restoreCustomDLCManager.setToolTipText("<html>Allows selectively removing Custom DLC modules and indicates if MP is affected</html>");

		restoreRevertBasegame = new JMenuItem("Restore basegame files");
		restoreRevertBasegame.setToolTipText("<html>Restores all basegame files that have been modified by installing mods</html>");

		restoreRevertUnpacked = new JMenuItem("Restore unpacked DLC files");
		restoreRevertUnpacked.setToolTipText("<html>Restores all unpacked DLC files that have been modified by installing mods</html>");

		restoreRevertBasegameUnpacked = new JMenuItem("Restore basegame + unpacked files");
		restoreRevertBasegameUnpacked.setToolTipText("<html>Restores all basegame and unpacked DLC files that have been modified by installing mods</html>");

		restoreVanillifyDLC = new JMenuItem("Vanillify game DLC");
		restoreVanillifyDLC.setToolTipText("<html>Removes custom DLC, deletes unpacked files, restores SFARs.</html>");

		restoreRevertAllDLC = new JMenuItem("Restore all DLC SFARs");
		restoreRevertAllDLC.setToolTipText("<html>Restores all DLC SFAR files.<br>This does not remove custom DLC modules.</html>");

		restoreRevertSPDLC = new JMenuItem("Restore SP DLC SFARs");
		restoreRevertSPDLC.setToolTipText("<html>Restores all SP DLCs.<br>This does not remove custom DLC modules.</html>");

		restoreRevertSPBaseDLC = new JMenuItem("Restore SP DLC SFARs + Basegame");
		restoreRevertSPBaseDLC.setToolTipText("<html>Restores all basegame files, and checks all SP DLC SFAR files.<br>This does not remove custom DLC modules.</html>");

		restoreRevertMPDLC = new JMenuItem("Restore MP DLC SFARs");
		restoreRevertMPDLC.setToolTipText("<html>Restores all MP DLC SFARs.<br>This does not remove custom DLC modules.</html>");

		restoreRevertMPBaseDLC = new JMenuItem("Restore MP DLC SFARs + Basegame");
		restoreRevertMPBaseDLC.setToolTipText(
				"<html>Restores all basegame files, and checks all Multiplayer DLC files.<br>This does not remove custom DLC modules.<br>If you are doing multiplayer mods, you should use this to restore</html>");
		restoreRevertCoal = new JMenuItem("Restore vanilla Coalesced.bin");
		restoreRevertCoal.setToolTipText("<html>Restores the basegame coalesced file</html>");

		restoreSelective.addActionListener(this);
		restoreRevertEverything.addActionListener(this);
		restoreDeleteUnpacked.addActionListener(this);
		restoredeleteAllCustomDLC.addActionListener(this);
		restoreCustomDLCManager.addActionListener(this);
		restoreRevertBasegame.addActionListener(this);
		restoreRevertUnpacked.addActionListener(this);
		restoreVanillifyDLC.addActionListener(this);
		restoreRevertBasegameUnpacked.addActionListener(this);
		restoreRevertAllDLC.addActionListener(this);
		restoreRevertSPDLC.addActionListener(this);
		restoreRevertSPBaseDLC.addActionListener(this);
		restoreRevertMPDLC.addActionListener(this);
		restoreRevertMPBaseDLC.addActionListener(this);
		restoreRevertCoal.addActionListener(this);

		restoreMenu.add(restoreSelective);
		restoreMenu.add(restoreCustomDLCManager);
		restoreMenu.addSeparator();
		restoreMenu.add(restoreRevertEverything);
		restoreMenu.add(restoredeleteAllCustomDLC);
		restoreMenu.addSeparator();
		restoreMenu.add(restoreRevertBasegame);
		restoreMenu.add(restoreRevertUnpacked);
		restoreMenu.add(restoreRevertBasegameUnpacked);
		restoreMenu.add(restoreDeleteUnpacked);
		restoreMenu.addSeparator();
		restoreMenu.add(restoreVanillifyDLC);
		restoreMenu.add(restoreRevertAllDLC);
		restoreMenu.add(restoreRevertMPDLC);
		restoreMenu.add(restoreRevertMPBaseDLC);
		restoreMenu.add(restoreRevertSPDLC);
		restoreMenu.add(restoreRevertSPBaseDLC);
		restoreMenu.addSeparator();
		restoreMenu.add(restoreRevertCoal);
		menuBar.add(restoreMenu);

		sqlMenu = new JMenu("SQL");
		sqlWavelistParser = new JMenuItem("Wavelist Parser");
		sqlDifficultyParser = new JMenuItem("Biodifficulty Parser");
		sqlAIWeaponParser = new JMenuItem("BioAI Parser");
		sqlPowerCustomActionParser = new JMenuItem("CustomAction Parser");
		sqlPowerCustomActionParser2 = new JMenuItem("CustomAction Editor");
		sqlConsumableParser = new JMenuItem("Consumable Parser");
		sqlGearParser = new JMenuItem("Gear Parser");

		sqlWavelistParser.addActionListener(this);
		sqlDifficultyParser.addActionListener(this);
		sqlAIWeaponParser.addActionListener(this);
		sqlPowerCustomActionParser.addActionListener(this);
		sqlPowerCustomActionParser2.addActionListener(this);
		sqlConsumableParser.addActionListener(this);
		sqlGearParser.addActionListener(this);

		sqlMenu.add(sqlWavelistParser);
		sqlMenu.add(sqlDifficultyParser);
		sqlMenu.add(sqlAIWeaponParser);
		sqlMenu.add(sqlPowerCustomActionParser);
		sqlMenu.add(sqlPowerCustomActionParser2);
		sqlMenu.add(sqlConsumableParser);
		sqlMenu.add(sqlGearParser);
		if (ModManager.IS_DEBUG) {
			menuBar.add(sqlMenu);
		}

		helpMenu = HelpMenu.constructHelpMenu();
		menuBar.add(helpMenu);

		return menuBar;
	}

	private void verifyBackupCoalesced() {
		File restoreTest = new File(ModManager.getDataDir() + "Coalesced.original");
		if (!restoreTest.exists()) {
			ModManager.debugLogger.writeMessage("Didn't find Coalesced.original - checking existing installed one, will copy if verified.");
			// try to copy the current one
			String patch3CoalescedHash = "540053c7f6eed78d92099cf37f239e8e";
			File coalesced = new File(ModManager.appendSlash(fieldBiogameDir.getText().toString()) + "CookedPCConsole\\Coalesced.bin");
			// Take the MD5 first to verify it.
			if (coalesced.exists()) {
				try {
					if (patch3CoalescedHash.equals(MD5Checksum.getMD5Checksum(coalesced.toString()))) {
						// back it up
						Files.copy(coalesced.toPath(), restoreTest.toPath());
						restoreRevertCoal.setEnabled(true);
						ModManager.debugLogger.writeMessage("Backed up Coalesced.");
					} else {
						ModManager.debugLogger.writeMessage("Didn't back up coalecsed, hash mismatch.");
						restoreRevertCoal.setEnabled(false);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					ModManager.debugLogger.writeErrorWithException("I/O Exception while verifying backup coalesced.", e);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					ModManager.debugLogger.writeErrorWithException("General Exception while verifying backup coalesced.", e);
				}
			} else {
				ModManager.debugLogger.writeMessage("Coalesced.bin was not found - unable to back up automatically");
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		// too bad we can't do a switch statement on the object :(
		if (e.getSource() == buttonBioGameDir) {
			dirChooser = new JFileChooser();
			File tryDir = new File(fieldBiogameDir.getText());
			if (tryDir.exists()) {
				dirChooser.setCurrentDirectory(new File(fieldBiogameDir.getText()));
			} else {
				if (ModManager.logging) {
					ModManager.debugLogger.writeMessage("Directory " + fieldBiogameDir.getText() + " does not exist, defaulting to working directory.");
				}
				dirChooser.setCurrentDirectory(new java.io.File("."));
			}
			dirChooser.setDialogTitle("Select BIOGame directory");
			dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			//
			// disable the "All files" option.
			//
			dirChooser.setAcceptAllFileFilterUsed(false);
			//
			if (dirChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				checkForValidBioGame();
			} else {
				if (ModManager.logging) {
					ModManager.debugLogger.writeMessage("No directory selected...");
				}
			}
		} else if (e.getSource() == modManagementModMaker) {
			if (validateBIOGameDir()) {
				if (ModManager.validateNETFrameworkIsInstalled()) {
					ModManager.debugLogger.writeMessage("Opening ModMaker Entry Window");
					updateApplyButton();
					new ModMakerEntryWindow(this, fieldBiogameDir.getText());
				} else {
					updateApplyButton();
					labelStatus.setText(".NET Framework 4.5 or higher is missing");
					ModManager.debugLogger.writeMessage("ModMaker: Missing .NET Framework");
					new NetFrameworkMissingWindow("You must install .NET Framework 4.5 or higher in order to use ModMaker.");
				}
			} else {
				labelStatus.setText("ModMaker requires valid BIOGame directory to start");
				labelStatus.setVisible(true);
				JOptionPane.showMessageDialog(null, "The BIOGame directory is not valid.\nFix the BIOGame directory before continuing.", "Invalid BioGame Directory",
						JOptionPane.ERROR_MESSAGE);
			}
		} else

		if (e.getSource() == backupBackupDLC) {
			if (validateBIOGameDir()) {
				backupDLC(fieldBiogameDir.getText());
			} else {
				labelStatus.setText("Backing up DLC requires valid BIOGame directory");
				labelStatus.setVisible(true);
				JOptionPane.showMessageDialog(null, "The BIOGame directory is not valid.\nFix the BIOGame directory before continuing.", "Invalid BioGame Directory",
						JOptionPane.ERROR_MESSAGE);
			}
		} else

		if (e.getSource() == backupBasegameUnpacked) {
			if (validateBIOGameDir()) {
				String me3dir = (new File(fieldBiogameDir.getText())).getParent();
				String backupLocation = ModManager.appendSlash(me3dir) + "cmmbackup";
				JOptionPane.showMessageDialog(this,
						"<html><div style=\"width:330px;\">Basegame and unpacked DLC files are automatically backed up by Mod Manager when a Mod Manager mod replaces or removes that file while being applied.<br>"
								+ "The game repair database verifies the file being backed up matches the metadata in the database so it can restore back to that version later. When restoring, the backed up file is checked again to make sure it wasn't modified. Otherwise you may restore files of different sizes and the game will crash.<br>"
								+ "This is why modifications outside of Mod Manager are not backed up and are not supported. If you want to use modifications outside of Mod Manager, update the game repair database after you make your changes outside of Mod Manager. Make sure your game is in a working state before you do this or you will restore to a broken snapshot.<br><br>"
								+ "The backup files created by Mod Manager are placed in the following folder:<br>" + backupLocation
								+ "<br><br>MixIns do not support modified files except for those modified by other non-finalizing MixIns.</div></html>",
						"Backup of basegame/unpacked files", JOptionPane.INFORMATION_MESSAGE);
			} else {
				labelStatus.setText("ModMaker requires valid BIOGame directory to start");
				labelStatus.setVisible(true);
				JOptionPane.showMessageDialog(null, "The BIOGame directory is not valid.\nFix the BIOGame directory before continuing.", "Invalid BioGame Directory",
						JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == backupCreateGDB) {
			if (validateBIOGameDir()) {
				createBasegameDB(fieldBiogameDir.getText());
			} else {
				JOptionPane.showMessageDialog(null,
						"The BioGame directory is not valid.\nMod Manager cannot update or create the game repair database.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else

		if (e.getSource() == restoreRevertCoal) {
			if (validateBIOGameDir()) {
				restoreCoalesced(fieldBiogameDir.getText());
			} else {
				labelStatus.setText("Cannot restore files without valid BIOGame directory");
				JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nMod Manager cannot do any restorations.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else

		if (e.getSource() == restoreRevertAllDLC) {
			if (validateBIOGameDir()) {
				restoreDataFiles(fieldBiogameDir.getText(), RestoreMode.ALLDLC);
			} else {
				labelStatus.setText("Cannot restore files without valid BIOGame directory");
				JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nMod Manager cannot do any restorations.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == restoredeleteAllCustomDLC) {
			if (validateBIOGameDir()) {
				if (JOptionPane.showConfirmDialog(this, "This will delete all folders in the BIOGame/DLC folder that aren't known to be official.\nDelete all custom DLC?",
						"Delete all Custom DLC", JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {

					restoreDataFiles(fieldBiogameDir.getText(), RestoreMode.REMOVECUSTOMDLC);
				}
			} else {
				labelStatus.setText("Can't remove custom DLC with invalid BIOGame directory");
				JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nMod Manager cannot do any restorations.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == restoreCustomDLCManager) {
			if (validateBIOGameDir()) {
				new CustomDLCWindow(fieldBiogameDir.getText());
			} else {
				labelStatus.setText("Custom DLC Manager requires valid BIOGame directory");
				JOptionPane.showMessageDialog(null,
						"The BioGame directory is not valid.\nCustom DLC Manager requires a valid directory.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == restoreSelective) {
			if (validateBIOGameDir()) {
				new SelectiveRestoreWindow(fieldBiogameDir.getText());
			} else {
				labelStatus.setText("Custom Restore requires valid BIOGame directory");
				JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nCustom Restore requires a valid directory.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == restoreRevertBasegame) {
			if (validateBIOGameDir()) {
				restoreDataFiles(fieldBiogameDir.getText(), RestoreMode.BASEGAME);
			} else {
				labelStatus.setText("Cannot restore files without valid BIOGame directory");
				JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nMod Manager cannot do any restorations.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == restoreRevertUnpacked) {
			if (validateBIOGameDir()) {
				restoreDataFiles(fieldBiogameDir.getText(), RestoreMode.UNPACKED);
			} else {
				labelStatus.setText("Cannot restore files without valid BIOGame directory");
				JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nMod Manager cannot do any restorations.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == restoreRevertBasegameUnpacked) {
			if (validateBIOGameDir()) {
				restoreDataFiles(fieldBiogameDir.getText(), RestoreMode.UNPACKEDBASEGAME);
			} else {
				labelStatus.setText("Cannot restore files without valid BIOGame directory");
				JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nMod Manager cannot do any restorations.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == restoreVanillifyDLC) {
			if (validateBIOGameDir()) {
				if (JOptionPane.showConfirmDialog(this,
						"This will delete all unpacked DLC items, including backups of those files.\nThe backup files are deleted because you shouldn't restore unpacked files if your DLC isn't set up for unpacked files.\nMake sure you have your *original* SFARs backed up! Otherwise you will have to use Origin to download them again.\nAre you sure you want to continue?",
						"Delete unpacked DLC files", JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {

					restoreDataFiles(fieldBiogameDir.getText(), RestoreMode.VANILLIFYDLC);
				}
			} else {
				labelStatus.setText("Cannot restore files without valid BIOGame directory");
				JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nMod Manager cannot do any restorations.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == restoreRevertSPDLC) {
			if (validateBIOGameDir()) {
				restoreDataFiles(fieldBiogameDir.getText(), RestoreMode.SP);
			} else {
				labelStatus.setText("Cannot restore files without valid BIOGame directory");
				JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nMod Manager cannot do any restorations.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}

		} else if (e.getSource() == restoreRevertMPDLC) {
			if (validateBIOGameDir()) {
				restoreDataFiles(fieldBiogameDir.getText(), RestoreMode.MP);
			} else {
				labelStatus.setText("Cannot restore files without valid BIOGame directory");
				JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nMod Manager cannot do any restorations.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == restoreRevertSPBaseDLC) {
			if (validateBIOGameDir()) {
				restoreDataFiles(fieldBiogameDir.getText(), RestoreMode.SPBASE);
			} else {
				labelStatus.setText("Cannot restore files without valid BIOGame directory");
				JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nMod Manager cannot do any restorations.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == restoreRevertMPBaseDLC) {
			if (validateBIOGameDir()) {
				restoreDataFiles(fieldBiogameDir.getText(), RestoreMode.MPBASE);
			} else {
				labelStatus.setText("Cannot restore files without valid BIOGame directory");
				JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nMod Manager cannot do any restorations.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else

		if (e.getSource() == restoreRevertEverything) {
			if (validateBIOGameDir()) {
				if (validateBIOGameDir()) {
					if (JOptionPane.showConfirmDialog(this,
							"This will delete all unpacked DLC items, including backups of those files.\nThe backup files are deleted because you shouldn't restore unpacked files if your DLC isn't set up for unpacked files.\nMake sure you have your *original* SFARs backed up! Otherwise you will have to use Origin to download them again.\nAre you sure you want to continue?",
							"Delete unpacked DLC files", JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {

						restoreCoalesced(fieldBiogameDir.getText());
						restoreDataFiles(fieldBiogameDir.getText(), RestoreMode.ALL);
					}
				} else {
					labelStatus.setText("Cannot restore files without valid BIOGame directory");
					JOptionPane.showMessageDialog(null,
							"The BioGame directory is not valid.\nMod Manager cannot do any restorations.\nFix the BioGame directory before continuing.",
							"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
				}
			}
		} else if (e.getSource() == restoreDeleteUnpacked) {
			if (validateBIOGameDir()) {
				if (JOptionPane.showConfirmDialog(this,
						"This will delete all unpacked DLC items, including backups of those files.\nThe backup files are deleted because you shouldn't restore unpacked files if your DLC isn't set up for unpacked files.\nMake sure you have your *original* SFARs backed up! Otherwise you will have to use Origin to download them again.\nAre you sure you want to continue?",
						"Delete unpacked DLC files", JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
					restoreDataFiles(fieldBiogameDir.getText(), RestoreMode.REMOVEUNPACKEDITEMS);
				}
			} else {
				labelStatus.setText("Cannot delete files with invalid BIOGame directory");

				JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nMod Manager cannot do any restorations.\nFix the BioGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else

		if (e.getSource() == actionModMaker) {
			URI theURI;
			try {
				theURI = new URI("http://me3tweaks.com/modmaker");
				java.awt.Desktop.getDesktop().browse(theURI);
			} catch (URISyntaxException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			} catch (IOException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			}
		} else if (e.getSource() == modWebsiteLink) {
			Mod mod = modModel.get(modList.getSelectedIndex());
			try {
				ModManager.openWebpage(new URI(mod.getModSite()));
			} catch (URISyntaxException e1) {
				// TODO Auto-generated catch block
				ModManager.debugLogger.writeErrorWithException("Unable to open this mod's web site:", e1);
			}
		} else if (e.getSource() == modManagementImportAlreadyInstalled) {
			if (validateBIOGameDir()) {
				new ModImportDLCWindow(this, fieldBiogameDir.getText());
			}
		} else if (e.getSource() == modManagementImportFromArchive) {
			new ModImportArchiveWindow();
		}
		if (e.getSource() == actionVisitMe) {
			URI theURI;
			try {
				theURI = new URI("http://me3tweaks.com");
				java.awt.Desktop.getDesktop().browse(theURI);
			} catch (URISyntaxException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			} catch (IOException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			}
		} else if (e.getSource() == actionReload) {
			// Reload this jframe
			new ModManagerWindow(false);
		} else

		if (e.getSource() == actionExit) {
			ModManager.debugLogger.writeMessage("User selected exit from Actions Menu");
			System.exit(0);
		} else if (e.getSource() == buttonApplyMod) {
			if (validateBIOGameDir()) {
				ModManager.debugLogger.writeMessage("Applying selected mod: Biogame Dir is valid.");
				if (ModManager.validateNETFrameworkIsInstalled()) {
					updateApplyButton();
					ModManager.debugLogger.writeMessage(".NET is installed");
					applyMod();
				} else {
					updateApplyButton();
					labelStatus.setText(".NET Framework 4.5 or higher is missing");
					ModManager.debugLogger.writeMessage("Applying selected mod: .NET is not installed");
					new NetFrameworkMissingWindow("You must install .NET Framework 4.5 or higher in order to install mods.");
				}
			} else {
				labelStatus.setText("Installing a mod requires valid BIOGame path");
				labelStatus.setVisible(true);
				JOptionPane.showMessageDialog(null, "The BIOGame directory is not valid.\nFix the BIOGame directory before continuing.", "Invalid BioGame Directory",
						JOptionPane.ERROR_MESSAGE);
			}
		} else

		if (e.getSource() == buttonStartGame) {
			if (validateBIOGameDir()) {
				ModManager.debugLogger.writeMessage("Starting game/launcherwv.");
				startGame(ModManager.appendSlash(fieldBiogameDir.getText()));
			} else {
				labelStatus.setText("Starting the game requires a valid BIOGame directory");
				labelStatus.setVisible(true);
				JOptionPane.showMessageDialog(null,
						"The BIOGame directory is not valid.\nMod Manager does not know where to launch the game executable.\nFix the BIOGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else

		if (e.getSource() == toolME3Explorer) {
			if (ModManager.validateNETFrameworkIsInstalled()) {
				updateApplyButton();
				ModManager.debugLogger.writeMessage(".NET is installed");
				ArrayList<String> commandBuilder = new ArrayList<String>();
				String me3expdir = ModManager.appendSlash(ModManager.getME3ExplorerEXEDirectory(true));
				commandBuilder.add(me3expdir + "ME3Explorer.exe");
				// System.out.println("Building command");
				String[] command = commandBuilder.toArray(new String[commandBuilder.size()]);
				// Debug stuff
				StringBuilder sb = new StringBuilder();
				for (String arg : command) {
					sb.append(arg + " ");
				}
				ModManager.debugLogger.writeMessage("Executing ME3Explorer (via action menu) via command: " + sb.toString());
				try {
					ProcessBuilder pb = new ProcessBuilder(command);
					pb.directory(new File(me3expdir)); // this is where you set the root folder for the executable to run with
					pb.start();
				} catch (IOException ex) {
					ModManager.debugLogger.writeMessage(ExceptionUtils.getStackTrace(ex));
				}
			} else {
				updateApplyButton();
				labelStatus.setText(".NET Framework 4.5 or higher is missing");
				ModManager.debugLogger.writeMessage("Run ME3Explorer: .NET is not installed");
				new NetFrameworkMissingWindow("ME3Explorer requires .NET 4.5 or higher in order to run.");
			}
		} else if (e.getSource() == toolTankmasterCoalFolder) {
			ResourceUtils.openDir(ModManager.getTankMasterCompilerDir());
		} else if (e.getSource() == modManagementOpenModsFolder) {
			ResourceUtils.openDir(ModManager.getModsDir());
		} else if (e.getSource() == toolTankmasterTLK) {
			if (ModManager.validateNETFrameworkIsInstalled()) {
				updateApplyButton();
				ArrayList<String> commandBuilder = new ArrayList<String>();
				String tankmasterTLKDir = ModManager.getTankMasterTLKDir();
				commandBuilder.add(tankmasterTLKDir + "MassEffect3.TlkEditor.exe");
				// System.out.println("Building command");
				String[] command = commandBuilder.toArray(new String[commandBuilder.size()]);
				// Debug stuff
				StringBuilder sb = new StringBuilder();
				for (String arg : command) {
					sb.append(arg + " ");
				}
				ModManager.debugLogger.writeMessage("Executing Tankmaster TLK (via action menu) via command: " + sb.toString());
				try {
					ProcessBuilder pb = new ProcessBuilder(command);
					pb.directory(new File(tankmasterTLKDir)); // this is where you set the root folder for the executable to run with
					pb.start();
				} catch (IOException ex) {
					ModManager.debugLogger.writeMessage(ExceptionUtils.getStackTrace(ex));
				}
			} else {
				updateApplyButton();
				labelStatus.setText(".NET Framework 4.5 or higher is missing");
				ModManager.debugLogger.writeMessage("Run TLK: .NET is not installed");
				new NetFrameworkMissingWindow("Tankmaster's TLK Tool requires .NET 4.5 or higher in order to run.");
			}
		} else if (e.getSource() == toolTankmasterCoalUI) {
			if (ModManager.validateNETFrameworkIsInstalled()) {
				new CoalescedWindow();
			} else {
				updateApplyButton();
				labelStatus.setText(".NET Framework 4.5 or higher is missing");
				ModManager.debugLogger.writeMessage("Run ME3Explorer: .NET is not installed");
				new NetFrameworkMissingWindow("ME3Explorer requires .NET 4.5 or higher in order to run.");
			}
		} else if (e.getSource() == actionOptions) {
			new OptionsWindow(this);
		} else if (e.getSource() == toolsMergeMod) {
			if (ModManager.validateNETFrameworkIsInstalled()) {
				ModManager.debugLogger.writeMessage("Opening Mod Merging utility");
				updateApplyButton();
				new MergeModWindow(this);
			} else {
				updateApplyButton();
				labelStatus.setText(".NET Framework 4.5 or higher is missing");
				ModManager.debugLogger.writeMessage("Merge Tool: Missing .NET Framework");
				new NetFrameworkMissingWindow("You must install .NET Framework 4.5 or higher in order to merge mods.");
			}
		} else if (e.getSource() == toolsUnpackDLC) {
			if (validateBIOGameDir()) {
				if (ModManager.validateNETFrameworkIsInstalled()) {
					ModManager.debugLogger.writeMessage("Opening Unpack DLC window");
					updateApplyButton();
					new UnpackWindow(this, fieldBiogameDir.getText());
				} else {
					updateApplyButton();
					labelStatus.setText(".NET Framework 4.5 or higher is missing");
					ModManager.debugLogger.writeMessage("Unpack DLC Tool: Missing .NET Framework");
					new NetFrameworkMissingWindow("You must install .NET Framework 4.5 or higher in order to unpack DLC.");
				}
			} else {
				labelStatus.setText("Unpacking DLC requires a valid BIOGame directory");
				labelStatus.setVisible(true);
				JOptionPane.showMessageDialog(null,
						"The BIOGame directory is not valid.\nCannot unpack DLC without a valid BIOGame directory.\nFix the BIOGame directory before continuing.",
						"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == toolsMountdlcEditor) {
			new MountFileEditorWindow();
		} else if (e.getSource() == toolsOpenME3Dir) {
			openME3Dir();
		} else if (e.getSource() == modutilsAutoTOC) {
			if (ModManager.validateNETFrameworkIsInstalled()) {
				ModManager.debugLogger.writeMessage("Running AutoTOC.");
				updateApplyButton();
				autoTOC(AutoTocWindow.LOCALMOD_MODE);
			} else {
				updateApplyButton();
				labelStatus.setText(".NET Framework 4.5 or higher is missing");
				ModManager.debugLogger.writeMessage("AutoTOC: Missing .NET Framework");
				new NetFrameworkMissingWindow("You must install .NET Framework 4.5 or higher in order to use the AutoTOC feature.");
			}

		} else if (e.getSource() == modutilsInfoEditor) {
			showInfoEditor();
		} else if (e.getSource() == sqlWavelistParser) {
			new WavelistGUI();
		} else if (e.getSource() == modutilsCheckforupdate) {
			new SingleModUpdateCheckThread(modModel.getElementAt(modList.getSelectedIndex())).execute();
		} else if (e.getSource() == modutilsInstallCustomKeybinds) {
			new KeybindsInjectionWindow(this, modModel.getElementAt(modList.getSelectedIndex()), false);
		} else if (e.getSource() == modManagementCheckallmodsforupdate) {
			checkAllModsForUpdates(true);
		} else if (e.getSource() == modManagementPatchLibary) {
			if (validateBIOGameDir()) {
				if (ModManager.validateNETFrameworkIsInstalled()) {
					ModManager.debugLogger.writeMessage("Opening patch library window.");
					updateApplyButton();
					new PatchLibraryWindow();
				} else {
					updateApplyButton();
					labelStatus.setText(".NET Framework 4.5 or higher is missing");
					ModManager.debugLogger.writeMessage("Patch Library: Missing .NET Framework");
					new NetFrameworkMissingWindow("You must install .NET Framework 4.5 or higher in order to use MixIns.");
				}
			} else {
				labelStatus.setText("Use of the patch library requires a valid BIOGame folder");
				labelStatus.setVisible(true);
				JOptionPane.showMessageDialog(null, "The BIOGame directory is not valid.\nFix the BIOGame directory before continuing.", "Invalid BioGame Directory",
						JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == modDeltaRevert) {
			//if (validateBIOGameDir()) {
			if (ModManager.validateNETFrameworkIsInstalled()) {
				ModManager.debugLogger.writeMessage("Reverting a delta.");
				new DeltaWindow(modModel.get(modList.getSelectedIndex()));
			} else {
				labelStatus.setText(".NET Framework 4.5 or higher is missing");
				ModManager.debugLogger.writeMessage("Revert Delta: Missing .NET Framework");
				new NetFrameworkMissingWindow("You must install .NET Framework 4.5 or higher to switch mod variants.");
			}
		} else if (e.getSource() == modutilsVerifyDeltas) {
			if (ModManager.validateNETFrameworkIsInstalled()) {
				ModManager.debugLogger.writeMessage("Verifying deltas");
				Mod mod = modModel.get(modList.getSelectedIndex());
				for (ModDelta delta : mod.getModDeltas()) {
					new DeltaWindow(mod, delta, true, false);
				}
			} else {
				labelStatus.setText(".NET Framework 4.5 or higher is missing");
				ModManager.debugLogger.writeMessage("Patch Library: Missing .NET Framework");
				new NetFrameworkMissingWindow("You must install .NET Framework 4.5 or higher to switch mod variants.");
			}
		} else if (e.getSource() == modutilsUpdateXMLGenerator) {
			ModManager.debugLogger.writeMessage(ModXMLTools.generateXMLFileList(modModel.getElementAt(modList.getSelectedIndex())));
		} else if (e.getSource() == sqlDifficultyParser) {
			new DifficultyGUI();
		} else

		if (e.getSource() == sqlAIWeaponParser) {
			new BioAIGUI();
		} else

		if (e.getSource() == sqlPowerCustomActionParser) {
			new PowerCustomActionGUI();
		} else if (e.getSource() == sqlPowerCustomActionParser2) {
			new PowerCustomActionGUI2();
		} else if (e.getSource() == sqlConsumableParser) {
			new ConsumableGUI();
		} else if (e.getSource() == sqlGearParser) {
			// new GearGUI();
		} else if (e.getSource() == toolsInstallLauncherWV) {
			if (validateBIOGameDir()) {
				ModManager.debugLogger.writeMessage("Installing manual LauncherWV bypass.");
				installBypass();
			} else {
				labelStatus.setText("Installing DLC bypass requires valid BIOGame directory");
				labelStatus.setVisible(true);
				JOptionPane.showMessageDialog(null, "The BIOGame directory is not valid.\nFix the BIOGame directory before continuing.", "Invalid BioGame Directory",
						JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == toolsInstallBinkw32) {
			if (validateBIOGameDir()) {
				ModManager.debugLogger.writeMessage("Installing manual Binkw32 bypass.");
				installBinkw32Bypass();
			} else {
				labelStatus.setText("Installing DLC bypass requires valid BIOGame directory");
				labelStatus.setVisible(true);
				JOptionPane.showMessageDialog(null, "The BIOGame directory is not valid.\nFix the BIOGame directory before continuing.", "Invalid BioGame Directory",
						JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == toolsUninstallBinkw32) {
			if (validateBIOGameDir()) {
				ModManager.debugLogger.writeMessage("Uninstalling manual binkw32 bypass.");
				uninstallBinkw32Bypass();
			} else {
				labelStatus.setText("Installing DLC bypass requires valid BIOGame directory");
				labelStatus.setVisible(true);
				JOptionPane.showMessageDialog(null, "The BIOGame directory is not valid.\nFix the BIOGame directory before continuing.", "Invalid BioGame Directory",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void checkAllModsForUpdates(boolean isManualCheck) {
		//Fix for moonshine mod v1
		for (int i = 0; i < modModel.size(); i++) {
			Mod mod = modModel.getElementAt(i);
			if ("MoonShine".equals(mod.getAuthor())) {
				if ("360 Controller Support".equals(mod.getModName())) {
					if (mod.getVersion() == 0) {
						mod.setModUpdateCode(15);
						mod.setVersion(1);
					}
				}
			}
		}

		ArrayList<Mod> updatableMods = new ArrayList<Mod>();
		for (int i = 0; i < modModel.size(); i++) {
			Mod mod = modModel.get(i);
			if (mod.isME3TweaksUpdatable()) {
				updatableMods.add(mod);
			} else {
				ModManager.debugLogger.writeMessage(mod.getModName() + " is not ME3Tweaks updatable");
			}
		}

		if (updatableMods.size() > 0) {
			new AllModsUpdateWindow(this, isManualCheck, updatableMods);
		} else {
			if (isManualCheck) {
				JOptionPane.showMessageDialog(null,
						"No mods are eligible for the Mod Manager update service.\nEligible mods include ModMaker mods and ones hosted on ME3Tweaks.com.", "No updatable mods",
						JOptionPane.WARNING_MESSAGE);
			}
			Wini ini;
			try {
				File settings = new File(ModManager.SETTINGS_FILENAME);
				if (!settings.exists())
					settings.createNewFile();
				ini = new Wini(settings);
				ini.put("Settings", "lastautocheck", System.currentTimeMillis());
				ModManager.debugLogger.writeMessage("Updating last-autocheck date in ini");
				ini.store();
				ModManager.LAST_AUTOUPDATE_CHECK = System.currentTimeMillis();
			} catch (InvalidFileFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				ModManager.debugLogger.writeErrorWithException("Settings file encountered an I/O error while attempting to write it. Settings not saved.", e);
			}
		}
	}

	/**
	 * Opens a explorer.exe window with ME3 directory
	 */
	private void openME3Dir() {
		File file = new File(fieldBiogameDir.getText());
		if (!file.exists()) {
			JOptionPane.showMessageDialog(null, "The BioGame directory does not exist.", "Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			Desktop.getDesktop().open(new File(fieldBiogameDir.getText()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			ModManager.debugLogger.writeErrorWithException("I/O Exception while opening ME3Dir.", e);
		}
	}

	/**
	 * Deletes the Custom DLC of this mod selected
	 */
	private void uninstallCustomDLC() {
		if (validateBIOGameDir()) {
			String dlcdir = ModManager.appendSlash(fieldBiogameDir.getText()) + "DLC" + File.separator;
			Mod mod = modModel.get(modList.getSelectedIndex());
			ModManager.debugLogger.writeMessage("Deleting custom content for mod: " + mod.getModName());
			for (ModJob job : mod.jobs) {
				if (job.getJobType() != ModJob.CUSTOMDLC) {
					continue; // skip
				}

				for (String customDLCfolder : job.getDestFolders()) {
					File custDLC = new File(dlcdir + customDLCfolder);
					ModManager.debugLogger.writeMessage("Checking for custom content installed: " + custDLC.getAbsolutePath());

					if (custDLC.exists()) {
						try {
							FileUtils.forceDelete(custDLC);
							ModManager.debugLogger.writeMessage("Deleted custom DLC content " + custDLC.getAbsolutePath());
						} catch (IOException e) {
							ModManager.debugLogger.writeError("Couldn't delete custom DLC content " + custDLC.getAbsolutePath());
							ModManager.debugLogger.writeException(e);
						}
					}
				}
			}
			labelStatus.setText("Deleted custom DLC content");
		}
	}

	/**
	 * Shows the name/descip editor
	 */
	private void showInfoEditor() {
		// TODO Auto-generated method stub
		int selectedIndex = modList.getSelectedIndex();
		if (selectedIndex < 0) {
			return; // shouldn't be able to toc an unselected mod eh?
		}
		// System.out.println("SELECTED VALUE: " + selectedValue);
		Mod mod = modModel.get(selectedIndex);
		new ModInfoEditorWindow(this, mod);
	}

	private void createBasegameDB(String biogameDir) {
		File file = new File(biogameDir);
		try {
			new BasegameHashDB(this, file.getParent(), true);
		} catch (SQLException e) {
			//nothing yet...
		}
	}

	private void backupDLC(String bioGameDir) {
		// Check that biogame is valid
		if (validateBIOGameDir()) {
			new BackupWindow(this, bioGameDir);
		} else {
			// Biogame is invalid
			JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nDLC cannot be not backed up.\nFix the BioGame directory before continuing.",
					"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			labelStatus.setText("DLC backup failed");
			labelStatus.setVisible(true);
		}
		return;
	}

	private void autoBackupDLC(String bioGameDir, String dlcName) {
		// Check that biogame is valid
		if (validateBIOGameDir()) {
			new BackupWindow(this, bioGameDir, dlcName);
		} else {
			// Biogame is invalid
			JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nDLC cannot be not backed up.\nFix the BioGame directory before continuing.",
					"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
			labelStatus.setText("DLC backup failed");
			labelStatus.setVisible(true);
		}
		return;
	}

	/**
	 * Checks that the user has chosen a correct biogame directory.
	 */
	private void checkForValidBioGame() {
		File coalesced = new File(ModManager.appendSlash(dirChooser.getSelectedFile().toString()) + "CookedPCConsole\\Coalesced.bin");
		if (coalesced.exists()) {
			String YesNo[] = { "Yes", "No" };
			int saveDir = JOptionPane.showOptionDialog(null, "BioGame directory set to: " + dirChooser.getSelectedFile().toString() + "\nSave this path?", "Save BIOGame path?",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, YesNo, YesNo[0]);
			if (saveDir == 0) {
				Wini ini;
				try {
					File settings = new File(ModManager.SETTINGS_FILENAME);
					if (!settings.exists())
						settings.createNewFile();
					ini = new Wini(settings);
					ini.put("Settings", "biogame_dir", ModManager.appendSlash(dirChooser.getSelectedFile().toString()));
					ModManager.debugLogger.writeMessage(ModManager.appendSlash(dirChooser.getSelectedFile().toString()));
					ini.store();
				} catch (InvalidFileFormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					ModManager.debugLogger.writeErrorWithException("Settings file encountered an I/O error while attempting to write it. Settings not saved.", e);
				}
				labelStatus.setText("Saved BioGame directory to me3cmm.ini");
				labelStatus.setVisible(true);
			}
			fieldBiogameDir.setText(dirChooser.getSelectedFile().toString());
		} else {
			JOptionPane.showMessageDialog(null, "Coalesced.bin not found in " + dirChooser.getSelectedFile().toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Checks if the string in the biogamedir is a valid biogame directory.
	 * 
	 * @return True if valid, false otherwise
	 */
	public static boolean validateBIOGameDir() {
		File coalesced = new File(ModManager.appendSlash(ModManagerWindow.ACTIVE_WINDOW.fieldBiogameDir.getText()) + "CookedPCConsole\\Coalesced.bin");
		if (coalesced.exists()) {
			return true;
		} else {
			return false;
		}
	}

	private String getInitialBiogameDirText() {
		ModManager.debugLogger.writeMessage("Getting location of Mass Effect 3 directory.");
		Wini settingsini;
		String defaultDir = "C:\\Program Files (x86)\\Origin Games\\Mass Effect 3\\BIOGame\\";
		String setDir = "C:\\Program Files (x86)\\Origin Games\\Mass Effect 3\\BIOGame\\";
		String os = System.getProperty("os.name");
		ModManager.debugLogger.writeMessage("Entering getInitialBiogameDirText() try block");

		try {
			settingsini = new Wini(new File(ModManager.SETTINGS_FILENAME));
			setDir = settingsini.get("Settings", "biogame_dir");
			ModManager.debugLogger.writeMessage("setDir = " + setDir);
			if ((setDir == null || setDir.equals("")) && os.contains("Windows")) {
				String installDir = null;
				String _32bitpath = "SOFTWARE\\BioWare\\Mass Effect 3";
				String _64bitpath = "SOFTWARE\\Wow6432Node\\BioWare\\Mass Effect 3";
				ModManager.debugLogger.writeMessage("OS contains windows and setDir is null or blank. trying 64bit registry key");
				try {
					installDir = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, _64bitpath, "Install Dir");
					ModManager.debugLogger.writeMessage("found installdir via 64bit reg key");
				} catch (com.sun.jna.platform.win32.Win32Exception keynotfoundException) {
					ModManager.debugLogger.writeMessage("Exception looking at 64 registry key: " + _64bitpath);
				}

				if (installDir == null) {
					//try 32bit key
					try {
						installDir = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, _32bitpath, "Install Dir");
						ModManager.debugLogger.writeMessage("OS contains windows and setDir is null or blank. trying 32bit registry key");
					} catch (com.sun.jna.platform.win32.Win32Exception keynotfoundException) {
						ModManager.debugLogger.writeMessage("Exception looking at 32bit registry key: " + _32bitpath);
					}
				}
				if (installDir != null) {
					ModManager.debugLogger.writeMessage("Found mass effect 3 location in registry: " + installDir);
					setDir = ModManager.appendSlash(installDir) + "BIOGame";
				} else {
					ModManager.debugLogger.writeError("Could not find Mass Effect 3 registry key in both 32 and 64 bit locations.");
				}
			}
		} catch (InvalidFileFormatException e) {
			ModManager.debugLogger.writeError("Invalid file format exception writing to settings ini.");
		} catch (IOException e) {
			ModManager.debugLogger.writeErrorWithException("I/O Exception attemping to get ME3 install directory. Could be a settings file not writing: ", e);
		} catch (Throwable e) {
			ModManager.debugLogger.writeErrorWithException("Error occured while attempting to get/set the biogame directory! Could be the JNA crash.", e);
			return "C:\\Program Files (x86)\\Origin Games\\Mass Effect 3\\BIOGame";
		}
		ModManager.debugLogger.writeMessage("Directory that was fetched: " + setDir);
		return (setDir != null && !setDir.equals("")) ? setDir : defaultDir;
	}

	/**
	 * Installs the mod.
	 * 
	 * @return True if the file copied, otherwise false
	 */
	private boolean applyMod() {
		// Prepare
		labelStatus.setText("Installing mod...");
		labelStatus.setVisible(true);

		// Validate BioGame Dir
		File coalesced = new File(ModManager.appendSlash(fieldBiogameDir.getText()) + "CookedPCConsole\\" + "Coalesced.bin");
		if (ModManager.logging) {
			ModManager.debugLogger.writeMessage("Validating BioGame dir: " + coalesced);
		}
		if (!coalesced.exists()) {
			JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.", "Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);

			labelStatus.setText("Mod not installed");
			labelStatus.setVisible(true);
			return false;
		}

		Mod mod = modModel.get(modList.getSelectedIndex());
		// Read mod
		// Apply The Mod
		// Update Coalesced (main game)
		if (mod.modsCoal()) {
			if (ModManager.checkDoOriginal(fieldBiogameDir.getText())) {
				// check source file
				File source = new File(ModManager.appendSlash(mod.getModPath()) + "Coalesced.bin");

				if (!source.exists()) {
					labelStatus.setText("Mod not installed");
					labelStatus.setVisible(true);
					JOptionPane.showMessageDialog(null,
							"Coalesced.bin was not found in the Mod file's directory.\nIt might have moved, been deleted, or renamed.\nPlease check this mod's directory.",
							"Coalesced not found", JOptionPane.ERROR_MESSAGE);

					return false;
				}

				String destFile = coalesced.toString();
				String sourceFile = ModManager.appendSlash(mod.getModPath()) + "Coalesced.bin";
				try {
					Files.copy(Paths.get(sourceFile), Paths.get(destFile), StandardCopyOption.REPLACE_EXISTING);
					if (mod.getJobs().length == 0) {
						labelStatus.setText(mod.getModName() + " installed");
					} else {
						labelStatus.setText("Injecting files into DLC modules...");
					}
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, "Copying Coalesced.bin failed. Stack trace:\n" + e.getMessage(), "Error copying Coalesced.bin", JOptionPane.ERROR_MESSAGE);
					labelStatus.setText("Mod failed to install");
				}
			} else {

				labelStatus.setText("Mod not installed");
				labelStatus.setVisible(true);
				return false;
			}
		}

		if (mod.getJobs().length > 0) {
			checkBackedUp(mod);
			new ModInstallWindow(this, mod.getJobs(), fieldBiogameDir.getText(), mod);
		} else {
			ModManager.debugLogger.writeMessage("No dlc mod job, finishing mod installation");
		}
		return true;
	}

	private void checkBackedUp(Mod mod) {
		ModJob[] jobs = mod.getJobs();
		for (ModJob job : jobs) {
			if (job.getJobType() == ModJob.BASEGAME || job.getJobType() == ModJob.CUSTOMDLC) {
				continue; // we can't really check for a .bak of Coalesced.
			}
			// Default.sfar
			File mainFile = new File(ModManager.appendSlash(fieldBiogameDir.getText()) + job.getDLCFilePath() + "\\Default.sfar");
			boolean defaultsfarMainFileExists = mainFile.exists();
			File backFile = new File(ModManager.appendSlash(fieldBiogameDir.getText()) + job.getDLCFilePath() + "\\Default.sfar.bak");
			System.out.println("Checking for backup file: " + backFile.getAbsolutePath());
			if (!backFile.exists()) {
				// Patch_001.sfar
				mainFile = new File(ModManager.appendSlash(fieldBiogameDir.getText()) + job.getDLCFilePath() + "\\Patch_001.sfar");
				boolean patch001farMainFileExists = mainFile.exists();
				backFile = new File(ModManager.appendSlash(fieldBiogameDir.getText()) + job.getDLCFilePath() + "\\Patch_001.sfar.bak");
				System.out.println("Checking for TESTPATCH file: " + backFile.getAbsolutePath());

				if ((defaultsfarMainFileExists || patch001farMainFileExists) && !backFile.exists()) {
					String YesNo[] = { "Yes", "No" }; // Yes/no buttons
					int showDLCBackup = JOptionPane.showOptionDialog(null, "<html>" + job.getJobName() + " DLC has not been backed up.<br>Back it up now?</hmtl>", "Backup DLC",
							JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, YesNo, YesNo[0]);
					if (showDLCBackup == 0) {
						autoBackupDLC(fieldBiogameDir.getText(), job.getJobName());
					}
				}
			}
		}
	}

	/**
	 * Handles looking up the name of a mod from the mod object that it comes
	 * from. Uses a hash map.
	 * 
	 * @param modName
	 *            Name of the mod from the list (display name)
	 * @return File that describes the selected mod
	 */

	@Override
	public void valueChanged(ListSelectionEvent listChange) {
		if (listChange.getValueIsAdjusting() == false) {
			if (modList.getSelectedIndex() == -1) {
				buttonApplyMod.setEnabled(false);
				if (ModManager.NET_FRAMEWORK_IS_INSTALLED) {
					buttonApplyMod.setToolTipText("Select a mod on the left");
				} else {
					buttonApplyMod.setToolTipText("Mod Manager requires .NET Framework 4.5 or higher in order to install mods");
				}
				fieldDescription.setText(selectAModDescription);
				modMenu.setEnabled(false);
				modMenu.setToolTipText("Select a mod to enable this menu");
			} else {
				Mod selectedMod = modModel.get(modList.getSelectedIndex());
				modMenu.setToolTipText(null);

				// Update mod description
				fieldDescription.setText(selectedMod.getModDisplayDescription());
				fieldDescription.setCaretPosition(0);
				buttonApplyMod.setEnabled(true);
				if (ModManager.NET_FRAMEWORK_IS_INSTALLED) {
					buttonApplyMod.setToolTipText(
							"<html>Apply this mod to the game.<br>If another mod is already installed, restore your game first!<br>You can merge Mod Manager mods in the Tools menu.</html>");
				} else {
					buttonApplyMod.setToolTipText("Mod Manager requires .NET Framework 4.5 or higher in order to install mods");
				}
				modutilsHeader.setText(modModel.get(modList.getSelectedIndex()).getModName());
				modMenu.setEnabled(true);
				if (selectedMod.isME3TweaksUpdatable()) {
					modutilsCheckforupdate.setEnabled(true);
					modutilsCheckforupdate.setText("Check for updates");
					modutilsCheckforupdate.setToolTipText("Checks for updates to this mod from ME3Tweaks");
				} else {
					modutilsCheckforupdate.setEnabled(false);
					modutilsCheckforupdate.setText("Mod not eligible for updates");
					modutilsCheckforupdate.setToolTipText("<html>Mod update eligibility requires a floating point version number<br>and an update code from ME3Tweaks</html>");
				}

				UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" });
				if (selectedMod.getModSite() != null && urlValidator.isValid(selectedMod.getModSite())) {
					modWebsiteLink.setToolTipText(selectedMod.getModSite());
					modWebsiteLink.setVisible(true);
					modWebsiteLink.setText("<html><u><font color='#000099'>Visit " + selectedMod.getModName() + "'s web site</u></font></html>");
				} else {
					modWebsiteLink.setVisible(false);
				}

				mountMenu.removeAll();

				if (selectedMod.containsCustomDLCJob()) {
					mountMenu.setVisible(true);
					for (ModJob job : selectedMod.getJobs()) {
						if (job.getJobType() == ModJob.CUSTOMDLC) {
							//has custom dlc task
							//for (: job.get
							for (int i = 0; i < job.getFilesToReplace().size(); i++) {
								String target = job.getFilesToReplace().get(i);
								if (FilenameUtils.getName(target).equalsIgnoreCase("mount.dlc")) {
									//mount.dlc file found!
									File mountFile = new File(target);
									String folderName = FilenameUtils.getBaseName(mountFile.getParentFile().getParent());
									JMenuItem mountItem = new JMenuItem("Mount.dlc in " + folderName);
									mountItem.addActionListener(new ActionListener() {

										@Override
										public void actionPerformed(ActionEvent arg0) {
											new MountFileEditorWindow(target);
										}
									});
									mountMenu.add(mountItem);
								}
							}
						}
					}
				} else {
					mountMenu.setVisible(false);
				}

				if (selectedMod.getModDeltas().size() > 0) {
					modDeltaMenu.removeAll();
					modDeltaMenu.add(modDeltaRevert);
					modDeltaMenu.add(modutilsVerifyDeltas);
					File originalVariantFolder = new File(selectedMod.getModPath() + Mod.VARIANT_FOLDER + File.separator + Mod.ORIGINAL_FOLDER);
					modDeltaRevert.setEnabled(originalVariantFolder.exists());
					if (originalVariantFolder.exists()) {
						modDeltaRevert.setToolTipText("Revert to the original version of this mod");
					} else {
						modDeltaRevert.setToolTipText("No variants have been applied yet, so this is the original");
					}

					modDeltaMenu.addSeparator();
					modDeltaMenu.setText(selectedMod.getModDeltas().size() + " available variant" + (selectedMod.getModDeltas().size() != 1 ? "s" : ""));
					modDeltaMenu.setVisible(true);
					modNoDeltas.setVisible(false);
					for (ModDelta delta : selectedMod.getModDeltas()) {
						JMenuItem deltaItem = new JMenuItem(delta.getDeltaName());
						deltaItem.setToolTipText("<html>" + delta.getDeltaDescription() + "</html>");
						deltaItem.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								ModManager.debugLogger.writeMessage("Applying delta " + delta.getDeltaName() + " to " + selectedMod.getModName());
								new DeltaWindow(selectedMod, delta, false, false);
								modDeltaRevert.setEnabled(true);
								modDeltaRevert.setText("Revert to original version");
								modDeltaRevert.setToolTipText("<html>Restores the mod to the original version, without variants applied</html>");
							}
						});
						modDeltaMenu.add(deltaItem);
					}
				} else {
					modDeltaMenu.setVisible(false);
					modNoDeltas.setVisible(true);
				}
			}
		}
	}

	/**
	 * Legacy method from Mod Manager 1+2 to restore the original coalesced file
	 * 
	 * @param bioGameDir
	 * @return
	 */
	private boolean restoreCoalesced(String bioGameDir) {
		String patch3CoalescedHash = "540053c7f6eed78d92099cf37f239e8e";
		File cOriginal = new File(ModManager.getDataDir() + "Coalesced.original");
		if (cOriginal.exists()) {
			// Take the MD5 first to verify it.
			try {
				if (patch3CoalescedHash.equals(MD5Checksum.getMD5Checksum(cOriginal.toString()))) {
					// file is indeed the original
					// Copy
					String destFile = ModManager.appendSlash(bioGameDir) + "CookedPCConsole\\Coalesced.bin";
					if (new File(destFile).exists() == false) {
						JOptionPane.showMessageDialog(null,
								"Coalesced.bin to be restored was not found in the specified BIOGame\\CookedPCConsole directory.\nYou must fix the directory before you can restore Coalesced.",
								"Coalesced not found", JOptionPane.ERROR_MESSAGE);
						labelStatus.setText("Coalesced.bin not restored");
						labelStatus.setVisible(true);
						return false;
					}
					String sourceFile = ModManager.getDataDir() + "Coalesced.original";

					Files.copy(new File(sourceFile).toPath(), new File(destFile).toPath(), StandardCopyOption.REPLACE_EXISTING);
					ModManager.debugLogger.writeMessage("Restored Coalesced.bin");
					labelStatus.setText("Coalesced.bin restored");
					labelStatus.setVisible(true);
					return true;
				} else {
					labelStatus.setText("Coalesced.bin not restored.");
					labelStatus.setVisible(true);
					JOptionPane.showMessageDialog(null,
							"Your backed up original Coalesced.bin file does not match the known original from Mass Effect 3.\nYou'll need to manually restore the original (or what you call your original).\nIf you lost your original you can find a copy of Patch 3's Coalesced on http://me3tweaks.com/tools/modmanager/faq.\nYour current Coalesced has not been changed.",
							"Coalesced Backup Error", JOptionPane.ERROR_MESSAGE);
					return false;
				}
			} catch (Exception e) {
				ModManager.debugLogger.writeErrorWithException("Coalesced.bin was unable to be restored due to an error:", e);
				labelStatus.setText("Coalesced.bin not restored");
				labelStatus.setVisible(true);
				return false;
			}
		} else {
			labelStatus.setText("Coalesced.bin not restored");
			labelStatus.setVisible(true);
			JOptionPane.showMessageDialog(null,
					"The backed up Coalesced.bin file (data/Coalesced.original) does not exist.\nYou'll need to manually restore the original (or what you call your original).\nIf you lost your original you can find a copy of Patch 3's Coalesced on http://me3tweaks.com/tools/modmanager/faq.\nYour current Coalesced has not been changed.\n\nThis error should have been caught but can be thrown due to file system changes \nwhile the program is open.",
					"Coalesced Backup Error", JOptionPane.ERROR_MESSAGE);

		}
		return false;
	}

	/**
	 * Initiates a restore procedure using the specified directory and restore
	 * mode
	 * 
	 * @param bioGameDir
	 *            Directory to biogame folder
	 * @param restoreMode
	 *            constant defining the restore procedure
	 * @return True if all were restored, false otherwise
	 */
	private boolean restoreDataFiles(String bioGameDir, int restoreMode) {
		// Check to make sure biogame is correct
		if (validateBIOGameDir()) {
			new RestoreFilesWindow(bioGameDir, restoreMode);
			return true;
		} else {
			labelStatus.setText("Invalid BioGame Directory");
			labelStatus.setVisible(true);
			JOptionPane.showMessageDialog(null, "The BioGame directory is not valid. Files cannot be restored.\nFix the directory and try again.", "Invalid BioGame Directory",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	private void startGame(String CookedDir) {
		File startingDir = new File(CookedDir);
		ModManager.debugLogger.writeMessage("Starting game.");
		boolean binkw32bypass = checkForBinkBypass(); // favor bink over WV
		startingDir = new File(startingDir.getParent());
		File executable = new File(startingDir.toString() + "\\Binaries\\Win32\\MassEffect3.exe");
		//check ME3 version for 1.6
		try {
			int minorBuildNum = EXEFileInfo.getMinorVersionOfProgram(executable.getAbsolutePath());
			if (minorBuildNum > 5) {
				ModManager.debugLogger.writeMessage("!!!!This user has >1.5 version of Mass Effect 3!!!!");
				JOptionPane.showMessageDialog(this, "<html><div style='width: 300px'>You have a version of Mass Effect 3 higher than 1.5 (1."+minorBuildNum+"), which is the main version most of the world uses.<br>"
						+ "It seems BioWare has been slowly pushing this version out to some users starting in September 2013. Not all users will get this version.<br><br>If you encounter issues you may consider downgrading to the 1.5 EXE. If you play Multiplayer, you will only be able to connect to other 1.6 users, which are very few.<br>Check the ME3Tweaks forums for info on how to downgrade.</div></html>","Mass Effect 3 Rare Version Detected", JOptionPane.WARNING_MESSAGE);
			}
		} catch (Exception e) {
			//do nothing. Continue like the old mod manager did.
			ModManager.debugLogger.writeErrorWithException("Error getting Mass Effect 3 EXE version. This error will be ignored.", e);
		}

		if (!binkw32bypass) { // try to find Launcher_WV
			executable = new File(startingDir.toString() + "\\Binaries\\Win32\\Launcher_WV.exe");
			if (!executable.exists()) {
				// Try the other name he uses
				executable = new File(startingDir.toString() + "\\Binaries\\Win32\\LauncherWV.exe");
				if (!executable.exists()) {
					ModManager.debugLogger.writeMessage("Warranty Voider's memory patcher launcher was not found, using the main one.");
					executable = new File(startingDir.toString() + "\\Binaries\\Win32\\MassEffect3.exe"); // use
																											// standard
				}
			}
		} else {
			ModManager.debugLogger.writeMessage("Binkw32 installed. Launching standard ME3.");
		}
		ModManager.debugLogger.writeMessage("Launching: " + executable.getAbsolutePath());

		// check if the new one exists
		if (!executable.exists()) {
			JOptionPane.showMessageDialog(null,
					"Unable to find game executable in the specified directory:\n" + executable.getAbsolutePath() + "\nMake sure your BIOGame directory is correct.",
					"Unable to Launch Game", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// Executable exists.
		String[] command = { "cmd.exe", "/c", "start", "cmd.exe", "/c", executable.getAbsolutePath() };
		try {
			labelStatus.setText("Launched Mass Effect 3");
			this.setExtendedState(JFrame.ICONIFIED);
			Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			ModManager.debugLogger.writeErrorWithException("I/O Exception while launching ME3 or LauncherWV.", e);

		}
		ModManager.debugLogger.writeMessage("Path: " + executable.getAbsolutePath() + " - Exists? " + executable.exists());
	}

	/**
	 * Checks for the binkw32 bypass. If its there it will skip the LauncherWV
	 * check.
	 * 
	 * @return
	 */
	private boolean checkForBinkBypass() {
		File bgdir = new File(fieldBiogameDir.getText());
		if (!bgdir.exists()) {
			return false;
		}
		File gamedir = bgdir.getParentFile();
		ModManager.debugLogger.writeMessage("Game directory: " + gamedir.toString());
		File bink32 = new File(gamedir.toString() + "\\Binaries\\Win32\\binkw32.dll");
		File bink23 = new File(gamedir.toString() + "\\Binaries\\Win32\\binkw23.dll");
		try {
			String originalBink32MD5 = "128b560ef70e8085c507368da6f26fe6";
			String binkhash = MD5Checksum.getMD5Checksum(bink32.toString());
			if (!binkhash.equals(originalBink32MD5) && bink23.exists()) {
				return true;
			}
		} catch (Exception e) {
			ModManager.debugLogger.writeErrorWithException("Exception while attempting to find DLC bypass (Binkw32).", e);
			ModManager.debugLogger.writeMessage("Bink bypass was not found.");
			ModManager.debugLogger.writeMessage(ExceptionUtils.getStackTrace(e));
		}
		return false;
	}

	/**
	 * Creates a new Mod Maker Compiler dialog with the specified code. Called
	 * from the code entry dialog.
	 * 
	 * @param code
	 *            Code to use for downloading the mod.
	 */
	public void startModMaker(String code, ArrayList<String> languages) {
		new ModMakerCompilerWindow(code, languages);
	}

	private void autoTOC(int mode) {
		// update the PCConsoleTOC's of a specific mod.
		int selectedIndex = modList.getSelectedIndex();
		if (selectedIndex < 0) {
			return; // shouldn't be able to toc an unselected mod eh?
		}
		// System.out.println("SELECTED VALUE: " + selectedValue);
		Mod mod = modModel.getElementAt(selectedIndex);
		new AutoTocWindow(mod, AutoTocWindow.LOCALMOD_MODE, ModManagerWindow.ACTIVE_WINDOW.fieldBiogameDir.getText());
	}

	private boolean installBypass() {
		if (validateBIOGameDir()) {
			boolean result = ModManager.installLauncherWV(fieldBiogameDir.getText());
			if (result) {
				// ok
				labelStatus.setText("Launcher WV installed. Start Game will now use it.");
			} else {
				labelStatus.setText("FAILURE: Launcher WV bypass not installed!");
			}
			return result;
		}
		JOptionPane.showMessageDialog(null,
				"The BioGame directory is not valid.\nMod Manager cannot install the LauncherWV DLC bypass.\nFix the BioGame directory before continuing.",
				"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
		return false;
	}

	private boolean installBinkw32Bypass() {
		if (validateBIOGameDir()) {
			boolean result = ModManager.installBinkw32Bypass(fieldBiogameDir.getText());
			if (result) {
				// ok
				labelStatus.setText("Binkw32 installed. DLC will always authorize.");
			} else {
				labelStatus.setText("FAILURE: Binkw32 bypass not installed!");
			}
			return result;
		}
		JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nMod Manager cannot install Binkw32.dll DLC bypass.\nFix the BioGame directory before continuing.",
				"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
		return false;
	}

	private boolean uninstallBinkw32Bypass() {
		if (validateBIOGameDir()) {
			boolean result = ModManager.uninstallBinkw32Bypass(fieldBiogameDir.getText());
			if (result) {
				// ok
				labelStatus.setText("Binkw32 bypass uninstalled.");
			} else {
				labelStatus.setText("FAILURE: Binkw32 bypass not uninstalled!");
			}
			return result;
		}
		JOptionPane.showMessageDialog(null,
				"The BioGame directory is not valid.\nMod Manager cannot revert the Binkw32.dll DLC bypass.\nFix the BioGame directory before continuing.",
				"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
		return false;
	}

	/**
	 * Gets all mods targeting CMM3 and above
	 * 
	 * @return list of mods targeting CMM3 and above
	 */
	public ArrayList<Mod> getCMM3ModsFromDirectory() {
		ArrayList<Mod> cmm3mods = new ArrayList<Mod>();
		for (int i = 0; i < modModel.size(); i++) {
			Mod mod = modModel.getElementAt(i);
			if (mod.modCMMVer >= 3) {
				cmm3mods.add(mod);
			}
		}
		return cmm3mods;
	}

	public ArrayList<Patch> getPatchesFromLibrary() {

		return null;
	}

	public ArrayList<Patch> getPatchList() {
		return patchList;
	}

	public void setPatchList(ArrayList<Patch> patchList) {
		this.patchList = patchList;
	}

}