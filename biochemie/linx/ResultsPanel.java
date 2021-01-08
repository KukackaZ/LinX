package biochemie.linx;

import biochemie.Defaults;
import biochemie.Protein;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.*;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 *
 * @author Janek
 */
public class ResultsPanel extends JPanel {

	private static final String LEFT = "N-terminus";
	private static final String RIGHT = "C-terminus";
	private static final String DIR = "Temp";

	private static final int INDEX_EXP = 0;
	private static final int INDEX_THR = 1;
	private static final int INDEX_ERR = 2;
	private static final int INDEX_PROTEIN = 3;
	private static final int INDEX_MODS = 5;
	private static final int INDEX_BONDS = 6;
	private static final int INDEX_INTENSITY = 7;
	private static final int INDEX_TIME = 8;
	private static final int INDEX_OTHER = 9;

	public enum Save {
		SELECTED, SHOWN, SELECTED_SHOWN_ALL, SHOWN_ALL
	}

	private LinX parent;
	private ComputingCard computingCard;
	private ResultsCard resultsCard;
	private LinkedHashMap<String, Protein> proteins;
	private boolean filter;
	private DecimalFormat errorFormat;
	private FileName measurementName;
	private String defaultName;

	private JLabel successRate;
	private JScrollPane resultsScrollPane;
	private JTable resultsTable;

	public ResultsPanel(LinX parent, FileName measurementFile, String defaultName, ResultsCard resultsCard, ComputingCard computingCard, LinkedHashMap<String, Protein> proteins, boolean filter, DecimalFormat errorFormat) {
		this.parent = parent;
		this.resultsCard = resultsCard;
		this.computingCard = computingCard;
		this.proteins = proteins;
		this.filter = filter;
		this.errorFormat = errorFormat;
		this.measurementName = measurementFile;
		this.defaultName = defaultName;

		successRate = new JLabel("Computation interrupted.");
		resultsScrollPane = new JScrollPane();
		resultsTable = new JTable(new javax.swing.table.DefaultTableModel(new Object[][]{}, new String[]{"Exp. Mass", "Thr. Mass", "Error",
			"Protein (from, to)", "Chain", "Modifications", "Bonds", "Intensity", "Retention time", "Other"}) {
			private Class[] classes = new Class[]{Double[].class, Double[].class, Double[].class, String[].class, String[].class, String[].class, String[].class,
				Long[].class, String[].class, String[].class};

			@Override
			public Class getColumnClass(int columnIndex) {
				return classes[columnIndex];
			}

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
		});

		resultsScrollPane.setViewportView(resultsTable);

		resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		resultsTable.setPreferredScrollableViewportSize(new Dimension(400, 200));
		resultsTable.getColumn(resultsTable.getColumnName(INDEX_EXP)).setCellRenderer(new MassCR());
		resultsTable.getColumn(resultsTable.getColumnName(INDEX_THR)).setCellRenderer(new MassCR());
		resultsTable.getColumn(resultsTable.getColumnName(INDEX_ERR)).setCellRenderer(new ErrorCR());
		resultsTable.getColumn(resultsTable.getColumnName(INDEX_INTENSITY)).setCellRenderer(new IntensityCR());
		resultsTable.getColumn(resultsTable.getColumnName(INDEX_TIME)).setCellRenderer(new NumberCR());
		resultsTable.setDefaultRenderer(String[].class, new StringCR());
		TableRowSorter<javax.swing.table.TableModel> trs = new ArrayRowSorter(resultsTable.getModel());
		trs.setMaxSortKeys(resultsTable.getColumnCount());
		trs.setComparator(INDEX_EXP, new ArrayComparator(new Comparator<Double>() {
			@Override
			public int compare(Double o1, Double o2) {
				if (Objects.equals(o1, o2) || o1.equals(o2) || Defaults.uMassShortFormat.format(o1).equals(Defaults.uMassShortFormat.format(o2))) {
					return 0;
				}
				return o1.compareTo(o2);
			}
		}));
		trs.setComparator(INDEX_THR, new ArrayComparator(new Comparator<Double>() {
			@Override
			public int compare(Double o1, Double o2) {
				if (Objects.equals(o1, o2) || o1.equals(o2) || Defaults.uMassShortFormat.format(o1).equals(Defaults.uMassShortFormat.format(o2))) {
					return 0;
				}
				return o1.compareTo(o2);
			}
		}));
		trs.setComparator(INDEX_ERR, new ArrayComparator(new Comparator<Double>() {
			@Override
			public int compare(Double o1, Double o2) {
				if (Objects.equals(o1, o2) || o1.equals(o2) || ResultsPanel.this.errorFormat.format(o1).equals(ResultsPanel.this.errorFormat.format(o2))) {
					return 0;
				}
				return o1.compareTo(o2);
			}
		}));
		trs.setComparator(INDEX_PROTEIN, new ArrayComparator(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				String[] s1 = o1.split(" - ");
				String[] s2 = o2.split(" - ");

				if (s1.length != s2.length) {
					return Integer.compare(s1.length, s2.length);
				}

				for (int i = 0; i < s1.length; i++) {
					int res = s1[i].substring(0, s1[i].lastIndexOf('(')).compareTo(s2[i].substring(0, s2[i].lastIndexOf('(')));
					if (res != 0) {
						return res;
					}

					res = Integer.compare(Integer.parseInt(s1[i].substring(s1[i].lastIndexOf(" (") + 2, s1[i].lastIndexOf(", "))),
							  Integer.parseInt(s2[i].substring(s2[i].lastIndexOf(" (") + 2, s2[i].lastIndexOf(", "))));
					if (res != 0) {
						return res;
					}

					res = Integer.compare(Integer.parseInt(s1[i].substring(s1[i].lastIndexOf(", ") + 2, s1[i].lastIndexOf(")"))),
							  Integer.parseInt(s2[i].substring(s2[i].lastIndexOf(", ") + 2, s2[i].lastIndexOf(")"))));
					if (res != 0) {
						return res;
					}
				}

				return 0;
			}
		}));
		trs.setComparator(INDEX_MODS, new ArrayComparator(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				String[] p1 = o1.split(" \\| ", -1);
				String[] p2 = o2.split(" \\| ", -1);
				int minP = Math.min(p1.length, p2.length);

				for (int i = 0; i < minP; i++) {
					if (p1[i].isEmpty() || p2[i].isEmpty()) {
						if (p1[i].isEmpty() && p2[i].isEmpty()) {
							continue;
						} else {
							return p1[i].compareTo(p2[i]);
						}
					}

					String[] m1 = p1[i].split("(?<=\\))[,;] ");
					String[] m2 = p2[i].split("(?<=\\))[,;] ");
					int minM = Math.min(m1.length, m2.length);
					int res;

					for (int j = 0; j < minM; j++) {
						res = m1[j].substring(0, m1[j].lastIndexOf('(')).compareTo(m2[j].substring(0, m2[j].lastIndexOf('(')));
						if (res != 0) {
							return res;
						}

						int i1z = m1[j].lastIndexOf("(");
						int i1t = m1[j].lastIndexOf(".");
						int i2z = m2[j].lastIndexOf("(");
						int i2t = m2[j].lastIndexOf(".");
						int i1;
						int i2;

						String s1 = m1[j].substring(Math.max(i1z, i1t) + 1, m1[j].lastIndexOf(")"));
						String s2 = m2[j].substring(Math.max(i2z, i2t) + 1, m2[j].lastIndexOf(")"));
						switch (s1) {
							case LEFT:
								i1 = Integer.MIN_VALUE;
								break;
							case RIGHT:
								i1 = Integer.MAX_VALUE;
								break;
							default:
								i1 = Integer.parseInt(s1);
								break;
						}
						switch (s2) {
							case LEFT:
								i2 = Integer.MIN_VALUE;
								break;
							case RIGHT:
								i2 = Integer.MAX_VALUE;
								break;
							default:
								i2 = Integer.parseInt(s2);
								break;
						}
						res = Integer.compare(i1, i2);
						if (res != 0) {
							return res;
						}

						res = ((i1z < i1t) ? m1[j].substring(i1z + 1, i1t + 1) : "").compareTo((i2z < i2t) ? m2[j].substring(i2z + 1, i2t + 1) : "");
						if (res != 0) {
							return res;
						}
					}

					res = Integer.compare(m1.length, m2.length);
					if (res != 0) {
						return res;
					}
				}

				return Integer.compare(p1.length, p2.length);
			}
		}));
		trs.setComparator(INDEX_BONDS, new ArrayComparator(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				String[] p1 = o1.split(" \\| ", -1);
				String[] p2 = o2.split(" \\| ", -1);
				int minP = Math.min(p1.length, p2.length);

				for (int i = 0; i < minP; i++) {
					if (p1[i].isEmpty() || p2[i].isEmpty()) {
						if (p1[i].isEmpty() && p2[i].isEmpty()) {
							continue;
						} else {
							return p1[i].compareTo(p2[i]);
						}
					}

					String[] b1 = p1[i].split("(?<=\\))[,;] ");
					String[] b2 = p2[i].split("(?<=\\))[,;] ");
					int minB = Math.min(b1.length, b2.length);
					int res;

					for (int j = 0; j < minB; j++) {
						res = b1[j].substring(0, b1[j].lastIndexOf('(')).compareTo(b2[j].substring(0, b2[j].lastIndexOf('(')));
						if (res != 0) {
							return res;
						}

						int i1z = b1[j].lastIndexOf("(");
						int i1s = b1[j].lastIndexOf(";");
						int i11 = b1[j].lastIndexOf(".", i1s);
						int i12 = b1[j].lastIndexOf(".");
						int i2z = b2[j].lastIndexOf("(");
						int i2s = b2[j].lastIndexOf(";");
						int i21 = b2[j].lastIndexOf(".", i2s);
						int i22 = b2[j].lastIndexOf(".");
						int i1;
						int i2;

						String s1 = b1[j].substring(Math.max(i1z, i11) + 1, i1s);
						String s2 = b2[j].substring(Math.max(i2z, i21) + 1, i2s);
						switch (s1) {
							case LEFT:
								i1 = Integer.MIN_VALUE;
								break;
							case RIGHT:
								i1 = Integer.MAX_VALUE;
								break;
							default:
								i1 = Integer.parseInt(s1);
								break;
						}
						switch (s2) {
							case LEFT:
								i2 = Integer.MIN_VALUE;
								break;
							case RIGHT:
								i2 = Integer.MAX_VALUE;
								break;
							default:
								i2 = Integer.parseInt(s2);
								break;
						}
						res = Integer.compare(i1, i2);
						if (res != 0) {
							return res;
						}

						s1 = b1[j].substring(Math.max(i1s + 1, i12) + 1, b1[j].lastIndexOf(")"));
						s2 = b2[j].substring(Math.max(i2s + 1, i22) + 1, b2[j].lastIndexOf(")"));
						switch (s1) {
							case LEFT:
								i1 = Integer.MIN_VALUE;
								break;
							case RIGHT:
								i1 = Integer.MAX_VALUE;
								break;
							default:
								i1 = Integer.parseInt(s1);
								break;
						}
						switch (s2) {
							case LEFT:
								i2 = Integer.MIN_VALUE;
								break;
							case RIGHT:
								i2 = Integer.MAX_VALUE;
								break;
							default:
								i2 = Integer.parseInt(s2);
								break;
						}
						res = Integer.compare(i1, i2);
						if (res != 0) {
							return res;
						}

						res = ((i1z < i11) ? b1[j].substring(i1z + 1, i11 + 1) : "").compareTo(i2z < i21 ? b2[j].substring(i2z + 1, i21 + 1) : "");
						if (res != 0) {
							return res;
						}
						res = ((i1s < i12) ? b1[j].substring(i1s + 2, i12 + 1) : "").compareTo(i2s < i22 ? b2[j].substring(i2s + 2, i22 + 1) : "");
						if (res != 0) {
							return res;
						}
					}

					res = Integer.compare(b1.length, b2.length);
					if (res != 0) {
						return res;
					}
				}

				return Integer.compare(p1.length, p2.length);
			}
		}));
		resultsTable.setRowSorter(trs);
		resultsTable.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (resultsTable.getSelectedRowCount() > 0) {
					if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU && resultsTable.getSelectedRowCount() > 0) {
						int min = resultsTable.getRowCount() - 1;
						int max = 0;
						for (int row : resultsTable.getSelectedRows()) {
							if (min > row) {
								min = row;
							}
							if (max < row) {
								max = row;
							}
						}
						java.awt.Rectangle a = resultsTable.getVisibleRect();
						java.awt.Rectangle b = resultsTable.getCellRect(max, 0, true);
						b.x = a.x;
						b.width = a.width;
						resultsTable.scrollRectToVisible(b);
						b = resultsTable.getCellRect(min, 0, true);
						b.x = a.x;
						b.width = a.width;
						resultsTable.scrollRectToVisible(b);
						showPopup(a.x + a.width / 2, b.y + b.height / 2);
					} else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
						deleteSelected();
					}
				}
			}
		});
		resultsTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					if (!resultsTable.isRowSelected(resultsTable.rowAtPoint(e.getPoint()))) {
						resultsTable.setRowSelectionInterval(resultsTable.rowAtPoint(e.getPoint()), resultsTable.rowAtPoint(e.getPoint()));
					}
					showPopup(e.getPoint().x, e.getPoint().y);
				}
			}
		});

		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				  .addGroup(layout.createSequentialGroup()
							 .addComponent(successRate, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
							 .addComponent(resultsScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				  .addComponent(successRate, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
				  .addComponent(resultsScrollPane));

		if (!filter) {
			for (String column : new String[]{"Exp. Mass", "Error", "Intensity", "Retention time", "Other"}) {
				resultsTable.removeColumn(resultsTable.getColumn(column));
			}
		}

		resultsTable.setVisible(false);
	}

	private void showPopup(int x, int y) {
		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem mi = new JMenuItem("Delete", KeyEvent.VK_D);
		mi.setAccelerator(KeyStroke.getKeyStroke("D"));
		mi.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				deleteSelected();
			}
		});
		popupMenu.add(mi);
		popupMenu.addSeparator();
		JMenu smi = new JMenu("Grouping");
		smi.setMnemonic(KeyEvent.VK_G);
		mi = new JMenuItem("Group modifications", KeyEvent.VK_M);
		mi.setAccelerator(KeyStroke.getKeyStroke('M'));
		mi.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					mergeDuplications(INDEX_MODS);
				} catch (InterruptedException ie) {
				}
			}
		});
		smi.add(mi);
		mi = new JMenuItem("Group bonds", KeyEvent.VK_B);
		mi.setAccelerator(KeyStroke.getKeyStroke('B'));
		mi.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					mergeDuplications(INDEX_BONDS);
				} catch (InterruptedException ie) {
				}
			}
		});
		smi.add(mi);
		mi = new JMenuItem("Group by masses", KeyEvent.VK_E);
		mi.setAccelerator(KeyStroke.getKeyStroke('G'));
		mi.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					mergeDuplications(INDEX_EXP);
				} catch (InterruptedException ie) {
				}
			}
		});
		smi.add(mi);
		popupMenu.add(smi);
		smi = new JMenu("Ungrouping");
		smi.setMnemonic(KeyEvent.VK_U);
		mi = new JMenuItem("Ungroup by masses", 'H');
		mi.setAccelerator(KeyStroke.getKeyStroke('H'));
		mi.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				splitDuplications(INDEX_EXP);
			}
		});
		smi.add(mi);
		mi = new JMenuItem("Ungroup bonds", 'C');
		mi.setAccelerator(KeyStroke.getKeyStroke('C'));
		mi.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				splitDuplications(INDEX_BONDS);
			}
		});
		smi.add(mi);
		mi = new JMenuItem("Ungroup modifications", 'N');
		mi.setAccelerator(KeyStroke.getKeyStroke('N'));
		mi.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				splitDuplications(INDEX_MODS);
			}
		});
		smi.add(mi);
		popupMenu.add(smi);
		popupMenu.addSeparator();
		mi = new JMenuItem("Find alternatives", KeyEvent.VK_A);
		mi.setEnabled(LinX.alternative == null);
		mi.setAccelerator(KeyStroke.getKeyStroke("A"));
		mi.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int[] selected = resultsTable.getSelectedRows();
				TreeSet<Integer> ok = new TreeSet();
				for (int i : selected) {
//          if (resultsTable.getModel().getValueAt(resultsTable.convertRowIndexToModel(i), 1) != null) {
//            for (Double mass : (Double[])resultsTable.getModel().getValueAt(resultsTable.convertRowIndexToModel(i), 1)) {
//              if (mass != null) {
					ok.add(i);
//                break;
//              }
//            }
//          }
				}
				if (ok.isEmpty()) {
					JOptionPane.showMessageDialog(ResultsPanel.this, "No matched row is selected.", "Invalid selection", JOptionPane.WARNING_MESSAGE);
					return;
				}
				int index;
				String[] settings;
				if (ResultsPanel.this.filter) {
					index = 0;
					settings = null;
				} else {
					JPanel panel = new JPanel();
					JLabel precisionLabel = new JLabel();
					JFormattedTextField precisionTextField = new JFormattedTextField();
					JRadioButton ppmRadioButton = new JRadioButton();
					JRadioButton daRadioButton = new JRadioButton();
					MeasurementCard.initPrecision(precisionLabel, precisionTextField, ppmRadioButton, daRadioButton, new java.util.concurrent.atomic.AtomicBoolean());
					panel.add(precisionLabel);
					panel.add(precisionTextField);
					panel.add(ppmRadioButton);
					panel.add(daRadioButton);
					if (JOptionPane.showConfirmDialog(ResultsPanel.this, panel, "Additional settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION) {
						return;
					}
					settings = new String[]{"Settings:", "Tolerance is " + precisionTextField.getValue() + (ppmRadioButton.isSelected() ? "ppm" : "Da") + "."};
					index = 1;
				}
				resultsTable.clearSelection();
				for (int i : ok) {
					resultsTable.addRowSelectionInterval(i, i);
				}
				try {
					String alt = File.createTempFile(ResultsPanel.this.defaultName + "-", ".sen", new File(DIR)).getPath();
					File che = File.createTempFile(ResultsPanel.this.defaultName + "-", ".txt", new File(DIR));
					try (PrintWriter pw = new PrintWriter(che)) {
						TreeSet<Double> masses = new TreeSet();
						saveResults(alt, true, false, Save.SELECTED, settings);
						for (int i = 0; i < resultsTable.getModel().getRowCount(); i++) {
							masses.addAll(Arrays.asList((Double[]) resultsTable.getModel().getValueAt(i, index)));
						}
						for (Double mass : masses) {
							pw.println(Defaults.uMassFullFormat.format(mass));
						}
					}
//          Runtime.getRuntime().exec(new String[] { "java", "-jar",
//                                    java.nio.file.Paths.get(ResultsPanel.this.parent.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toFile().getPath(),
//                                    alt, che.getPath() });
					ProcessBuilder pb = new ProcessBuilder("java", "-jar",
							  java.nio.file.Paths.get(ResultsPanel.this.parent.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toFile().getPath(),
							  alt, che.getPath());
					pb.redirectError(java.lang.ProcessBuilder.Redirect.appendTo(new File("errors.txt")));
					pb.redirectOutput(new File("console.txt"));
					pb.start();
				} catch (IOException | URISyntaxException ex) {
					JOptionPane.showMessageDialog(ResultsPanel.this, ex.getLocalizedMessage(), "Unexpected exception", JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
				resultsTable.clearSelection();
				for (int i : selected) {
					resultsTable.addRowSelectionInterval(i, i);
				}
			}
		});
		popupMenu.add(mi);

		HashSet<Character> used = new HashSet();
		used.add('D');
		used.add('M');
		used.add('B');
		used.add('G');
		used.add('H');
		used.add('C');
		used.add('N');
		used.add('A');
		for (JMenuItem item : resultsCard.getAnalyzers(used)) {
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					analyzeResults(item.getActionCommand(), Save.SELECTED_SHOWN_ALL);
				}
			});
			popupMenu.add(item);
		}

		popupMenu.show(resultsTable, x, y);
	}

	public void setLabel(String title) {
		successRate.setText(title);
	}

	public void addRow(Double expM, Double thrM, Double err, String pos, String peptide, String mods, String bonds, Long intensity, String retT, String other) {
		((DefaultTableModel) resultsTable.getModel()).addRow(new Object[]{new Double[]{expM}, new Double[]{thrM}, new Double[]{err},
		new String[]{pos}, new String[]{peptide}, new String[]{mods}, new String[]{bonds},
		new Long[]{intensity}, new String[]{retT}, new String[]{other}
		});
	}

	public void removeDuplications() throws InterruptedException {
		List<? extends RowSorter.SortKey> oldSortKeys = resultsTable.getRowSorter().getSortKeys();
		ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>(resultsTable.getColumnCount());
		for (int i = 0; i < resultsTable.getColumnCount(); i++) {
			sortKeys.add(new RowSorter.SortKey(resultsTable.convertColumnIndexToModel(i), SortOrder.ASCENDING));
		}
		resultsTable.getRowSorter().setSortKeys(sortKeys);
		computingCard.add(1);
		resultsTable.clearSelection();
		for (int i = 1; i < resultsTable.getRowCount(); i++) {
			boolean del = true;
			for (int j = 0; j < resultsTable.getColumnCount(); j++) {
				Object[] curr = (Object[]) resultsTable.getValueAt(i, j);
				Object[] prev = (Object[]) resultsTable.getValueAt(i - 1, j);
				if (curr != prev && (curr == null || prev == null || (curr[0] != prev[0] && (curr[0] == null || !curr[0].equals(prev[0]))))) {
					del = false;
					break;
				}
			}
			if (del) {
				resultsTable.addRowSelectionInterval(i, i);
			}
		}
		computingCard.add(1);
		resultsTable.getRowSorter().setSortKeys(null);
		deleteSelected();
		if (resultsTable.getRowCount() > 0) {
			resultsTable.clearSelection();
//			resultsTable.scrollRectToVisible(resultsTable.getCellRect(0, 0, true));
		}
		resultsTable.getRowSorter().setSortKeys(oldSortKeys);
		computingCard.add(1);
	}

	public void finish() {
		resultsTable.setVisible(true);
	}

	public void logDefaults(Properties defaults) {
		Defaults.putTableDefaults(resultsTable, defaults);
	}

	public void applyDefaults(Properties defaults) {
		Defaults.setTableDefaults(resultsTable, defaults);
	}

	public int rows() {
		return resultsTable.getRowCount();
	}

	public void setRowFilter(RowFilter rowFilter) {
		((TableRowSorter) resultsTable.getRowSorter()).setRowFilter(rowFilter);
	}

	public boolean isFiltered() {
		return ((TableRowSorter) this.resultsTable.getRowSorter()).getRowFilter() != null;
	}

	private void deleteSelected() {
		if (resultsTable.getSelectedRowCount() == 0) {
			return;
		}

		DefaultTableModel model = ((DefaultTableModel) resultsTable.getModel());
		if (resultsTable.getSelectedRowCount() == model.getRowCount()) {
			model.setRowCount(0);
			return;
		}

		Cursor c = getCursor();
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		int last = -1;
		if (resultsTable.getSelectedRowCount() * (long) resultsTable.getSelectedRowCount() < model.getRowCount()) {
			while (resultsTable.getSelectedRowCount() > 0) {
				last = resultsTable.getSelectedRow();
				model.removeRow(resultsTable.convertRowIndexToModel(last));
			}
			if (last >= resultsTable.getRowCount()) {
				last = resultsTable.getRowCount() - 1;
			}
		} else {
			ArrayList<Object[]> table = new ArrayList(model.getRowCount() - resultsTable.getSelectedRowCount());
			for (int i = 0; i < model.getRowCount(); i++) {
				int im = resultsTable.convertRowIndexToView(i);
				if (resultsTable.isRowSelected(im)) {
					last = Math.max(last, im);
				} else {
					Object[] row = new Object[model.getColumnCount()];
					for (int j = 0; j < row.length; j++) {
						row[j] = model.getValueAt(i, j);
					}
					table.add(row);
				}
			}
			last -= resultsTable.getSelectedRowCount();
			model.setRowCount(0);
			List<? extends RowSorter.SortKey> sortKeys = resultsTable.getRowSorter().getSortKeys();
			resultsTable.getRowSorter().setSortKeys(null);
			for (Object[] row : table) {
				model.addRow(row);
			}
			resultsTable.getRowSorter().setSortKeys(sortKeys);
			if (last < resultsTable.getRowCount() - 1) {
				last++;
			}
		}
		if (last >= 0) {
//			resultsTable.scrollRectToVisible(resultsTable.getCellRect(last, 0, true));
			resultsTable.setRowSelectionInterval(last, last);
		}

		setCursor(c);
	}

	public void mergeDuplicationsAll(int i) throws InterruptedException {
		RowFilter rowFilter = ((TableRowSorter) this.resultsTable.getRowSorter()).getRowFilter();
		((TableRowSorter) this.resultsTable.getRowSorter()).setRowFilter(null);

		resultsTable.selectAll();
		mergeDuplications(i);

		((TableRowSorter) this.resultsTable.getRowSorter()).setRowFilter(rowFilter);
	}

	private void mergeDuplications(int skip) throws InterruptedException {
		if (skip < 0 || skip >= resultsTable.getModel().getColumnCount()) {
			computingCard.add(4);
			return;
		}

		Cursor cursor = getCursor();
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		RowFilter rowFilter = ((TableRowSorter) this.resultsTable.getRowSorter()).getRowFilter();
		List<? extends RowSorter.SortKey> oldSortKeys = resultsTable.getRowSorter().getSortKeys();
		try {
			((TableRowSorter) this.resultsTable.getRowSorter()).setRowFilter(null);
			ArrayList<RowSorter.SortKey> sortKeys = new ArrayList();
			if (skip == INDEX_EXP) {
				sortKeys.add(new RowSorter.SortKey(filter ? INDEX_EXP : INDEX_THR, SortOrder.ASCENDING));
			} else {
				for (int i = 0; i < resultsTable.getModel().getColumnCount(); i++) {
					if (!(i == skip || i == INDEX_THR || i == INDEX_ERR) && resultsTable.convertColumnIndexToView(i) >= 0) {
						sortKeys.add(new RowSorter.SortKey(i, SortOrder.ASCENDING));
					}
				}
				sortKeys.add(new RowSorter.SortKey(INDEX_THR, SortOrder.ASCENDING));
				if (filter) {
					sortKeys.add(new RowSorter.SortKey(INDEX_ERR, SortOrder.ASCENDING));
				}
			}
			sortKeys.addAll(oldSortKeys);
			resultsTable.getRowSorter().setSortKeys(sortKeys);
			computingCard.add(1);
			ArrayList<Object[][]> rows = new ArrayList<>(resultsTable.getRowCount());
			for (int i = 0; i < resultsTable.getRowCount(); i++) {
				Object[][] row = new Object[resultsTable.getModel().getColumnCount()][];
				for (int j = 0; j < resultsTable.getModel().getColumnCount(); j++) {
					row[j] = (Object[]) resultsTable.getModel().getValueAt(resultsTable.convertRowIndexToModel(i), j);
				}
				rows.add(row);
			}

			int[] indexes = resultsTable.getSelectedRows();
			Arrays.sort(indexes);

			computingCard.add(1);
			((DefaultTableModel) resultsTable.getModel()).setRowCount(0);
			resultsTable.getRowSorter().setSortKeys(null);

			switch (skip) {
				case INDEX_EXP:
					ArrayList<Object[][]> lines = new ArrayList<>();
					for (int i = indexes.length - 1; i > 0; i--) {
						if (rows.get(indexes[i])[filter ? INDEX_EXP : INDEX_THR][0] == rows.get(indexes[i - 1])[filter ? INDEX_EXP : INDEX_THR][0]
								  || (rows.get(indexes[i])[filter ? INDEX_EXP : INDEX_THR][0] != null && rows.get(indexes[i - 1])[filter ? INDEX_EXP : INDEX_THR][0] != null
								  && Defaults.uMassShortFormat.format(rows.get(indexes[i])[filter ? INDEX_EXP : INDEX_THR][0])
											 .equals(Defaults.uMassShortFormat.format(rows.get(indexes[i - 1])[filter ? INDEX_EXP : INDEX_THR][0])))) {
							lines.add(rows.get(indexes[i]));
							rows.remove(indexes[i]);
						} else if (!lines.isEmpty()) {
							lines.add(rows.get(indexes[i]));
							Object[][] line = merge(lines);
							System.arraycopy(line, 0, rows.get(indexes[i]), 0, line.length);
							lines = new ArrayList<>();
						}
					}
					if (!lines.isEmpty()) {
						lines.add(rows.get(indexes[0]));
						Object[][] line = merge(lines);
						System.arraycopy(line, 0, rows.get(indexes[0]), 0, line.length);
					}
					break;
				default:
					for (int i = indexes.length - 1; i > 0; i--) {
						boolean merge = true;
						for (int j = 0; j < rows.get(indexes[i]).length; j++) {
							if (j == skip) {
								continue;
							}
							if (rows.get(indexes[i])[j] != rows.get(indexes[i - 1])[j]) {
								if (rows.get(indexes[i])[j] == null || rows.get(indexes[i - 1])[j] == null || rows.get(indexes[i])[j].length != rows.get(indexes[i - 1])[j].length) {
									merge = false;
								} else {
									for (int k = 0; k < rows.get(indexes[i])[j].length; k++) {
										if (rows.get(indexes[i])[j][k] == null) {
											if (rows.get(indexes[i - 1])[j][k] == null) {
												continue;
											} else {
												merge = false;
												break;
											}
										} else if (rows.get(indexes[i - 1])[j][k] == null) {
											merge = false;
											break;
										}
										// TODO: Nejspíš zbytečné, jak jsem změnil komparátory - otestovat. Možná celkově blbost, protože pak se ze "stejných" čísel vybere jedno.
										// Na druhou stranu vlastně ten problém se zaokrouhlováním, i když to by možná už s BigDecimal nemusela být pravda...
										switch (j) {
											case INDEX_THR:
												if (!Defaults.uMassShortFormat.format(rows.get(indexes[i])[j][k]).equals(Defaults.uMassShortFormat.format(rows.get(indexes[i - 1])[j][k]))) {
													merge = false;
												}
												break;
											case INDEX_ERR:
												if (!errorFormat.format(rows.get(indexes[i])[j][k]).equals(errorFormat.format(rows.get(indexes[i - 1])[j][k]))) {
													merge = false;
												}
												break;
											default:
												if (!rows.get(indexes[i])[j][k].equals(rows.get(indexes[i - 1])[j][k])) {
													merge = false;
												}
												break;
										}
									}
								}
							}
							if (!merge) {
								break;
							}
						}
						if (merge) {
							if (rows.get(indexes[i - 1])[skip] == null) { // ??? Ale to by asi nemělo nastat, že oba slučované null a všechny; Ačkoliv viz předchozí TODO
								if (rows.get(indexes[i])[skip] == null) {
									rows.get(indexes[i - 1])[skip] = new String[]{"{  } | {  }"};
								} else {
									rows.get(indexes[i - 1])[skip] = rows.get(indexes[i])[skip];
									for (int j = 0; j < rows.get(indexes[i - 1])[skip].length; j++) {
										rows.get(indexes[i - 1])[skip] = clone(rows.get(indexes[i - 1])[skip], j,
												  "{  } | " + (rows.get(indexes[i - 1])[skip][j] == null ? "{  }" : rows.get(indexes[i - 1])[skip][j]));
									}
								}
							} else if (rows.get(indexes[i])[skip] == null) {
								for (int j = 0; j < rows.get(indexes[i - 1])[skip].length; j++) {
									rows.get(indexes[i - 1])[skip] = clone(rows.get(indexes[i - 1])[skip], j,
											  (rows.get(indexes[i - 1])[skip][j] == null ? "{  }" : rows.get(indexes[i - 1])[skip][j]) + " | {  }");
								}
							} else {
								switch (Integer.compare(rows.get(indexes[i - 1])[skip].length, rows.get(indexes[i])[skip].length)) {
									case -1:
										for (int j = 0; j < rows.get(indexes[i - 1])[skip].length; j++) {
											rows.get(indexes[i])[skip] = clone(rows.get(indexes[i])[skip], j,
													  (rows.get(indexes[i - 1])[skip][0] == null ? "{  }" : rows.get(indexes[i - 1])[skip][0]) + " | "
													  + (rows.get(indexes[i])[skip][j] == null ? "{  }" : rows.get(indexes[i])[skip][j]));
										}
										rows.get(indexes[i - 1])[skip] = rows.get(indexes[i])[skip];
										break;
									case 0:
										for (int j = 0; j < rows.get(indexes[i - 1])[skip].length; j++) {
											rows.get(indexes[i - 1])[skip] = clone(rows.get(indexes[i - 1])[skip], j,
													  (rows.get(indexes[i - 1])[skip][j] == null ? "{  }" : rows.get(indexes[i - 1])[skip][j]) + " | "
													  + (rows.get(indexes[i])[skip][j] == null ? "{  }" : rows.get(indexes[i])[skip][j]));
										}
										break;
									case 1:
										for (int j = 0; j < rows.get(indexes[i - 1])[skip].length; j++) {
											rows.get(indexes[i - 1])[skip] = clone(rows.get(indexes[i - 1])[skip], j,
													  (rows.get(indexes[i - 1])[skip][j] == null ? "{  }" : rows.get(indexes[i - 1])[skip][j]) + " | "
													  + (rows.get(indexes[i])[skip][0] == null ? "{  }" : rows.get(indexes[i])[skip][0]));
										}
										break;
								}
							}
							rows.remove(indexes[i]);
						}
					}
					break;
			}

			for (int i = 0; i < rows.size(); i++) {
				((DefaultTableModel) resultsTable.getModel()).addRow(rows.get(i));
			}
			computingCard.add(2);
		} catch (OutOfMemoryError e) {
			System.out.println("Isn't enough memory for merging entries.");
			computingCard.append("  isn't enough memory for merging entries...");
		} finally {
			((TableRowSorter) this.resultsTable.getRowSorter()).setRowFilter(rowFilter);
			resultsTable.getRowSorter().setSortKeys(oldSortKeys);
		}

		setCursor(cursor);
	}

	public int getNoofTableRows() {
		return resultsTable.getModel().getRowCount();
	}

	String[] getProteins() {
		String[] retVal = new String[proteins.size()];
		int i = 0;
		for (Protein protein : proteins.values()) {
			retVal[i++] = protein.getName();
		}
		return retVal;
	}

	public Object[] getTableRowData(int row) {
		Object[] retVal = new Object[resultsTable.getModel().getColumnCount()];
		for (int i = 0; i < retVal.length; i++) {
			Object[] foo = (Object[]) resultsTable.getValueAt(row, i);
			retVal[i] = foo[0];
		}
		return retVal;
	}

	public MeasurementCard.Tolerance getTolerance() {
		return resultsCard.getTolerance();
	}

	private String[] clone(Object[] original, int i, String update) {
		String[] ret = new String[original.length];
		for (int j = 0; j < ret.length; j++) {
			ret[j] = original[j].toString();
		}
		ret[i] = update;
		return ret;
	}

	private Object[][] merge(ArrayList<Object[][]> lines) {
		int count = 0;
		int[] counts = new int[lines.size()];
		for (int j = 0; j < lines.size(); j++) {
			int c = 1;
			for (Object[] cell : lines.get(j)) {
				if (cell != null) {
					c = Math.max(c, cell.length);
				}
			}
			count += c;
			counts[j] = c;
		}

		Object[][] merged = new Object[][]{new Double[count], new Double[count], new Double[count], new String[count], new String[count],
			new String[count], new String[count], new Long[count], new String[count], new String[count]};
		int zero = 0;
		for (int j = lines.size(); j-- > 0;) {
			Object[][] line = lines.get(j);
			for (int k = 0; k < line.length; k++) {
				if (line[k] == null) {
					for (int l = zero; l < zero + counts[j]; l++) {
						merged[k][l] = null;
					}
				} else {
					if (line[k].length != counts[j]) {
						for (int l = zero; l < zero + counts[j]; l++) {
							merged[k][l] = line[k][0];
						}
					} else {
						System.arraycopy(line[k], 0, merged[k], zero, counts[j]);
					}
				}
			}
			zero += counts[j];
		}

		for (int j = 0; j < merged.length; j++) {
			boolean same = true;
			for (int k = 1; k < merged[j].length; k++) {
				if (!(merged[j][k - 1] == merged[j][k] || (merged[j][k] != null && merged[j][k].equals(merged[j][k - 1])))) {
					same = false;
					break;
				}
			}
			if (same) {
				merged[j] = Arrays.copyOf(merged[j], 1);
			}
		}
		return merged;
	}

	public void splitDuplicationsAll(int i) {
		RowFilter rowFilter = ((TableRowSorter) this.resultsTable.getRowSorter()).getRowFilter();
		((TableRowSorter) this.resultsTable.getRowSorter()).setRowFilter(null);

		resultsTable.selectAll();
		splitDuplications(i);

		((TableRowSorter) this.resultsTable.getRowSorter()).setRowFilter(rowFilter);
	}

	private void splitDuplications(int skip) {
		Cursor cursor = getCursor();
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		RowFilter rowFilter = ((TableRowSorter) this.resultsTable.getRowSorter()).getRowFilter();
		List<? extends RowSorter.SortKey> sortKeys = resultsTable.getRowSorter().getSortKeys();
		try {
			((TableRowSorter) this.resultsTable.getRowSorter()).setRowFilter(null);
			ArrayList<Object[][]> rows = new ArrayList<>(resultsTable.getRowCount());
			for (int i = 0; i < resultsTable.getRowCount(); i++) {
				Object[][] row = new Object[resultsTable.getModel().getColumnCount()][];
				for (int j = 0; j < resultsTable.getColumnCount(); j++) {
					row[resultsTable.convertColumnIndexToModel(j)] = (Object[]) resultsTable.getValueAt(i, j);
				}
				rows.add(row);
			}

			int[] indexes = resultsTable.getSelectedRows();
			Arrays.sort(indexes);

			((DefaultTableModel) resultsTable.getModel()).setRowCount(0);
			resultsTable.getRowSorter().setSortKeys(null);

			switch (skip) {
				case INDEX_EXP:
					for (int i = indexes.length - 1; i >= 0; i--) {
						int count = 1;
						for (Object[] cell : rows.get(indexes[i])) {
							if (cell != null) {
								count = Math.max(count, cell.length);
							}
						}
						if (count > 1) {
							ArrayList<Object[][]> add = new ArrayList<>(count);
							for (int j = 0; j < count; j++) {
								add.add(new Object[rows.get(indexes[i]).length][]);
							}
							for (int j = 0; j < rows.get(indexes[i]).length; j++) {
								if (rows.get(indexes[i])[j] == null) {
									for (Object[][] line : add) {
										line[j] = new Object[1];
									}
								} else if (rows.get(indexes[i])[j].length == count) {
									for (int k = 0; k < rows.get(indexes[i])[j].length; k++) {
										add.get(k)[j] = Arrays.copyOfRange(rows.get(indexes[i])[j], k, k + 1);
									}
								} else {
									for (Object[][] line : add) {
										line[j] = Arrays.copyOf(rows.get(indexes[i])[j], 1);
									}
								}
							}

							rows.remove(indexes[i]);
							rows.addAll(indexes[i], add);
						}
					}
					break;
				default:
					for (int i = indexes.length - 1; i >= 0; i--) {
						if (rows.get(indexes[i])[skip] != null && rows.get(indexes[i])[skip].length == 1
								  && rows.get(indexes[i])[skip][0] != null && rows.get(indexes[i])[skip][0].toString().split(" \\| ", -1).length > 1) {
							String[] splitted = rows.get(indexes[i])[skip][0].toString().split(" \\| ", -1);
							ArrayList<Object[][]> add = new ArrayList<>(splitted.length);
							for (int j = 0; j < splitted.length; j++) {
								add.add(new Object[rows.get(indexes[i]).length][]);
							}
							for (int j = 0; j < rows.get(indexes[i]).length; j++) {
								if (j == skip) {
									for (int k = 0; k < splitted.length; k++) {
										add.get(k)[j] = new String[]{splitted[k]};
									}
								} else {
									for (int k = 0; k < splitted.length; k++) {
										add.get(k)[j] = rows.get(indexes[i])[j];
									}
								}
							}

							rows.remove(indexes[i]);
							rows.addAll(indexes[i], add);
						}
					}
			}

			for (int i = 0; i < rows.size(); i++) {
				((DefaultTableModel) resultsTable.getModel()).addRow(rows.get(i));
			}
		} catch (OutOfMemoryError e) {
			JOptionPane.showMessageDialog(this, "Isn't enough memory for ungroupping of items.", "Out of Memory", JOptionPane.ERROR_MESSAGE);
		} finally {
			((TableRowSorter) this.resultsTable.getRowSorter()).setRowFilter(rowFilter);
			resultsTable.getRowSorter().setSortKeys(sortKeys);
		}

		setCursor(cursor);
	}

	public void analyzeResults(String action, Save save) {
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					Process process = Runtime.getRuntime().exec(action);
					try (OutputStream outputStream = process.getOutputStream()) {
						PrintWriter printWriter = new PrintWriter(outputStream);
						parent.saveSetting(printWriter, measurementName);
						saveResults(printWriter, save);
						printWriter.flush();
						outputStream.flush();
					}
				} catch (IOException e) {
					JOptionPane.showMessageDialog(parent, e.getLocalizedMessage(), "Unexpected exception", JOptionPane.ERROR_MESSAGE);
				}
			}
		};
		thread.start();
	}

	public boolean saveResults(boolean useDefault, boolean confirm) {
		return saveResults(DIR + File.separator + defaultName + ".sen", useDefault, confirm, Save.SHOWN, null);
	}

	private boolean saveResults(String fileName, boolean silent, boolean confirm, Save save, String[] settings) {
		try (java.io.PrintWriter pw = silent ? parent.saveSetting(fileName, false, measurementName) : parent.saveFile(fileName, confirm, measurementName)) {
			if (pw == null) {
				return false;
			}

			if (settings != null && settings.length > 0) {
				pw.println();
				for (String set : settings) {
					pw.println(set);
				}
			}

			saveResults(pw, save);
		}
		return true;
	}

	private void saveResults(PrintWriter pw, Save selected) {
		pw.println();
		pw.println("--------------------------------------------------------------------------------");

		switch (selected) {
			case SELECTED:
				saveHeader(pw);
				saveSelected(pw);
				break;
			case SHOWN:
				for (int j = 0; j < resultsTable.getColumnCount(); j++) {
					if (j != 0) {
						pw.print('\t');
					}
					pw.print(resultsTable.getColumnName(j));
				}
				for (int i = 0; i < resultsTable.getRowCount(); i++) {
					pw.println();
					for (int j = 0; j < resultsTable.getColumnCount(); j++) {
						if (j != 0) {
							pw.print('\t');
						}
						switch (resultsTable.convertColumnIndexToModel(j)) {
							case INDEX_EXP:
							case INDEX_THR:
								pw.print(formatMass((Object[]) resultsTable.getValueAt(i, j), false));
								break;
							case INDEX_ERR:
								pw.print(formatError((Object[]) resultsTable.getValueAt(i, j)));
								break;
							case INDEX_INTENSITY:
								pw.print(formatIntensity((Object[]) resultsTable.getValueAt(i, j)));
								break;
							default:
								pw.print(formatString((Object[]) resultsTable.getValueAt(i, j)));
						}
					}
				}
				break;
			case SHOWN_ALL:
			case SELECTED_SHOWN_ALL:
				saveHeader(pw);
				if (selected == Save.SELECTED_SHOWN_ALL) {
					pw.println("----Selected--------------------------------------------------------------------");
					saveSelected(pw);
				}
				pw.println("----Filtered--------------------------------------------------------------------");
				for (int i = 0; i < resultsTable.getRowCount(); i++) {
					boolean first = true;
					for (int j = 0; j < resultsTable.getModel().getColumnCount(); j++) {
						if (resultsTable.convertColumnIndexToView(j) < 0) {
							continue;
						}
						if (first) {
							first = false;
						} else {
							pw.print('\t');
						}
						switch (j) {
							case INDEX_EXP:
							case INDEX_THR:
								pw.print(formatMass((Object[]) resultsTable.getModel().getValueAt(resultsTable.convertRowIndexToModel(i), j), true));
								break;
							case INDEX_ERR:
								pw.print(formatError((Object[]) resultsTable.getModel().getValueAt(resultsTable.convertRowIndexToModel(i), j)));
								break;
							case INDEX_INTENSITY:
								pw.print(formatIntensity((Object[]) resultsTable.getModel().getValueAt(resultsTable.convertRowIndexToModel(i), j)));
								break;
							default:
								pw.print(formatString((Object[]) resultsTable.getModel().getValueAt(resultsTable.convertRowIndexToModel(i), j)));
						}
					}
					pw.println();
				}
				pw.println("----All-------------------------------------------------------------------------");
				for (int i = 0; i < resultsTable.getModel().getRowCount(); i++) {
					boolean first = true;
					for (int j = 0; j < resultsTable.getModel().getColumnCount(); j++) {
						if (resultsTable.convertColumnIndexToView(j) < 0) {
							continue;
						}
						if (first) {
							first = false;
						} else {
							pw.print('\t');
						}
						switch (j) {
							case INDEX_EXP:
							case INDEX_THR:
								pw.print(formatMass((Object[]) resultsTable.getModel().getValueAt(i, j), true));
								break;
							case INDEX_ERR:
								pw.print(formatError((Object[]) resultsTable.getModel().getValueAt(i, j)));
								break;
							case INDEX_INTENSITY:
								pw.print(formatIntensity((Object[]) resultsTable.getModel().getValueAt(i, j)));
								break;
							default:
								pw.print(formatString((Object[]) resultsTable.getModel().getValueAt(i, j)));
						}
					}
					pw.println();
				}
				break;
			default:
				throw new UnsupportedOperationException("Unexpected save option");
		}
	}

	private void saveHeader(PrintWriter pw) {
		boolean first = true;
		for (int j = 0; j < resultsTable.getModel().getColumnCount(); j++) {
			if (resultsTable.convertColumnIndexToView(j) < 0) {
				continue;
			}
			if (first) {
				first = false;
			} else {
				pw.print('\t');
			}
			pw.print(resultsTable.getModel().getColumnName(j));
		}
		pw.println();
	}

	private void saveSelected(PrintWriter pw) {
		for (int i : resultsTable.getSelectedRows()) {
			boolean first = true;
			for (int j = 0; j < resultsTable.getModel().getColumnCount(); j++) {
				if (resultsTable.convertColumnIndexToView(j) < 0) {
					continue;
				}
				if (first) {
					first = false;
				} else {
					pw.print('\t');
				}
				switch (j) {
					case INDEX_EXP:
					case INDEX_THR:
						pw.print(formatMass((Object[]) resultsTable.getModel().getValueAt(resultsTable.convertRowIndexToModel(i), j), true));
						break;
					case INDEX_ERR:
						pw.print(formatError((Object[]) resultsTable.getModel().getValueAt(resultsTable.convertRowIndexToModel(i), j)));
						break;
					case INDEX_INTENSITY:
						pw.print(formatIntensity((Object[]) resultsTable.getModel().getValueAt(resultsTable.convertRowIndexToModel(i), j)));
						break;
					default:
						pw.print(formatString((Object[]) resultsTable.getModel().getValueAt(resultsTable.convertRowIndexToModel(i), j)));
				}
			}
			pw.println();
		}
	}

	public String formatMass(Object[] value, boolean full) {
		if (value == null) {
			return "";
		}
		switch (value.length) {
			case 0:
				return "";
			case 1:
				return value[0] == null ? "" : (full ? Defaults.uMassFullFormat : Defaults.uMassShortFormat).format(value[0]);
			default:
				String[] formatted = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					formatted[i] = value[i] == null ? "" : (full ? Defaults.uMassFullFormat : Defaults.uMassShortFormat).format(value[i]);
				}
				boolean same = true;
				StringBuilder buf = new StringBuilder("{ ").append(formatted[0]).append(" }");
				for (int i = 1; i < formatted.length; i++) {
					if (!formatted[i - 1].equals(formatted[i])) {
						same = false;
					}
					buf.append(" | { ").append(formatted[i]).append(" }");
				}
				return same ? formatted[0] : buf.toString();
		}
	}

	public String formatError(Object[] value) {
		if (value == null) {
			return "";
		}
		switch (value.length) {
			case 0:
				return "";
			case 1:
				return value[0] == null ? "" : errorFormat.format(value[0]);
			default:
				String[] formatted = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					formatted[i] = value[i] == null ? "" : errorFormat.format(value[i]);
				}
				boolean same = true;
				StringBuilder buf = new StringBuilder("{ ").append(formatted[0]).append(" }");
				for (int i = 1; i < formatted.length; i++) {
					if (!formatted[i - 1].equals(formatted[i])) {
						same = false;
					}
					buf.append(" | { ").append(formatted[i]).append(" }");
				}
				return same ? formatted[0] : buf.toString();
		}
	}

	public String formatIntensity(Object[] value) {
		if (value == null) {
			return "";
		}
		if (value.length == 1) {
			return value[0] == null ? "" : Defaults.intensityFormat.format(value[0]);
		} else {
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < value.length; i++) {
				buf.append(" | { ").append(value[i] == null ? "" : Defaults.intensityFormat.format(value[i])).append(" }");
			}
			return buf.substring(3);
		}
	}

	public String formatString(Object[] value) {
		if (value == null) {
			return "";
		}
		if (value.length == 1) {
			return value[0] == null ? "" : value[0].toString();
		} else {
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < value.length; i++) {
				buf.append(" | { ").append(value[i] == null ? "" : value[i].toString()).append(" }");
			}
			return buf.substring(3);
		}
	}

	protected class StringCR extends DefaultTableCellRenderer.UIResource {

		@Override
		public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setBorder(BorderFactory.createCompoundBorder(getBorder(), BorderFactory.createEmptyBorder(0, 2, 0, 5)));
			return this;
		}

		@Override
		public void setValue(Object value) {
			setText(formatString((Object[]) value));
		}
	}

	protected class NumberCR extends DefaultTableCellRenderer.UIResource {

		public NumberCR() {
			super();
			setHorizontalAlignment(JLabel.RIGHT);
		}

		@Override
		public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setBorder(BorderFactory.createCompoundBorder(getBorder(), BorderFactory.createEmptyBorder(0, 2, 0, 5)));
			return this;
		}

		@Override
		public void setValue(Object value) {
			setText(formatString((Object[]) value));
		}
	}

	protected class MassCR extends NumberCR {

		@Override
		public void setValue(Object value) {
			setText(formatMass((Object[]) value, false));
		}
	}

	protected class ErrorCR extends NumberCR {

		@Override
		public void setValue(Object value) {
			setText(formatError((Object[]) value));
		}
	}

	protected class IntensityCR extends NumberCR {

		@Override
		public void setValue(Object value) {
			setText(formatIntensity((Object[]) value));
		}
	}

	protected class ArrayComparator implements Comparator<Comparable[]> {

		Comparator comparator;

		public ArrayComparator(Comparator comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Comparable[] o1, Comparable[] o2) {
			if (o1 == null || o2 == null) {
				if (o2 != null) {
					return -1;
				} else if (o1 == null) {
					return 0;
				} else {
					return 1;
				}
			}

			int to = Math.min(o1.length, o2.length);
			for (int i = 0; i < to; i++) {
				if (o1[i] == null || o2[i] == null) {
					if (o2[i] != null) {
						return -1;
					} else if (o1[i] == null) {
						continue;
					} else {
						return 1;
					}
				}
				int res = comparator.compare(o1[i], o2[i]);
				if (res != 0) {
					return res;
				}
			}
			return Integer.compare(o1.length, o2.length);
		}
	}

	protected class ArrayRowSorter extends TableRowSorter<TableModel> {

		Comparator<Comparable[]> defaultComparator;
		Comparator[] comparators;

		public ArrayRowSorter(TableModel tableModel) {
			super(tableModel);
			defaultComparator = new ArrayComparator(new Comparator<Comparable>() {
				@Override
				public int compare(Comparable o1, Comparable o2) {
					return o1.compareTo(o2);
				}
			});
			comparators = new Comparator[tableModel.getColumnCount()];
			for (int i = 0; i < comparators.length; i++) {
				comparators[i] = defaultComparator;
			}
		}

		@Override
		protected boolean useToString(int column) {
			return false;
		}

		@Override
		public void setComparator(int column, Comparator<?> comparator) {
			super.setComparator(column, comparator);
			comparators[column] = comparator;
		}

		@Override
		public Comparator<?> getComparator(int column) {
			return comparators[column];
		}
	}
}
