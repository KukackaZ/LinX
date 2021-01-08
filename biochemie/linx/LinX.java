package biochemie.linx;

import biochemie.*;
import biochemie.dbedit.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author Janek
 */
public class LinX extends JFrame {

	private static final String VERSION_STRING = "1.13d";
	public static final String DEFAULT_INPUT = "input.sen";
	public static final String SETTINGS_FILE = "configuration";
	private static final String MAIN = "LinX";
	private static final String PROTEINS = MAIN + '.' + "Proteins";
	private static final String DIGEST = MAIN + '.' + "Proteases";
	private static final String MODIFICATIONS = MAIN + '.' + "Modifications";
	private static final String BONDS = MAIN + '.' + "Cross-links";
	private static final String MEASUREMENT = MAIN + '.' + "Measurement";
	private static final String COMPUTING = MAIN + '.' + "Computing";
	private static final String RESULTS = MAIN + '.' + "Results";
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenu propMenu;
	private JMenu modlMenu;
	private JMenu helpMenu;
	private JMenuItem loadItem;
	private JMenuItem saveItem;
	private JMenuItem prosItem;
	private JMenuItem modsItem;
	private JMenuItem bodsItem;
	private JMenuItem elesItem;
	private JMenuItem forsItem;
	private JMenuItem optsItem;
	private JMenuItem setpItem;
	private JMenuItem propItem;
	private JMenuItem meapItem;
	private JMenuItem analItem;
	private JMenuItem helpItem;
	private JMenuItem contItem;
	private String inputPath = ".";
	private JLabel[] status;
	private JPanel statusPanel;
	private JPanel mainPanel;
	private Card[] cards;
	private ProteinsCard proteinsCard;
	private JButton nonspecificButton;
	private JButton specificButton;
	private DigestCard digestCard;
	private boolean specific;
	private boolean update;
	private JButton backDigestButton;
	private JButton modificationsButton;
	private ModificationsCard modificationsCard;
	private JButton backModificationsButton;
	private JButton bondsButton;
	private JButton measurementModificationsButton;
	private BondsCard bondsCard;
	private boolean bonds;
	private JButton backBondsButton;
	private JButton measurementBondsButton;
	private MeasurementCard measurementCard;
	private JButton backMeasurementButton;
	private JButton runButton;
	private ComputingCard computingCard;
	private JButton cancelButton;
	private JButton stopButton;
	private ResultsCard resultsCard;
	private JButton backRunButton;
	private JButton newButton;

	/**
	 * Creates new form MainFrame
	 */
	public LinX() {
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setIconImages(prepareIconImages());
//		setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(alternative == null ? "linx.png" : "linx-a.png"));
		setName(MAIN);
		setTitle(alternative == null ? MAIN : (MAIN + " - alternatives"));
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				formWindowClosing(e);
			}
		});

		//<editor-fold defaultstate="collapsed" desc=" Menu ">
		menuBar = new JMenuBar();
		fileMenu = new JMenu("File");
		propMenu = new JMenu("Properties");
		modlMenu = new JMenu("Modules");
		helpMenu = new JMenu("Help");
		loadItem = new JMenuItem("Load settings");
		saveItem = new JMenuItem("Save settings");
		prosItem = new JMenuItem("Proteases");
		modsItem = new JMenuItem("Modifications");
		bodsItem = new JMenuItem("Bonds");
		elesItem = new JMenuItem("Elements");
		optsItem = new JMenuItem("Options");
		forsItem = new JMenuItem("Measurement formats");
		setpItem = new JMenuItem("Settings parsers");
		propItem = new JMenuItem("Proteins parsers");
		meapItem = new JMenuItem("Measurement parsers");
		analItem = new JMenuItem("Analyzers");
		helpItem = new JMenuItem("Manual");
		contItem = new JMenuItem("About LinX");

//    fileMenu.add(podMenu);
		fileMenu.setMnemonic('F');
		loadItem.setMnemonic('L');
		loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		loadItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				loadItemActionPerformed(evt);
			}
		});
		fileMenu.add(loadItem);
		saveItem.setMnemonic('S');
		saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		saveItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				try (PrintWriter pw = saveFile("", true, null)) {
				}
			}
		});
		fileMenu.add(saveItem);

		propMenu.setMnemonic('P');
		prosItem.setMnemonic('P');
		prosItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
		prosItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				prosItemActionPerformed(evt);
			}
		});
		propMenu.add(prosItem);
		modsItem.setMnemonic('M');
		modsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK));
		modsItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				modsItemActionPerformed(evt);
			}
		});
		propMenu.add(modsItem);
		bodsItem.setMnemonic('B');
		bodsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK));
		bodsItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				bodsItemActionPerformed(evt);
			}
		});
		propMenu.add(bodsItem);
		elesItem.setMnemonic('E');
		elesItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
		elesItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				elesItemActionPerformed(evt);
			}
		});
		propMenu.add(elesItem);
		forsItem.setMnemonic('F');
		forsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
		forsItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				forsItemActionPerformed(evt);
			}
		});
		propMenu.add(forsItem);
		propMenu.addSeparator();
		optsItem.setMnemonic('O');
		optsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		optsItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				optsItemActionPerformed(evt);
			}
		});
		propMenu.add(optsItem);

		modlMenu.setMnemonic('M');
		setpItem.setMnemonic('S');
		setpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK));
		setpItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				setpItemActionPerformed(evt);
			}
		});
		modlMenu.add(setpItem);
		propItem.setMnemonic('P');
		propItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
		propItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				propItemActionPerformed(evt);
			}
		});
		modlMenu.add(propItem);
		meapItem.setMnemonic('M');
		meapItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK));
		meapItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				meapItemActionPerformed(evt);
			}
		});
		modlMenu.add(meapItem);
		modlMenu.addSeparator();
		analItem.setMnemonic('A');
		analItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		analItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				analItemActionPerformed(evt);
			}
		});
		modlMenu.add(analItem);

		helpMenu.setMnemonic('H');
		helpItem.setMnemonic('M');
		helpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		helpItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				helpMenuActionPerformed(evt);
			}
		});
		helpMenu.add(helpItem);
		contItem.setMnemonic('C');
		contItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
		contItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				contMenuActionPerformed(evt);
			}
		});
		helpMenu.add(contItem);

		menuBar.add(fileMenu);
		menuBar.add(propMenu);
		menuBar.add(modlMenu);
		menuBar.add(helpMenu);

		setJMenuBar(menuBar);
		menuBar.setMinimumSize(menuBar.getPreferredSize());
		//</editor-fold>

		mainPanel = new JPanel(new java.awt.CardLayout());
		cards = new Card[7];

		//<editor-fold defaultstate="collapsed" desc=" Protein ">
		nonspecificButton = new JButton("Non-specific cleavage");
		specificButton = new JButton("Specific cleavage");

		nonspecificButton.setMnemonic('N');
		nonspecificButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				nonspecificActionPerformed(evt);
			}
		});

		specificButton.setMnemonic('S');
		specificButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				specificActionPerformed(evt);
			}
		});

		cards[0] = proteinsCard = new ProteinsCard(PROTEINS, new JButton[]{nonspecificButton, specificButton});
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc=" Cleavage ">
		update = false;
		backDigestButton = new JButton("Back");
		modificationsButton = new JButton("Modifications");

		backDigestButton.setMnemonic('B');
		backDigestButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				backDigestActionPerformed(evt);
			}
		});

		modificationsButton.setMnemonic('M');
		modificationsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				modificationsActionPerformed(evt);
			}
		});

		cards[1] = digestCard = new DigestCard(DIGEST, new JButton[]{modificationsButton, backDigestButton}, this);
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc=" Modifications ">
		backModificationsButton = new JButton("Back");
		measurementModificationsButton = new JButton("No cross-links");
		bondsButton = new JButton("Cross-links");

		backModificationsButton.setMnemonic('B');
		backModificationsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				backModificationActionPerformed(evt);
			}
		});

		measurementModificationsButton.setMnemonic('N');
		measurementModificationsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				bonds = false;
				measurementActionPerformed(evt);
			}
		});

		bondsButton.setMnemonic('L');
		bondsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				bondsActionPerformed(evt);
			}
		});

		cards[2] = modificationsCard = new ModificationsCard(MODIFICATIONS, new JButton[]{measurementModificationsButton, bondsButton, backModificationsButton});
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc=" Bonds ">
		bonds = true;
		backBondsButton = new JButton("Back");
		measurementBondsButton = new JButton("Measurement");

		backBondsButton.setMnemonic('B');
		backBondsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				backBondsActionPerformed(evt);
			}
		});

		measurementBondsButton.setMnemonic('M');
		measurementBondsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				bonds = true;
				measurementActionPerformed(evt);
			}
		});

		cards[3] = bondsCard = new BondsCard(BONDS, new JButton[]{measurementBondsButton, backBondsButton}, modificationsCard);
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc=" Measurement ">
		backMeasurementButton = new JButton("Back");
		runButton = new JButton("Run");

		backMeasurementButton.setMnemonic('B');
		backMeasurementButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				backMeasurementActionPerformed(evt);
			}
		});

		runButton.setMnemonic('R');
		runButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				runActionPerformed(evt);
			}
		});

		cards[4] = measurementCard = new MeasurementCard(MEASUREMENT, new JButton[]{runButton, backMeasurementButton}, this);
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc=" Computing ">
		cancelButton = new JButton("Cancel");
		stopButton = new JButton("Stop");

		cancelButton.setMnemonic('C');
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				cancelActionPerformed(evt);
			}
		});

		stopButton.setMnemonic('T');
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				stopActionPerformed(evt);
			}
		});

		cards[5] = computingCard = new ComputingCard(COMPUTING, new JButton[]{stopButton, cancelButton});
		//</editor-fold>

		//<editor-fold defaultstate="collapsed" desc=" Results ">
		newButton = new JButton("New");
		backRunButton = new JButton("Back");

		backRunButton.setMnemonic('B');
		backRunButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				backRunActionPerformed(evt);
			}
		});

		newButton.setMnemonic('N');
		newButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				newActionPerformed(null);
			}
		});

		cards[6] = resultsCard = new ResultsCard(RESULTS, alternative == null ? new JButton[]{newButton, backRunButton} : new JButton[0], this, computingCard);
		//</editor-fold>

		loadSettings(quick == null ? DEFAULT_INPUT : quick);

		//<editor-fold defaultstate="collapsed" desc=" Status ">
		statusPanel = new JPanel();
		GroupLayout statusLayout = new GroupLayout(statusPanel);
		status = new JLabel[cards.length * 2 - 1];
		for (int i = 0; i < cards.length; i++) {
			mainPanel.add(cards[i], cards[i].getName());
			final int j = 2 * i;
			status[j] = new JLabel(cards[i].getName().replaceFirst(MAIN + '.', ""));
			status[j].addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					labelMove(e, j / 2);
				}
			});
		}
		for (int i = 1; i < status.length; i += 2) {
			status[i] = new JLabel("➜");
		}

		statusPanel.setLayout(statusLayout);
		GroupLayout.SequentialGroup sg = statusLayout.createSequentialGroup();
		GroupLayout.ParallelGroup pg = statusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE);
		for (int i = 0; i < status.length; i++) {
			pg.addComponent(status[i]);
			if (i == 0) {
				sg.addContainerGap();
			} else {
				sg.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED);
			}
			sg.addComponent(status[i]);
		}
		status[0].setFont(status[0].getFont().deriveFont(Font.BOLD));
		statusLayout.setHorizontalGroup(
				  statusLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
							 .addGroup(sg.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
		);
		statusLayout.setVerticalGroup(
				  statusLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
							 .addGroup(GroupLayout.Alignment.TRAILING, statusLayout.createSequentialGroup()
										.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addGroup(pg)
										.addContainerGap())
		);
		//</editor-fold>

		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				  layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							 .addComponent(statusPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
							 .addComponent(mainPanel, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
		);
		layout.setVerticalGroup(
				  layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							 .addGroup(layout.createSequentialGroup()
										.addComponent(statusPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(mainPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
		);

		getRootPane().setDefaultButton(specificButton);
		pack();
		// Hack, neumí hlídat rovnou
		setMinimumSize(getMinimumSize());
		setMaximumSize(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize());

		Properties defaults = Defaults.getDefaults(MAIN);
		if (defaults.containsKey("Path")) {
			inputPath = defaults.getProperty("Path");
		}
		Defaults.setWindowDefaults(this, defaults);
		if (defaults.containsKey("ExtendedState")) {
			setExtendedState(Integer.parseInt(defaults.getProperty("ExtendedState")));
		}
		revalidate();
		for (Card card : cards) {
			card.applyDefaults();
		}
	}

	private java.util.List<? extends Image> prepareIconImages() {
		java.util.List<BufferedImage> retVal = new ArrayList<>();
		try {
			retVal.add(ImageIO.read(LinX.class.getResource("img/icon-a-16.png")));
			retVal.add(ImageIO.read(LinX.class.getResource("img/icon-a-24.png")));
			retVal.add(ImageIO.read(LinX.class.getResource("img/icon-a-32.png")));
			retVal.add(ImageIO.read(LinX.class.getResource("img/icon-a-48.png")));
			retVal.add(ImageIO.read(LinX.class.getResource("img/icon-a-64.png")));
			retVal.add(ImageIO.read(LinX.class.getResource("img/icon-a-256.png")));
		} catch (IOException | IllegalArgumentException ex) {
			return null;
		}
		return retVal;
	}

	public boolean saveSetting(PrintWriter pw, FileName measurementFile) {
		if (!proteinsCard.check(true)) {
			return false;
		}

		ArrayList<ArrayList<String>> parts = new ArrayList<>(6);
		for (int i = 0; i < 6; i++) {
			parts.add(new ArrayList<String>());
		}

		String panel = selectedPanel();
		LinkedHashMap<String, Protein> proteins = proteinsCard.getProteins();
		ArrayList<String> settings = new ArrayList<>();
		switch (panel) {
			case RESULTS:
				ArrayList<String> f = resultsCard.logFilters();
				if (!f.isEmpty()) {
					parts.get(4).add("Filters:");
					parts.get(4).addAll(f);
				}
			case COMPUTING:
			case MEASUREMENT:
				settings = measurementCard.logSettings();
				LinkedHashSet<FileName> measurement = measurementCard.getFileNames();
				if (measurementFile != null || measurement.size() != 0) {
					parts.get(5).add("Measurement:");
					if (measurementFile == null) {
						for (FileName mf : measurement) {
							parts.get(5).add(mf.toString());
						}
					} else {
						parts.get(5).add(measurementFile.toString());
					}
				}
			case BONDS:
				ArrayList<String> b = bondsCard.logBonds();
				if (!b.isEmpty()) {
					parts.get(1).add("Cross-links:");
					parts.get(1).addAll(b);
				}
			case MODIFICATIONS:
				ArrayList<String> mods = modificationsCard.logModifications();
				if (!mods.isEmpty()) {
					parts.get(0).add("Modifications:");
					parts.get(0).addAll(mods);
				}

				proteins = modificationsCard.getProteins();

				if (!specific) {
					parts.get(2).add("Nonspecific cleavage.");
					if (!settings.isEmpty()) {
						parts.get(3).add("Settings:");
						parts.get(3).addAll(settings);
					}
					break;
				}
			case DIGEST:
				parts.get(2).add("Specific cleavage:");
				parts.get(2).addAll(digestCard.logProteases());

				parts.get(3).add("Settings:");
				parts.get(3).addAll(digestCard.logSettings());
				parts.get(3).addAll(settings);
		}

		for (String name : proteins.keySet()) {
			String protein = proteins.get(name).toString();
			pw.print(name + ":");
			if (proteins.get(name).getShift() != 1) {
				pw.print(proteins.get(name).getShift());
			}
			pw.println();
			int start = 0;
			int length = 100;
			while (start + length < protein.length()) {
				String sub = protein.substring(start, start + length);
				int add = 2 * sub.replaceAll("[^/]", "").length();
				if (sub.contains("^")) {
					add++;
				} else if (protein.charAt(start + length) == '^') {
					add++;
				}
				if (sub.contains("$")) {
					add++;
				}
				if (length == 100 + add) {
					pw.println(sub);
					start += length;
				} else {
					length = 100 + add;
				}
			}
			pw.println(protein.substring(start));
		}

		for (ArrayList<String> part : parts) {
			if (!part.isEmpty()) {
				pw.println();
				for (String line : part) {
					pw.println(line);
				}
			}
		}

		pw.flush();
		return true;
	}

	public PrintWriter saveSetting(String path, boolean inform, FileName measurementFile) {
		Cursor c = getCursor();
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {
			PrintWriter pw = new PrintWriter(new FileWriter(path, false));
			if (saveSetting(pw, measurementFile)) {
				if (inform) {
					JOptionPane.showMessageDialog(this, "Settings were saved into file '" + path + "'.", "Settings saved", JOptionPane.INFORMATION_MESSAGE);
				}
			} else {
				JOptionPane.showMessageDialog(this, "No settings to save.", "Settings not saved", JOptionPane.INFORMATION_MESSAGE);
			}
			pw.flush();
			return pw;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "It isn't possible to save file.\n========================================\n" + e.getLocalizedMessage(),
					  "IO Exception", JOptionPane.WARNING_MESSAGE);
			return null;
		} finally {
			setCursor(c);
		}
	}

	public PrintWriter saveFile(String defaultName, boolean inform, FileName measurementFile) {
		JFileChooser jfc = new JFileChooser(inputPath);
		if (defaultName != null && !defaultName.isEmpty()) {
			if (defaultName.contains(File.separator)) {
				defaultName = defaultName.substring(defaultName.lastIndexOf(File.separator) + File.separator.length());
			}
			if (defaultName.endsWith(".sen")) {
				defaultName = defaultName.substring(0, defaultName.length() - 4);
			}
			jfc.setSelectedFile(new File(defaultName));
		}
		jfc.setFileFilter(new FileNameExtensionFilter("Settings file", "sen"));
		do {
			int res = jfc.showSaveDialog(jfc);
			if (res == JFileChooser.CANCEL_OPTION) {
				return null;
			} else if (res == JFileChooser.ERROR_OPTION) {
				JOptionPane.showMessageDialog(this, "Error occured, please try it again.", "Unexpected error", JOptionPane.ERROR_MESSAGE);
			} else if (res == JFileChooser.APPROVE_OPTION) {
				String path = jfc.getSelectedFile().getPath();
				if (!path.endsWith(".sen")) {
					path += ".sen";
				}
				if (new File(path).exists()) {
					int ret = JOptionPane.showConfirmDialog(this, "File with the chosen name already exists.\nDo you want to overwrite it?", "File exist",
							  JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
					if (ret == JOptionPane.NO_OPTION) {
						continue;
					}
					if (ret != JOptionPane.YES_OPTION) {
						return null;
					}
				}
				PrintWriter pw;
				if ((pw = saveSetting(path, inform, measurementFile)) != null) {
					try {
						inputPath = Paths.get(System.getProperty("user.dir")).relativize(jfc.getSelectedFile().getParentFile().toPath()).toString();
					} catch (Exception e) {
						inputPath = jfc.getSelectedFile().getParentFile().getPath();
					}
					if (inputPath.isEmpty()) {
						inputPath = ".";
					}
					return pw;
				}
			}
		} while (true);
	}

	private void loadItemActionPerformed(ActionEvent evt) {
		JFileChooser jfc = new JFileChooser(inputPath);
		jfc.setFileFilter(new FileNameExtensionFilter("Settings file", "sen"));
		do {
			int res = jfc.showOpenDialog(jfc);
			if (res == JFileChooser.CANCEL_OPTION) {
				break;
			} else if (res == JFileChooser.ERROR_OPTION) {
				JOptionPane.showMessageDialog(this, "Error occured, please try it again.", "Unexpected error", JOptionPane.ERROR_MESSAGE);
			} else if (res == JFileChooser.APPROVE_OPTION) {
				loadSettings(jfc.getSelectedFile().getPath());
				try {
					inputPath = Paths.get(System.getProperty("user.dir")).relativize(jfc.getSelectedFile().getParentFile().toPath()).toString();
				} catch (Exception e) {
					inputPath = jfc.getSelectedFile().getParentFile().getPath();
				}
				if (inputPath.isEmpty()) {
					inputPath = ".";
				}
				break;
			}
		} while (true);
	}

	private void loadSettings(String filename) {
		Cursor c = getCursor();
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		ArrayList<String[]> proteins = new ArrayList();
		boolean spec = specific;
		ArrayList<String> proteases = new ArrayList();
		ArrayList<String> mods = new ArrayList();
		ArrayList<String> cls = new ArrayList();
		ArrayList<String> settings = new ArrayList();
		ArrayList<String[]> measurements = new ArrayList();
		ArrayList<String> filters = new ArrayList();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)))) {
			String line;
			while ((line = reader.readLine()) != null && !line.isEmpty()) {
				if (line.charAt(0) == 0xfeff) {
					line = line.substring(1);
					if (line.isEmpty()) {
						break;
					}
				}
				if (line.matches(".*:$")) {
					proteins.add(new String[]{line.substring(0, line.length() - 1), "1", ""});
				} else if (line.matches(".*:-?[0-9]*$")) {
					proteins.add(new String[]{line.substring(0, line.lastIndexOf(':')), line.substring(line.lastIndexOf(':') + 1), ""});
				} else {
					if (proteins.isEmpty()) {
						proteins.add(new String[]{"", "1", line});
					} else {
						proteins.get(proteins.size() - 1)[2] += line;
					}
				}
			}
			do {
				if (line.equals("Modifications:")) {
					while (!(line = reader.readLine()).isEmpty()) {
						mods.add(line);
					}
				} else if (line.equals("Cross-links:")) {
					while (!(line = reader.readLine()).isEmpty()) {
						cls.add(line);
					}
				} else if (line.equals("Specific cleavage:")) {
					spec = true;
					while (!(line = reader.readLine()).isEmpty()) {
						proteases.add(line);
					}
				} else if (line.equals("Nonspecific cleavage.")) {
					spec = false;
				} else if (line.equals("Settings:")) {
					while (!(line = reader.readLine()).isEmpty()) {
						settings.add(line);
					}
				} else if (line.equals("Measurement:")) {
					while (!(line = reader.readLine()).isEmpty()) {
						String[] tmp = line.split("\t");
						switch (tmp.length) {
							case 0:
								measurements.add(new String[]{"", ""});
								break;
							case 1:
								measurements.add(new String[]{tmp[0], ""});
								break;
							default:
								measurements.add(tmp);
						}
					}
				} else if (line.equals("Filters:")) {
					while (!(line = reader.readLine()).isEmpty()) {
						filters.add(line);
					}
				}
			} while ((line = reader.readLine()) != null && !line.matches("-+"));
		} catch (Exception e) {
		}

		switch (selectedPanel()) {
			case PROTEINS:
				proteinsCard.clean();
				proteinsCard.setProteins(proteins);
				specific = spec;
			case DIGEST:
				digestCard.clean();
				digestCard.setProteases(proteases);
				digestCard.setConstraints(settings);
			case MODIFICATIONS:
				modificationsCard.clean();
				modificationsCard.setProteases(specific ? digestCard.getProteases() : new ArrayList<java.util.Collection<Protease>>(0));
				if (proteinsCard.check(false)) {
					modificationsCard.setProteins(proteinsCard.getProteins());
				}
				modificationsCard.setModifications(mods);
			case BONDS:
				bondsCard.clean();
				if (proteinsCard.check(false)) {
					bondsCard.setProteins(proteinsCard.getProteins());
				}
				bondsCard.setBonds(cls);
				bonds = !cls.isEmpty();
			case MEASUREMENT:
				measurementCard.setFiles(measurements);
				measurementCard.setConstraints(settings);
			case RESULTS:
				if (!filters.isEmpty()) {
					resultsCard.setFilters(filters);
				}
		}

		setCursor(c);
	}

	private void prosItemActionPerformed(ActionEvent evt) {
		if (ProteasesDialog.showProteasesDialog(this)) {
			String panel = selectedPanel();
			if (specific && !PROTEINS.equals(panel)) {
				if (COMPUTING.equals(panel) || RESULTS.equals(panel)) {
					JOptionPane.showMessageDialog(this, "List of proteases has been updated.", "Update was successful", JOptionPane.INFORMATION_MESSAGE);
					update = true;
				} else {
					JOptionPane.showMessageDialog(this, "List of proteases has been updated.\nSelected items could be affected.\nThey must be checked manually.",
							  "Update was successful", JOptionPane.INFORMATION_MESSAGE);
					if (DIGEST.equals(panel)) {
						digestCard.updateProteasesPanels();
					} else {
						update = true;
						if (MEASUREMENT.equals(panel)) {
							backMeasurementButton.doClick();
							panel = selectedPanel();
						}
						if (BONDS.equals(panel)) {
							backBondsButton.doClick();
							panel = selectedPanel();
						}
						if (MODIFICATIONS.equals(panel)) {
							backModificationsButton.doClick();
						}
					}
				}
			} else {
				digestCard.updateProteasesPanels();
				JOptionPane.showMessageDialog(this, "List of proteases has been updated.", "Update was successful", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	private void modsItemActionPerformed(ActionEvent evt) {
		if (ModificationsDialog.showModificationsDialog(this)) {
			modificationsCard.reloadMods();
			JOptionPane.showMessageDialog(this, "List od modifications has been updated.\nSelected modifications weren't affected, they must be checked manually.",
					  "The update was successful", JOptionPane.INFORMATION_MESSAGE);
			if (MEASUREMENT.equals(selectedPanel())) {
				backMeasurementButton.doClick();
			}
			if (BONDS.equals(selectedPanel())) {
				backBondsButton.doClick();
			}
		}
	}

	private void bodsItemActionPerformed(ActionEvent evt) {
		if (BondsDialog.showBondsDialog(this)) {
			bondsCard.reloadBonds();
			JOptionPane.showMessageDialog(this, "List od bonds has been updated.\nSelected bonds weren't affected, they must be checked manually.",
					  "The update was successful", JOptionPane.INFORMATION_MESSAGE);
			if (MEASUREMENT.equals(selectedPanel()) && bonds) {
				backMeasurementButton.doClick();
			}
		}
	}

	private void elesItemActionPerformed(ActionEvent evt) {
		if (ElementsDialog.showElementsDialog(this)) {
			JOptionPane.showMessageDialog(this, "List od elements has been updated.\nSelected proteases, modifications and bonds must be checked manually.",
					  "The update was successful", JOptionPane.INFORMATION_MESSAGE);
			String panel = selectedPanel();
			if (MEASUREMENT.equals(panel)) {
				backMeasurementButton.doClick();
				panel = selectedPanel();
			}
			if (BONDS.equals(panel)) {
				backBondsButton.doClick();
				panel = selectedPanel();
			}
			if (specific && MODIFICATIONS.equals(panel)) {
				backModificationsButton.doClick();
			}
		}
	}

	private void forsItemActionPerformed(ActionEvent evt) {
		if (FormatsDialog.showFormatsDialog(this, measurementCard.getForbiddenMsvs())) {
			measurementCard.reloadMsvs();
			JOptionPane.showMessageDialog(this, "List od measurement formats has been updated.\nMeasurement file must be reloaded manually.",
					  "The update was successful", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void optsItemActionPerformed(ActionEvent evt) {
		if (SettingsDialog.popup(this, new SettingsPanel[]{biochemie.Defaults.getSettingsPanel(),
			biochemie.linx.MeasurementCard.getSettingsPanel(),
			biochemie.linx.ResultsCard.getSettingsPanel(),
			biochemie.linx.MeasurementCard.getSettingsPanel2()})) {
			JOptionPane.showMessageDialog(this, "Some changes take effect after a restart of the application.", "Changes saved", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void setpItemActionPerformed(ActionEvent evt) {
		if (ParsersDialog.showMeasurementsDialog(this, "Settings", SETTINGS_FILE, new ArrayList())) {
			// TODO: Has forbidden names prepared in an array
			//reloadParsers();
		}
	}

	private void propItemActionPerformed(ActionEvent evt) {
		if (ParsersDialog.showMeasurementsDialog(this, "Proteins", ProteinsCard.PROTEINS_FILE, proteinsCard.getForbiddenNames())) {
			//proteinCard.reloadParsers();
		}
	}

	private void meapItemActionPerformed(ActionEvent evt) {
		if (ParsersDialog.showMeasurementsDialog(this, "Measurements", MeasurementCard.MEASUREMENTS_FILE, measurementCard.getForbiddenFormats())) {
			measurementCard.reloadFormats();
			JOptionPane.showMessageDialog(this, "List od measurement formats has been updated.\nMeasurement file must be reloaded manually.",
					  "The update was successful", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void analItemActionPerformed(ActionEvent evt) {
		if (AnalysersDialog.showAnalysersDialog(this)) {
			resultsCard.reloadAnalyzers();
		}
	}

	private void helpMenuActionPerformed(java.awt.event.ActionEvent evt) {
		try {
			Desktop.getDesktop().open(new File("Help", "Manual_LinX.mht"));
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	private void contMenuActionPerformed(java.awt.event.ActionEvent evt) {
		new AboutDialog(this, VERSION_STRING).setVisible(true);
	}

	private void labelMove(MouseEvent e, int target) {
		if (e.getClickCount() == 2) {
			int start = selectedPanelIndex();
			if (start > target) {
				int current = start;
				do {
					switch (current) {
						case 1:
							backDigestButton.doClick();
							break;
						case 2:
							backModificationsButton.doClick();
							break;
						case 3:
							backBondsButton.doClick();
							break;
						case 4:
							backMeasurementButton.doClick();
							break;
						case 5:
							cancelButton.doClick();
							break;
						case 6:
							backRunButton.doClick();
							break;
					}
				} while ((current = selectedPanelIndex()) > target);
				if (current < target) {
					switch (current) {
						case 0:
							specificButton.doClick();
							break;
						case 2:
							bondsButton.doClick();
							break;
					}
				}
			}
		}
	}

	private void nonspecificActionPerformed(ActionEvent evt) {
		if (proteinsCard.check(true)) {
			specific = false;
			getRootPane().setDefaultButton(bondsButton);
			flipFonts(status[0], status[4]);
			modificationsCard.setProteases(new ArrayList<Collection<Protease>>(0));
			modificationsCard.setProteins(proteinsCard.getProteins());
			((CardLayout) mainPanel.getLayout()).show(mainPanel, MODIFICATIONS);
			status[2].setEnabled(false);
		}
		setMinimumSize(null);
		setMinimumSize(getMinimumSize());
	}

	private void specificActionPerformed(ActionEvent evt) {
		if (proteinsCard.check(true)) {
			specific = true;
			getRootPane().setDefaultButton(modificationsButton);
			flipFonts(status[0], status[2]);
			if (update) {
				digestCard.updateProteasesPanels();
				update = false;
			}
			((CardLayout) mainPanel.getLayout()).show(mainPanel, DIGEST);
		}
	}

	private void backDigestActionPerformed(ActionEvent evt) {
		getRootPane().setDefaultButton(specificButton);
		flipFonts(status[2], status[0]);
		((CardLayout) mainPanel.getLayout()).show(mainPanel, PROTEINS);
	}

	private void modificationsActionPerformed(ActionEvent evt) {
		if (digestCard.getLevels() == 0) {
			JOptionPane.showMessageDialog(this, "No protease was selected.", "Select proteases", JOptionPane.WARNING_MESSAGE);
			return;
		}
		getRootPane().setDefaultButton(bondsButton);
		flipFonts(status[2], status[4]);
		modificationsCard.setProteases(digestCard.getProteases());
		modificationsCard.setProteins(proteinsCard.getProteins());
		((CardLayout) mainPanel.getLayout()).show(mainPanel, MODIFICATIONS);
		setMinimumSize(null);
		setMinimumSize(getMinimumSize());
	}

	private void backModificationActionPerformed(ActionEvent evt) {
		modificationsCard.release();
		if (specific) {
			getRootPane().setDefaultButton(modificationsButton);
			flipFonts(status[4], status[2]);
			if (update) {
				digestCard.updateProteasesPanels();
				update = false;
			}
			((CardLayout) mainPanel.getLayout()).show(mainPanel, DIGEST);
		} else {
			status[2].setEnabled(true);
			getRootPane().setDefaultButton(specificButton);
			flipFonts(status[4], status[0]);
			((CardLayout) mainPanel.getLayout()).show(mainPanel, PROTEINS);
		}
	}

	private void bondsActionPerformed(ActionEvent evt) {
		if (modificationsCard.check()) {
			getRootPane().setDefaultButton(measurementBondsButton);
			flipFonts(status[4], status[6]);
			bondsCard.setProteins(modificationsCard.getProteins());
			((CardLayout) mainPanel.getLayout()).show(mainPanel, BONDS);
		}
	}

	private void backBondsActionPerformed(ActionEvent evt) {
		bondsCard.release();
		getRootPane().setDefaultButton(bondsButton);
		flipFonts(status[6], status[4]);
		((CardLayout) mainPanel.getLayout()).show(mainPanel, MODIFICATIONS);
	}

	private void measurementActionPerformed(ActionEvent evt) {
		if ((bonds && bondsCard.check(true)) || (!bonds && modificationsCard.check())) {
			getRootPane().setDefaultButton(runButton);
			flipFonts(status[bonds ? 6 : 4], status[8]);
			((CardLayout) mainPanel.getLayout()).show(mainPanel, MEASUREMENT);
		}
	}

	private void backMeasurementActionPerformed(ActionEvent evt) {
		if (bonds) {
			getRootPane().setDefaultButton(measurementBondsButton);
			flipFonts(status[8], status[6]);
			((CardLayout) mainPanel.getLayout()).show(mainPanel, BONDS);
		} else {
			getRootPane().setDefaultButton(measurementModificationsButton);
			flipFonts(status[8], status[4]);
			((CardLayout) mainPanel.getLayout()).show(mainPanel, MODIFICATIONS);
		}
	}

	private void runActionPerformed(ActionEvent evt) {
		if (!measurementCard.check()) {
			return;
		}
		boolean filter = measurementCard.isFiltered();
		if (!bonds) {
			bondsCard.clean();
		}
		ArrayList<String> header = new ArrayList(5);
		header.add("Proteins:\t" + proteinsCard.getProteinNames().toString().substring(1, proteinsCard.getProteinNames().toString().length() - 1));
		header.add("Modifications:\t" + modificationsCard.logModificationsShort());
		header.add("Bonds:\t" + bondsCard.logBondsShort());
		header.add(specific ? ("Specific digest:\t" + digestCard.logProteasesShort()) : "Non-specific digest.");
		StringBuilder buf = new StringBuilder();
		if (specific) {
			for (String string : digestCard.logSettings()) {
				if (buf.length() != 0) {
					buf.append(" ");
				}
				buf.append(string);
			}
		}
		for (String string : measurementCard.logSettings()) {
			if (buf.length() != 0) {
				buf.append(" ");
			}
			buf.append(string);
		}
		header.add("Settings:\t" + buf.toString());
		if (filter || alternative != null) {
			if (!resultsCard.initFiltered(header, modificationsCard.getProteins(), bondsCard.getBonds(filter), measurementCard.getMasses(),
					  measurementCard.getCheckMasses(), measurementCard.getFileNames(), measurementCard.getTolerance(),
					  measurementCard.minLength(), measurementCard.maxLength(), measurementCard.minMass(), measurementCard.maxMass())) {
				return;
			}
		} else {
			if (!resultsCard.initUnfiltered(header, modificationsCard.getProteins(), bondsCard.getBonds(filter), measurementCard.minLength(), measurementCard.maxLength(),
					  measurementCard.minMass(), measurementCard.maxMass())) {
				return;
			}
		}

		getRootPane().setDefaultButton(null);
		flipFonts(status[8], status[10]);
		((CardLayout) mainPanel.getLayout()).show(mainPanel, COMPUTING);
		if (quick == null) {
			try (PrintWriter pw = saveSetting(DEFAULT_INPUT, false, null)) {
			}
		} else {
			quick = null;
		}
		modificationsCard.applyModifications(filter);
		bondsCard.applyBonds(filter);
		try {
			Task task;
			if (specific) {
				task = new Task(this, resultsCard, digestCard.getProteases(), digestCard.getMCLimit());
			} else {
				task = new Task(this, resultsCard);
			}
			task.execute();
		} catch (Exception e) {
		}
	}

	private void cancelActionPerformed(ActionEvent evt) {
		if (!computingCard.setStatus(ComputingCard.CANCEL)) {
			return;
		}
		if (alternative != null) {
			this.dispose();
		}

		for (Protein protein : modificationsCard.getProteins().values()) {
			protein.removeModifications();
		}
		getRootPane().setDefaultButton(runButton);
		flipFonts(status[10], status[8]);
		((CardLayout) mainPanel.getLayout()).show(mainPanel, MEASUREMENT);
	}

	private void stopActionPerformed(ActionEvent evt) {
		if (!computingCard.setStatus(ComputingCard.STOP)) {
			return;
		}
		JOptionPane.showMessageDialog(this, "The computation is terminated prematurely.", "Termination", JOptionPane.WARNING_MESSAGE);
		while (computingCard.getStatus() != ComputingCard.FREE) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
			}
		}
		showResults();
//    }
	}

	public void showResults() {
		try {
			computingCard.next();
			computingCard.append("Saving results.");
		} catch (InterruptedException ex) {
		}
		//getRootPane().setDefaultButton(newButton);
		flipFonts(status[10], status[12]);
		((CardLayout) mainPanel.getLayout()).show(mainPanel, RESULTS);
		resultsCard.saveAll(true);
	}

	public void disableRun() {
		newButton.setEnabled(false);
		runButton.setEnabled(false);
	}

	private void backRunActionPerformed(ActionEvent evt) {
		for (Protein protein : modificationsCard.getProteins().values()) {
			protein.removeModifications();
		}
		getRootPane().setDefaultButton(runButton);
		flipFonts(status[12], status[8]);
		((CardLayout) mainPanel.getLayout()).show(mainPanel, MEASUREMENT);
	}

	private void newActionPerformed(Action evt) {
		status[2].setEnabled(true);
		getRootPane().setDefaultButton(specificButton);
		flipFonts(status[12], status[0]);
		((CardLayout) mainPanel.getLayout()).show(mainPanel, PROTEINS);
		resultsCard.clean();
	}

	private String selectedPanel() {
		if (proteinsCard.isVisible()) {
			return PROTEINS;
		}
		if (digestCard.isVisible()) {
			return DIGEST;
		}
		if (modificationsCard.isVisible()) {
			return MODIFICATIONS;
		}
		if (bondsCard.isVisible()) {
			return BONDS;
		}
		if (measurementCard.isVisible()) {
			return MEASUREMENT;
		}
		if (computingCard.isVisible()) {
			return COMPUTING;
		}
		if (resultsCard.isVisible()) {
			return RESULTS;
		}
		return null;
	}

	private int selectedPanelIndex() {
		for (int i = 0; i < cards.length; i++) {
			if (cards[i].isVisible()) {
				return i;
			}
		}
		return -1;
	}

	private void flipFonts(JLabel a, JLabel b) {
		Font font = a.getFont();
		a.setFont(b.getFont());
		b.setFont(font);
	}

	public boolean quick() throws IOException {
		if (!proteinsCard.check(true)) {
			return false;
		}
		if (specific) {
			specificButton.doClick();
			if (digestCard.getLevels() == 0) {
				JOptionPane.showMessageDialog(this, "No protease was found.", "Corrupted proteases", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			modificationsButton.doClick();
		} else {
			nonspecificButton.doClick();
		}
		if (!modificationsCard.check()) {
			return false;
		}
		if (bonds) {
			bondsButton.doClick();
			if (!bondsCard.check(false)) {
				return false;
			}
			measurementBondsButton.doClick();
		} else {
			measurementModificationsButton.doClick();
		}
		if (!measurementCard.check()) {
			return false;
		}
		runButton.doClick();
		return true;
	}

	private void formWindowClosing(WindowEvent evt) {
		computingCard.setStatus(ComputingCard.CANCEL);
		Properties dict = new Properties();
		dict.setProperty("Path", inputPath);
		dict.setProperty("ExtendedState", String.valueOf(getExtendedState()));
		setExtendedState(NORMAL);
		Defaults.putWindowDefaults(this, dict);
		Defaults.addDefaults(MAIN, dict);
		for (Card card : cards) {
			card.saveDefaults();
		}
	}

	public static String quick;
	public static String alternative;

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
		try {
			for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			java.util.logging.Logger.getLogger(LinX.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		//</editor-fold>
		//</editor-fold>

		//args = new String[] { "test\\alternatives.sen", "test\\alternatives.txt" };
		switch (args.length) {
			case 0:
				quick = null;
				alternative = null;
				break;
			case 1:
				quick = args[0];
				alternative = null;
				break;
			default:
				quick = args[0];
				alternative = args[1];
		}

		/* Create and display the form */
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				LinX linx = null;
				try {
					linx = new LinX();
					if (quick != null && !linx.quick() && alternative != null) {
						System.exit(-1);
					}
					linx.setVisible(true);
				} catch (Throwable e) {
					try (PrintWriter pw = new PrintWriter(new FileWriter("error.txt", true))) {
						pw.println(new java.util.Date().toString());
						pw.println("--------------------------------");
						pw.println(e.getMessage());
						pw.println("--------------------------------");
						e.printStackTrace(pw);
						pw.println("--------------------------------");
						if (linx == null) {
							if (Files.exists(Paths.get(DEFAULT_INPUT))) {
								for (String line : Files.readAllLines(Paths.get(DEFAULT_INPUT), Defaults.CHARSET)) {
									if (line.matches("-+")) {
										break;
									}
									pw.println(line);
								}
							}
						} else {
							linx.saveSetting(pw, null);
						}
						pw.append("################################################################" + System.lineSeparator());
						pw.append(System.lineSeparator());
						pw.flush();
					} catch (Exception f) {
						Logger.getLogger(LinX.class.getName()).log(Level.SEVERE, null, e);
						Logger.getLogger(LinX.class.getName()).log(Level.SEVERE, null, f);
					} finally {
						JOptionPane.showMessageDialog(linx, e.getMessage() + System.lineSeparator() + "More informations in 'error.txt'.", "Unexpected exception", JOptionPane.ERROR_MESSAGE);
						System.exit(9);
					}
				}
			}
		});
	}
}
