package biochemie.linx;

import biochemie.Defaults;
import biochemie.dbedit.SettingsPanel;
import java.awt.Cursor;
import java.awt.HeadlessException;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

/**
 *
 * @author Janek
 */
public class MeasurementCard extends Card {

	public static final String MSVS_FILE = "msvs";
	public static final String MEASUREMENTS_FILE = "measurements";
	public static final String SEPARATOR = "\t";
	private static final String TEXT = "Path to file with measurement";
	private static Properties PROPERTIES;
	private static final boolean QUIET;
	private static int TABS;
	private String PATH = ".";
	private static final String ALL = "All Files";
	private static final String AMF = "All measurement formats";
	private static final String MGF = "Mascot Generic File";
	private static final String MGFs = ".mgf";
	private static final String KUK = "Kuky's format";
	private static final String KUKs = ".txt";

	static {
		PROPERTIES = Defaults.getDefaults("Measurement");

		if (PROPERTIES.getProperty("Quiet") == null) {
			QUIET = true;
			PROPERTIES.setProperty("Quiet", "true");
		} else {
			QUIET = !PROPERTIES.getProperty("Quiet").toLowerCase().equals("false");
		}

		if (PROPERTIES.getProperty("Alternatives") == null) {
			TABS = 10;
			PROPERTIES.setProperty("Alternatives", "10");
		} else {
			TABS = Integer.parseInt(PROPERTIES.getProperty("Alternatives"));
		}

		Defaults.addDefaults("Measurement", PROPERTIES);
	}

	public static SettingsPanel getSettingsPanel() {
		SettingsPanel ret = new SettingsPanel("Measurement panel") {
			private JLabel recognizeLabel;
			private JComboBox recognizeComboBox;

			{
				recognizeLabel = new JLabel("Automatically assign recognized formats:");
				recognizeComboBox = new JComboBox(new String[]{"Yes", "No"});

				recognizeComboBox.setSelectedIndex(QUIET ? 0 : 1);
				recognizeComboBox.setName(recognizeComboBox.getSelectedItem().toString());

				GroupLayout measurementLayout = new GroupLayout(this);
				setLayout(measurementLayout);
				measurementLayout.setHorizontalGroup(
						  measurementLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
									 .addGroup(measurementLayout.createSequentialGroup()
												.addContainerGap()
												.addComponent(recognizeLabel)
												.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
												.addComponent(recognizeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
												.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				);
				measurementLayout.setVerticalGroup(
						  measurementLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
									 .addGroup(measurementLayout.createSequentialGroup()
												.addContainerGap()
												.addGroup(measurementLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
														  .addComponent(recognizeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
														  .addComponent(recognizeLabel))
												.addContainerGap()));
			}

			@Override
			public boolean isChanged() {
				return !recognizeComboBox.getName().equals(recognizeComboBox.getSelectedItem());
			}

			@Override
			public void save() {
				MeasurementCard.PROPERTIES.setProperty("Quiet", recognizeComboBox.getSelectedIndex() == 1 ? "false" : "true");
				Defaults.addDefaults("Measurement", MeasurementCard.PROPERTIES);
			}
		};
		return ret;
	}

	public static SettingsPanel getSettingsPanel2() {
		SettingsPanel ret = new SettingsPanel("Alternatives") {
			private JLabel tabsLabel;
			private JSpinner tabsSpinner;

			{
				tabsLabel = new JLabel("Maximal number of tabs:");
				tabsSpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(TABS), Integer.valueOf(1), null, Integer.valueOf(1)));

				((JSpinner.DefaultEditor) tabsSpinner.getEditor()).getTextField().setColumns(3);
				tabsSpinner.setName(MeasurementCard.PROPERTIES.getProperty("Alternatives"));

				GroupLayout alternativesLayout = new GroupLayout(this);
				setLayout(alternativesLayout);
				alternativesLayout.setHorizontalGroup(alternativesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						  .addGroup(alternativesLayout.createSequentialGroup()
									 .addContainerGap()
									 .addComponent(tabsLabel)
									 .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
									 .addComponent(tabsSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
									 .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				);
				alternativesLayout.setVerticalGroup(alternativesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						  .addGroup(alternativesLayout.createSequentialGroup()
									 .addContainerGap()
									 .addGroup(alternativesLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
												.addComponent(tabsSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
												.addComponent(tabsLabel))
									 .addContainerGap()));
			}

			@Override
			public boolean isChanged() {
				return !tabsSpinner.getName().equals(tabsSpinner.getValue().toString());
			}

			@Override
			public void save() {
				MeasurementCard.PROPERTIES.setProperty("Alternatives", tabsSpinner.getValue().toString());
				Defaults.addDefaults("Measurement", MeasurementCard.PROPERTIES);
			}
		};

		return ret;
	}

	private JFrame parent;
	private boolean filtered;
	private CheckBoxBorder filterBorder;
	private JLabel measurementLabel;
	private JTextField measurementTextField;
	private JButton findButton;
	private Properties formats;
	private Properties msvs;
	private String old;
	private JLabel precisionLabel;
	private JFormattedTextField precisionTextField;
	private JRadioButton ppmRadioButton;
	private JRadioButton daRadioButton;
	private JCheckBox lengthCheckBox;
	private JFormattedTextField minLengthFormattedTextField;
	private JFormattedTextField maxLengthFormattedTextField;
	private JCheckBox massCheckBox;
	private JFormattedTextField minMassFormattedTextField;
	private JFormattedTextField maxMassFormattedTextField;
	private LinkedHashSet<FileName> fileNames;
	private TreeMap<BigDecimal, HashSet<IMeasurement>> masses;

	public MeasurementCard(String id, JButton[] movement, JFrame parent) {
		super(id, movement);
		if (defaults.containsKey("Path")) {
			PATH = defaults.getProperty("Path");
		}
		this.parent = parent;
		JPanel mainPanel = new JPanel();
		JPanel measurementPanel = new JPanel();
		measurementLabel = new JLabel("Measurements:");
		measurementTextField = new JTextField(TEXT);
		old = TEXT;
		findButton = new JButton("Find");
		reloadFormats();
		reloadMsvs();
		precisionLabel = new JLabel();
		precisionTextField = new JFormattedTextField();
		ppmRadioButton = new JRadioButton();
		daRadioButton = new JRadioButton();
		lengthCheckBox = new JCheckBox("Peptide length limit:");
		NumberFormatter numberFormatter = new NumberFormatter(NumberFormat.getIntegerInstance(Defaults.LOCALE));
		numberFormatter.setMinimum(1);
		minLengthFormattedTextField = new JFormattedTextField(new DefaultFormatterFactory(numberFormatter), 1);
		JLabel minLengthLabel = new JLabel("(min),");
		maxLengthFormattedTextField = new JFormattedTextField(new DefaultFormatterFactory(numberFormatter), 1000000);
		JLabel maxLengthLabel = new JLabel("(max).");
		massCheckBox = new JCheckBox("Peptide mass limit:");
		NumberFormat format = (NumberFormat) Defaults.uMassShortFormat.clone();
		format.setMinimumFractionDigits(0);
		numberFormatter = new NumberFormatter(format);
		numberFormatter.setValueClass(BigDecimal.class);
		numberFormatter.setMinimum(BigDecimal.ZERO);
		minMassFormattedTextField = new JFormattedTextField(new DefaultFormatterFactory(numberFormatter), BigDecimal.ZERO);
		JLabel minMassLabel = new JLabel("(min),");
		maxMassFormattedTextField = new JFormattedTextField(new DefaultFormatterFactory(numberFormatter), new BigDecimal(1000000));
		JLabel maxMassLabel = new JLabel("(max).");
		masses = null;

		mainScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		mainScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		mainScrollPane.setViewportView(mainPanel);

		measurementTextField.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				measurementTextField.selectAll();
				old = measurementTextField.getText();
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (measurementTextField.getText() == null || measurementTextField.getText().isEmpty()) {
					measurementTextField.setText(TEXT);
				}
				if (!measurementTextField.getText().equals(old)) {
					masses = null;
				}
			}
		});

		findButton.setMnemonic('F');
		findButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser(PATH);
				fc.setMultiSelectionEnabled(true);
				TreeSet<String> all = new TreeSet();
				TreeMap<String, String[]> sorted = new TreeMap();
				sorted.put(MGF, new String[]{MGFs});
				sorted.put(KUK, new String[]{KUKs});
				all.add(MGFs);
				all.add(KUKs);
				for (String name : formats.stringPropertyNames()) {
					String value = formats.getProperty(name);
					String[] extensions = value.isEmpty() ? new String[0] : value.split(SEPARATOR)[0].split(", ");
					sorted.put(name, extensions);
					all.addAll(Arrays.asList(extensions));
				}
				for (String name : msvs.stringPropertyNames()) {
					String value = msvs.getProperty(name);
					String[] extensions = value.isEmpty() ? new String[0] : value.split(SEPARATOR)[0].split(", ");
					sorted.put(name, extensions);
					all.addAll(Arrays.asList(extensions));
				}
				all.remove("");
				fc.setFileFilter(filter(AMF, all.toArray(new String[all.size()])));
				for (Map.Entry<String, String[]> entry : sorted.entrySet()) {
					fc.addChoosableFileFilter(filter(entry.getKey(), entry.getValue()));
				}
				if (fc.showDialog(measurementTextField, "Select") == JFileChooser.APPROVE_OPTION) {
					Cursor c = getCursor();
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

					ArrayList<String> files = new ArrayList(fc.getSelectedFiles().length);
					for (File file : fc.getSelectedFiles()) {
						Path path;
						try {
							path = Paths.get(System.getProperty("user.dir")).relativize(file.toPath());
						} catch (Exception f) {
							path = file.toPath();
						}
						files.add(path.toString());

						path = path.getParent();
						if (path == null) {
							PATH = ".";
						} else {
							PATH = path.toString();
							if (PATH == null || PATH.isEmpty()) {
								PATH = ".";
							}
						}
					}

					TreeMap<BigDecimal, HashSet<IMeasurement>> tmpMasses = masses;
					LinkedHashSet<FileName> tmpFileNames = loadFile(files, fc.getFileFilter(), false);
					if (tmpFileNames == null) {
						masses = tmpMasses;
					} else {
						fileNames = tmpFileNames;
						old = fileNames.isEmpty() ? TEXT : MeasurementCard.this.toString(fileNames).substring(2);
						measurementTextField.setText(old);
						defaults.setProperty("Path", PATH);
					}
					setCursor(c);
				}
			}

			private FileFilter filter(final String description, final String[] extensions) {
				return new FileFilter() {
					@Override
					public boolean accept(File f) {
						if (f.isDirectory() || extensions.length == 0) {
							return true;
						}
						String ext = f.getName();
						int i = ext.lastIndexOf(".");
						if (i < 0) {
							ext = "";
						} else {
							ext = ext.substring(i);
						}
						if (ext != null) {
							for (String extension : extensions) {
								if (ext.equals(extension)) {
									return true;
								}
							}
						}
						return false;
					}

					@Override
					public String getDescription() {
						if (extensions.length == 0) {
							return description;
						}
						StringBuilder sb = new StringBuilder(description).append(" [").append(extensions[0]);
						for (int i = 1; i < extensions.length; i++) {
							sb.append(", ").append(extensions[i]);
						}
						return sb.append("]").toString();
					}
				};
			}
		});

		initPrecision(precisionLabel, precisionTextField, ppmRadioButton, daRadioButton, new java.util.concurrent.atomic.AtomicBoolean());

		if (LinX.alternative == null) {
			filterBorder = new CheckBoxBorder(measurementPanel, BorderFactory.createEtchedBorder(), "Filter peptides");
			filterBorder.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					filtered = e.getStateChange() == ItemEvent.SELECTED;
					measurementLabel.setEnabled(filtered);
					measurementTextField.setEnabled(filtered);
					findButton.setEnabled(filtered);
					precisionLabel.setEnabled(filtered);
					precisionTextField.setEnabled(filtered);
					ppmRadioButton.setEnabled(filtered);
					daRadioButton.setEnabled(filtered);
				}
			});
			measurementPanel.setBorder(filterBorder);
		} else {
			filterBorder = null;
			measurementPanel.setBorder(new TitledBorder("Filter peptides"));
		}
		filtered = true;

		GroupLayout measurementLayout = new GroupLayout(measurementPanel);
		measurementPanel.setLayout(measurementLayout);
		measurementLayout.setHorizontalGroup(measurementLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				  .addGroup(measurementLayout.createSequentialGroup()
							 .addContainerGap(GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							 .addGroup(measurementLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
										.addGroup(measurementLayout.createSequentialGroup()
												  .addComponent(measurementLabel)
												  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
												  .addComponent(measurementTextField, 200, 200, Short.MAX_VALUE)
												  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
												  .addComponent(findButton))
										.addGroup(measurementLayout.createSequentialGroup()
												  .addGroup(measurementLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
															 .addGroup(measurementLayout.createSequentialGroup()
																		.addComponent(precisionLabel)
																		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(precisionTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
																		.addComponent(ppmRadioButton)
																		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
																		.addComponent(daRadioButton)))))
							 .addContainerGap(GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		measurementLayout.setVerticalGroup(
				  measurementLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
							 .addGroup(measurementLayout.createSequentialGroup()
										.addGroup(measurementLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
												  .addComponent(measurementLabel)
												  .addComponent(measurementTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
												  .addComponent(findButton))
										.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(measurementLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
												  .addComponent(precisionLabel)
												  .addComponent(precisionTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
												  .addComponent(ppmRadioButton)
												  .addComponent(daRadioButton)))
		);

		lengthCheckBox.setMnemonic('L');
		lengthCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				minLengthFormattedTextField.requestFocus();
			}
		});

		minLengthFormattedTextField.setColumns(6);
		minLengthFormattedTextField.setHorizontalAlignment(JTextField.TRAILING);
		minLengthFormattedTextField.setMargin(new java.awt.Insets(1, 1, 1, 0));
		minLengthFormattedTextField.addPropertyChangeListener("value", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				lengthCheckBox.setSelected(true);
			}
		});

		maxLengthFormattedTextField.setColumns(7);
		maxLengthFormattedTextField.setHorizontalAlignment(JTextField.TRAILING);
		maxLengthFormattedTextField.setMargin(new java.awt.Insets(1, 1, 1, 0));
		maxLengthFormattedTextField.addPropertyChangeListener("value", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				lengthCheckBox.setSelected(true);
			}
		});

		massCheckBox.setMnemonic('M');
		massCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				minMassFormattedTextField.requestFocus();
			}
		});

		minMassFormattedTextField.setColumns(6);
		minMassFormattedTextField.setHorizontalAlignment(JTextField.TRAILING);
		minMassFormattedTextField.setMargin(new java.awt.Insets(1, 1, 1, 0));
		minMassFormattedTextField.addPropertyChangeListener("value", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				massCheckBox.setSelected(true);
			}
		});

		maxMassFormattedTextField.setColumns(7);
		maxMassFormattedTextField.setHorizontalAlignment(JTextField.TRAILING);
		maxMassFormattedTextField.setMargin(new java.awt.Insets(1, 1, 1, 0));
		maxMassFormattedTextField.addPropertyChangeListener("value", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				massCheckBox.setSelected(true);
			}
		});

		GroupLayout mainLayout = new GroupLayout(mainPanel);
		mainPanel.setLayout(mainLayout);
		mainLayout.setHorizontalGroup(mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				  .addGroup(mainLayout.createSequentialGroup()
							 .addContainerGap()
							 .addGroup(mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
										.addComponent(measurementPanel)
										.addGroup(mainLayout.createSequentialGroup()
												  .addGroup(mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
															 .addComponent(lengthCheckBox)
															 .addComponent(massCheckBox))
												  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
												  .addGroup(mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
															 .addComponent(minLengthFormattedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
															 .addComponent(minMassFormattedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
												  .addGroup(mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
															 .addComponent(minLengthLabel)
															 .addComponent(minMassLabel))
												  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
												  .addGroup(mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
															 .addComponent(maxLengthFormattedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
															 .addComponent(maxMassFormattedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
												  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
												  .addGroup(mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
															 .addComponent(maxMassLabel)
															 .addComponent(maxLengthLabel))))
							 .addContainerGap())
		);
		mainLayout.setVerticalGroup(
				  mainLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
							 .addGroup(mainLayout.createSequentialGroup()
										.addContainerGap()
										.addComponent(measurementPanel)
										.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(mainLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
												  .addComponent(lengthCheckBox)
												  .addComponent(minLengthFormattedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
												  .addComponent(minLengthLabel)
												  .addComponent(maxLengthFormattedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
												  .addComponent(maxLengthLabel))
										.addGroup(mainLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
												  .addComponent(massCheckBox)
												  .addComponent(minMassFormattedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
												  .addComponent(minMassLabel)
												  .addComponent(maxMassFormattedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
												  .addComponent(maxMassLabel))
										.addContainerGap(0, Short.MAX_VALUE))
		);

		createLayout(new JButton[]{});
	}

	/**
	 * Kvůli hledání alternativ nefiltrovaných souborem měření
	 *
	 * @param precisionLabel
	 * @param precisionTextField
	 * @param ppmRadioButton
	 * @param daRadioButton
	 * @param fix
	 */
	public static void initPrecision(JLabel precisionLabel, final JFormattedTextField precisionTextField,
			  JRadioButton ppmRadioButton, JRadioButton daRadioButton, final java.util.concurrent.atomic.AtomicBoolean fix) {
		precisionLabel.setText("Precision:");
		precisionLabel.setLabelFor(precisionTextField);
		precisionTextField.setColumns(9);
		precisionTextField.setHorizontalAlignment(JTextField.TRAILING);
		precisionTextField.setValue(BigDecimal.ONE);

		ppmRadioButton.setText("ppm");
		daRadioButton.setText("Da");
		ppmRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (fix.get()) {
					precisionTextField.setValue(((BigDecimal) precisionTextField.getValue()).multiply(new BigDecimal(100)));
					fix.set(false);
				}
				NumberFormatter nf = new NumberFormatter(Defaults.uPpmFormat);
				nf.setValueClass(BigDecimal.class);
				nf.setMinimum(BigDecimal.ZERO);
				precisionTextField.setFormatterFactory(new DefaultFormatterFactory(nf));
				if (precisionTextField.getText().isEmpty()) {
					precisionTextField.setValue(nf.getMinimum());
				}
			}
		});
		daRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!fix.get()) {
					precisionTextField.setValue(((BigDecimal) precisionTextField.getValue()).divide(new BigDecimal(100)));
					fix.set(true);
				}
				NumberFormatter nf = new NumberFormatter(Defaults.uDaFormat);
				nf.setValueClass(BigDecimal.class);
				nf.setMinimum(BigDecimal.ZERO);
				precisionTextField.setFormatterFactory(new DefaultFormatterFactory(nf));
				if (precisionTextField.getText().isEmpty()) {
					precisionTextField.setValue(nf.getMinimum());
				}
			}
		});
		ButtonGroup precisionButtonGroup = new ButtonGroup();
		precisionButtonGroup.add(ppmRadioButton);
		precisionButtonGroup.add(daRadioButton);
		ppmRadioButton.doClick();
	}

	@Override
	public void applyDefaults() {
		if (defaults.containsKey("Path")) {
			PATH = defaults.getProperty("Path");
		}
	}

	public ArrayList<String> logSettings() {
		ArrayList<String> ret = new ArrayList<>(3);
		if (isFiltered() || LinX.alternative != null) {
			ret.add("Tolerance is " + (ppmRadioButton.isSelected() ? Defaults.uPpmFormat.format(precisionTextField.getValue()) + "ppm"
					  : Defaults.uDaFormat.format(precisionTextField.getValue()) + "Da") + ".");
		}
		if (lengthCheckBox.isSelected()) {
			ret.add("Peptides must contain minimum of " + minLengthFormattedTextField.getText() + " and maximum " + maxLengthFormattedTextField.getText() + " aminoacids.");
		}
		if (massCheckBox.isSelected()) {
			ret.add("Peptides must weigh at least " + minMassFormattedTextField.getText() + "Da and no more than " + maxMassFormattedTextField.getText() + "Da.");
		}
		return ret;
	}

	public void reloadFormats() {
		formats = Defaults.loadDefaults(MEASUREMENTS_FILE);
		masses = null;
	}

	public void reloadMsvs() {
		msvs = Defaults.loadDefaults(MSVS_FILE);
		masses = null;
	}

	public ArrayList<String> getForbiddenFormats() {
		ArrayList<String> ret = new ArrayList<>(msvs.stringPropertyNames());
		ret.add(MGF);
		ret.add(KUK);
		ret.add(ALL);
		return ret;
	}

	public ArrayList<String> getForbiddenMsvs() {
		ArrayList<String> ret = new ArrayList<>(formats.stringPropertyNames());
		ret.add(MGF);
		ret.add(KUK);
		ret.add(ALL);
		return ret;
	}

	public LinkedHashSet<FileName> getFileNames() {
		return (fileNames == null || (filterBorder != null && !filterBorder.isSelected())) ? new LinkedHashSet() : fileNames;
	}

	private String[][] getFilename() {
		if (!isFiltered() || measurementTextField.getText().isEmpty()) {
			return new String[0][];
		}

		String[] parts = measurementTextField.getText().split("((?<!\\([^()]{0," + (Integer.MAX_VALUE - 1) + "})\\s*(" + File.pathSeparator + "\\s*)+)|"
				  + "(\\s*(" + File.pathSeparator + "\\s*)+(?![^()]*\\)))");
		String[][] ret = new String[parts.length][];

		String last = "";
		for (int i = parts.length - 1; i >= 0; i--) {
			if (parts[i].endsWith(")") && parts[i].contains("(")) {
				last = parts[i].substring(parts[i].lastIndexOf('('));
				break;
			}
		}
		for (int i = parts.length - 1; i >= 0; i--) {
			if (parts[i].endsWith(")") && parts[i].contains("(")) {
				last = parts[i].substring(parts[i].lastIndexOf('('));
				ret[i] = new String[]{parts[i].replaceFirst("\\s*\\([^(]*$", ""), last};
			} else {
				ret[i] = new String[]{parts[i], last};
			}
		}

		return ret;
	}

	public boolean isFiltered() {
		return filtered;
	}

	public TreeMap<BigDecimal, HashSet<IMeasurement>> getMasses() {
		return masses;
	}

	public HashMap<FileName, TreeSet<BigDecimal>> getCheckMasses() {
		if (masses == null) {
			return null;
		}
		if (LinX.alternative == null) {
			HashMap<FileName, TreeSet<BigDecimal>> ret = new HashMap();
			for (Map.Entry<BigDecimal, HashSet<IMeasurement>> entry : masses.entrySet()) {
				for (IMeasurement measurement : entry.getValue()) {
					if (!ret.containsKey(measurement.getFileName())) {
						ret.put(measurement.getFileName(), new TreeSet());
					}
					ret.get(measurement.getFileName()).add(entry.getKey());
				}
			}
			return ret;
		} else {
			TreeSet<BigDecimal> tmp = new TreeSet(masses.keySet());

			try (BufferedReader br = new BufferedReader(new FileReader(LinX.alternative))) {
				String line;
				while ((line = br.readLine()) != null) {
					try {
						tmp.add((BigDecimal) Defaults.uMassShortFormat.parse(line));
					} catch (ParseException e) {
					}
				}

			} catch (IOException e) {
				Logger.getLogger(MeasurementCard.class.getName()).log(Level.SEVERE, null, e);
			}
			HashMap<FileName, TreeSet<BigDecimal>> ret = new HashMap(1);
			for (HashSet<IMeasurement> ms : masses.values()) {
				for (IMeasurement m : ms) {
					ret.put(m.getFileName(), tmp);
				}
			}
			return ret;
		}
	}

	public Tolerance getTolerance() {
		if (daRadioButton.isSelected()) {
			return new AbsoluteTolerance((BigDecimal) precisionTextField.getValue());
		}
		if (ppmRadioButton.isSelected()) {
			return new RelativeTolerance(((BigDecimal) precisionTextField.getValue()).divide(new BigDecimal(1000000)));
		}
		return null;
	}

	public int minLength() {
		return lengthCheckBox.isSelected() ? (Integer) minLengthFormattedTextField.getValue() : 1;
	}

	public int maxLength() {
		return lengthCheckBox.isSelected() ? (Integer) maxLengthFormattedTextField.getValue() : 1000000;
	}

	public BigDecimal minMass() {
		BigDecimal min = massCheckBox.isSelected() ? (BigDecimal) minMassFormattedTextField.getValue() : BigDecimal.ZERO;
		if ((isFiltered() || LinX.alternative != null) && masses != null && !masses.isEmpty()) {
			if (ppmRadioButton.isSelected()) {
				min = min.max(masses.firstKey().divide(BigDecimal.ONE.add(((BigDecimal) precisionTextField.getValue()).divide(new BigDecimal(1000000))), 2 * Defaults.sPpmFormat.getMaximumFractionDigits() + 6, RoundingMode.FLOOR));
			} else if (daRadioButton.isSelected()) {
				min = min.max(masses.firstKey().subtract((BigDecimal) precisionTextField.getValue()));
			}
		}
		return min;
	}

	public BigDecimal maxMass() {
		BigDecimal max = massCheckBox.isSelected() ? (BigDecimal) maxMassFormattedTextField.getValue() : new BigDecimal(Double.MAX_VALUE);
		if ((isFiltered() || LinX.alternative != null) && masses != null && !masses.isEmpty()) {
			if (ppmRadioButton.isSelected()) {
				max = max.min(masses.lastKey().divide(BigDecimal.ONE.subtract(((BigDecimal) precisionTextField.getValue()).divide(new BigDecimal(1000000))), 2 * Defaults.sPpmFormat.getMaximumFractionDigits() + 6, RoundingMode.CEILING));
			} else if (daRadioButton.isSelected()) {
				max = max.min(masses.lastKey().add((BigDecimal) precisionTextField.getValue()));
			}
		}
		return max;
	}

	public void setFiles(ArrayList<String[]> filenames) {
		masses = null;
		if (filterBorder != null && (!(filenames == null || filenames.isEmpty()) ^ filterBorder.isSelected())) {
			filterBorder.doClick();
		}
		if (filenames == null || filenames.isEmpty()) {
			measurementTextField.setText(TEXT);
		} else {
			HashSet<String> formats = new HashSet(filenames.size());
			for (String[] name : filenames) {
				if (name.length > 1 && name[1] != null) {
					formats.add(name[1]);
				}
			}
			StringBuilder sb = new StringBuilder();
			if (formats.size() == 1) {
				for (String[] name : filenames) {
					sb.append(File.pathSeparator).append(" ").append(name[0]);
				}
				if (!formats.toArray(new String[1])[0].isEmpty()) {
					sb.append(' ').append(formats.toArray()[0]);
				}
			} else {
				for (String[] name : filenames) {
					sb.append(File.pathSeparator).append(" ").append(name[0]);
					if (!name[1].isEmpty()) {
						sb.append(" ").append(name[1]);
					}
				}
			}
			measurementTextField.setText(sb.length() > File.pathSeparator.length() + 1 ? sb.substring(File.pathSeparator.length() + 1) : TEXT);
		}
	}

	public void setConstraints(ArrayList<String> settings) {
		if (lengthCheckBox.isSelected()) {
			lengthCheckBox.doClick();
		}
		if (massCheckBox.isSelected()) {
			massCheckBox.doClick();
		}
		for (String string : settings) {
			if (string.contains("Tolerance is ")) {
				if (string.matches(".*[0-9],?ppm.$")) {
					ppmRadioButton.doClick();
				} else if (string.matches(".*[0-9],?Da.$")) {
					daRadioButton.doClick();
				}
				precisionTextField.setText(string.replaceFirst("^.*Tolerance is ", "").replaceFirst("((ppm)|(Da)).*$", ""));
				try {
					precisionTextField.commitEdit();
					precisionTextField.setValue(precisionTextField.getValue());
				} catch (ParseException ex) {
				}
			} else {
				Matcher m;
				if ((m = Pattern.compile("Peptides must contain minimum of (.+) and maximum (.+) aminoacids.").matcher(string)).find()) {
					lengthCheckBox.setSelected(true);
					minLengthFormattedTextField.setText(m.group(1));
					try {
						minLengthFormattedTextField.commitEdit();
						minLengthFormattedTextField.setValue(minLengthFormattedTextField.getValue());
					} catch (ParseException e) {
					}
					maxLengthFormattedTextField.setText(m.group(2));
					try {
						maxLengthFormattedTextField.commitEdit();
						maxLengthFormattedTextField.setValue(maxLengthFormattedTextField.getValue());
					} catch (ParseException e) {
					}
				} else if ((m = Pattern.compile("Peptides must weigh at least (.+)Da and no more than (.+)Da.").matcher(string)).find()) {
					massCheckBox.setSelected(true);
					minMassFormattedTextField.setText(m.group(1));
					try {
						minMassFormattedTextField.commitEdit();
						minMassFormattedTextField.setValue(minMassFormattedTextField.getValue());
					} catch (ParseException e) {
					}
					maxMassFormattedTextField.setText(m.group(2));
					try {
						maxMassFormattedTextField.commitEdit();
						maxMassFormattedTextField.setValue(maxMassFormattedTextField.getValue());
					} catch (ParseException e) {
					}
				}
			}
		}
	}

	public boolean check() {
		try {
			precisionTextField.commitEdit();
			precisionTextField.setValue(precisionTextField.getValue());
			minLengthFormattedTextField.commitEdit();
			minLengthFormattedTextField.setValue(minLengthFormattedTextField.getValue());
			maxLengthFormattedTextField.commitEdit();
			maxLengthFormattedTextField.setValue(maxLengthFormattedTextField.getValue());
			if (lengthCheckBox.isSelected() && minLength() > maxLength()) {
				JOptionPane.showMessageDialog(this, "Minimal length cannot be higher than maximal length.", "Incorrect interval", JOptionPane.WARNING_MESSAGE);
				return false;
			}
			minMassFormattedTextField.commitEdit();
			minMassFormattedTextField.setValue(minMassFormattedTextField.getValue());
			maxMassFormattedTextField.commitEdit();
			maxMassFormattedTextField.setValue(maxMassFormattedTextField.getValue());
			if (massCheckBox.isSelected() && minMass().compareTo(maxMass()) > 0) {
				JOptionPane.showMessageDialog(this, "Minimal mass cannot be higher than maximal mass.", "Incorrect interval", JOptionPane.WARNING_MESSAGE);
				return false;
			}
		} catch (HeadlessException | ParseException ex) {
		}

		if ((isFiltered() || LinX.alternative != null) && (masses == null || masses.isEmpty() || !measurementTextField.getText().equals(old))) {
			Cursor c = getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			masses = null;

			if (LinX.alternative == null) {
				String[][] names = getFilename();
				if (names.length == 0) {
					JOptionPane.showMessageDialog(this, "No measurement file is selected.", "Specify file", JOptionPane.WARNING_MESSAGE);
					setCursor(c);
					measurementTextField.requestFocus();
					return false;
				}

				ArrayList<AbstractMap.SimpleEntry<ArrayList<String>, FileFilter>> files = new ArrayList();
				ArrayList<String> last = new ArrayList();
				last.add(names[0][0]);
				for (int i = 1; i < names.length; i++) {
					if (!names[i][1].equals(names[i - 1][1])) {
						files.add(new AbstractMap.SimpleEntry(last, fileFilter(names[i - 1][1])));
						last = new ArrayList();
					}
					last.add(names[i][0]);
				}
				files.add(new AbstractMap.SimpleEntry(last, fileFilter(names[names.length - 1][1])));

				fileNames = new LinkedHashSet(names.length);
				for (AbstractMap.SimpleEntry<ArrayList<String>, FileFilter> entry : files) {
					TreeMap<BigDecimal, HashSet<IMeasurement>> prev = masses;
					LinkedHashSet<FileName> tmp = loadFile(entry.getKey(), entry.getValue(), false);
					if (tmp == null) {
						fileNames = null;
						setCursor(c);
						measurementTextField.requestFocus();
						return false;
					}
					fileNames.addAll(tmp);
					addMasses(prev);
				}

				old = fileNames.isEmpty() ? TEXT : toString(fileNames).substring(2);
				measurementTextField.setText(old);
			} else {
				fileNames = loadFile(null, null, false);
				if (fileNames == null) {
					setCursor(c);
					measurementTextField.requestFocus();
					return false;
				}
			}

			setCursor(c);
		}
		return true;
	}

	private FileFilter fileFilter(final String text) {
		if (text.endsWith(")") && text.contains("(")) {
			return new FileFilter() {
				private String desc = text.substring(text.lastIndexOf('(') + 1, text.length() - 1) + " []";

				@Override
				public boolean accept(File f) {
					throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
				}

				@Override
				public String getDescription() {
					return desc;
				}
			};
		} else {
			return new FileFilter() {
				@Override
				public boolean accept(File f) {
					throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
				}

				@Override
				public String getDescription() {
					return ALL + " []";
				}
			};
		}
	}

	private LinkedHashSet<FileName> loadFile(List<String> files, FileFilter description, boolean unknown) {
		try {
			if (LinX.alternative != null) {
				FileName fn = new FileName(LinX.quick);
				masses = new TreeMap<>();
				if (alt(fn, Files.readAllLines(Paths.get(LinX.quick), java.nio.charset.Charset.defaultCharset()))) {
					LinkedHashSet<FileName> ret = new LinkedHashSet(1);
					ret.add(fn);
					if (TABS >= masses.size()) {
						ret.clear();
						for (BigDecimal mass : masses.keySet()) {
							fn = new FileName(Defaults.uMassShortFormat.format(mass));
							ret.add(fn);
							for (IMeasurement meas : masses.get(mass)) {
								((MeasurementAlternatives) meas).setFileName(fn);
							}
						}
						if (masses.size() != ret.size()) {
							ret.clear();
							for (BigDecimal mass : masses.keySet()) {
								fn = new FileName(Defaults.uMassFullFormat.format(mass));
								ret.add(fn);
								for (IMeasurement meas : masses.get(mass)) {
									((MeasurementAlternatives) meas).setFileName(fn);
								}
							}
						}
					}
					return ret;
				} else {
					JOptionPane.showMessageDialog(this, "Some file does not contain valid line.", "Empty file", JOptionPane.ERROR_MESSAGE);
					return null;
				}
			}

			ArrayList<String> problematic = new ArrayList(0);
			for (String file : files) {
				if (Files.notExists(Paths.get(file))) {
					problematic.add(file);
				}
			}
			if (!problematic.isEmpty() || files.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				for (String file : problematic) {
					if (!file.isEmpty()) {
						sb.append(System.lineSeparator()).append(file);
					}
				}
				if (files.size() == problematic.size() && sb.length() == 0) {
					JOptionPane.showMessageDialog(this, "No file is selected.", "File doesn't exist", JOptionPane.WARNING_MESSAGE);
					masses = null;
					return null;
				} else {
					JOptionPane.showMessageDialog(this, "Following files don't exist:" + sb.toString(), "File doesn't exist", JOptionPane.WARNING_MESSAGE);
					masses = null;
					return null;
				}
			}

			ArrayList<AbstractMap.SimpleImmutableEntry<String, List<String>>> lines = new ArrayList(files.size());
			for (String file : files) {
				lines.add(new AbstractMap.SimpleImmutableEntry(file, Files.readAllLines(Paths.get(file), java.nio.charset.Charset.defaultCharset())));
			}

			masses = new TreeMap<>();
			if (description.getDescription().startsWith(MGF + " [")) {
				LinkedHashSet<FileName> ret = new LinkedHashSet(files.size());
				for (AbstractMap.SimpleImmutableEntry<String, List<String>> entry : lines) {
					FileName fn = new FileName(entry.getKey(), '(' + MGF + ')');
					ret.add(fn);
					if (!mgf(fn, entry.getValue())) {
						problematic.add(entry.getKey());
					}
				}
				if (problematic.isEmpty()) {
					return ret;
				}
			}
			if (description.getDescription().startsWith(KUK + " [")) {
				LinkedHashSet<FileName> ret = new LinkedHashSet(files.size());
				for (AbstractMap.SimpleImmutableEntry<String, List<String>> entry : lines) {
					FileName fn = new FileName(entry.getKey(), '(' + KUK + ')');
					ret.add(fn);
					if (!kuk(fn, entry.getValue())) {
						problematic.add(entry.getKey());
					}
				}
				if (problematic.isEmpty()) {
					return ret;
				}
			}
			for (String name : msvs.stringPropertyNames()) {
				if (description.getDescription().equals(name) || description.getDescription().startsWith(name + " [")) {
					LinkedHashSet<FileName> ret = new LinkedHashSet(files.size());
					for (AbstractMap.SimpleImmutableEntry<String, List<String>> entry : lines) {
						FileName fn = new FileName(entry.getKey(), '(' + name + ')');
						ret.add(fn);
						String[] parts = msvs.getProperty(name).split(SEPARATOR, 5);
						if (!msf(fn, entry.getValue(), parts[4], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]))) {
							problematic.add(entry.getKey());
						}
					}
					if (problematic.isEmpty()) {
						return ret;
					}
				}
			}
			for (String name : formats.stringPropertyNames()) {
				if (description.getDescription().equals(name) || description.getDescription().startsWith(name + " [")) {
					LinkedHashSet<FileName> ret = new LinkedHashSet(files.size());
					for (AbstractMap.SimpleImmutableEntry<String, List<String>> entry : lines) {
						FileName fn = new FileName(entry.getKey(), '(' + name + ')');
						ret.add(fn);
						String[] parts = formats.getProperty(name).split(SEPARATOR, 3);
						if (!parser(fn, entry.getValue(), parts[1])) {
							problematic.add(entry.getKey());
						}
					}
					if (problematic.isEmpty()) {
						return ret;
					}
				}
			}
			if (!problematic.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				for (String file : problematic) {
					if (!file.isEmpty()) {
						sb.append(System.lineSeparator()).append(file);
					}
				}
				JOptionPane.showMessageDialog(this, "No valid line was found in following files:" + sb.toString(), "Reading error", JOptionPane.WARNING_MESSAGE);
				masses = null;
				return null;
			}

			if (!(description.getDescription().startsWith(" [") || description.getDescription().startsWith(ALL) || description.getDescription().startsWith(AMF + " ["))) {
				unknown = true;
			}

			TreeMap<Double, String> matches = new TreeMap();
			TreeMap<Double, String> extended = new TreeMap();
			LinkedHashSet<FileName> ret = new LinkedHashSet(files.size());
			Boolean[] add = new Boolean[]{true, true};
			masses = new TreeMap<>();
			for (AbstractMap.SimpleImmutableEntry<String, List<String>> entry : lines) {
				FileName fn = new FileName(entry.getKey(), '(' + MGF + ')');
				ret.add(fn);
				if (mgf(fn, entry.getValue())) {
					boolean none = true;
					for (String ext : MGFs.split(", ")) {
						if (entry.getKey().endsWith(ext)) {
							none = false;
						}
					}
					if (none) {
						add[1] = false;
					}
				} else {
					add[0] = false;
					add[1] = false;
				}
			}
			if (add[0]) {
				addHit(matches, MGF);
				if (add[1]) {
					addHit(extended, MGF);
				}
			} else {
				masses = new TreeMap<>();
			}
			TreeMap<BigDecimal, HashSet<IMeasurement>> tmpMasses = masses;
			LinkedHashSet<FileName> tmpFileNames = new LinkedHashSet(lines.size());
			add = new Boolean[]{true, true};
			masses = new TreeMap<>();
			for (AbstractMap.SimpleImmutableEntry<String, List<String>> entry : lines) {
				FileName fn = new FileName(entry.getKey(), '(' + KUK + ')');
				tmpFileNames.add(fn);
				if (kuk(fn, entry.getValue())) {
					boolean none = true;
					for (String ext : KUKs.split(", ")) {
						if (entry.getKey().endsWith(ext)) {
							none = false;
						}
					}
					if (none) {
						add[1] = false;
					}
				} else {
					add[0] = false;
					add[1] = false;
				}
			}
			if (add[0]) {
				addHit(matches, KUK);
				if (add[1]) {
					addHit(extended, KUK);
					ret = tmpFileNames;
				} else if (extended.isEmpty()) {
					ret = tmpFileNames;
				} else {
					masses = tmpMasses;
				}
			} else {
				masses = tmpMasses;
			}
			for (String name : msvs.stringPropertyNames()) {
				tmpMasses = masses;
				tmpFileNames = new LinkedHashSet(lines.size());
				add = new Boolean[]{true, true};
				masses = new TreeMap<>();
				String[] parts = msvs.getProperty(name).split(SEPARATOR, 5);
				for (AbstractMap.SimpleImmutableEntry<String, List<String>> entry : lines) {
					FileName fn = new FileName(entry.getKey(), '(' + name + ')');
					tmpFileNames.add(fn);
					if (msf(fn, entry.getValue(), parts[4], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]))) {
						boolean none = true;
						for (String ext : parts[0].split(", ")) {
							if (entry.getKey().endsWith(ext)) {
								none = false;
							}
						}
						if (none) {
							add[1] = false;
						}
					} else {
						add[0] = false;
						add[1] = false;
					}
				}
				if (add[0]) {
					addHit(matches, name);
					if (add[1]) {
						addHit(extended, name);
						ret = tmpFileNames;
					} else if (extended.isEmpty()) {
						ret = tmpFileNames;
					} else {
						masses = tmpMasses;
					}
				} else {
					masses = tmpMasses;
				}
			}
			for (String name : formats.stringPropertyNames()) {
				tmpMasses = masses;
				tmpFileNames = new LinkedHashSet(lines.size());
				add = new Boolean[]{true, true};
				masses = new TreeMap<>();
				String[] parts = formats.getProperty(name).split(SEPARATOR, 3);
				for (AbstractMap.SimpleImmutableEntry<String, List<String>> entry : lines) {
					FileName fn = new FileName(entry.getKey(), '(' + name + ')');
					tmpFileNames.add(fn);
					if (parser(fn, entry.getValue(), parts[2])) {
						boolean none = true;
						for (String ext : parts[0].split(", ")) {
							if (entry.getKey().endsWith(ext)) {
								none = false;
							}
						}
						if (none) {
							add[1] = false;
						}
					} else {
						add[0] = false;
						add[1] = false;
					}
				}
				if (add[0]) {
					addHit(matches, name);
					if (add[1]) {
						addHit(extended, name);
						ret = tmpFileNames;
					} else if (extended.isEmpty()) {
						ret = tmpFileNames;
					} else {
						masses = tmpMasses;
					}
				} else {
					masses = tmpMasses;
				}
			}

			HashSet<String> extensions = new HashSet();
			for (String file : files) {
				if (file.lastIndexOf(File.separator) < file.lastIndexOf('.')) {
					extensions.add(file.substring(file.lastIndexOf('.')));
				}
			}

			int count = (!QUIET || extended.isEmpty()) ? matches.size() : extended.size();
			if (count == 1) {
				if (unknown) {
					JOptionPane.showMessageDialog(this, "The format '" + description.getDescription().substring(0, description.getDescription().lastIndexOf('[') - 1)
							  + "' was not found.", "Unknown format", JOptionPane.INFORMATION_MESSAGE);
					unknown = false;
				}
				switch (QUIET ? JOptionPane.YES_OPTION
						  : JOptionPane.showConfirmDialog(this, "Do you wish to parse files according to the format \"" + matches.lastEntry().getValue() + "\"?",
									 "Confirm format", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)) {

					case JOptionPane.NO_OPTION:
						break;
					case JOptionPane.YES_OPTION:
						if (extended.isEmpty() && !extensions.isEmpty()) {
							if (msvs.containsKey(matches.lastEntry().getValue())) {
								String extension = filterExtensions(matches.lastEntry().getValue(), extensions);
								if (JOptionPane.showConfirmDialog(this, "The definition of the matching format \"" + matches.lastEntry().getValue()
										  + "\" doesn't contains extensions of some files (" + extension + ").\n"
										  + "Do you wish to add that extensions to the definition of the format?", "Add extension to matching format?",
										  JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
									addExtensions(msvs, MSVS_FILE, matches.lastEntry().getValue(), extension);
								}
							} else if (formats.containsKey(matches.lastEntry().getValue())) {
								String extension = filterExtensions(matches.lastEntry().getValue(), extensions);
								if (JOptionPane.showConfirmDialog(this, "The definition of the matching format \"" + matches.lastEntry().getValue()
										  + "\" doesn't contains extensions of some files (" + extension + ").\n"
										  + "Do you wish to add that extensions to the definition of the format?", "Add extension to matching format?",
										  JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
									addExtensions(formats, MEASUREMENTS_FILE, matches.lastEntry().getValue(), extension);
								}
							}
						}
						return ret;
					default:
						masses = null;
						return null;
				}
			} else if (count > 1) {
				int sel1;
				final Object sel2;
				if (!QUIET) {
					JPanel panel = new JPanel();
					BoxLayout layout = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
					panel.setLayout(layout);
					if (unknown) {
						JLabel label = new JLabel("The format '" + description.getDescription().substring(0, description.getDescription().lastIndexOf('[') - 1) + "' was not found.");
						label.setAlignmentX(0.0f);
						panel.add(label);
						label = new JLabel("Please, select another format for following files:");
						label.setAlignmentX(0.0f);
						panel.add(label);
					} else {
						JLabel label = new JLabel("Please, select the right format for following files:");
						label.setAlignmentX(0.0f);
						panel.add(label);
					}
					for (String file : files) {
						JLabel label = new JLabel(file);
						label.setAlignmentX(0.0f);
						panel.add(label);
					}
					JComboBox jcb = new JComboBox();
					for (String string : extended.descendingMap().values()) {
						jcb.addItem(string);
					}
					for (String string : matches.descendingMap().values()) {
						if (!extended.containsValue(string)) {
							jcb.addItem(string + " !");
						}
					}
					jcb.setSelectedIndex(0);
					jcb.setAlignmentX(0.0f);
					panel.add(jcb);
					if ((sel1 = JOptionPane.showConfirmDialog(this, panel, "Ambiguity", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)) == JOptionPane.YES_OPTION) {
						sel2 = extended.containsValue(jcb.getSelectedItem()) ? jcb.getSelectedItem() : jcb.getSelectedItem().toString().replaceFirst(" !$", "");
					} else {
						sel2 = null;
					}
				} else {
					StringBuilder sb = new StringBuilder();
					for (String file : files) {
						sb.append('\n').append(file);
					}
					if (!extended.isEmpty()) {
						matches = extended;
					}
					sel2 = JOptionPane.showInputDialog(this, (unknown ? "The format '" + description.getDescription().substring(0, description.getDescription().lastIndexOf('[') - 1)
							  + "' was not found.\nPlease, select another format for following files: "
							  : "Too many matching formats.\nPlease, select the right one for following files: ") + sb.toString(),
							  "Ambiguity", JOptionPane.QUESTION_MESSAGE, null, matches.descendingMap().values().toArray(), matches.lastEntry().getValue());
					sel1 = sel2 == null ? JOptionPane.CANCEL_OPTION : JOptionPane.YES_OPTION;
				}
				switch (sel1) {
					case JOptionPane.NO_OPTION:
						break;
					case JOptionPane.YES_OPTION:
						if (extended.isEmpty() && !extensions.isEmpty()) {
							if (msvs.containsKey(sel2)) {
								String extension = filterExtensions(sel2.toString(), extensions);
								if (JOptionPane.showConfirmDialog(this, "The definition of the selected format doesn't contains extensions of some files (" + extension + ").\n"
										  + "Do you wish to add that extensions to the definition of that format?", "Add extension to matching format?",
										  JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
									addExtensions(msvs, MSVS_FILE, sel2.toString(), extension);
								}
							} else if (formats.containsKey(sel2)) {
								String extension = filterExtensions(sel2.toString(), extensions);
								if (JOptionPane.showConfirmDialog(this, "The definition of the selected format doesn't contains extensions of some files (" + extension + ").\n"
										  + "Do you wish to add that extensions to the definition of that format?", "Add extension to matching format?",
										  JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
									addExtensions(formats, MEASUREMENTS_FILE, sel2.toString(), extension);
								}
							}
						}
						return loadFile(files, new FileFilter() {
							@Override
							public boolean accept(File f) {
								throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
							}

							@Override
							public String getDescription() {
								return sel2.toString() + " []";
							}
						}, false);
					default:
						masses = null;
						return null;
				}
				unknown = false;
			}

			if (files.size() > 1) {
				switch (JOptionPane.showConfirmDialog(this, unknown ? "The format '" + description.getDescription().substring(0, description.getDescription().lastIndexOf('[') - 1)
						  + "' was not found and no format match all files.\nDo you wish to parse them separately?"
						  : "No format match all files.\nDo you wish to parse them separately?",
						  "No matching format", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE)) {
					case JOptionPane.NO_OPTION:
						break;
					case JOptionPane.YES_OPTION:
						ret = new LinkedHashSet(files.size());
						for (int i = 0; i < files.size(); i++) {
							tmpMasses = masses;
							LinkedHashSet<FileName> subret = loadFile(files.subList(i, i + 1), description, false);
							if (subret == null) {
								return null;
							}
							ret.addAll(subret);
							addMasses(tmpMasses);
						}
						return ret;
					default:
						masses = null;
						return null;
				}
			} else if (unknown) {
				JOptionPane.showMessageDialog(this, "The format '" + description.getDescription().substring(0, description.getDescription().lastIndexOf('[') - 1)
						  + "' was not found.", "Unknown format", JOptionPane.INFORMATION_MESSAGE);
			}
			MeasurementDialog md = new MeasurementDialog(parent);
			ArrayList<FileName> unnamed = new ArrayList(lines.size());
			while (md.getReturnStatus() == MeasurementDialog.RET_OK) {
				masses = new TreeMap<>();
				StringBuilder sb = new StringBuilder();
				for (AbstractMap.SimpleImmutableEntry<String, List<String>> entry : lines) {
					FileName fileName = new FileName(entry.getKey(), "[" + md.getMassSpinner() + " " + md.getIntensitySpinner() + " " + md.getTimeSpinner() + " " + md.getSeparatorTextField() + ']');
					if (!msf(fileName, entry.getValue(), md.getSeparatorTextField(), md.getMassSpinner(), md.getTimeSpinner(), md.getIntensitySpinner())) {
						sb.append(System.lineSeparator()).append(entry.getKey());
					}
					unnamed.add(fileName);
				}
				if (sb.length() == 0) {
					break;
				}
				JOptionPane.showMessageDialog(this, "No valid line was found in following files: " + sb.toString(), "Reading error", JOptionPane.WARNING_MESSAGE);
				md.setVisible(true);
				unnamed = new ArrayList(lines.size());
			}
			if (md.getReturnStatus() == MeasurementDialog.RET_OK) {
				if (extensions.isEmpty()) {
					return new LinkedHashSet(unnamed);
				}
				ArrayList<String> similar = new ArrayList<>(0);
				for (String name : msvs.stringPropertyNames()) {
					if (msvs.getProperty(name).endsWith(SEPARATOR + md.getSeparatorTextField() + SEPARATOR + md.getMassSpinner()
							  + SEPARATOR + md.getTimeSpinner() + SEPARATOR + md.getIntensitySpinner())) {
						similar.add(name);
					}
				}
				if (similar.size() == 1) {
					String extension = filterExtensions(similar.get(0), extensions);
					if (JOptionPane.showConfirmDialog(this, "There is another format with the same pattern.\nDo you wish to add current extensions ("
							  + extension + ") to it?\n" + "================================\n"
							  + similar.get(0) + ": " + msvs.getProperty(similar.get(0)),
							  "Similar rule exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
						addExtensions(msvs, MSVS_FILE, similar.get(0), extension);
						ret = new LinkedHashSet(files.size());
						for (FileName fileName : unnamed) {
							fileName.setFormat('(' + similar.get(0) + ')');
							ret.add(fileName);
						}
						return ret;
					}
				} else if (!similar.isEmpty()) {
					String sel = (String) JOptionPane.showInputDialog(this, "There are another formats with the same pattern.\nDo you wish to add this extension to some of them?",
							  "Similar rules exist", JOptionPane.QUESTION_MESSAGE, null, similar.toArray(), similar.get(0));
					if (sel != null) {
						filterExtensions(sel, extensions);
						ret = new LinkedHashSet(files.size());
						for (FileName fileName : unnamed) {
							fileName.setFormat('(' + sel + ')');
							ret.add(fileName);
						}
						return ret;
					}
				}
				String ret2 = JOptionPane.showInputDialog(this, "In the case that you wish to save this format,\nplease specify description.\n"
						  + "(Description cannot contains brackets.)", "Do you wish to save this format?", JOptionPane.QUESTION_MESSAGE);
				while (!(ret2 == null || ret2.isEmpty())) {
					if (!(msvs.containsKey(ret2) || formats.contains(ret2) || MGF.equals(ret2) || KUK.equals(ret2) || ALL.equals(ret2))) {
						String extension = "";
						for (String ext : extensions) {
							extension += ", " + ext;
						}
						if (!extension.isEmpty()) {
							extension = extension.substring(2);
						}
						msvs.setProperty(ret2.replaceAll("[()\\[\\]]", ""), extension + SEPARATOR + md.getMassSpinner() + SEPARATOR + md.getTimeSpinner()
								  + SEPARATOR + md.getIntensitySpinner() + SEPARATOR + md.getSeparatorTextField());
						Defaults.saveDefaults(MSVS_FILE, msvs);
						ret = new LinkedHashSet(files.size());
						for (FileName fileName : unnamed) {
							fileName.setFormat('(' + ret2 + ')');
							ret.add(fileName);
						}
						return ret;
					}
					ret2 = (String) JOptionPane.showInputDialog(this, "Selected desctiption is already used by another format.\nPlease use a another description.\n"
							  + "(Description cannot contains brackets.)", "Do you wish to save this format?",
							  JOptionPane.QUESTION_MESSAGE, null, null, ret2);
				}
				return new LinkedHashSet(unnamed);
			} else {
				masses = null;
				return null;
			}
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "It isn't possible to read some of files." + System.lineSeparator() + ex.getMessage(), "IO Exception",
					  JOptionPane.WARNING_MESSAGE);
			masses = null;
			return null;
		}
	}

	private void addHit(TreeMap<Double, String> hits, String hit) {
		hits.put(masses.size() + 1.0 / (hits.size() + 1), hit);
	}

	private String filterExtensions(String format, Set<String> extensions) {
		HashSet<String> filtered = new HashSet(extensions);
		for (Object ext : msvs.getProperty(format, "").split(SEPARATOR)[0].split("[, ]+")) {
			filtered.remove(ext);
		}
		String ret = "";
		for (String ext : filtered) {
			ret += ", " + ext;
		}
		if (!ret.isEmpty()) {
			ret = ret.substring(2);
		}
		return ret;
	}

	private void addExtensions(Properties properties, String filename, String format, String extensions) {
		properties.setProperty(format, extensions + ", " + properties.getProperty(format));
		Defaults.saveDefaults(filename, properties);
	}

	private String toString(LinkedHashSet<FileName> files) {
		StringBuilder sb = new StringBuilder();
		for (FileName file : files) {
			sb.append(File.pathSeparator).append(' ').append(file);
		}
		return sb.toString();
	}

	private void addMasses(TreeMap<BigDecimal, HashSet<IMeasurement>> add) {
		if (add == null) {
			return;
		}
		if (masses == null) {
			masses = add;
			return;
		}
		for (Map.Entry<BigDecimal, HashSet<IMeasurement>> entry : add.entrySet()) {
			if (masses.containsKey(entry.getKey())) {
				masses.get(entry.getKey()).addAll(entry.getValue());
			} else {
				masses.put(entry.getKey(), entry.getValue());
			}
		}
	}

	private boolean mgf(FileName fileName, List<String> lines) {
		boolean any = false;
		String time = "";
		for (String line : lines) {
			if (!(line.isEmpty() || line.startsWith("#") || line.startsWith(";") || line.startsWith("!") || line.startsWith("/")
					  || line.startsWith("BEGIN IONS") || line.startsWith("END IONS"))) {
				String[] parts = line.split("\\t");
				try {
					BigDecimal mass = new BigDecimal(parts[0]);
					double intensity;
					if (parts.length > 1) {
						intensity = Double.parseDouble(parts[1]);
					} else {
						intensity = -1;
					}
					StringBuilder buff = new StringBuilder();
					for (int i = 2; i < parts.length; i++) {
						if (parts[i].contains("rt=")) {
							time = parts[i].substring(3).trim();
						} else {
							buff.append(parts[i]).append('\t');
						}
					}
					if (buff.length() != 0) {
						buff.deleteCharAt(buff.length() - 1);
					}
					if (!masses.containsKey(mass)) {
						masses.put(mass, new HashSet<>(1));
					}
					masses.get(mass).add(new Measurement(fileName, intensity, time, buff.toString()));
					any = true;
				} catch (NumberFormatException e) {
					if ((line.startsWith("RTINSECONDS") || line.startsWith("SCANS")) && line.contains("=")) {
						time = line.substring(line.indexOf('=') + 1).trim();
					}
				}
			} else if (line.matches(".*[0-9]+\\.[0-9]+(\\-[0-9]+\\.[0-9]+)?min.*")) {
				java.util.regex.Matcher m = java.util.regex.Pattern.compile("[0-9]+\\.[0-9]+(\\-[0-9]+\\.[0-9]+)?(?=min)").matcher(line);
				m.find();
				time = m.group();
			}
		}
		return any;
	}

	private boolean kuk(FileName fileName, List<String> lines) {
		boolean any = false;
		for (String line : lines) {
			String[] parts = line.split("\\t");
			if (parts.length == 7) {
				try {
					Byte.parseByte(parts[0]);
					BigDecimal mass = new BigDecimal(parts[1]);
					if (!(parts[2].equalsIgnoreCase("Y") || parts[2].equalsIgnoreCase("N"))) {
						continue;
					}
					for (String part : parts[4].split("-")) {
						Integer.parseInt(part);
					}
					Byte.parseByte(parts[5]);
					boolean ok = true;
					for (String fragment : parts[6].split(" ")) {
						String[] f = fragment.split("~");
						if (f.length != 2) {
							ok = false;
							break;
						}
						Byte.parseByte(f[0]);
						Double.parseDouble(f[1]);
					}
					if (!ok) {
						continue;
					}
					if (!masses.containsKey(mass)) {
						masses.put(mass, new HashSet<>(1));
					}
					masses.get(mass).add(new Measurement(fileName, Double.parseDouble(parts[3]), parts[4], parts[0] + '\t' + parts[2] + '\t' + parts[5] + '\t' + parts[6]));
					any = true;
				} catch (NumberFormatException e) {
				}
			}
		}
		return any;
	}

	private boolean msf(FileName fileName, List<String> lines, String separator, int massIndex, int timeIndex, int intensityIndex) {
		boolean any = false;
		for (String line : lines) {
			String[] parts;
			if ((parts = line.split(separator)).length >= Math.max(massIndex, Math.max(timeIndex, intensityIndex))) {
				try {
					double intensity;
					if (intensityIndex > 0) {
						intensity = Double.parseDouble(parts[intensityIndex - 1]);
					} else {
						intensity = -1;
					}
					String time;
					if (timeIndex > 0) {
						time = parts[timeIndex - 1];
						if (!time.matches(".*[0-9].*")) {
							throw new Exception();
						}
					} else {
						time = "";
					}
					BigDecimal mass = new BigDecimal(parts[massIndex - 1]);
					StringBuilder buff = new StringBuilder();
					for (int i = 1; i <= parts.length; i++) {
						if (i != massIndex && i != intensityIndex && i != timeIndex) {
							buff.append(parts[i - 1]).append('\t');
						}
					}
					if (buff.length() != 0) {
						buff.deleteCharAt(buff.length() - 1);
					}
					if (!masses.containsKey(mass)) {
						masses.put(mass, new HashSet<>(1));
					}
					masses.get(mass).add(new Measurement(fileName, intensity, time, buff.toString()));
					any = true;
				} catch (Exception e) {
				}
			}
		}
		return any;
	}

	private boolean parser(FileName fileName, List<String> lines, String command) {
		boolean any = false;
		try {
			Process process = Runtime.getRuntime().exec(command.replace("<FILE>", fileName.getFileName()));
			try (Scanner reader = new Scanner(process.getInputStream())) {
				while (reader.hasNextLine()) {
					String[] parts;
					if ((parts = reader.nextLine().split("\t", 4)).length > 0 && !parts[0].isEmpty()) {
						try {
							double intensity;
							if (parts.length > 1 && parts[1].isEmpty()) {
								intensity = -1;
							} else {
								intensity = Double.parseDouble(parts[1]);
							}
							String time;
							if (parts.length > 2 && !parts[2].isEmpty()) {
								time = parts[2];
								if (!time.matches(".*[0-9].*")) {
									throw new Exception();
								}
							} else {
								time = "";
							}
							BigDecimal mass = new BigDecimal(parts[0]);
							if (!masses.containsKey(mass)) {
								masses.put(mass, new HashSet<>(1));
							}
							masses.get(mass).add(new Measurement(fileName, intensity, time, parts.length < 4 ? "" : parts[3]));
							any = true;
						} catch (Exception e) {
						}
					}
				}
			}
		} catch (IOException e) {
		}
		return any;
	}

	private boolean alt(FileName fileName, List<String> lines) {
		Iterator<String> iterator = lines.iterator();
		while (iterator.hasNext()) {
			String line = iterator.next();
			if (line.matches("^-+$")) {
				break;
			}
		}
		if (!iterator.hasNext()) {
			return false;
		}
		String[] header = iterator.next().split("\t");
		filtered = header.length > 5;
		int[] indices = new int[5];
		if (filtered) {
			indices[0] = 1;
			indices[1] = 3;
			indices[2] = 4;
			indices[3] = 5;
			indices[4] = 6;
		} else {
			indices[0] = 0;
			indices[1] = 1;
			indices[2] = 2;
			indices[3] = 3;
			indices[4] = 4;
		}
		// TODO: Měla by být kontrola, příp. přeuspořádání, příp. i počítání odzadu, když je 'Other' dřív
		boolean any = false;
		while (iterator.hasNext()) {
			try {
				String[] parts;
				if ((parts = iterator.next().split("\t", header.length)).length == header.length) {
					int length = 1;
					String[] tmp = (parts[indices[0]].startsWith("{ ") ? parts[indices[0]].substring(2, parts[indices[0]].length() - 2) : parts[indices[0]]).split(" \\} \\| \\{ ", -1);
					double[] massT = new double[tmp.length];
					for (int i = 0; i < massT.length; i++) {
						try {
							massT[i] = Defaults.uMassFullFormat.parse(tmp[i]).doubleValue();
						} catch (ParseException e) {
							massT[i] = Double.NaN;
						}
					}
					length = Math.max(length, massT.length);
					double[] error;
					if (filtered) {
						tmp = (parts[2].startsWith("{ ") ? parts[2].substring(2, parts[2].length() - 2) : parts[2]).split(" \\} \\| \\{ ", -1);
						error = new double[tmp.length];
						for (int i = 0; i < error.length; i++) {
							try {
								error[i] = Defaults.sPpmFormat.parse(tmp[i]).doubleValue();
							} catch (ParseException e) {
								error[i] = Double.NaN;
							}
						}
						length = Math.max(length, error.length);
					} else {
						error = new double[]{Double.NaN};
					}
					String[] prots = (parts[indices[1]].startsWith("{ ") ? parts[indices[1]].substring(2, parts[indices[1]].length() - 2) : parts[indices[1]]).split(" \\} \\| \\{ ", -1);
					length = Math.max(length, prots.length);
					String[] chain = (parts[indices[2]].startsWith("{ ") ? parts[indices[2]].substring(2, parts[indices[2]].length() - 2) : parts[indices[2]]).split(" \\} \\| \\{ ", -1);
					length = Math.max(length, chain.length);
					tmp = (parts[indices[3]].startsWith("{ ") ? parts[indices[3]].substring(2, parts[indices[3]].length() - 2) : parts[indices[3]]).split(" \\} \\| \\{ ", -1);
					String[][] modif = new String[tmp.length][];
					for (int i = 0; i < modif.length; i++) {
						modif[i] = tmp[i].split(" ?\\| ", -1);
					}
					length = Math.max(length, modif.length);
					tmp = (parts[indices[4]].startsWith("{ ") ? parts[indices[4]].substring(2, parts[indices[4]].length() - 2) : parts[indices[4]]).split(" \\} \\| \\{ ", -1);
					String[][] bonds = new String[tmp.length][];
					for (int i = 0; i < bonds.length; i++) {
						bonds[i] = tmp[i].split(" ?\\| ", -1);
					}
					length = Math.max(length, bonds.length);
					if (length > 1) {
						if (filtered) {
							if (massT.length == 1) {
								double tmd = massT[0];
								Arrays.fill(massT = new double[length], tmd);
							}
						}
						if (error.length == 1) {
							double tmd = error[0];
							Arrays.fill(error = new double[length], tmd);
						}
						if (prots.length == 1) {
							String tms = prots[0];
							Arrays.fill(prots = new String[length], tms);
						}
						if (chain.length == 1) {
							String tms = chain[0];
							Arrays.fill(chain = new String[length], tms);
						}
						if (modif.length == 1) {
							String[] tma = modif[0];
							Arrays.fill(modif = new String[length][], tma);
						}
						if (bonds.length == 1) {
							String[] tma = bonds[0];
							Arrays.fill(bonds = new String[length][], tma);
						}
						if ((filtered && massT.length != length) || error.length != length || prots.length != length || chain.length != length || modif.length != length || bonds.length != length) {
							throw new IndexOutOfBoundsException("Elements don't have the same length.");
						}
					}
					if (filtered) {
						BigDecimal massE = (BigDecimal) Defaults.uMassFullFormat.parse(parts[0]);
						if (!masses.containsKey(massE)) {
							masses.put(massE, new HashSet<>(1));
						}
						masses.get(massE).add(new MeasurementAlternatives(fileName, massT, error, prots, chain, modif, bonds));
					} else {
						tmp = (parts[indices[0]].startsWith("{ ") ? parts[indices[0]].substring(2, parts[indices[0]].length() - 2) : parts[indices[0]]).split(" \\} \\| \\{ ", -1);
						BigDecimal[] massE = new BigDecimal[tmp.length];
						for (int i = 0; i < massT.length; i++) {
							try {
								massE[i] = (BigDecimal) Defaults.uMassFullFormat.parse(tmp[i]);
							} catch (ParseException e) {
								massE[i] = null;
							}
						}
						if (massT.length == 1) {
							double[] tmd = massT;
							Arrays.fill(massT = new double[length], tmd[0]);
						}
						for (int i = 0; i < massE.length; i++) {
							if (!masses.containsKey(massE[i])) {
								masses.put(massE[i], new HashSet<>(1));
							}
							masses.get(massE[i]).add(new MeasurementAlternatives(fileName, massT, error, prots, chain, modif, bonds));
						}
					}

					any = true;
				}
			} catch (IndexOutOfBoundsException | ParseException e) {
				e.printStackTrace();
			}
		}
		return any;
	}

	public interface Tolerance {

		BigDecimal lower(BigDecimal value);

		BigDecimal upper(BigDecimal value);

		Double error(BigDecimal experimental, BigDecimal theoretical);

		DecimalFormat getErrorFormat();
	}

	private class AbsoluteTolerance implements Tolerance {

		BigDecimal tolerance;

		public AbsoluteTolerance(BigDecimal tolerance) {
			this.tolerance = tolerance;
		}

		@Override
		public BigDecimal lower(BigDecimal value) {
			return value.subtract(tolerance);
		}

		@Override
		public BigDecimal upper(BigDecimal value) {
			return value.add(tolerance);
		}

		@Override
		public Double error(BigDecimal experimental, BigDecimal theoretical) {
			return experimental.doubleValue() - theoretical.doubleValue();
		}

		@Override
		public DecimalFormat getErrorFormat() {
			return Defaults.sDaFormat;
		}
	}

	private class RelativeTolerance implements Tolerance {

		BigDecimal tolerance;

		public RelativeTolerance(BigDecimal tolerance) {
			this.tolerance = tolerance;
		}

		@Override
		public BigDecimal lower(BigDecimal value) {
			return value.subtract(tolerance.multiply(value));
		}

		@Override
		public BigDecimal upper(BigDecimal value) {
			return value.add(tolerance.multiply(value));
		}

		@Override
		public Double error(BigDecimal experimental, BigDecimal theoretical) {
			return 1000000 * (experimental.doubleValue() - theoretical.doubleValue()) / theoretical.doubleValue();
		}

		@Override
		public DecimalFormat getErrorFormat() {
			return Defaults.sPpmFormat;
		}
	}
}
