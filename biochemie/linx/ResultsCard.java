package biochemie.linx;

import biochemie.*;
import biochemie.dbedit.SettingsPanel;
import java.awt.event.*;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;

/**
 *
 * @author Janek
 */
public class ResultsCard extends Card {

	CheckNsDialog nsDialog;

	private static final String UNFILTERED = "unfiltered";
	private static final String LEFT = "N-terminus";
	private static final String RIGHT = "C-terminus";
	public static final String ANALYSERS = "analysers";
	private static Properties PROPERTIES;
	private static boolean WRAP;

	static {
		PROPERTIES = Defaults.getDefaults("Results");

		if (PROPERTIES.getProperty("Wrap") == null) {
			WRAP = false;
			PROPERTIES.setProperty("Wrap", "false");
		} else {
			WRAP = Boolean.parseBoolean(PROPERTIES.getProperty("Wrap"));
		}

		Defaults.addDefaults("Results", PROPERTIES);
	}

	public static SettingsPanel getSettingsPanel() {
		SettingsPanel ret = new SettingsPanel("Results panel") {
			private JLabel wrapLabel;
			private JComboBox wrapComboBox;

			{
				wrapLabel = new JLabel("Wrap input:");
				wrapComboBox = new JComboBox(new String[]{"Yes", "No"});

				wrapComboBox.setSelectedIndex(WRAP ? 0 : 1);
				wrapComboBox.setName(wrapComboBox.getSelectedItem().toString());

				GroupLayout resultsLayout = new GroupLayout(this);
				setLayout(resultsLayout);
				resultsLayout.setHorizontalGroup(
						  resultsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
									 .addGroup(resultsLayout.createSequentialGroup()
												.addContainerGap()
												.addComponent(wrapLabel)
												.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
												.addComponent(wrapComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
												.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				);
				resultsLayout.setVerticalGroup(
						  resultsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
									 .addGroup(resultsLayout.createSequentialGroup()
												.addContainerGap(GroupLayout.DEFAULT_SIZE / 2, GroupLayout.DEFAULT_SIZE / 2)
												.addGroup(resultsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
														  .addComponent(wrapComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
														  .addComponent(wrapLabel))
												.addContainerGap(GroupLayout.DEFAULT_SIZE / 2, GroupLayout.DEFAULT_SIZE / 2)));
			}

			@Override
			public boolean isChanged() {
				return !wrapComboBox.getName().equals(wrapComboBox.getSelectedItem());
			}

			@Override
			public void save() {
				ResultsCard.PROPERTIES.setProperty("Wrap", wrapComboBox.getSelectedIndex() == 1 ? "false" : "true");
				Defaults.addDefaults("Results", ResultsCard.PROPERTIES);
			}
		};

		return ret;
	}

	private LinX parent;
	private Properties analyzers;
	private ComputingCard computingCard;
	private JPanel mainPanel;
	private JScrollPane inputScrollPane;
	private JTextArea input;
	private String basicInput;
	private JTabbedPane tabbedPane;
	private JButton analyzeButton;
	private HashMap<FileName, ResultsPanel> resultsPanels;
	private LinkedHashMap<String, Protein> proteins;
	private ArrayList<BondReal> bonds;
	private boolean filter;
	private TreeMap<BigDecimal, HashSet<IMeasurement>> masses;
	private HashMap<FileName, TreeSet<BigDecimal>> check;
	private MeasurementCard.Tolerance tolerance;
	private int minLength;
	private int maxLength;
	private BigDecimal minMass;
	private BigDecimal maxMass;
	private FilterDialog filters;

	public ResultsCard(String id, JButton[] movement, LinX parent, ComputingCard computingCard) {
		super(id, movement);
		this.parent = parent;
		this.computingCard = computingCard;
		mainPanel = new JPanel();
		inputScrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		input = new JTextArea("Proteins:\nProteases:\nModifications:\nBonds:\nMeasurement:", 5, 100);
		tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		resultsPanels = new HashMap(1);
		JButton saveAllButton = new JButton("Save all");
		JButton saveCurButton = new JButton("Save current");

		analyzeButton = new JButton();
		reloadAnalyzers();
		final JButton groupButton = new JButton("Grouping    ▲");
		final JButton filterButton = new JButton("Filtering");
		final JButton checkNsButton = new JButton("Check 15N labeling");

		mainScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		mainScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		mainScrollPane.setViewportView(mainPanel);

		input.setEditable(false);
		input.setFont(UIManager.getFont("Label.font"));
		input.setLineWrap(WRAP);
		input.setWrapStyleWord(WRAP);
		inputScrollPane.setViewportView(input);

		GroupLayout layout = new GroupLayout(mainPanel);
		mainPanel.setLayout(layout);
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				  .addGroup(layout.createSequentialGroup()
							 .addComponent(inputScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							 .addComponent(tabbedPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				  .addComponent(inputScrollPane)
				  .addComponent(tabbedPane));

		saveAllButton.setMnemonic('A');
		saveAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				saveAll(false);
			}
		});

		saveCurButton.setMnemonic('C');
		saveCurButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				((ResultsPanel) tabbedPane.getSelectedComponent()).saveResults(false, true);
			}
		});

		analyzeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				switch (analyzers.size()) {
					case 0:
						break;
					case 1:
						analyze(analyzers.getProperty(analyzeButton.getText()));
						break;
					default:
						JPopupMenu jpm = new JPopupMenu("Analysers");
						for (JMenuItem item : getAnalyzers(new HashSet<Character>())) {
							item.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
									analyze(item.getActionCommand());
								}
							});
							jpm.add(item);
						}
						jpm.show(ResultsCard.this, analyzeButton.getX(), analyzeButton.getY() - jpm.getPreferredSize().height);
				}
			}
		});

		groupButton.setMnemonic('G');
		groupButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				JPopupMenu jpm = new JPopupMenu("Grouping");
				JMenuItem jmi;
				jmi = new JMenuItem("Group modifications", KeyEvent.VK_M);
				jmi.setAccelerator(KeyStroke.getKeyStroke('M'));
				jmi.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							mergeDuplicationsAll(5);
						} catch (InterruptedException ie) {
						}
					}
				});
				jpm.add(jmi);
				jmi = new JMenuItem("Group bonds", KeyEvent.VK_B);
				jmi.setAccelerator(KeyStroke.getKeyStroke('B'));
				jmi.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							mergeDuplicationsAll(6);
						} catch (InterruptedException ie) {
						}
					}
				});
				jpm.add(jmi);
				jmi = new JMenuItem("Group by masses", KeyEvent.VK_G);
				jmi.setAccelerator(KeyStroke.getKeyStroke('G'));
				jmi.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							mergeDuplicationsAll(0);
						} catch (InterruptedException ie) {
						}
					}
				});
				jpm.add(jmi);
				jpm.addSeparator();
				jmi = new JMenuItem("Ungroup by masses", 'H');
				jmi.setAccelerator(KeyStroke.getKeyStroke('H'));
				jmi.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						splitDuplicationsAll(0);
					}
				});
				jpm.add(jmi);
				jmi = new JMenuItem("Ungroup bonds", 'C');
				jmi.setAccelerator(KeyStroke.getKeyStroke('C'));
				jmi.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						splitDuplicationsAll(6);
					}
				});
				jpm.add(jmi);
				jmi = new JMenuItem("Ungroup modifications", 'N');
				jmi.setAccelerator(KeyStroke.getKeyStroke('N'));
				jmi.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						splitDuplicationsAll(5);
					}
				});
				jpm.add(jmi);
				jmi = new JMenuItem("Ungroup all", 'A');
				jmi.setAccelerator(KeyStroke.getKeyStroke('A'));
				jmi.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						defaults.setProperty("Merge", "");
						int count = 0;
						while (true) {
							int tmp = rows();
							if (tmp == count) {
								break;
							}
							count = tmp;
							splitDuplicationsAll(0);
							splitDuplicationsAll(6);
							splitDuplicationsAll(5);
						}
					}
				});
				jpm.add(jmi);
				jpm.show(ResultsCard.this, groupButton.getX(), groupButton.getY() - jpm.getPreferredSize().height);
			}
		});

		filters = new FilterDialog(parent, defaults);
		filterButton.setMnemonic('F');
		filterButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if (filters.showFilterDialog(filter)) {
					input.setText(basicInput);
					RowFilter rf = filters.getRowFilter();
					if (rf != null) {
						ArrayList<String> rows = filters.toStringRowFilter();
						input.setText(input.getText() + "\nFilters:\t" + rows.get(0));
						for (int i = 1; i < rows.size(); i++) {
							input.setText(input.getText() + "\n\t" + rows.get(i));
						}
					}
					input.setRows(Math.min(input.getLineCount(), 10));
					input.setCaretPosition(0);
					for (ResultsPanel resultsPanel : resultsPanels.values()) {
						resultsPanel.setRowFilter(rf);
					}
					mainPanel.revalidate();
				}
			}
		});

		checkNsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				int tmpScanTol;
				do {
					String input = JOptionPane.showInputDialog(ResultsCard.this, "Scan tolerance for N masses:", CheckNsDialog.SCAN_TOL);
					if (input == null) {
						return;
					}
					try {
						tmpScanTol = Integer.parseInt(input);
					} catch (NumberFormatException ex) {
						tmpScanTol = -1;
					}
				} while (tmpScanTol < 0);

				double tmpMinInt;
				do {
					String input = JOptionPane.showInputDialog(ResultsCard.this, "Minimal intensity for correction factors calculation:", String.format("%s", (double) CheckNsDialog.MIN_INT_FOR_BG));
					if (input == null) {
						return;
					}
					try {
						tmpMinInt = Double.parseDouble(input);
					} catch (NumberFormatException ex) {
						tmpMinInt = -1;
					}
				} while (tmpMinInt < 0);

				int tmpOutTol;
				do {
					String input = JOptionPane.showInputDialog(ResultsCard.this, "Maximal difference from median for correction factors calculation:", CheckNsDialog.MED_DIF_FILTER);
					if (input == null) {
						return;
					}
					try {
						tmpOutTol = Integer.parseInt(input);
					} catch (NumberFormatException ex) {
						tmpOutTol = -1;
					}
				} while (tmpOutTol < 0);

				if (resultsPanels.size() > 1) {
					JOptionPane.showMessageDialog(ResultsCard.this, "Several analyses will be analysed.\nPlease check which analysis is displayed.", "Note", JOptionPane.INFORMATION_MESSAGE);
				}

				final int scanTol = tmpScanTol;
				final long minInt = Math.round(tmpMinInt);
				final int outTol = tmpOutTol;
				ProgressMonitor monitor = new ProgressMonitor(ResultsCard.this, "Loading Progress", "Waiting for window", 0, 100);
				monitor.setMillisToPopup(100);
				monitor.setMillisToDecideToPopup(100);

				new SwingWorker<Object, Object>() {
					@Override
					protected Object doInBackground() throws Exception {
						try {
							nsDialog = new CheckNsDialog(parent, resultsPanels, masses, monitor, scanTol, minInt, outTol);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						return null;
					}

					@Override
					protected void done() {
						if (nsDialog == null) {
							JOptionPane.showMessageDialog(parent, "Unable to create window, exceptions written to console.", "Cannot create window", JOptionPane.ERROR_MESSAGE);
							return;
						}
						nsDialog.setLocationRelativeTo(parent);
						nsDialog.setVisible(true);
					}
				}.execute();
			}
		});

		createLayout(new JButton[]{saveAllButton, saveCurButton, analyzeButton, groupButton, filterButton, checkNsButton});

		if (defaults.getProperty("Merge") == null) {
			defaults.setProperty("Merge", "");
		}
	}

	public ArrayList<JMenuItem> getAnalyzers(HashSet<Character> used) {
		ArrayList<JMenuItem> ret = new ArrayList();

		for (String property : analyzers.stringPropertyNames()) {
			JMenuItem jmi = new JMenuItem(property);
			jmi.setActionCommand(analyzers.getProperty(property));
			for (char c : property.toUpperCase().toCharArray()) {
				if (!used.contains(c)) {
					jmi.setMnemonic(KeyEvent.getExtendedKeyCodeForChar(c));
					jmi.setAccelerator(KeyStroke.getKeyStroke(c));
					used.add(c);
					break;
				}
			}
			ret.add(jmi);
		}

		return ret;
	}

	public void reloadAnalyzers() {
		this.analyzers = Defaults.loadDefaults(ANALYSERS);

		if (analyzers.isEmpty()) {
			analyzeButton.setText("");
			analyzeButton.setEnabled(false);
			analyzeButton.setVisible(false);
		} else {
			if (analyzers.size() == 1) {
				analyzeButton.setText(analyzers.propertyNames().nextElement().toString());
				for (char c : analyzeButton.getText().toUpperCase().toCharArray()) {
					if (c != 'F' && c != 'P' && c != 'M' && c != 'H' && c != 'A' && c != 'C' && c != 'G' && c != 'F' && c != 'B' && c != 'N') {
						analyzeButton.setMnemonic(c);
						break;
					}
				}
			} else {
				analyzeButton.setText("Analyse    ▲");
				analyzeButton.setMnemonic('E');
			}
			analyzeButton.setEnabled(true);
			analyzeButton.setVisible(true);
		}

	}

	@Override
	public void logDefaults() {
		if (tabbedPane.getSelectedIndex() != -1) {
			((ResultsPanel) tabbedPane.getSelectedComponent()).logDefaults(defaults);
		}
	}

	@Override
	public void applyDefaults() {
		for (ResultsPanel resultsPanel : resultsPanels.values()) {
			resultsPanel.applyDefaults(defaults);
		}
	}

	public ArrayList<String> logFilters() {
		ArrayList<String> ret = new ArrayList<>(3);
		if (tabbedPane.getSelectedIndex() != -1 && ((ResultsPanel) tabbedPane.getSelectedComponent()).isFiltered()) {
			ret.addAll(filters.toStringRowFilter());
		}
		return ret;
	}

	public void setFilters(ArrayList<String> filters) {
		this.filters.fromStringRowFilter(filters);
	}

	public void saveAll(boolean useDefault) {
		int tab = tabbedPane.getSelectedIndex();
		for (ResultsPanel resultsPanel : resultsPanels.values()) {
			if (!useDefault) {
				tabbedPane.setSelectedComponent(resultsPanel);
			}
			if (!resultsPanel.saveResults(useDefault, false)) {
				return;
			}
		}
		if (!useDefault) {
			tabbedPane.setSelectedIndex(tab);
			JOptionPane.showMessageDialog(this, "All files were successfully saved.", "Settings saved", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	public boolean initUnfiltered(ArrayList<String> header, LinkedHashMap<String, Protein> proteins, ArrayList<BondReal> bonds,
			  int minLength, int maxLength, final BigDecimal minMass, final BigDecimal maxMass) {
		LinkedHashMap<FileName, String> fns = new LinkedHashMap(1);
		fns.put(new FileName(null, null), "output");
		this.tolerance = new MeasurementCard.Tolerance() {
			@Override
			public BigDecimal lower(BigDecimal value) {
				return minMass;
			}

			@Override
			public BigDecimal upper(BigDecimal value) {
				return maxMass;
			}

			@Override
			public Double error(BigDecimal experimental, BigDecimal theoretical) {
				return null;
			}

			@Override
			public java.text.DecimalFormat getErrorFormat() {
				return null;
			}
		};
		init(header, proteins, bonds, minLength, maxLength, minMass, maxMass, fns, false);
		this.masses = new TreeMap();
		this.masses.put(minMass, new HashSet(1));
		FileName fn = new FileName(null, null);
		this.masses.get(minMass).add(new Measurement(fn, -1, null, null));
		this.check = new HashMap(1);
		this.check.put(fn, new TreeSet(this.masses.keySet()));
		return true;
	}

	public boolean initFiltered(ArrayList<String> header, LinkedHashMap<String, Protein> proteins, ArrayList<BondReal> bonds,
			  TreeMap<BigDecimal, HashSet<IMeasurement>> masses, HashMap<FileName, TreeSet<BigDecimal>> check, LinkedHashSet<FileName> defaultNames,
			  MeasurementCard.Tolerance tolerance, int minLength, int maxLength, BigDecimal minMass, BigDecimal maxMass) {
		String[][] parts = new String[defaultNames.size()][];
		int index = 0;
		for (FileName fileName : defaultNames) {
			int file = fileName.getFileName().lastIndexOf(File.separator);
			int dot = fileName.getFileName().lastIndexOf(".");
			if (dot <= file || fileName.getFileName().endsWith(".sen")) {
				dot = -1;
			}
			parts[index++] = new String[]{file < 0 ? "" : fileName.getFileName().substring(0, file + File.separator.length()),
				dot < 0 ? (file < 0 ? fileName.getFileName() : fileName.getFileName().substring(file + File.separator.length()))
				: fileName.getFileName().substring(file < 0 ? 0 : file + File.separator.length(), dot),
				dot < 0 ? "" : fileName.getFileName().substring(dot)};
		}

		// Je potřeba přípona?
		HashSet<String> tests = new HashSet(defaultNames.size());
		HashSet<String> names = new HashSet(parts.length);
		for (FileName fileName : defaultNames) {
			tests.add(fileName.getFileName());
		}
		for (String[] part : parts) {
			names.add(part[0] + part[1]);
		}
		if (tests.size() == names.size()) {
			for (String[] part : parts) {
				part[2] = "";
			}
		}

		// Složky
		names = new HashSet(parts.length);
		for (String[] part : parts) {
			names.add(part[1] + part[2]);
		}
		boolean first = true;
		String dirSep = "-";
		while (names.size() != tests.size()) {
			names = new HashSet(parts.length);
			dirSep = JOptionPane.showInputDialog(this, first ? "Please specify substitution for the folder separator in names of output files."
					  : "Specified separator causes ambiguous or forbidden file names. Please specify another separator.",
					  "Folder separator", JOptionPane.QUESTION_MESSAGE);
			first = false;
			if (dirSep == null) {
				return false;
			}
			if (dirSep.contains(File.pathSeparator) || dirSep.contains(File.separator)) {
				continue;
			}
			for (String[] part : parts) {
				try {
					String tmp = part[0].replace(File.separator, dirSep) + part[1] + part[2];
					Paths.get(tmp);
					names.add(tmp);
				} catch (Exception e) {
					break;
				}
			}
		}
		if (first) {
			for (String[] part : parts) {
				part[0] = "";
			}
		}

		// Indexy u duplicit
		String iSep = "-";
		first = true;
		while (parts.length != names.size()) {
			names = new HashSet(parts.length);
			iSep = JOptionPane.showInputDialog(this, first ? "There are several files with the same name, please specify the index separator."
					  : "Specified separator causes ambiguous or forbidden file names. Please specify another separator.",
					  "Index separator", JOptionPane.QUESTION_MESSAGE);
			first = false;
			if (iSep == null) {
				return false;
			}
			if (iSep.contains(File.pathSeparator) || iSep.contains(File.separator)) {
				continue;
			}
			tests = new HashSet(parts.length);
			for (String[] part : parts) {
				String tested = part[0].replace(File.separator, dirSep) + part[1] + part[2];
				if (tests.contains(tested)) {
					if (names.contains(tested)) {
						names.remove(tested);
						try {
							String tmp = tested + iSep + 1;
							Paths.get(tmp);
							names.add(tmp);
						} catch (Exception e) {
							break;
						}
					}
					for (int j = 2; j < Integer.MAX_VALUE; j++) {
						if (!names.contains(tested + iSep + j)) {
							names.add(tested + iSep + j);
							break;
						}
					}
				} else {
					tests.add(tested);
					names.add(tested);
				}
			}
		}
		for (String[] part : parts) {
			String tested = part[0].replace(File.separator, dirSep) + part[1] + part[2];
			if (names.contains(tested)) {
				names.remove(tested);
			} else {
				for (int j = 1; j < Integer.MAX_VALUE; j++) {
					if (names.contains(tested + iSep + j)) {
						part[2] += iSep + j;
						names.remove(tested + iSep + j);
						break;
					}
				}
			}
		}

		LinkedHashMap<FileName, String> dn = new LinkedHashMap(defaultNames.size());
		index = 0;
		for (FileName fileName : defaultNames) {
			dn.put(fileName, parts[index][0].replace(File.separator, dirSep) + parts[index][1] + parts[index][2]);
			index++;
		}

		this.tolerance = tolerance;
		init(header, proteins, bonds, minLength, maxLength, minMass, maxMass, dn, true);
		this.masses = masses;
		this.check = check;
		return true;
	}

	private void init(ArrayList<String> header, LinkedHashMap<String, Protein> proteins, ArrayList<BondReal> bonds,
			  int minLength, int maxLength, BigDecimal minMass, BigDecimal maxMass, LinkedHashMap<FileName, String> defaultNames, boolean filtered) {
		// Aby opakované Cancel nezanechalo příliš mnoho vláken s dobíhajícími výpočty, které by se mohli prát o proměnné.
		if (!computingCard.clean(filtered ? 8 : 7)) {
			try {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						JOptionPane.showMessageDialog(ResultsCard.this, "Waiting for the completion of the previsoudly canceled computation.", "Waiting", JOptionPane.INFORMATION_MESSAGE);
					}
				});
			} catch (Exception e) {
			}
			while (!computingCard.clean(filtered ? 8 : 7)) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ex) {
				}
			}
		}

		try {
			computingCard.next();
			computingCard.append("Initialization.");
		} catch (InterruptedException e) {
		}

		tabbedPane.removeAll();
		resultsPanels.clear();
		for (Map.Entry<FileName, String> entry : defaultNames.entrySet()) {
			resultsPanels.put(entry.getKey(), new ResultsPanel(parent, entry.getKey().getFileName() == null ? null : entry.getKey(), entry.getValue(), this, computingCard, proteins, filtered, tolerance.getErrorFormat()));
			tabbedPane.addTab(entry.getKey().getFileName() == null ? UNFILTERED : entry.getKey().toString(), resultsPanels.get(entry.getKey()));
			resultsPanels.get(entry.getKey()).applyDefaults(defaults);
		}
		this.proteins = proteins;
		for (Protein protein : this.proteins.values()) {
			protein.unblockPositions();
		}
		this.bonds = bonds;
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.minMass = minMass;
		this.maxMass = maxMass;
		this.filter = filtered;
		input.setRows(Math.min(header.size(), 10));
		basicInput = "";
		for (int i = 0; i < header.size(); i++) { // Není moc efektivní, ale provede se jen párkrát...
			basicInput += header.get(i) + "\n";
		}
		basicInput = basicInput.replace("\t|\t", " ;    ");
		basicInput = basicInput.substring(0, basicInput.length() - 1);
		input.setText(basicInput);
		filters.update();
	}

	public void clean() {
		tabbedPane.removeAll();
		resultsPanels.clear();
		proteins = null;
		bonds = null;
		masses = null;
		tolerance = null;
	}

	public void nonspecificDigest() throws InterruptedException {
		HashMap<String, Set<Peptide>> potential = new HashMap<>(proteins.size());
		HashMap<Integer, Character> emptyMap = new HashMap<>(0);
		for (Protein protein : proteins.values()) {
			potential.put(protein.getName(), new HashSet<>());
			Cleavage[] cleavages = new Cleavage[protein.length() + 1];
			cleavages[0] = new Cleavage(0, new HashMap<>(0), new HashMap<>(0), 0, 0, 0, new Protease("", Protease.SEPARATOR));
			for (int i = 1; i < protein.length(); i++) {
				cleavages[i] = new Cleavage(i, new HashMap<>(0), new HashMap<>(0), 0, 0, 1, new Protease("", Protease.SEPARATOR));
			}
			cleavages[protein.length()] = new Cleavage(protein.length(), new HashMap<>(0), new HashMap<>(0), 0, 0, 0, new Protease("", Protease.SEPARATOR));

			for (int i = 0; i <= protein.length() - minLength; i++) {
				BigDecimal minMass = protein.minMassN().add(protein.minMassC());
				BigDecimal maxMass = protein.maxMassN().add(protein.maxMassC());
				for (int j = i; j < Math.min(protein.length(), i + maxLength); j++) {
					minMass = minMass.add(protein.minMassAt(j));
					maxMass = maxMass.add(protein.maxMassAt(j));
					if (ChemicalElement.mass("+").add(minMass).compareTo(this.maxMass) > 0) {
						break;
					}
					if (j - i + 1 < minLength) { // minMass nemá smysl kontrolovat, protože můstky to mohou změnit
						continue;
					}
					potential.get(protein.getName()).add(new Peptide(protein, cleavages[i], cleavages[j + 1], emptyMap, minMass, maxMass));
				}
			}
		}

		checkPeptides(potential);
	}

	public void specificDigest(Collection<Collection<Protease>> proteases, int mcLimit) throws InterruptedException {
		HashMap<String, Set<Peptide>> potential;
		switch (Protease.worstProteaseClass(proteases)) {
			case EMPTY:
				maxLength = Math.max(maxLength, mcLimit + 1);
				nonspecificDigest();
				return;
			case ONE_CHAR:
				potential = digestTrivial(proteases, mcLimit);
				break;
			case CONSTANT_LENGTH:
				potential = digestOverlapped(proteases, mcLimit);
				break;
			case ANCHORED:
				throw new UnsupportedOperationException("Not implemented");
			default:
				throw new UnknownError("Nemělo by nastat, ale aby kompilátor neřval...");
		}

		checkPeptides(potential);
	}

	//<editor-fold defaultstate="collapsed" desc=" Overlapped Digest ">
	private HashMap<String, Set<Peptide>> digestOverlapped(Collection<Collection<Protease>> proteases, int mcLimit) {
		boolean lockLeft = false;
		boolean lockRight = false;
		BigDecimal minLeft = BigDecimal.ZERO; // 0 a ne +-nekonečno kvůli okrajům
		BigDecimal minRight = BigDecimal.ZERO;
		BigDecimal maxLeft = BigDecimal.ZERO;
		BigDecimal maxRight = BigDecimal.ZERO;
		for (Collection<Protease> level : proteases) {
			for (Protease protease : level) {
				if (protease.isLockedRight()) {
					lockLeft = true;
				}
				if (protease.isLockedLeft()) {
					lockRight = true;
				}
				minLeft = minLeft.min(protease.getModificationRight());
				maxLeft = maxLeft.max(protease.getModificationRight());
				minRight = minRight.min(protease.getModificationLeft());
				maxRight = maxRight.max(protease.getModificationLeft());
			}
		}

		Collection<Collection<Protease>> pros;
		if (LinX.alternative == null) {
			pros = proteases;
		} else {
			pros = new ArrayList<>(proteases.size());
			for (Collection<Protease> collection : proteases) {
				ArrayList<Protease> tmp = new ArrayList(collection);
				tmp.add(new Protease("Nonspecific", Protease.SEPARATOR));
				pros.add(tmp);
			}
		}
		HashMap<String, Set<Peptide>> potential = new HashMap<>(proteins.size());
		for (Protein protein : proteins.values()) {
			potential.put(protein.getName(), new HashSet<>());
			if (protein.length() < 1) {
				continue;
			}

			// Seznam okolí pro místa štěpení
			TreeMap<Integer, ArrayList<Cleavage>> cleavagesM = cleavagesOverlapped(proteases, protein);
			TreeMap<Integer, ArrayList<Cleavage>> cleavagesA = cleavagesOverlapped(pros, protein);

			for (Integer i : cleavagesA.keySet()) {
				BigDecimal minMass = protein.minMassN(lockLeft).add(minLeft.add(minRight.add(protein.minMassC(lockRight))));
				BigDecimal maxMass = protein.maxMassN(lockLeft).add(maxLeft.add(maxRight.add(protein.maxMassC(lockRight))));
				for (Integer j : cleavagesA.subMap(i, false, i + maxLength, true).keySet()) {
					for (int k = cleavagesA.lowerKey(j); k < j; k++) {
						minMass = minMass.add(protein.minMassAt(k));
						maxMass = maxMass.add(protein.maxMassAt(k));
					}
					if (ChemicalElement.mass("+").add(minMass).compareTo(this.maxMass) > 0) {
						break;
					}
					if (j - i >= minLength) { // minMass nemá smysl kontrolovat, protože můstky to mohou změnit
						for (Cleavage begin : cleavagesA.get(i)) {
							for (Cleavage end : cleavagesA.get(j)) {
								// Když nejsou kompatibilní mutace
								boolean incompatible = false;
								if (i > j - end.getLeft()) {
									for (Integer fixed : begin.getLeftMutations().keySet()) {
										if (end.getLeftMutations().containsKey(fixed) && !Objects.equals(begin.getLeftMutations().get(fixed), end.getLeftMutations().get(fixed))) {
											incompatible = true;
											break;
										}
									}
								}
								if (incompatible) {
									continue;
								}
								if (i + begin.getRight() > j - end.getLeft()) {
									for (Integer fixed : begin.getRightMutations().keySet()) {
										if (end.getLeftMutations().containsKey(fixed) && !Objects.equals(begin.getRightMutations().get(fixed), end.getLeftMutations().get(fixed))) {
											incompatible = true;
											break;
										}
									}
								}
								if (incompatible) {
									continue;
								}
								if (i + begin.getRight() > j) {
									for (Integer fixed : end.getRightMutations().keySet()) {
										if (begin.getRightMutations().containsKey(fixed) && !Objects.equals(begin.getRightMutations().get(fixed), end.getRightMutations().get(fixed))) {
											incompatible = true;
											break;
										}
									}
								}
								if (incompatible) {
									continue;
								}

								// Když nejsou kompatibilní místa štěpení
								if (!((begin.getLevel() <= end.getLevel() && i <= j - end.getLeft())
										  || (begin.getLevel() >= end.getLevel() && i + begin.getRight() <= j))) {
									continue;
								}

								// Určitě nemůže být příliš m-c
								Map<Integer, Character> fixed = new HashMap<>(begin.getRightMutations());
								fixed.putAll(end.getLeftMutations());
								NavigableMap<Integer, ArrayList<Cleavage>> subMap = cleavagesM.subMap(i, false, j, false);
								if (subMap.size() <= mcLimit) {
									potential.get(protein.getName()).add(new Peptide(protein, begin, end, fixed, minMass, maxMass));
									continue;
								}

								// Hrubý limit - počet jistých míst štěpení; počet teoreticky možných míst štěpení
								TreeMap<Integer, ArrayList<Cleavage>> possible = new TreeMap<>();
								HashSet<Integer> sure = new HashSet<>();
								for (Integer middle : subMap.keySet()) {
									for (Cleavage cleavage : subMap.get(middle)) {
										// Zda je dané místo štěpení vůbec kompatibilní s okraji
										boolean compatible = true;

										// Test zda nevyžaduje někde jinou mutaci
										if (i > cleavage.getPosition() - cleavage.getLeft()) {
											for (Integer fix : begin.getLeftMutations().keySet()) {
												if (cleavage.getLeftMutations().containsKey(fix) && !Objects.equals(begin.getLeftMutations().get(fix), cleavage.getLeftMutations().get(fix))) {
													compatible = false;
													break;
												}
											}
										}
										if (!compatible) {
											continue;
										}
										if (i + begin.getRight() >= cleavage.getPosition() - cleavage.getLeft()) {
											for (Integer fix : begin.getRightMutations().keySet()) {
												if ((cleavage.getLeftMutations().containsKey(fix) && !Objects.equals(begin.getRightMutations().get(fix), cleavage.getLeftMutations().get(fix)))
														  || (cleavage.getRightMutations().containsKey(fix) && !Objects.equals(begin.getRightMutations().get(fix), cleavage.getRightMutations().get(fix)))) {
													compatible = false;
													break;
												}
											}
										}
										if (!compatible) {
											continue;
										}
										if (cleavage.getPosition() + cleavage.getRight() >= j - end.getLeft()) {
											for (Integer fix : end.getLeftMutations().keySet()) {
												if ((cleavage.getLeftMutations().containsKey(fix) && !Objects.equals(end.getLeftMutations().get(fix), cleavage.getLeftMutations().get(fix)))
														  || (cleavage.getRightMutations().containsKey(fix) && !Objects.equals(end.getLeftMutations().get(fix), cleavage.getRightMutations().get(fix)))) {
													compatible = false;
													break;
												}
											}
										}
										if (!compatible) {
											continue;
										}
										if (cleavage.getPosition() + cleavage.getRight() > j) {
											for (Integer fix : end.getRightMutations().keySet()) {
												if (cleavage.getRightMutations().containsKey(fix) && !Objects.equals(end.getRightMutations().get(fix), cleavage.getRightMutations().get(fix))) {
													compatible = false;
													break;
												}
											}
										}
										if (!compatible) {
											continue;
										}

										// Test, zda se nevylučují délky okrajů
										if ((begin.getLevel() <= cleavage.getLevel() && i > cleavage.getPosition() - cleavage.getLeft())
												  || (cleavage.getLevel() >= end.getLevel() && cleavage.getPosition() + cleavage.getRight() > j)) {
											compatible = false;
										}
										if (!compatible) {
											continue;
										}

										// Dané místo tedy může být missed-cleavage
										if (!possible.containsKey(middle)) {
											possible.put(middle, new ArrayList<>(1));
										}
										possible.get(middle).add(cleavage);

										// Test, zda dané místo určitě je missed-cleavage, tj. zda nevyžaduje žádné další nastavení mutací
										boolean gen = true;
										for (Integer fix : cleavage.getLeftMutations().keySet()) { // Pokud obsahuje, víme z předchozího, že jsou stejné
											if (fix >= i && !(begin.getRightMutations().containsKey(fix) && end.getLeftMutations().containsKey(fix))) {
												gen = false;
												break;
											}
										}
										for (Integer fix : cleavage.getRightMutations().keySet()) {
											if (fix <= j && !(begin.getRightMutations().containsKey(fix) && end.getLeftMutations().containsKey(fix))) {
												gen = false;
												break;
											}
										}
										if (gen) {
											sure.add(middle);
											possible.remove(middle);
											break;
										}
									}
								}
								// Vždy bude příliš missed-cleavage
								if (sure.size() > mcLimit) {
									continue;
								}
								// Nikdy nebude dost missed-cleavage
								if (sure.size() + possible.size() <= mcLimit) {
									potential.get(protein.getName()).add(new Peptide(protein, begin, end, fixed, minMass, maxMass));
									continue;
								}

								// Nutné počítat pro konkrétní možná nastavení mutací
								int min = Math.min(i - begin.getLeft(), j - end.getLeft());
								int max = Math.max(i + begin.getRight(), j + end.getRight());
								for (ArrayList<Cleavage> middles : subMap.values()) {
									for (Cleavage cleavage : middles) {
										if (min > cleavage.getPosition() - cleavage.getLeft()) {
											min = cleavage.getPosition() - cleavage.getLeft();
										}
										if (max < cleavage.getPosition() + cleavage.getRight()) {
											max = cleavage.getPosition() + cleavage.getRight();
										}
									}
								}
								Map<Integer, Character> fixed2 = new HashMap<>(fixed);
								fixed2.putAll(begin.getLeftMutations());
								fixed2.putAll(end.getRightMutations());
								if (protein.reset(begin, end, fixed2)) {
									do {
										int hits = 0;
										Map<Integer, Character> configuration = protein.getConfigurationMutations();
										for (ArrayList<Cleavage> middles : possible.values()) {
											for (Cleavage cleavage : middles) {
												boolean correct = true;
												for (Integer position : cleavage.getLeftMutations().keySet()) {
													if (configuration.containsKey(position) && !Objects.equals(configuration.get(position), cleavage.getLeftMutations().get(position))) {
														correct = false;
														break;
													}
												}
												if (!correct) {
													continue;
												}
												for (Integer position : cleavage.getRightMutations().keySet()) {
													if (configuration.containsKey(position) && !Objects.equals(configuration.get(position), cleavage.getRightMutations().get(position))) {
														correct = false;
														break;
													}
												}
												if (correct) {
													hits++;
													break;
												}
											}
											if (hits + sure.size() > mcLimit) {
												break;
											}
										}
										if (hits + sure.size() <= mcLimit) {
											potential.get(protein.getName()).add(new Peptide(protein, begin, end, protein.getConfigurationMutations(), minMass, maxMass));
										}
									} while (protein.nextConfiguration());
								}
							}
						}
					}
					// NON-OPT: Počítat fixedCleavage, aby se netestovaly zbytečně dlouhé peptidy (na druhou stranu to možná omezí maximální hmota peptidu)
				}
			}
		}

		return potential;
	}

	private TreeMap<Integer, ArrayList<Cleavage>> cleavagesOverlapped(Collection<Collection<Protease>> proteases, Protein protein) {
		// Seznam možných štěpení na daných pozicích.
		TreeMap<Integer, ArrayList<Cleavage>> cleavages = new TreeMap<>();
		// Vložení krajních zarážek
		cleavages.put(0, new ArrayList<>(1));
		cleavages.get(0).add(new Cleavage(0, new HashMap<>(0), new HashMap<>(0), 0, 0, 0, new Protease("", ";")));
		cleavages.put(protein.length(), new ArrayList<>(1));
		cleavages.get(protein.length()).add(new Cleavage(protein.length(), new HashMap<>(0), new HashMap<>(0), 0, 0, 0, new Protease("", ";")));

		int n = 1;
		for (Collection<Protease> level : proteases) {
			for (Protease protease : level) {
				int lengthLeft = protease.getLengthLeft();
				int lengthRight = protease.getLengthRight();
				Pattern patternLeft = Pattern.compile(protease.getRuleLeft());
				Pattern patternRight = Pattern.compile(protease.getRuleRight());
				if (protease.getLeftClass() == Protease.ProteaseClass.EMPTY) {
					if (protease.getRightClass() == Protease.ProteaseClass.EMPTY) {
						for (int i = 1; i < protein.length(); i++) {
							if (!cleavages.containsKey(i)) {
								cleavages.put(i, new ArrayList<>(1));
							}
							cleavages.get(i).add(new Cleavage(i, new HashMap<>(0), new HashMap<>(0), 0, 0, n, protease));
						}
					} else {
						for (int i = 1; i <= protein.length() - lengthRight; i++) {
							if (protein.resetMutations(new SimplifiedCleavage(i, new HashMap<>(0), new HashMap<>(0), n, protease), i + lengthRight)) {
								do {
									Matcher matcher = patternRight.matcher(protein);
									if (matcher.find(i) && matcher.start() == i) {
										if (!cleavages.containsKey(i)) {
											cleavages.put(i, new ArrayList<>(1));
										}
										cleavages.get(i).add(new Cleavage(i, new HashMap<>(0), protein.getConfigurationMutations(), 0, lengthRight, n, protease));
									}
								} while (protein.nextConfigurationMutations());
							}
						}
					}
				} else {
					if (protease.getRightClass() == Protease.ProteaseClass.EMPTY) {
						for (int i = 0; i < protein.length() - lengthLeft; i++) {
							if (protein.resetMutations(i, new SimplifiedCleavage(i + lengthLeft, new HashMap<>(0), new HashMap<>(0), n, protease))) {
								do {
									Matcher matcher = patternLeft.matcher(protein);
									if (matcher.find(i) && matcher.start() == i) {
										if (!cleavages.containsKey(i + 1)) {
											cleavages.put(i + 1, new ArrayList<>(1));
										}
										cleavages.get(i + 1).add(new Cleavage(i + 1, protein.getConfigurationMutations(), new HashMap<>(0), lengthLeft, 0, n, protease));
									}
								} while (protein.nextConfigurationMutations());
							}
						}
					} else {
						for (int i = lengthLeft; i <= protein.length() - lengthRight; i++) {
							Set<Map<Integer, Character>> configurations = new HashSet<>();
							if (protein.resetMutations(i - lengthLeft, new SimplifiedCleavage(i, new HashMap<>(0), new HashMap<>(0), n, protease))) {
								do {
									Matcher matcher = patternLeft.matcher(protein);
									if (matcher.find(i - lengthLeft) && matcher.start() == i - lengthLeft) {
										configurations.add(protein.getConfigurationMutations());
									}
								} while (protein.nextConfigurationMutations());
							}
							if (configurations.isEmpty()) {
								continue;
							}
							if (protein.resetMutations(new SimplifiedCleavage(i, new HashMap<>(0), new HashMap<>(0), n, protease), i + lengthRight)) {
								do {
									Matcher matcher = patternRight.matcher(protein);
									if (matcher.find(i) && matcher.start() == i) {
										for (Map<Integer, Character> conf : configurations) {
											if (!cleavages.containsKey(i)) {
												cleavages.put(i, new ArrayList<>(1));
											}
											cleavages.get(i).add(new Cleavage(i, conf, protein.getConfigurationMutations(), lengthLeft, lengthRight, n, protease));
										}
									}
								} while (protein.nextConfigurationMutations());
							}
						}
					}
				}
			}
			n++;
		}

		return cleavages;
	}
	//</editor-fold>

	//<editor-fold defaultstate="collapsed" desc=" Trivial Digest ">
	private HashMap<String, Set<Peptide>> digestTrivial(Collection<Collection<Protease>> proteases, int mcLimit) {
		StringBuilder buf = new StringBuilder();
		for (Collection<Protease> level : proteases) {
			for (Protease protease : level) {
				buf.append("(");
				if (protease.getLengthLeft() == 0) {
					buf.append(".");
				}
				buf.append(protease.getRuleWithoutSeparator());
				if (protease.getLengthRight() == 0) {
					buf.append(".");
				}
				buf.append(")|");
			}
		}
		buf.deleteCharAt(buf.length() - 1);
		Pattern proteasesPattern = Pattern.compile(buf.toString());

		boolean lockLeft = false;
		boolean lockRight = false;
		BigDecimal minLeft = BigDecimal.ZERO; // 0 a ne +-nekonečno kvůli okrajům
		BigDecimal minRight = BigDecimal.ZERO;
		BigDecimal maxLeft = BigDecimal.ZERO;
		BigDecimal maxRight = BigDecimal.ZERO;
		for (Collection<Protease> level : proteases) {
			for (Protease protease : level) {
				if (protease.isLockedRight()) {
					lockLeft = true;
				}
				if (protease.isLockedLeft()) {
					lockRight = true;
				}
				minLeft = minLeft.min(protease.getModificationRight());
				maxLeft = maxLeft.max(protease.getModificationRight());
				minRight = minRight.min(protease.getModificationLeft());
				maxRight = maxRight.max(protease.getModificationLeft());
			}
		}

		Collection<Collection<Protease>> pros;
		if (LinX.alternative == null) {
			pros = proteases;
		} else { // Nelze jen nespecificky kvůli modifikacím...
			pros = new ArrayList(proteases.size());
			for (Collection<Protease> pro : proteases) {
				ArrayList<Protease> tmp = new ArrayList(pro);
				tmp.add(new Protease("Nonspecific", Protease.SEPARATOR));
				pros.add(tmp);
			}
		}
		HashMap<String, Set<Peptide>> potential = new HashMap<>(proteins.size());
		for (Protein protein : proteins.values()) {
			// Speciální případ
			if (protein.length() < 1) {
				continue;
			}

			potential.put(protein.getName(), new HashSet<>());
			// Nalezení míst štěpení
			TreeMap<Integer, Set<SimplifiedCleavage>> hitsA = missedCleavagesTrivial(pros, protein);
			TreeMap<Integer, Set<SimplifiedCleavage>> hitsM = missedCleavagesTrivial(proteases, protein);
			// Vložení krajních zarážek, které nemohou být missed-cleavage
			hitsA.put(0, new HashSet<>(1));
			hitsA.get(0).add(new SimplifiedCleavage(0, new HashMap<>(0), new HashMap<>(0), 0, new Protease("", ";")));
			hitsA.put(protein.length(), new HashSet<>(1));
			hitsA.get(protein.length()).add(new SimplifiedCleavage(protein.length(), new HashMap<>(0), new HashMap<>(0), 0, new Protease("", ";")));
			// Do pole pro umožnění přímého přístupu
			Integer[] sorted = new Integer[hitsA.size()];
			hitsA.keySet().toArray(sorted);

			for (int i = 0; i < sorted.length - 1; i++) {
				int fixedCleavages = 0;
				int j = i;
				int mc = 0;
				BigDecimal minMass = protein.minMassN(lockLeft).add(minLeft.add(minRight.add(protein.minMassC(lockRight))));
				BigDecimal maxMass = protein.maxMassN(lockLeft).add(maxLeft.add(maxRight.add(protein.maxMassC(lockRight))));
				while (fixedCleavages <= mcLimit && ++j < sorted.length && sorted[j] - sorted[i] <= maxLength) {
					for (int k = sorted[j - 1]; k < sorted[j]; k++) {
						minMass = minMass.add(protein.minMassAt(k));
						maxMass = maxMass.add(protein.maxMassAt(k));
					}
					if (ChemicalElement.mass("+").add(minMass).compareTo(this.maxMass) > 0) {
						break;
					}
					if (minLength <= sorted[j] - sorted[i]) { // minMass nemá smysl kontrolovat, protože můstky to mohou změnit
						for (SimplifiedCleavage begin : hitsA.get(sorted[i])) {
							for (SimplifiedCleavage end : hitsA.get(sorted[j])) {
								Map<Integer, Character> fixed = new HashMap<>(begin.getRightMutations());
								fixed.putAll(end.getLeftMutations());
								if (mc <= mcLimit) {
									if (sorted[j] - sorted[i] != 1 || begin.getRightMutations().isEmpty() || end.getLeftMutations().isEmpty()
											  || Objects.equals(begin.getRightMutations().get(sorted[i]), end.getLeftMutations().get(sorted[i]))) {
										potential.get(protein.getName()).add(new Peptide(protein, begin, end, fixed, minMass, maxMass));
									}
								} else {
									// Zda není dostatek missed cleavage pro jednotlivé konfigurace
									// NON-OPT: Není nutné hledat znovu, stačí doplněk ke konfiguracím, které připouští vše
									if (protein.reset(begin, end, fixed)) {
										do {
											Matcher m = proteasesPattern.matcher(protein);
											int cc = 0;
											int index = sorted[i];
											while (m.find(index) && m.end() <= sorted[j]) {
												cc++;
												index = m.start() + 1;
											}
											if (cc <= mcLimit) {
												potential.get(protein.getName()).add(new Peptide(protein, begin, end, protein.getConfigurationMutations(), minMass, maxMass));
											}
										} while (protein.nextConfigurationMutations());
									}
								}
							}
						}
					}
					// Test fixnosti cleavage.
					if (hitsM.containsKey(sorted[j])) {
						mc++;
						for (SimplifiedCleavage cleavage : hitsM.get(sorted[j])) {
							if (cleavage.getLeftMutations().isEmpty() && cleavage.getRightMutations().isEmpty()) {
								fixedCleavages++;
								break;
							}
						}
					}
				}
			}
		}

		return potential;
	}

	private static TreeMap<Integer, Set<SimplifiedCleavage>> missedCleavagesTrivial(Collection<Collection<Protease>> proteases, Protein protein) {
		HashSet<SimplifiedCleavage> cleavages = new HashSet<>();
		// Nalezení míst štěpení
		int level = 0;
		for (Collection<Protease> phase : proteases) {
			level++;
			for (Protease rule : phase) {
				if (rule.getLeftClass() == Protease.ProteaseClass.EMPTY) {
					Pattern pattern = Pattern.compile(rule.getRuleRight());
					for (int i = 1; i < protein.length(); i++) {
						if (protein.resetMutations(new SimplifiedCleavage(i, new HashMap<>(0), new HashMap<>(0), level, rule), i + 1)) {
							do {
								// NON-OPT: Možná není nutné tvořit Matcher znovu a znovu.
								Matcher matcher = pattern.matcher(protein);
								// NON-OPT: To hledání může zbytečně prohledávat celý protein... očekávaná délka cca 20x delší, než je nutné; Další možnost znovu tvořit
								if (matcher.find(i) && matcher.start() == i) {
									cleavages.add(new SimplifiedCleavage(i, new HashMap<>(0), protein.getConfigurationMutations(), level, rule));
								}
							} while (protein.nextConfigurationMutations());
						}
					}
				} else if (rule.getRightClass() == Protease.ProteaseClass.EMPTY) {
					Pattern pattern = Pattern.compile(rule.getRuleLeft());
					for (int i = 0; i < protein.length() - 1; i++) {
						if (protein.resetMutations(i, new SimplifiedCleavage(i + 1, new HashMap<>(0), new HashMap<>(0), level, rule))) {
							do {
								// NON-OPT: Možná není nutné tvořit Matcher znovu a znovu.
								Matcher matcher = pattern.matcher(protein);
								// NON-OPT: To hledání může zbytečně prohledávat celý protein... očekávaná délka cca 20x delší, než je nutné; Další možnost znovu tvořit
								if (matcher.find(i) && matcher.start() == i) {
									cleavages.add(new SimplifiedCleavage(i + 1, protein.getConfigurationMutations(), new HashMap<>(0), level, rule));
								}
							} while (protein.nextConfigurationMutations());
						}
					}
				} else {
					Pattern patternLeft = Pattern.compile(rule.getRuleLeft());
					Pattern patternRight = Pattern.compile(rule.getRuleRight());
					for (int i = 1; i < protein.length(); i++) {
						List<Map<Integer, Character>> configurationsLeft = new ArrayList<>();
						List<Map<Integer, Character>> configurationsRight = new ArrayList<>();
						// Lepší hledat left a rigt samostatně - očekávaná délka 2x20, než dvojici - očekávaná délka 20^2.
						if (protein.resetMutations(i - 1, new SimplifiedCleavage(i, new HashMap<>(0), new HashMap<>(0), level, rule))) {
							do {
								// NON-OPT: Možná není nutné tvořit Matcher znovu a znovu.
								Matcher matcherLeft = patternLeft.matcher(protein);
								// NON-OPT: To hledání může zbytečně prohledávat celý protein... očekávaná délka cca 20x delší, než je nutné; Další možnost znovu tvořit
								if (matcherLeft.find(i - 1) && matcherLeft.start() == i - 1) {
									configurationsLeft.add(protein.getConfigurationMutations());
								}
							} while (protein.nextConfigurationMutations());
						}
						if (protein.resetMutations(new SimplifiedCleavage(i, new HashMap<>(0), new HashMap<>(0), level, rule), i + 1)) {
							do {
								// NON-OPT: Možná není nutné tvořit Matcher znovu a znovu.
								Matcher matcherRight = patternRight.matcher(protein);
								// NON-OPT: To hledání může zbytečně prohledávat celý protein... očekávaná délka cca 20x delší, než je nutné; Další možnost znovu tvořit
								if (matcherRight.find(i) && matcherRight.start() == i) {
									configurationsRight.add(protein.getConfigurationMutations());
								}
							} while (protein.nextConfigurationMutations());
						}
						for (Map<Integer, Character> configurationLeft : configurationsLeft) {
							for (Map<Integer, Character> configurationRight : configurationsRight) {
								cleavages.add(new SimplifiedCleavage(i, configurationLeft, configurationRight, level, rule));
							}
						}
					}
				}
			}
		}

		TreeMap<Integer, Set<SimplifiedCleavage>> ret = new TreeMap<>();
		for (SimplifiedCleavage cleavage : cleavages) {
			if (!ret.containsKey(cleavage.getPosition())) {
				ret.put(cleavage.getPosition(), new HashSet<>());
			}
			ret.get(cleavage.getPosition()).add(cleavage);
		}
		return ret;
	}
	//</editor-fold>

	private void checkPeptides(Map<String, Set<Peptide>> potential) throws InterruptedException {
		Runtime runtime = Runtime.getRuntime();
		long start = System.currentTimeMillis();
		long time = System.currentTimeMillis();
		long[] c = new long[13];
		TreeMap<BigDecimal, HashSet<IMeasurement>> unused = new TreeMap();
		for (Map.Entry<BigDecimal, HashSet<IMeasurement>> entry : masses.entrySet()) {
			unused.put(entry.getKey(), new HashSet(entry.getValue()));
		}
		HashMap<FileName, Integer> rows = null;
		boolean load = filter;

		try {
			System.out.println("Start:  0 " + (System.currentTimeMillis() - time) + " " + runtime.totalMemory());

			try {
				int count = 0;
				for (Set<Peptide> set : potential.values()) {
					count += set.size();
				}
				computingCard.next(count);
				computingCard.append("Testing single peptides: " + count + " candidates (peptides)...");
				computingCard.append("  - yet found 0 hits;");
				c = new long[13];
				for (Set<Peptide> set : potential.values()) {
					c[0]++;
					for (Peptide peptide : set) {
						c[1]++;
						if (minMass.compareTo(ChemicalElement.mass("+").add(peptide.maxMass())) <= 0 && ChemicalElement.mass("+").add(peptide.minMass()).compareTo(maxMass) <= 0 && peptide.reset()) {
							c[2]++;
							do {
								computingCard.checkStatus();
								c[3]++;
								// Navíc H+ z měření
								BigDecimal mass = ChemicalElement.mass("+").add(peptide.getMass());
								if (minMass.compareTo(mass) <= 0 && mass.compareTo(maxMass) <= 0) {
									c[4]++;
									NavigableMap<BigDecimal, HashSet<IMeasurement>> subMap = masses.subMap(tolerance.lower(mass), true, tolerance.upper(mass), true);
									if (subMap.isEmpty()) {
										continue;
									}
									String pos = peptide.proteinName() + " (" + (peptide.getBegin()) + ", " + peptide.getEnd() + ")";
									String config = peptide.toStringConfiguration();
									String mods = peptide.toStringModifications();
									String bons = peptide.toStringBonds();
//                  drawResults.get(peptide.getProtein()).add(new biochemie.vyza.Peptide(peptide.getBegin(), peptide.getEnd(), new String[] { mods, bonds, "", config }));
									for (BigDecimal w : subMap.keySet()) {
										c[5]++;
										Double err = tolerance.error(w, mass);
										for (IMeasurement m : masses.get(w)) {
											boolean skip = false;
											for (BigDecimal cd : peptide.getCheckDiff()) {
												if (check.get(m.getFileName()).subSet(tolerance.lower(mass.add(cd)), true, tolerance.upper(mass.add(cd)), true).isEmpty()) {
													skip = true;
													break;
												}
											}
											if (skip) {
												continue;
											}
											if (LinX.alternative != null) {
												if (((MeasurementAlternatives) m).contains(mass.doubleValue(), err, pos, config, mods, bons)) {
													continue;
												} else {
													for (String rest : ((MeasurementAlternatives) m).getRests()) {
														c[6]++;
														resultsPanels.get(m.getFileName()).addRow(w.doubleValue(), mass.doubleValue(), err, pos, config, mods, bons,
																  m.getIntensity() < 0 ? null : (long) m.getIntensity(),
																  m.getRetentionTime(), rest.replaceAll("\\t", "    "));
													}
												}
											} else if (filter) {
												c[6]++;
												resultsPanels.get(m.getFileName()).addRow(w.doubleValue(), mass.doubleValue(), err, pos, config, mods, bons,
														  m.getIntensity() < 0 ? null : (long) m.getIntensity(),
														  m.getRetentionTime(), m.getRest().replaceAll("\\t", "    "));
											} else {
												c[6]++;
												resultsPanels.get(m.getFileName()).addRow(null, mass.doubleValue(), null, pos, config, mods, bons, null, null, null);
											}
											unused.get(w).remove(m);
											if (System.currentTimeMillis() - time > 100) {
												System.out.println(c[6] + " " + (System.currentTimeMillis() - time) + " "
														  + runtime.maxMemory() + " " + runtime.totalMemory() + " " + runtime.freeMemory() + " "
														  + c[0] + " " + c[1] + " " + c[2] + " " + c[3] + " " + c[4] + " " + c[5] + " " + c[6]);
												computingCard.replace("  - yet found " + c[6] + " hits;");
												time = System.currentTimeMillis();
												if (runtime.totalMemory() == runtime.maxMemory() && runtime.freeMemory() * 2 < runtime.totalMemory()) {
													throw new OutOfMemoryError("");
												}
											}
										}
									}
								}
							} while (peptide.nextConfiguration());
						}
						computingCard.add(1);
					}
				}

				computingCard.replace("  found " + c[6] + " single matching peptides.");
				System.out.println("Fáze A: " + rows() + " " + (System.currentTimeMillis() - time) + " " + runtime.totalMemory() + " " + c[0] + " " + c[1] + " " + c[2] + " " + c[3] + " " + c[4] + " " + c[5] + " " + c[6]);
				time = System.currentTimeMillis();

				// Naklonování peptidů, ať se nebijí konfigurace
				Map<String, Set<Peptide>> clone = new HashMap<>(potential.size());
				for (String name : potential.keySet()) {
					Protein cloned = proteins.get(name).clone();
					clone.put(name, new HashSet<>(potential.get(name).size()));
					for (Peptide peptide : potential.get(name)) {
						clone.get(name).add(peptide.clone(cloned));
					}
				}

				System.out.println(c[6] + " " + (System.currentTimeMillis() - time) + " " + runtime.totalMemory());
				c = new long[13];
				time = System.currentTimeMillis();

				HashSet<BondReal> orriented = new HashSet(bonds.size());
				for (BondReal bond : bonds) {
					orriented.add(bond);
					if (!((Objects.equals(bond.getPositionLeft(), bond.getPositionRight()) || (bond.getPositionLeft() != null && bond.getPositionLeft().equals(bond.getPositionRight())))
							  && bond.getAaLeft() == bond.getAaRight() && bond.getProteinLeft().equals(bond.getProteinRight()))) {
						orriented.add(bond.reverse());
					}
				}

				count = 0;
				for (BondReal bond : orriented) {
					// Kvůli vyřazení symetrií
					if (bond.getProteinLeft().compareTo(bond.getProteinRight()) > 0) {
						continue;
					}
					TreeSet<Integer> positions1;
					TreeSet<Integer> positions2;
					if (bond.getPositionLeft() == null) {
						positions1 = proteins.get(bond.getProteinLeft()).positions(bond.getAaLeft());
					} else {
						positions1 = new TreeSet<>();
						positions1.add(bond.getPositionLeft());
					}
					if (bond.getPositionRight() == null) {
						positions2 = proteins.get(bond.getProteinRight()).positions(bond.getAaRight());
					} else {
						positions2 = new TreeSet<>();
						positions2.add(bond.getPositionRight());
					}
					count += positions1.size() * positions2.size();
				}
				computingCard.next(count);
				computingCard.append("Testing cross-links between different peptides: " + count + " candidates (bonds)...");
				computingCard.append("  - yet found 0 hits.");

				// Pro každý můstek
				for (BondReal bond : orriented) {
					// Kvůli vyřazení symetrií
					if (bond.getProteinLeft().compareTo(bond.getProteinRight()) > 0) {
						continue;
					}
					c[0]++;
					TreeSet<Integer> positions1;
					TreeSet<Integer> positions2;
					if (bond.getPositionLeft() == null) {
						positions1 = proteins.get(bond.getProteinLeft()).positions(bond.getAaLeft());
					} else {
						positions1 = new TreeSet<>();
						positions1.add(bond.getPositionLeft());
					}
					if (bond.getPositionRight() == null) {
						positions2 = proteins.get(bond.getProteinRight()).positions(bond.getAaRight());
					} else {
						positions2 = new TreeSet<>();
						positions2.add(bond.getPositionRight());
					}
					// Navíc H+ z měření
					BigDecimal diff = ChemicalElement.mass("+").add(bond.getBond());
					// Pro každé jeho umístění
					for (Integer pos1 : positions1) {
						c[1]++;
						for (Integer pos2 : bond.getProteinLeft().equals(bond.getProteinRight()) ? positions2.tailSet(pos1, bond.getAaLeft() <= bond.getAaRight()) : positions2) {
							c[2]++;
							// Pro každou dvojici peptidů, která ho může obsahovat
							for (Peptide peptide1 : potential.get(bond.getProteinLeft())) {
								if (peptide1.reset(pos1, bond.getAaLeft())) {
									c[3]++;
									for (Peptide peptide2 : clone.get(bond.getProteinRight())) {
										if (pos1.equals(pos2) && bond.getAaLeft() == bond.getAaRight() && bond.getProteinLeft().equals(bond.getProteinRight())
												  && (peptide1.getBegin() > peptide2.getBegin() || (peptide1.getBegin() == peptide2.getBegin() && peptide1.getEnd() > peptide2.getEnd()))) {
											continue;
										}
										if (bond.withinOneMolecule() && peptide1.getBegin() <= peptide2.getEnd() && peptide1.getEnd() >= peptide2.getBegin()) {
											continue;
										}
										c[4]++;
										if (peptide2.reset(pos2, bond.getAaRight())) {
											c[5]++;
											if (minMass.compareTo(diff.add(peptide1.maxMass()).add(peptide2.maxMass())) <= 0 && diff.add(peptide1.minMass()).add(peptide2.minMass()).compareTo(maxMass) <= 0 && peptide1.reset()) {
												c[6]++;
												do {
													c[7]++;
													BigDecimal mass1 = diff.add(peptide1.getMass());
													if (minMass.compareTo(mass1.add(peptide2.maxMass())) <= 0 && mass1.add(peptide2.minMass()).compareTo(maxMass) <= 0 && peptide2.reset()) {
														c[8]++;
														do {
															computingCard.checkStatus();
															c[9]++;
															BigDecimal mass = mass1.add(peptide2.getMass());
															if (minMass.compareTo(mass) <= 0 && mass.compareTo(maxMass) <= 0) {
																NavigableMap<BigDecimal, HashSet<IMeasurement>> subMap = masses.subMap(tolerance.lower(mass), true, tolerance.upper(mass), true);
																if (subMap.isEmpty()) {
																	continue;
																}
																// Pořadí testů podle toho, aby se to i v případě jednoho můstku co nejrychleji zamítlo a netestovaly se dlouhé stejné
																if (pos1.equals(pos2) && peptide1.getBegin() == peptide2.getBegin() && peptide1.getEnd() == peptide2.getEnd()
																		  && bond.getAaLeft() == bond.getAaRight() && bond.getProteinLeft().equals(bond.getProteinRight())
																		  && (peptide1.toStringConfiguration().compareTo(peptide2.toStringConfiguration()) > 0
																		  || (peptide1.toStringConfiguration().equals(peptide2.toStringConfiguration())
																		  && (peptide1.toStringModifications().compareTo(peptide2.toStringModifications()) > 0
																		  || (peptide1.toStringModifications().equals(peptide2.toStringModifications())
																		  && peptide1.toStringBonds().compareTo(peptide2.toStringBonds()) > 0))))) {
																	continue;
																}
																c[10]++;
																String config1 = peptide1.toStringConfiguration();
																String config2 = peptide2.toStringConfiguration();
//                              drawResults.get(proteins.get(peptide1.proteinName())).add(new biochemie.vyza.Peptide(peptide1.getBegin(), peptide1.getEnd(),
//                                              new String[] { peptide1.toStringModifications(), peptide1.toStringBonds(), bond.getName() + " (" + (pos1+1) + "; -)", config1 }));
//                              drawResults.get(proteins.get(peptide2.proteinName())).add(new biochemie.vyza.Peptide(peptide2.getBegin(), peptide2.getEnd(),
//                                              new String[] { peptide2.toStringModifications(), peptide2.toStringBonds(), bond.getName() + " (" + (pos2+1) + "; -)", config2 }));
																String pos = peptide1.proteinName() + " (" + peptide1.getBegin() + ", " + peptide1.getEnd() + ") [A] - "
																		  + peptide2.proteinName() + " (" + peptide2.getBegin() + ", " + peptide2.getEnd() + ") [B]";
																String config = config1 + " - " + config2;
																String mods1 = peptide1.toStringModifications("A.");
																String mods2 = peptide2.toStringModifications("B.");
																String mods = mods1 + (mods1.isEmpty() || mods2.isEmpty() ? "" : "; ") + mods2;
																String bonds1 = peptide1.toStringBonds("A.");
																String bonds2 = peptide2.toStringBonds("B.");
																String bods = bonds1 + (bonds1.isEmpty() ? "" : "; ") + bond.getName()
																		  + " (A." + (bond.getAaLeft() == '^' ? LEFT : bond.getAaLeft() == '$' ? RIGHT : pos1 + peptide1.start())
																		  + "; B." + (bond.getAaRight() == '^' ? LEFT : bond.getAaRight() == '$' ? RIGHT : pos2 + peptide2.start()) + ")"
																		  + (bonds2.isEmpty() ? "" : "; ") + bonds2;
																for (BigDecimal w : subMap.keySet()) {
																	c[11]++;
																	Double err = tolerance.error(w, mass);
																	for (IMeasurement m : masses.get(w)) {
																		boolean skip = false;
																		for (BigDecimal cd1 : peptide1.getCheckDiff()) {
																			for (BigDecimal cd2 : peptide2.getCheckDiff()) {
																				BigDecimal cd = cd1.add(cd2);
																				if (check.get(m.getFileName()).subSet(tolerance.lower(mass.add(cd)), true, tolerance.upper(mass.add(cd)), true).isEmpty()) {
																					skip = true;
																					break;
																				}
																				cd = cd.add(bond.getCheckDiff());
																				if (check.get(m.getFileName()).subSet(tolerance.lower(mass.add(cd)), true, tolerance.upper(mass.add(cd)), true).isEmpty()) {
																					skip = true;
																					break;
																				}
																			}
																		}
																		if (skip) {
																			continue;
																		}
																		if (LinX.alternative != null) {
																			if (((MeasurementAlternatives) m).contains(mass.doubleValue(), err, pos, config, mods, bods)) {
																				continue;
																			} else {
																				for (String rest : ((MeasurementAlternatives) m).getRests()) {
																					c[12]++;
																					resultsPanels.get(m.getFileName()).addRow(w.doubleValue(), mass.doubleValue(), err, pos, config, mods, bods,
																							  m.getIntensity() < 0 ? null : (long) m.getIntensity(),
																							  m.getRetentionTime(), rest.replaceAll("\\t", "    "));
																				}
																			}
																		} else if (filter) {
																			c[12]++;
																			resultsPanels.get(m.getFileName()).addRow(w.doubleValue(), mass.doubleValue(), err, pos, config, mods, bods,
																					  m.getIntensity() < 0 ? null : (long) m.getIntensity(),
																					  m.getRetentionTime(), m.getRest().replaceAll("\\t", "    "));
																		} else {
																			c[12]++;
																			resultsPanels.get(m.getFileName()).addRow(null, mass.doubleValue(), null, pos, config, mods, bods, null, null, null);
																		}
																		unused.get(w).remove(m);
																		if (System.currentTimeMillis() - time > 100) {
																			System.out.println(c[12] + " " + (System.currentTimeMillis() - time) + " "
																					  + runtime.maxMemory() + " " + runtime.totalMemory() + " " + runtime.freeMemory() + " "
																					  + c[0] + " " + c[1] + " " + c[2] + " " + c[3] + " " + c[4] + " " + c[5] + " " + c[6] + " "
																					  + c[7] + " " + c[8] + " " + c[9] + " " + c[10] + " " + c[11] + " " + c[12]);
																			computingCard.replace("  - yet found " + c[12] + " hits.");
																			time = System.currentTimeMillis();
																			if (runtime.totalMemory() == runtime.maxMemory() && runtime.freeMemory() * 2 < runtime.totalMemory()) {
																				throw new OutOfMemoryError("");
																			}
																		}
																	}
																}
															}
														} while (peptide2.nextConfiguration());
													}
												} while (peptide1.nextConfiguration());
											}
										}
									}
								}
							}
						}
						computingCard.add(positions2.size());
					}
				}
				computingCard.replace("  found " + c[12] + " cross-linked matching pairs of peptides.");
				System.out.println("Fáze B: " + c[12] + " " + (System.currentTimeMillis() - time) + " " + runtime.totalMemory() + " " + c[0] + " " + c[1] + " " + c[2] + " " + c[3] + " " + c[4] + " " + c[5] + " " + c[6] + " " + c[7] + " " + c[8] + " " + c[9] + " " + c[10] + " " + c[11] + " " + c[12]);
				time = System.currentTimeMillis();
				System.out.println("Celkem: " + rows() + " " + (System.currentTimeMillis() - time) + " " + runtime.totalMemory());
			} catch (final OutOfMemoryError e) {
				parent.disableRun();
				load = false;
				try {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							JOptionPane.showMessageDialog(ResultsCard.this, "Too many matching items,\nprogram does not have enough memory to work with them.\nPartional "
									  + "results will be processed.\n" + e.getMessage(), "Available memory is too small", JOptionPane.ERROR_MESSAGE);
						}
					});
				} catch (Exception ef) {
				}
				System.out.println("MEMORY: " + rows() + " " + (System.currentTimeMillis() - time) + " " + runtime.maxMemory() + " " + runtime.totalMemory() + " " + runtime.freeMemory() + " " + c[0] + " " + c[1] + " " + c[2] + " " + c[3] + " " + c[4] + " " + c[5] + " " + c[6] + " " + c[7] + " " + c[8] + " " + c[9] + " " + c[10] + " " + c[11] + " " + c[12]);
				computingCard.append("Don't have enough memory.");
			} catch (InterruptedException ie) {
				throw ie;
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}

			computingCard.next(3, 3 * resultsPanels.size());
			System.out.print("Odstraňování duplicit: ");
			computingCard.append("Removing duplications...");
			for (ResultsPanel resultsPanel : resultsPanels.values()) {
				resultsPanel.removeDuplications();
			}
			computingCard.append("  remained " + rows() + " hits.");
			System.out.println(rows() + " " + (System.currentTimeMillis() - time) + " " + runtime.totalMemory());
			time = System.currentTimeMillis();

			rows = new HashMap(resultsPanels.size());
			for (Map.Entry<FileName, ResultsPanel> entry : resultsPanels.entrySet()) {
				rows.put(entry.getKey(), entry.getValue().rows());
			}
			computingCard.next(4 * defaults.getProperty("Merge").split(",").length * resultsPanels.size());
			System.out.print("Slučování podobných výsledků: ");
			computingCard.append("Merging similar results...");
			boolean zero = false;
			for (String index : defaults.getProperty("Merge").split(",")) {
				try {
					if (index.equals("1")) {
						zero = true;
					}
					for (ResultsPanel resultsPanel : resultsPanels.values()) {
						resultsPanel.mergeDuplicationsAll(Integer.parseInt(index) - 1);
					}
				} catch (InterruptedException | NumberFormatException e) {
				}
			}
			computingCard.append("  remained " + rows() + " entries.");
			System.out.println(rows() + " " + (System.currentTimeMillis() - time) + " " + runtime.totalMemory());
			time = System.currentTimeMillis();

			c = null;
			insertUnassigned(load, rows, unused, zero, time);
			time = System.currentTimeMillis();

			computingCard.next();
			System.out.print("Setřídění výsledků: ");
			computingCard.append("Sorting results.");
			System.out.println(rows() + " " + (System.currentTimeMillis() - time) + " " + runtime.totalMemory());
			time = System.currentTimeMillis();

			computingCard.checkStatus();
		} catch (InterruptedException ie) {
			throw ie;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Unexpected exception occured during computation.\nProcess was not successfully finished.\n"
					  + "================================\n" + e.getMessage(), "Unexpected exception", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} finally {
			try {
				if (c != null && computingCard.setStatus(ComputingCard.FINALIZE)) {
					if (rows == null) {
						rows = new HashMap(resultsPanels.size());
						for (Map.Entry<FileName, ResultsPanel> entry : resultsPanels.entrySet()) {
							rows.put(entry.getKey(), entry.getValue().rows());
						}
					}
					insertUnassigned(load, rows, unused, false, time);
				}
			} catch (InterruptedException ie) {
				throw ie;
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			} finally {
				finish(start);
			}
		}
	}

	private void finish(long start) throws InterruptedException {
		System.out.println("KONEC:  " + rows() + " " + (System.currentTimeMillis() - start) + " " + Runtime.getRuntime().totalMemory());
		for (ResultsPanel resultsPanel : resultsPanels.values()) {
			resultsPanel.finish();
		}
		computingCard.release();
	}

	private void insertUnassigned(boolean load, HashMap<FileName, Integer> rows, TreeMap<BigDecimal, HashSet<IMeasurement>> unused, boolean zero, long time) throws InterruptedException {
		if (load) {
			computingCard.next(3 + (zero ? 4 * resultsPanels.size() : 0));
			System.out.print("Vkládání nespárovaných měření: ");
			computingCard.append("Insert unpaired records...");

			int count = rows();
			HashMap<FileName, Integer> ms = new HashMap(resultsPanels.size());
			HashMap<FileName, Integer> rf = new HashMap(resultsPanels.size());
			for (FileName key : resultsPanels.keySet()) {
				ms.put(key, 0);
				rf.put(key, 0);
			}
			for (HashSet<IMeasurement> list : masses.values()) {
				for (IMeasurement key : list) {
					ms.put(key.getFileName(), ms.get(key.getFileName()) + 1);
				}
			}
			computingCard.add(1);

			for (BigDecimal w : unused.keySet()) {
				for (IMeasurement m : unused.get(w)) {
					rf.put(m.getFileName(), rf.get(m.getFileName()) + 1);
					if (LinX.alternative == null) {
						resultsPanels.get(m.getFileName()).addRow(w.doubleValue(), null, null, null, null, null, null,
								  m.getIntensity() < 0 ? null : (long) m.getIntensity(), m.getRetentionTime(), m.getRest().replaceAll("\\t", "    "));
					} else {
						for (String rest : ((MeasurementAlternatives) m).getRests()) {
							resultsPanels.get(m.getFileName()).addRow(w.doubleValue(), null, null, null, null, null, null,
									  m.getIntensity() < 0 ? null : (long) m.getIntensity(), m.getRetentionTime(), rest.replaceAll("\\t", "    "));
						}
					}
				}
			}
			computingCard.add(1);

			if (zero) {
				if (count < rows()) {
					for (ResultsPanel resultsPanel : resultsPanels.values()) {
						resultsPanel.mergeDuplicationsAll(0);
					}
				} else {
					computingCard.add(4);
				}
			}

			int[] cs = new int[2];
			for (FileName key : resultsPanels.keySet()) {
				cs[0] += ms.get(key) - rf.get(key);
				cs[1] += ms.get(key);
				resultsPanels.get(key).setLabel(rows.get(key) + " hits, matched " + (ms.get(key) - rf.get(key)) + " of " + ms.get(key) + " entries.");
			}
			computingCard.add(1);

			System.out.println(rows() + " " + (System.currentTimeMillis() - time) + " " + Runtime.getRuntime().totalMemory());
			computingCard.append("  matched " + cs[0] + " of " + cs[1] + " entries.");
//			time = System.currentTimeMillis();
		} else {
			for (Map.Entry<FileName, ResultsPanel> entry : resultsPanels.entrySet()) {
				entry.getValue().setLabel(rows.get(entry.getKey()) + " peptides meet the conditions.");
			}
		}
	}

	private int rows() {
		int count = 0;
		for (ResultsPanel resultsPanel : resultsPanels.values()) {
			count += resultsPanel.rows();
		}
		return count;
	}

	private void mergeDuplicationsAll(int i) throws InterruptedException {
		String s = "" + (i + 1);
		if (!(defaults.containsKey("Merge") && (defaults.getProperty("Merge").equals(s) || defaults.getProperty("Merge").startsWith(s + ',')
				  || defaults.getProperty("Merge").endsWith(',' + s) || defaults.getProperty("Merge").contains(',' + s + ',')))) {
			defaults.setProperty("Merge", defaults.getProperty("Merge").isEmpty() ? s : defaults.getProperty("Merge") + ',' + s);
		}

		for (ResultsPanel resultsPanel : resultsPanels.values()) {
			resultsPanel.mergeDuplicationsAll(i);
		}
	}

	private void splitDuplicationsAll(int i) {
		String s = "" + (i + 1);
		if (defaults.getProperty("Merge").equals(s)) {
			defaults.setProperty("Merge", "");
		} else if (defaults.getProperty("Merge").endsWith(',' + s)) {
			defaults.setProperty("Merge", defaults.getProperty("Merge").substring(0, defaults.getProperty("Merge").length() - 2));
		} else if (defaults.getProperty("Merge").startsWith(s + ',')) {
			if (i == 0) {
				defaults.setProperty("Merge", defaults.getProperty("Merge").substring(2));
			} else {
				if (!(defaults.getProperty("Merge").endsWith(",1") || defaults.getProperty("Merge").contains(",1,"))) {
					defaults.setProperty("Merge", defaults.getProperty("Merge").substring(2));
				}
			}
		} else if (defaults.getProperty("Merge").contains(',' + s + ',')) {
			if (i == 0) {
				defaults.setProperty("Merge", defaults.getProperty("Merge").replace(',' + s + ',', ","));
			} else {
				String rest = ',' + defaults.getProperty("Merge").split(',' + s + ',')[1];
				if (!(rest.endsWith(",1") || rest.contains(",1,"))) {
					defaults.setProperty("Merge", defaults.getProperty("Merge").replace(',' + s + ',', ","));
				}
			}
		}

		for (ResultsPanel resultsPanel : resultsPanels.values()) {
			resultsPanel.splitDuplicationsAll(i);
		}
	}

	private void analyze(String action) {
		if (tabbedPane.getSelectedIndex() != -1) {
			((ResultsPanel) tabbedPane.getSelectedComponent()).analyzeResults(action, ResultsPanel.Save.SHOWN_ALL);
		}
	}

	MeasurementCard.Tolerance getTolerance() {
		return tolerance;
	}
}
