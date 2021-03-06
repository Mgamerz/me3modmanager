package com.me3tweaks.modmanager;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.*;

import org.apache.commons.io.FilenameUtils;

import com.me3tweaks.modmanager.objects.Mod;
import com.me3tweaks.modmanager.objects.ModJob;
import com.me3tweaks.modmanager.objects.ModTypeConstants;
import com.me3tweaks.modmanager.objects.ProcessResult;
import com.me3tweaks.modmanager.objects.TocBatchDescriptor;

@SuppressWarnings("serial")
public class AutoTocWindow extends JDialog {
	JLabel infoLabel;
	JProgressBar progressBar;
	public int mode = LOCALMOD_MODE;
	private HashMap<String, String> updatedGameTOCs;
	private Mod mod;
	public static final int LOCALMOD_MODE = 0;
	public static final int UPGRADE_UNPACKED_MODE = 1;
	public static final int INSTALLED_MODE = 1;

	/**
	 * Starts AutoTOC in game-wide autotoc mode. Will update Game TOC files for
	 * basegame and all DLC folders that have a PCConsoleTOC file present. For
	 * official DLCs, the SFAR files are file-size checked, and if they are
	 * original, the step is ignored. Otherwise it updates them via SFAR method
	 * in ME3Explorer.
	 * 
	 * @param biogameDir
	 */
	public AutoTocWindow(String biogameDir) {
		super(null, Dialog.ModalityType.APPLICATION_MODAL);
		if (ModManager.isMassEffect3Running()) {
			JOptionPane.showMessageDialog(ModManagerWindow.ACTIVE_WINDOW, "Mass Effect 3 must be closed in order to run AutoTOC on it.", "MassEffect3.exe is running",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		setupWindow("<center>Updating Basegame and DLC TOC files<br>This may take some time</center>");
		progressBar.setStringPainted(false);
		updatedGameTOCs = new HashMap<String, String>();
		ModManager.debugLogger.writeMessage("===Starting AutoTOC. Mode: GAME-WIDE ===");
		this.setTitle("Mod Manager AutoTOC");

		new GameWideTOCWorker(biogameDir).execute();
		this.setVisible(true);
	}

	/**
	 * Automatically updates PCConsoleTOC files
	 * 
	 * @param mod
	 *            Mod to use for sizes and possible TOC files
	 * @param mode
	 *            mode to use. Local mode uses mod's Toc files. Installed mode
	 *            uses game toc files.
	 * @param biogameDir
	 *            Directory of biogame. Can be null.
	 */
	public AutoTocWindow(Mod mod, int mode, String biogameDir) {
		super(null, Dialog.ModalityType.APPLICATION_MODAL);
		this.mode = mode;
		this.mod = mod;
		updatedGameTOCs = new HashMap<String, String>();
		ModManager.debugLogger.writeMessage("===Starting AutoTOC. Mode: " + mode + "===");
		switch (mode) {
		case LOCALMOD_MODE:
			setupWindow("Updating " + mod.getModName() + "'s PCConsoleTOC files");
			progressBar.setStringPainted(true);
			break;
		case INSTALLED_MODE:
			setupWindow("Updating installed PCConsoleTOCs for " + mod.getModName());
			progressBar.setStringPainted(false);
			break;
		default:
			ModManager.debugLogger.writeError("Unknown AutoTOC mode: " + mode);
			JOptionPane.showMessageDialog(AutoTocWindow.this, "Unknown AutoTOC mode: " + mode, "AutoTOC Error", JOptionPane.ERROR_MESSAGE);
			dispose();
			return;
		}
		new TOCWorker(mod, biogameDir).execute();
		setVisible(true);
	}

	private void setupWindow(String labelText) {
		setTitle("Mod Manager AutoTOC");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setResizable(false);
		setIconImages(ModManager.ICONS);

		JPanel aboutPanel = new JPanel(new BorderLayout());
		infoLabel = new JLabel("<html><center>" + labelText + "</center></html>");
		infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
		aboutPanel.add(infoLabel, BorderLayout.NORTH);
		progressBar = new JProgressBar(0, 100);
		progressBar.setIndeterminate(false);
		aboutPanel.add(progressBar, BorderLayout.CENTER);
		aboutPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		getContentPane().add(aboutPanel);
		pack();
		setLocationRelativeTo(ModManagerWindow.ACTIVE_WINDOW);
	}

	/**
	 * Returns the hashmap of updated TOC files (JOB HEADER NAME => PATH OF TOC)
	 * 
	 * @return null if autotoc did not run in gametoc mode, otherwise list of
	 *         updated TOC files copied from the game and updated
	 */
	public HashMap<String, String> getUpdatedGameTOCs() {
		return updatedGameTOCs;
	}

	/**
	 * Standard Mod-based AutoTOC
	 * 
	 * @author Mgamerz
	 *
	 */
	class TOCWorker extends SwingWorker<Boolean, String> {
		AtomicInteger completed = new AtomicInteger(0);
		int numtoc = 0;
		String autotocEXE;
		Mod mod;
		ArrayList<String> failedTOC;

		/**
		 * Localmod mode constructor
		 * 
		 * @param mod
		 */
		protected TOCWorker(Mod mod, String biogameDir) {
			this.mod = mod;
			calculateNumberOfUpdates(mod.jobs);
			failedTOC = new ArrayList<String>();
			progressBar.setValue(0);
			autotocEXE = ModManager.getCommandLineToolsDir() + "FullAutoTOC.exe";
			ModManager.debugLogger.writeMessage("Starting the AutoTOC worker. Number of toc updates to do: " + numtoc);
		}

		class TOCTask implements Callable<Boolean> {
			private ModJob job;

			public TOCTask(ModJob job) {
				this.job = job;
			}

			@Override
			public Boolean call() throws Exception {
				if (job.getJobType() == ModJob.BALANCE_CHANGES) {
					return true; //no toc
				}
				if (job.getJobType() == ModJob.CUSTOMDLC) {
					if (mode == AutoTocWindow.INSTALLED_MODE) {
						completed.incrementAndGet();
						return true; //skip, this is done AFTER mod has been installed, and will run outside of autotoc window.
					}
					ArrayList<String> folders = new ArrayList<>();
					int tocd = 0;
					for (String srcFolder : job.getOriginalSourceFolders()) {
						folders.add(mod.getModPath() + srcFolder);
						tocd++;
					}
					ProcessResult pr = ModManager.runAutoTOCOnFolders(folders);
					int returncode = pr.getReturnCode();
					if (returncode != 0 || pr.hadError()) {
						ModManager.debugLogger.writeError("Command line AutoTOC did not return with code 0! An error has occurred.");
					} else {
						completed.addAndGet(tocd);
						ModManager.debugLogger
								.writeMessage("[" + job.getJobName() + "]Number of completed tasks: " + completed + ", num left to do: " + (numtoc - completed.get()));
						publish(Integer.toString(completed.get()));
					}

					return true;
				}
				//job being TOC'd is not a custom dlc
				ModManager.debugLogger.writeMessage("[" + job.getJobName() + "]======AutoTOC job on module " + job.getJobName() + "=======");
				boolean hasTOC = false;
				if (mode == LOCALMOD_MODE) {
					//see if has toc file
					for (String file : job.filesToReplace) {
						String filename = FilenameUtils.getName(file);
						if (filename.equalsIgnoreCase("PCConsoleTOC.bin")) {
							hasTOC = true;
							break;
						}
					}
				} else {
					//get TOC file.
					String tocPath = ModManager.getGameFile(ModTypeConstants.getTOCPathFromHeader(job.getJobName()), job.getJobName(),
							ModManager.getTempDir() + job.getJobName() + "_PCConsoleTOC.bin");
					if (tocPath != null) {
						updatedGameTOCs.put(job.getJobName(), tocPath);
						hasTOC = true;
					} else {
						ModManager.debugLogger
								.writeError("[" + job.getJobName() + "]Unable to get module's PCConsoleTOC file: " + job.getJobName() + ". This update will be skipped.");
					}
				}

				if (hasTOC) { //toc this job
					//batches
					//ArrayList<TocBatchDescriptor> batchJobs = new ArrayList<TocBatchDescriptor>();
					//add first job
					TocBatchDescriptor tbd = new TocBatchDescriptor();
					//batchJobs.add(tbd);

					//break into batches
					ModManager.debugLogger.writeMessage("[" + job.getJobName() + "]Number of files in this job: " + (job.filesToReplace.size() + job.addFiles.size() - 1));
					/*
					 * for (String newFile : job.filesToReplace) { String
					 * filename = FilenameUtils.getName(newFile); if
					 * (filename.equalsIgnoreCase("PCConsoleTOC.bin")) {
					 * continue; //this doesn't need updated. } modulePath =
					 * FilenameUtils.getFullPath(newFile);
					 * tbd.addNameSizePair(filename, (new
					 * File(newFile)).length()); numJobsInCurrentBatch++; if
					 * (numJobsInCurrentBatch >= maxBatchSize) {
					 * batchJobs.add(tbd); tbd = new TocBatchDescriptor();
					 * numJobsInCurrentBatch = 0; } }
					 */

					//Autotoc once installed. Don't bother with add files otherwise

					//TOC to update
					String modulePath = FilenameUtils.getFullPath(job.filesToReplace.get(0));
					String tocPath = modulePath + "PCConsoleTOC.bin";
					if (updatedGameTOCs.containsKey(job.getJobName())) {
						tocPath = updatedGameTOCs.get(job.getJobName());
						ModManager.debugLogger.writeMessage("[" + job.getJobName() + "]TOCing Alternative File: " + tocPath);
					}
					//feed jobs into me3explorer for processing
					//for (TocBatchDescriptor batchJob : batchJobs) {
					ArrayList<String> commandBuilder = new ArrayList<String>();
					// <exe> -toceditorupdate <TOCFILE> <FILENAME> <SIZE>
					commandBuilder.add(autotocEXE);
					commandBuilder.add("--tocfile");
					commandBuilder.add(tocPath);
					commandBuilder.add("--tocupdates");

					ModManager.debugLogger.writeMessage("[" + job.getJobName() + "]Performing a batch TOC update on the following files:\n");
					String str = "";
					int numinbatch = 0;
					for (String newFile : job.filesToReplace) {
						String filename = FilenameUtils.getName(newFile);
						if (filename.equalsIgnoreCase("PCConsoleTOC.bin")) {
							continue; //this doesn't need updated.
						}
						String size = Long.toString(new File(newFile).length());
						commandBuilder.add(filename); //internal filename (if in DLC)
						commandBuilder.add(size);
						str += size + "\t" + filename + "\n";
						numinbatch++;
					}
					ModManager.debugLogger.writeMessage("[" + job.getJobName() + "] " + str);
					String[] command = commandBuilder.toArray(new String[commandBuilder.size()]);
					int returncode = 1;
					ProcessBuilder pb = new ProcessBuilder(command);
					ProcessResult pr = ModManager.runProcess(pb, job.getJobName());
					returncode = pr.getReturnCode();
					if (returncode != 0 || pr.hadError()) {
						ModManager.debugLogger.writeError("[" + job.getJobName() + "] FullAutoTOC returned a non 0 code (or threw error): " + returncode);
						return false;
					} else {
						completed.addAndGet(numinbatch);
						publish(Integer.toString(completed.get()));
					}
					//}
					return true;
				}
				// if does not have toc... ?
				if (job.getFilesToReplace().size() == 0) {
					return true;
				}
				return false;
			}
		}

		private void calculateNumberOfUpdates(ArrayList<ModJob> jobs) {
			for (ModJob job : jobs) {
				if (job.getJobType() == ModJob.CUSTOMDLC) {
					numtoc += job.getSourceFolders().size();
					continue;
				}
				boolean hasTOC = false;
				if (mode == LOCALMOD_MODE) {
					//find out if it has a toc file
					for (String file : job.filesToReplace) {
						String filename = FilenameUtils.getName(file);
						if (filename.equalsIgnoreCase("PCConsoleTOC.bin")) {
							hasTOC = true;
							break;
						}
					}
				} else {
					hasTOC = true; //force game toc
				}

				if (hasTOC) { //calc files
					for (String file : job.filesToReplace) {
						String filename = FilenameUtils.getName(file);
						if (filename.equalsIgnoreCase("PCConsoleTOC.bin") || filename.equalsIgnoreCase("Mount.dlc")) {
							continue;
						} else {
							//increment number of files to update
							numtoc++;
						}
					}
					for (String file : job.addFiles) {
						String filename = FilenameUtils.getName(file);
						if (filename.equalsIgnoreCase("PCConsoleTOC.bin") || filename.equalsIgnoreCase("Mount.dlc")) {
							continue;
						} else {
							//increment number of files to update
							numtoc++;
							ModManager.debugLogger.writeMessage("Number of files in TOC: " + numtoc);
						}
					}
				}
			}
		}

		@Override
		public Boolean doInBackground() {
			ModManager.debugLogger.writeMessage("AutoTOC background thread has started.");
			ExecutorService autotocExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

			ArrayList<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
			for (ModJob job : mod.jobs) {
				//submit jobs
				TOCTask jtask = new TOCTask(job);
				futures.add(autotocExecutor.submit(jtask));
			}
			autotocExecutor.shutdown();
			try {
				autotocExecutor.awaitTermination(5, TimeUnit.MINUTES);
				for (Future<Boolean> f : futures) {
					boolean b = f.get();
					if (!b) {
						ModManager.debugLogger.writeError("A job failed to toc...");
						//throw some sort of error here...
					}
				}
			} catch (ExecutionException e) {
				ModManager.debugLogger.writeErrorWithException("EXECUTION EXCEPTION WHILE TOCING FILES: ", e);
				return null;
			} catch (Exception e) {
				ModManager.debugLogger.writeErrorWithException("UNKNOWN EXCEPTION OCCURED: ", e);
				return null;
			}
			return true;
		}

		@Override
		protected void process(List<String> updates) {
			//System.out.println("Restoring next DLC");
			for (String update : updates) {
				try {
					Integer.parseInt(update); // see if we got a number. if we did that means we should update the bar
					if (numtoc != 0) {
						progressBar.setValue((int) (((double) completed.get() / numtoc) * 100));
					}
				} catch (NumberFormatException e) {
					// this is not a progress update, it's a string update
					//addToQueue(update);
				}
			}

		}

		@Override
		protected void done() {
			try {
				get();
			} catch (Exception e) {
				ModManager.debugLogger.writeErrorWithException("AutoTOC prematurely ended:", e);
			}

			if (numtoc != completed.get()) {
				ModManager.debugLogger
						.writeError("AutoToc DONE: Number of tasks DOES NOT EQUAL number of completed: " + numtoc + " total tasks, " + completed.get() + " completed");
				if (ModManagerWindow.ACTIVE_WINDOW != null) {
					ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("AutoTOC had an error (check logs)");
				}
				dispose();

			} else {
				//we're good
				if (ModManagerWindow.ACTIVE_WINDOW != null && mode == LOCALMOD_MODE) {
					ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText(mod.getModName() + " TOC files updated");
				}
				dispose();
			}
			return;
		}
	}

	class GameWideTOCWorker extends SwingWorker<Boolean, String> {
		AtomicInteger completed = new AtomicInteger(0);
		int numtoc = 0;
		String biogameDir;
		String autotocEXE;
		ArrayList<String> unpackedTOCPaths = new ArrayList<>(), sfarPaths = new ArrayList<>();

		/**
		 * Tocworker constructor for game-wide tocing
		 * 
		 * @param biogameDir
		 */
		public GameWideTOCWorker(String biogameDir) {
			// TODO Auto-generated constructor stub
			progressBar.setIndeterminate(true);
			this.biogameDir = biogameDir;
			ModManager.debugLogger.writeMessage("Running gamewide AutoTOC using FullAutoTOC.exe");
		}

		/*
		 * private void calculateTasksToDo(String biogameDir) { //add basegame
		 * File basegameToc = new File(ModManager.appendSlash(biogameDir) +
		 * File.separator + "PCConsoleTOC.bin"); if (basegameToc.exists()) {
		 * unpackedTOCPaths.add(basegameToc.getAbsolutePath());
		 * System.out.println(basegameToc); }
		 * 
		 * //add testpatch HashMap<String, Long> sizesMap =
		 * ModType.getSizesMap(); File testpatchSfar = new
		 * File(ModManager.appendSlash(biogameDir) + File.separator + "Patches"
		 * + File.separator + "PCConsole" + File.separator + "Patch_001.sfar");
		 * if (testpatchSfar.exists() && (testpatchSfar.length() !=
		 * sizesMap.get(ModType.TESTPATCH) && testpatchSfar.length() !=
		 * ModType.TESTPATCH_16_SIZE)) {
		 * sfarPaths.add(testpatchSfar.getAbsolutePath()); }
		 * System.out.println(testpatchSfar);
		 * 
		 * //iterate over DLC. File mainDlcDir = new
		 * File(ModManager.appendSlash(biogameDir) + "DLC" + File.separator);
		 * String[] directories = mainDlcDir.list(new FilenameFilter() {
		 * 
		 * @Override public boolean accept(File current, String name) { return
		 * new File(current, name).isDirectory(); } }); HashMap<String, String>
		 * nameMap = ModType.getHeaderFolderMap(); for (String dir :
		 * directories) { if (dir.toUpperCase().startsWith("DLC_")) {
		 * //System.out.println(dir); boolean isKnownDLC =
		 * ModType.isKnownDLCFolder(dir); File mainSfar = new File(biogameDir +
		 * File.separator + "DLC" + File.separator + dir + File.separator +
		 * "CookedPCConsole\\Default.sfar"); if (mainSfar.exists()) { //find the
		 * header (the lazy way) if (isKnownDLC) { String header = null; for
		 * (Map.Entry<String, String> entry : nameMap.entrySet()) { String
		 * localHeader = entry.getKey(); String foldername = entry.getValue();
		 * if (FilenameUtils.getBaseName(dir).equalsIgnoreCase(foldername)) {
		 * header = localHeader; break; } } if (mainSfar.length() ==
		 * sizesMap.get(header)) { //vanilla
		 * ModManager.debugLogger.writeMessage("Skipping vanilla SFAR: " +
		 * mainSfar); continue; } }
		 * 
		 * File externalTOC = new File(biogameDir + "/DLC/" + dir +
		 * "/PCConsoleTOC.bin"); if (externalTOC.exists() || !isKnownDLC) {
		 * //its unpacked ModManager.debugLogger.
		 * writeMessage("Found external toc file (or is custom dlc), adding to unpacked list: "
		 * + externalTOC); unpackedTOCPaths.add(externalTOC.getAbsolutePath());
		 * continue; } else { //its a modified SFAR ModManager.debugLogger
		 * .writeMessage("No external PCConsoleTOC.bin, and SFAR size is not vanilla (or is custom dlc), adding to sfar list to autotoc: "
		 * + mainSfar); sfarPaths.add(mainSfar.getAbsolutePath()); continue; } }
		 * else { continue; //not valid DLC } } else { //unnofficial DLC File
		 * externalTOC = new File(dir + "PCConsoleTOC.bin"); if
		 * (externalTOC.exists()) {
		 * unpackedTOCPaths.add(externalTOC.getAbsolutePath()); continue; } } }
		 * numtoc = unpackedTOCPaths.size() + sfarPaths.size(); }
		 */
		@Override
		public Boolean doInBackground() {
			ModManager.debugLogger.writeMessage("Game-Wide AutoTOC background thread has started");
			autotocEXE = ModManager.getCommandLineToolsDir() + "FullAutoTOC.exe";
			String gamepath = new File(biogameDir).getParent().toString();
			ArrayList<String> commandBuilder = new ArrayList<String>();
			// <exe> -toceditorupdate <TOCFILE> <FILENAME> <SIZE>
			commandBuilder.add(autotocEXE);
			commandBuilder.add("--gamepath");
			commandBuilder.add(gamepath);
			String[] command = commandBuilder.toArray(new String[commandBuilder.size()]);

			ProcessBuilder pb = new ProcessBuilder(command);

			ProcessResult pr = ModManager.runProcess(pb);
			return pr.getReturnCode() == 0;
		}

		class TOCTask implements Callable<ProcessResult> {
			private String filepath;
			private boolean SFAR;

			public TOCTask(String filepath, boolean SFAR) {
				this.filepath = filepath;
				this.SFAR = SFAR;
			}

			@Override
			public ProcessResult call() throws Exception {
				if (SFAR) {
					ArrayList<String> commandBuilder = new ArrayList<String>();
					// <exe> -toceditorupdate <TOCFILE> <FILENAME> <SIZE>
					commandBuilder.add(autotocEXE);
					commandBuilder.add("-sfarautotoc");
					commandBuilder.add(filepath);
					String[] command = commandBuilder.toArray(new String[commandBuilder.size()]);

					ProcessBuilder pb = new ProcessBuilder(command);
					ProcessResult pr = ModManager.runProcess(pb);
					if (pr.getReturnCode() == 0) {
						completed.incrementAndGet();
						publish(Integer.toString(completed.get()));
					}
					return pr;
				} else {
					ArrayList<String> commandBuilder = new ArrayList<String>();
					// <exe> -toceditorupdate <TOCFILE> <FILENAME> <SIZE>
					commandBuilder.add(autotocEXE);
					commandBuilder.add("-autotoc");
					commandBuilder.add(filepath);
					String[] command = commandBuilder.toArray(new String[commandBuilder.size()]);

					ProcessBuilder pb = new ProcessBuilder(command);

					ProcessResult pr = ModManager.runProcess(pb);
					if (pr.getReturnCode() == 0) {
						completed.incrementAndGet();
						publish(Integer.toString(completed.get()));
					}
					return pr;
				}
			}
		}

		@Override
		protected void process(List<String> updates) {
			//System.out.println("Restoring next DLC");
			for (String update : updates) {
				try {
					Integer.parseInt(update); // see if we got a number. if we did that means we should update the bar
					if (numtoc != 0) {
						progressBar.setValue((int) (((float) completed.get() / numtoc) * 100));
					}
				} catch (NumberFormatException e) {
					// this is not a progress update, it's a string update
					//addToQueue(update);
				}
			}

		}

		@Override
		protected void done() {
			boolean successful = false;
			try {
				successful = get();
			} catch (Exception e) {
				ModManager.debugLogger.writeErrorWithException("Game-Wide AutoTOC prematurely ended:", e);
			}

			if (successful) {
				//we're good
				if (ModManagerWindow.ACTIVE_WINDOW != null) {
					if (mode == LOCALMOD_MODE && mod != null) {
						ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText(mod.getModName() + " TOC files updated");
					} else {
						ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Game TOC files updated");
					}
					ModManager.debugLogger.writeMessage("AutoTOC has completed successfully");
				}
				dispose();
			} else {
				//we're bad
				dispose();
				if (ModManagerWindow.ACTIVE_WINDOW != null) {
					ModManager.debugLogger.writeError("AUTOTOC HAS FAILED! See process results from above to see why.");
					ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Failed to TOC files");
					JOptionPane.showMessageDialog(ModManagerWindow.ACTIVE_WINDOW, "Mod Manager AutoTOC failed. Check Mod Manager's logs to see why.", "AutoTOC failed",
							JOptionPane.ERROR_MESSAGE);
				}
			}
			return;
		}
	}
}
